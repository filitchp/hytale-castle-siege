# CLAUDE.md

Guidance for working on **CastleSiege**, a Hytale server-side co-op tower-defense minigame mod.

---

## Project basics

- **Language / build:** Java 25, Gradle with the `dev.scaffoldit` plugin (`settings.gradle.kts`). `./gradlew build` packages the jar and launches `:runServer` (long-running). Use `./gradlew compileJava` for a quick compile check.
- **Server API:** `com.hypixel.hytale:Server:0.6.8`. The version isn't pinned: `settings.gradle.kts` uses `usePatchline("release")` + `useVersion("latest")`, so a new Hytale release can break the build without any code change. The API is early access, and some documented APIs don't work (see "What doesn't work" below).
- **Plugin entry:** `dev.dooondi.CastleSiege` registers commands, events, ECS systems, interaction codecs, and loads persisted state.
- **Manifest:** `"IncludesAssetPack": true` in `src/main/resources/manifest.json` is mandatory for custom UI / item / NPC JSON to load. If assets stop loading, check the built jar with `unzip -p build/libs/dev.dooondi.castlesiege.jar manifest.json`. Keep `ServerVersion` in step with the resolved server version when bumping Hytale. (It was kept `false` in source on older builds, apparently to work around a bug there; `true` works on 0.6.8.)
- **Release scripts** (`scripts/`): `bump_version.py [major|minor|patch|X.Y.Z]` updates the manifest `Version` and the dev world's `DisplayName`. It is a dry run unless you pass `--no-dry-run`. `release.py` builds and packages the world + jar into `release/v<version>/`.
- **World data** (`devserver/`): the map, prefab paths (stored in `universe/worlds/default/chunks/*.region.bin`), world/server config and `permissions.json` are tracked. `auth.enc`, `logs/`, `telemetry/`, `universe/players/`, `mods/` (runtime state), `bans.json` and `*.bak` are gitignored and must stay that way. Any server run rewrites the region files (~20 MB of binaries), so only commit `devserver/` changes made on purpose, with the server stopped. Discard play-session changes with `git restore devserver/`. Never `git add` `devserver_*` backups.

### Key source layout

```
src/main/java/dev/dooondi/
  CastleSiege.java   — plugin main, wires everything up in setup()
  commands/          — CsCommand (/cs <action>), PrefabPathCommand (/prefabpath <action>)
  events/            — WelcomeEvent (PlayerReadyEvent), InputListener (PlayerChatEvent filter)
  systems/           — ECS EntityEventSystems: BlockBreakEventSystem, BlockPlaceEventSystem
  shop/              — ShopCatalog (items + prices), ShopUI (shop page), OpenShopAction/OpenShopActionBuilder (custom NPC action)
  ui/                — WaveHUD (CustomUIHud), WaveUI (InteractiveCustomUIPage)
  wave/              — WaveManager, WaveRewards, TeamBank, MobDeathTracker, OpenWaveUIInteraction, TriggerWaveInteraction

src/main/resources/
  manifest.json
  Common/UI/Custom/             — WaveHUD.ui, WaveUI.ui
  Server/Item/Items/            — CastleSiege_WaveHammer.json
  Server/Item/Interactions/     — OpenWaveUI.json, TriggerWave.json
  Server/Item/RootInteractions/ — Root_OpenWaveUI.json, Root_TriggerWave.json
  Server/NPC/Roles/Wave/        — wave mob role variants, all suffixed `_CS` (e.g. Rat_CS, Skeleton_Burnt_Praetorian_CS)
  Server/NPC/Roles/Shop/        — CastleSiege_Merchant (shopkeeper NPC)
  Server/Languages/en-US/       — castlesiege.lang (keys are prefixed `castlesiege.` from the file name)

devserver/     — dev server + Castle Siege world, tracked in git (no LFS for now)
temp_assets/   — vanilla Hytale assets for reference (gitignored, read-only, do not edit)
```

### Inspecting the server jar

The server API is not fully documented. When unsure about a class/method:

```
jar xf ~/.gradle/caches/modules-2/files-2.1/com.hypixel.hytale/Server/0.6.8/5946c2e66c3564a6e6a5454a94da6482a10a3566/Server-0.6.8.jar <class path>
javap -p <class file>
```

Run these as standalone bash commands (not chained through `&&`), because `jar` and `javap` are pre-approved and standalone calls won't prompt. Older jars (e.g. `0.6.2`) may also be in the Gradle cache. Always inspect the version the build actually resolves.

---

## Architecture cheatsheet

### Registering things in `setup()`

- **Commands:** `getCommandRegistry().registerCommand(new ...)`
- **Global events:** `getEventRegistry().registerGlobal(EventClass, Handler::method)`
- **ECS systems:** `getEntityStoreRegistry().registerSystem(new ...)`
- **Interaction codecs:** `getCodecRegistry(Interaction.CODEC).register("TypeName", Class, CODEC)`
- **Permissions:** `/cs` uses `setPermissionGroups("CastleSiege")`. `setup()` grants the command's permission to the `CastleSiege` group, and `WelcomeEvent` adds every joining player to that group via `PermissionsModule.get().addUserToGroup(...)`.

### Three ways to react to player actions

1. **Global events** (`getEventRegistry().registerGlobal(...)`) work for `PlayerReadyEvent` and `PlayerChatEvent`. They **do not work** for `PlayerInteractEvent` or `PlayerMouseButtonEvent` (see below).
2. **ECS EntityEventSystems:** extend `EntityEventSystem<EntityStore, EventClass>`, then override `handle(index, chunk, store, buffer, event)` and `getQuery()`. Good for `BreakBlockEvent`, `PlaceBlockEvent`, `UseBlockEvent`, `DropItemEvent` etc.
3. **Interactions:** extend `SimpleInstantInteraction` and wire it into an item via JSON (`Interactions/X.json`, `RootInteractions/Root_X.json`, item's `Interactions: { Primary: "Root_X" }`). This is the only reliable way to handle item clicks.
4. **Custom NPC actions** (right-clicking an NPC): extend `BuilderActionBase` (call `requireInstructionType(EnumSet.of(InstructionType.Interaction))` in `readConfig`) and `ActionBase`, then register in `setup()` with `NPCPlugin.get().registerCoreComponentType("TypeName", Builder::new)`. The role's `InteractionInstruction` calls it with `{ "Type": "TypeName" }` after a `HasInteracted` sensor. The interacting player is `support.getStateSupport().getInteractionIterationTarget()`, and opening a page directly from `execute` is safe. `OpenShopAction` is the canonical example (mirrors vanilla `OpenBarterShop`).

### Commands

`CsCommand` extends `AbstractTargetPlayerCommand`. It takes one required `action` string, plus an optional `--wave <n>` and a `--confirm` flag. To add a feature, prefer a new action in `CsCommand` over a new top-level command. Store-mutating work in a command runs on the world thread via `CompletableFuture.runAsync(() -> ..., world)`.

### UI

- **HUD** (`CustomUIHud`): non-blocking overlay; the player keeps control. HUDs are **keyed**: `super(playerRef, KEY)`, then `hudManager.addCustomHud(playerRef, hud)`, `getCustomHud(KEY)`, `removeCustomHud(playerRef, KEY)`. Example: `ui/WaveHUD.java`. Push runtime changes via `update(false, builder)`.
- **Page** (`InteractiveCustomUIPage<DataClass>`): modal, freezes movement, unlocks the mouse. Example: `ui/WaveUI.java`. Push runtime changes via `sendUpdate(builder)`.
- `.ui` files live under `Common/UI/Custom/`. Reference `$Common = "Common.ui";` to use the vanilla button/label styles.
- Pages can bind events on elements: `uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Btn")`. Button clicks route to `handleDataEvent` on the page.
- `UICommandBuilder.set(selector, value)` updates elements at runtime: `#Label.TextSpans` → `Message.raw(...)`, and `#Container.Visible` → `boolean`. To swap buttons, toggle the visibility of their containers (`#StartWaveBtnContainer` / `#ResetBtnContainer` in `WaveUI`).

### Opening a page from an interaction

Store writes are forbidden while the store is ticking. In `SimpleInstantInteraction.firstRun`, defer via:

```java
context.getCommandBuffer().run(store ->
    player.getPageManager().openCustomPage(context.getEntity(), store, new MyPage(playerRef))
);
```

`OpenWaveUIInteraction` is the canonical example.

### Iterating all players in a store

```java
store.forEachChunk(Player.getComponentType(), (chunk, buffer) -> {
    for (int i = 0; i < chunk.size(); i++) {
        Ref<EntityStore> ref = chunk.getReferenceTo(i);
        Player player = store.getComponent(ref, Player.getComponentType());
        // ...
    }
});
```

`Store<EntityStore>` implements `ComponentAccessor<EntityStore>`, so pass it straight into APIs like `player.giveItem(stack, ref, store)`.

### Getting the `World`

From a store: `store.getExternalData().getWorld()` (`EntityStore` is a `WorldProvider`). From a player or NPC: `player.getWorld()` / `npcEntity.getWorld()`. Several 0.6.x APIs take a `World` where older builds took a `Store` (see "API changes between Hytale versions").

### Tracking NPC deaths and kill attribution

Extend `DeathSystems.OnDeathSystem`, override `onComponentAdded` (NOT `onComponentSet`) and `getQuery()` returning `Query.any()`. Get the killer via:

```java
Damage deathInfo = deathComponent.getDeathInfo();
if (deathInfo.getSource() instanceof Damage.EntitySource src) {
    Ref<EntityStore> killerRef = src.getRef();
    PlayerRef killer = store.getComponent(killerRef, PlayerRef.getComponentType());
    UUID uuid = killer.getUuid();
}
```

`MobDeathTracker` is the canonical example. It also records player deaths (`WaveManager.recordPlayerDeath`).

### Persistence

State persists as plain text files in the plugin data directory (`getDataDirectory()`), loaded in `setup()`:

- `wave_progress.txt`: last defeated wave (`WaveManager.initPersistence` / `saveProgress`).
- `seen_players.txt`: UUIDs of players who have joined before, appended one per line (`WelcomeEvent.initPersistence`).
- `team_money.txt`: shared team balance (`TeamBank.initPersistence`).

### Item IDs (use the correct spelling)

- `Weapon_Axe_Crude`
- `Ore_Copper`
- `Tool_Hammer_Iron` (vanilla) is mirrored by `CastleSiege_WaveHammer` (custom; opens the Wave UI).

---

## API changes between Hytale versions

- **0.6.8:** `EventTitleUtil.showEventTitleToWorld(..., World)`: the last parameter is now `World`, not `Store`/`ComponentAccessor`. Pass `store.getExternalData().getWorld()`.
- **0.5.6:** `Vector3d` is `org.joml.Vector3d`, and rotations use `com.hypixel.hytale.math.vector.Rotation3f`.

When the build breaks after a version bump, `javap` the class in the newly resolved jar before changing code.

---

## What doesn't work in this Hytale build

These are real gotchas found by trial and error. **Check here before assuming a documented API will work.**

### Broken event listeners

- **`PlayerInteractEvent`**: the global listener never fires. Use an `Interaction` attached to the item instead.
- **`PlayerMouseButtonEvent`**: the global listener never fires. Same workaround: use an `Interaction`.

### Plugin setup ordering traps

- **`PlayerRef.getComponentType()` returns `null` during `setup()`** (lazy init). Using it in an EntityEventSystem's `getQuery()` throws `NullPointerException: query is null` at `ComponentRegistry.registerSystem`. **Fix:** return `Query.any()` and filter components inside `handle()`.
- **`OnDeathSystem` subclasses need both `onComponentAdded` AND `getQuery()`.** The parent class only supplies default `onComponentSet`/`onComponentRemoved` stubs. `RefChangeSystem` implements `QuerySystem`, so `getQuery()` is abstract. Return `Query.any()` (the framework already filters for `DeathComponent`).

### Store mutation from interaction context

Calling store-mutating APIs (`spawnNPC`, `openCustomPage`, etc.) directly inside `SimpleInstantInteraction.firstRun` throws `IllegalStateException: Store is currently processing!`. **Fix:** wrap the work in `context.getCommandBuffer().run(store -> ...)`, which runs between ticks.

### UICommandBuilder limitations

- **You cannot `set` `Text` or `Disabled` on a `$Common.@TextButton` at runtime.** Both are macro-time parameters. Attempting it raises `CustomUI set command couldn't set a new value. Selector: #StartWaveBtn`. **Fix:** put dynamic text in a sibling `Label` (e.g. `#StatusLabel`) and toggle the button's container `.Visible`. Still gate the action server-side (`WaveManager.spawnNextWave`).
- **`MouseButtonEvent.state` is a public field**, not `getState()`.

### Wave NPC behaviour pitfalls

- **The default `RunThreshold` (0.3) is higher than the patrol path `RelSpeed` (0.18–0.25)**, so mobs always walk. Set `RunThreshold: 0.1` in each wave role JSON, and raise `MaxSpeed` as needed.
- **Wave roles inherit default weapons from `Template_Intelligent`** and get swords equipped. **Fix:** set `"Weapons"` / `"OffHand"` explicitly (empty `[]` for creatures), plus `_InteractionVars` overrides defining the creature's natural attacks (e.g. Rat_Bite). The error if you miss this: `Missing replacement interactions for interaction: *Root_NPC_<X>_Attack_Interactions_0`.
- **Always double-check that the attack reference matches the creature family.** Snake_Rattle once had `Root_NPC_Rat_Attack` copy-pasted from the rat config.
- **New roles need a `MOB_STATS` entry** in `WaveManager` or `/cs debugmobs` warns `missing MOB_STATS entries`.

### Lever interactive block was buggy

A custom lever block with a `Use` interaction triggered unreliably in early access. It was replaced by the `CastleSiege_WaveHammer` item, whose `Primary` and `Secondary` both route to `Root_OpenWaveUI`, and the lever files have since been removed. `TriggerWaveInteraction` / `Root_TriggerWave` are still registered but no item uses them.

### Thread-safety

Death tracking runs on the ECS tick thread; UI updates run on button clicks; position tracking runs on `WAVE_SCHEDULER`. Shared counters (`currentWave`, `totalKills`, `currentWaveKills`, etc.) must be `AtomicInteger`/`AtomicBoolean`. Shared sets must be `ConcurrentHashMap.newKeySet()`. Per-player maps use `ConcurrentHashMap<UUID, AtomicInteger>`.

### NPC spawning must happen on the game thread

`NPCPlugin.get().spawnNPC()` mutates the store. Running it inside a `ScheduledExecutorService` lambda (off the game thread) fails silently: mobs never appear. **Fix:** call `spawnNPC()` from the command/tick handler, or hop back with `world.execute(() -> ...)` from a scheduled task (as `scheduleBossSpawn` does). Only defer lightweight, non-mutating calls like `npcEntity.getPathManager().setPrefabPath()` directly to the scheduler.

### Prefab path gotchas

- **`npcEntity.getPathManager().setPrefabPath(uuid, pathRef)` needs ~500ms after spawn.** Calling it immediately after `spawnNPC()` is unreliable because the entity's components haven't finished initializing. Defer via `WAVE_SCHEDULER.schedule(..., 500, TimeUnit.MILLISECONDS)`.
- **`PrefabPathHelper.addMarker()`** adds a waypoint to a prefab path. Use `(short) -1` as the index to append at the end. Parameters: `(store, playerEntityRef, pathUuid, pathName, pauseTime, observationAngle, insertIndex, worldGenId)`.
- **`pathData.removePrefabPathWaypoint(worldGenId, pathUuid, order)`** deletes a specific waypoint by its order index.
- **`pathData.removePrefabPath(worldGenId, pathUuid)`** deletes an entire prefab path.
- **`IPrefabPathWaypoint.getWaypointPosition(store)`** returns absolute `Vector3d` coordinates.
- **`ParticleUtil.spawnParticleEffect(particleName, position, store)`** spawns particles, which helps visualize waypoint locations. Use `"Totem_Heal_Extra"`, not `"Totem_Heal_Simple_Test"` (that is a spawner group definition, not a particle effect name).

---

## Current feature set

- **20-wave tower defense** (`WaveManager`): the table-driven `WAVE_TABLE` lists `MobEntry(role, count)` per wave, spawned in a grid formation from `SPAWN_ORIGIN`. Mobs follow prefab paths looked up from `WorldPathData` by hard-coded UUIDs: charge paths (close/med/far, picked by spawn Z), courtyard / front-lower / front-upper loop paths, and left/right flank paths for archers. A `WAVE_SCHEDULER` position-tracking task reassigns mobs to loop/flank paths once they cross Z thresholds.
- **Boss wave:** `Skeleton_Burnt_Praetorian_CS` is summoned into the courtyard after a 4s delay with scaled particles (`scheduleBossSpawn`). End-of-wave logic waits while `pendingBoss` is set.
- **Wave start/end:** start shows a world title and plays a sound, then grants `WaveRewards.awardWaveStart`. End grants `awardWaveEnd`, heals all players to full, shows a "Wave N Complete" title, saves progress, and plays a victory sound after the final wave.
- **Wave UI page** (`WaveUI`): opened by clicking the `CastleSiege_WaveHammer` or with `/cs ui`. It shows the wave number, mobs remaining, your kills and deaths, total mobs killed, and a status line. "Start Next Wave" (server-gated: it won't advance while a wave is in progress) becomes a "Reset" button (`fullReset`) after wave 20 is cleared.
- **Wave HUD** (`WaveHUD`, key `CastleSiege:WaveHUD`): added automatically on join and toggled with `/cs hud`. Refreshed for all players via `WaveManager.refreshAllWaveHuds`.
- **Kill/death tracking** (`MobDeathTracker`): per-player kills and deaths, plus total and per-wave kills. Stats reset when wave 1 starts.
- **Team money** (`TeamBank`): one balance shared by all players, shown on the Wave HUD as `$1,234`. Player kills of wave mobs pay per role (`KILL_REWARDS`). Starts at $100 and resets when wave 1 starts or on reset. `trySpend` is compare-and-set, so concurrent purchases can't overdraw.
- **Shop** (`ShopUI`, `ShopCatalog`): right-click the `CastleSiege_Merchant` NPC (Outlander Stalker model, unarmed, invulnerable) to buy swords, shields and armor with team money. Cards are appended at runtime (`ui.append("#ItemGrid", "ShopItemCard.ui")`, addressed as `#ItemGrid[i] #Child`). Purchases are refunded if the buyer's inventory is full (`giveItem` returns a remainder; it doesn't drop overflow).
- **Wave rewards** (`WaveRewards`): `WAVE_REWARDS` map of start/end `RewardItem`s per wave, granted to all online players. Health potions only. Crafting materials were removed along with the crafting stations, since gear now comes from the shop.
- **Block restrictions** (`BlockBreakEventSystem`, `BlockPlaceEventSystem`): non-Creative players can't break or place blocks.
- **Join flow** (`WelcomeEvent`): adds the player to the `CastleSiege` permission group, teleports them to the world spawn (deferred so the saved position doesn't override it), and adds the Wave HUD. On **first join only** (tracked in `seen_players.txt`) it clears the inventory, grants the Wave Hammer and a Crude Axe, and shows a welcome title.
- **Full reset** (`WaveManager.fullReset`, via `/cs reset --confirm` or the post-game UI button): resets the wave counter, despawns wave NPCs, teleports all players to spawn, and resets inventories to hammer + axe.
- **Chat filter** (`InputListener`): cancels messages containing a placeholder banned word.
- **Prefab path editor (DEV ONLY)** (`PrefabPathCommand`): `/prefabpath <action>`. Actions: `list`, `debug` (all paths + waypoint coords to the console), `edit <UUID>` (select a path; per-player selection kept in a `ConcurrentHashMap<UUID, UUID>`), `show` (particles at waypoints), `add` (append a waypoint at the player's position), `delnode` (delete the nearest waypoint), and `delete <UUID>` (remove the whole path).
- **Commands:** `/cs <reset [--confirm] | ui | hud | wave --wave <n> | next | debugmobs>` and `/prefabpath <action>`. `debugmobs` prints per-wave mob counts and melee/ranged DPS estimates from `MOB_STATS`.

---

## Working style preferences

- **Inspect the server jar** rather than guessing API shapes. `jar` + `javap` are your friends.
- **Run `./gradlew compileJava`** (or `./gradlew build` when a server run is wanted) to verify that a change compiles before saying it's done.
- **Don't re-introduce PlayerInteractEvent / PlayerMouseButtonEvent global listeners.** They don't fire. Use interactions or ECS systems.
- **Cite file paths with `path:line`** when pointing at code.
- **Keep comments minimal**: explain *why* a non-obvious choice was made, never *what* the code does.
