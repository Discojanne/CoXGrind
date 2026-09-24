# CoXGrind project context

Handoff for the next agent. Read this before changing the plugin. Launch steps for the owner are in `README.md`. That README matches the current panel: one row of filters and buttons, then one row of tabs: Active, Target, Bests, Purples. Targets live in the plugin settings. Trust this file and the code if they disagree.

Do not name the owner, the character, teammates, the account hash, the home folder, or a raid id in README, this file, commits, or anything that could be published. Say "the owner" and "the local player". Paths are `%USERPROFILE%` or "this repo". Work on this Windows PC only. Do not use Cursor Origin, WSL, or a cloud agent unless the owner explicitly asks. Prefer normal `git` and GitHub.

## What this product is

CoXGrind is a personal RuneLite plugin that logs completed Chambers of Xeric raids (normal and Challenge Mode) and shows them in a side panel. It replaces the owner's current split between two Plugin Hub plugins:

- Cox Analytics, for room and floor times
- Raid Data Tracker, for points and purples

The report follows the owner's local C++ tool, Coxparser, for column names, outlier cuts, purple math, and the extra tables. It reads only the JSON log under `.runelite\cox-grind\`. Do not join Cox Analytics files to Raid Data Tracker logs at runtime. Do not port Coxparser line for line.

Coxparser features now in the report, from the JSON log:

- Time table with best, average, recent, and Last N
- Manual comparison targets (Regular and CM sheets), not another player's log file
- The Active tab follows the raid in progress. It clears when a raid starts and adds each split against your filtered average. The list is drawn, not monospace: a kill-count line, then Rooms, Olm, and Finish groups. The time and the colored difference stay in two columns on the right. Mage hand prints as Mage P1, and Olm phase 1 prints as Olm P1. A quiet line follows the upper floor and the middle floor, after Shamans and after Tightrope when those rooms are present. The list stays short enough that the graph fits without a scrollbar. Target and Bests use the same drawn list. Hovering PPH says points per hour. The graph under the list is one pace line centered on your average pace. The personal best is a second line above that, the time still to gain. Above the average line means this raid is ahead of that average. Past the PB line the point is gold. Mage hand is not a point, because that time is already inside the phase. Phase points are replaced by the single Olm point when Olm ends. A finished raid's last point is your PB minus the actual finish. Leaving before the raid ends returns the tab to the latest saved raid. Two captions sit under the graph: one against the average, one against the PB.
- Bests tab: fastest valid split for each room in the current filter, plus Pre-Olm, Between rooms, Total Points, and PPH, with that raid's KC. Times are gold and start in the same column as Active and Target. Points and PPH use the highest value. There is no title line.
- Room points-per-hour, most common prep rooms, 5-room / 6-room / other counts
- Discarded-outlier list, printed last in the full report
- Logged death count, plus the old points-based death estimate
- Purple summary, item table with Expected and On Rate, history map, tracked purple list. The full-report Difference line is colored. On the purple tab the sections are Summary, Items, History, and Tracked items. Regular raid count is cyan, CM is gold, and Total points sits under Avg points. The history is squares, not the text `@` line. Pet, kit, and dust are a side count under Items, a history color when the raid is not purple, and a colored row in Tracked items. They are not in the full-report purple text.
- Account breakdown of the raids in this log

Left in Coxparser on purpose: a live join of the two old log sources, League filtering, lifetime untracked-KC guesses, hand-typed lifetime item counts, and the pre/post drop-rate split. A one-time read of those old files, for the post-update window only, is already in the JSON log. Do not repeat it unless the owner asks.

V1 is passive analytics only. No game-screen overlay, no prayer helper, no tile or stand markers, no attack prediction, no auto-typing, and no combat advice. Stay inside Jagex third-party client rules and the RuneLite rejected-features policy. Reimplement behavior. Do not copy Plugin Hub source into this repo. Both reference plugins are BSD 2-Clause; attribute them only if substantial code is actually reused. None has been reused so far.

## Where the project lives

- Local folder: this repo
- GitHub: the CoXGrind repo on the owner's account. On 2026-09-24 that public repo was deleted and created again. The older commits from before that are not in this repository. Do not restore them, and do not put names, the account hash, the home folder, or raid ids back into a commit.
- Branch: `main`
- License: BSD 2-Clause (`LICENSE`). The copyright line stays as it is. Do not add the character name beside it.
- Package: `com.coxgrind`
- Version: `1.0.0`
- Java bytecode target: 11 (`options.release` in `build.gradle`)
- Installed JDK on this machine: Temurin 21 (also Temurin 17). Both can compile this project.
- RuneLite client that actually launched here: 1.12.39. `build.gradle` uses `runeLiteVersion = 'latest.release'`, so a later build may move forward.

This folder was empty when the current code was written. It is a new implementation of the agreed design, not a recovered copy of an earlier cloud session. An older compiled jar from that earlier attempt is still on disk:

`%USERPROFILE%\.runelite\sideloaded-plugins\cox-grind-1.0.0.jar.old`

Developer mode loads every `.jar` in `sideloaded-plugins`. The `.old` suffix keeps that jar from loading beside this project. Do not rename it back to `.jar` while using `gradlew run`. `javap` on that jar is allowed if you need to compare the previous shape. Do not treat it as source of truth over this tree.

## How the owner runs it

Daily play is the developer client, not the Jagex Launcher.

1. File Explorer: this repo folder
2. Double-click `Run-CoXGrind.bat`
3. Leave the black window open. Closing it closes the game.

Command Prompt equivalent, from that folder:

```
gradlew.bat test
gradlew.bat jar
gradlew.bat run
```

`test` is JUnit 4 and does not open the game. `jar` writes `build\libs\cox-grind-1.0.0.jar`. That jar is not how the owner plays. `run` executes `com.coxgrind.CoxGrindPluginTest`, which calls `ExternalPluginManager.loadBuiltin(CoxGrindPlugin.class)` and `RuneLite.main` with `--developer-mode --debug`.

Batch files must stay CRLF. `.gitattributes` marks `*.bat` as `eol=crlf`. A LF-only `.bat` fails in `cmd.exe`.

The normal Jagex Launcher and the Start menu RuneLite shortcut do not load this plugin. Sideloading into the official client was already unreliable for the owner. Do not send them back to that path.

### Jagex account login

The owner uses a Jagex account. The developer client cannot complete that login by itself. The official one-time procedure is https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

Already done on this PC:

- `%LOCALAPPDATA%\RuneLite\settings.json` has client argument `--insecure-write-credentials`.
- The Jagex Launcher preference for OSRS is the RuneLite client (`osrs_runelite`).
- Launcher exe: `C:\Program Files (x86)\Jagex Launcher\JagexLauncher.exe`
- Configure shortcut: Start menu `RuneLite (configure)`, target `%LOCALAPPDATA%\RuneLite\RuneLite.exe --configure`, working directory that same folder. Launcher settings are loaded from the process working directory, which is why `settings.json` is next to `RuneLite.exe` and not in `.runelite`.

Not confirmed after that setup: whether `%USERPROFILE%\.runelite\credentials.properties` exists. Do not read that file. Do not print it. Do not commit it. Do not ask the owner to paste it. It is a game login token, not the password, and anyone who obtains the file can log into the account. The owner can delete it, and "End sessions" on runescape.com invalidates it. The owner has already been told this is not zero-risk.

## What has been verified

Verified on 2026-09-22, before the Coxparser report sections, teammate purples, deaths, and target adjustments:

- `gradlew.bat test` passed.
- `gradlew.bat jar` passed.
- `gradlew.bat run` booted RuneLite 1.12.39 and logged `Plugin CoxGrindPlugin is now running`.
- The first boot failed with `No implementation for com.coxgrind.CoxGrindConfig was bound` because the plugin did not provide its config. `@Provides CoxGrindConfig provideConfig(ConfigManager)` fixed it. In RuneLite 1.12.39, `Plugin.configure()` is empty, so an `@Inject` config field is not bound unless the plugin provides it. Keep that method.
- The same first boot also loaded the old sideloaded jar. That is why the jar was renamed.

Verified later the same day, after the report sections, then again after the panel and log rewrite:

- `gradlew.bat test` passed after the one-row sidebar, the dropped `totalSeconds` field, and the CM target defaults.
- The owner opened the new panel in the live client and said it looked better. That was before the one-row sidebar and the CM defaults.
- The first raid file was rewritten to `account-<hash>.json`. The old `.jsonl` is gone. Do not write the hash into this file or the README.

Verified on the first live raid, 2026-09-22, a solo CM on the account file:

- Solo CM, 29:21, 63,782 personal points, 0 deaths, no purple. KC 214. `challengeMode` true.
- Prep rooms and Olm phases matched the expected solo shape, including mage hand phases 1 and 2 only. Olm NPC ids worked on this raid. Object `29881` was not proven; the mage-hand spawn path is enough to explain the phases.
- Phases plus head are 8:13. The Olm split is 9:00. The extra 47 seconds is the gap between phases.
- Floor lines were in the client log and were dropped. The text was `@mes_hl_mag@Upper level complete!` (and Middle, Lower). `RaidChat.plain` now strips that token. This saved raid still has no Floor rows. Leave it that way. Do not backfill floors onto KC 214 from the old Cox Analytics file.

Second live raid the same day:

- Solo CM, 28:34, 64,226 personal points, 0 deaths, no purple. KC 215. `challengeMode` true. Timestamp `2026-09-22T20:43:19Z`.
- Floor rows are present: Floor 1, Floor 2, and Floor 3. That is the check the KC 214 file could not provide.

On 2026-09-23 the same account file was rewritten from the owner's old logs. Read only. The Cox Analytics files and `raid_tracker_data.log` were not modified. The plugin does not import them. The rewrite kept KC 214 and KC 215 as this plugin saved them, and added the raids after the drop-rate cutoffs:

- Regular solos, KC 1833–1910. That is 78 raids. KC 1832 itself stays out.
- Challenge Mode, KC 139–213, including team raids. KC 138 stays out. At the moment of that import, together with the two live solos, CM in the file was KC 139–215. Later live raids continued past that. Do not treat 215 as the end of the file.
- Team CM in that window: KC 139, 140, and 141 at team size 3, and KC 142 at team size 28. KC 142 has a teammate purple, an Arcane prayer scroll. Do not record that teammate's name here.
- Every times raid in that window matched a points row. Nothing was skipped.
- Imported deaths are 0 because the points log had no personal death count. Do not treat that as proof those raids were deathless.
- `totalSeconds` is no longer in the file. Duration is the Raid Completed split.
- A copy of the two live raids from before that rewrite is `%USERPROFILE%\.runelite\cox-grind\pre-import-backup.txt`. The plugin does not load it. The name does not end in `.json` or `.jsonl`.

`gradlew.bat test` passed on 2026-09-23 after the ice-milking gate, the unfiltered purple sections, the account breakdown, the Bests tab, the remembered filters, and the Active / Target / Bests / Purples tab row. It passed again later the same day after the shutdown flush, the settings ranges, and the panel layout below. The owner has seen the four tab names in the client and said Purples was clipped. Do not claim they have looked at that earlier layout pass: no status line, stretched tabs, time-tab rules, and indented Regular and CM counts. Later on 2026-09-23 they looked at the size-13 time text and the average-centered graph and said the font size was right. They then asked for the graph colors to follow the average, gold past the PB, a second caption for the PB gap, gold Bests times in the same column as the other time tabs, larger purple text with Tracked items left at 11, the purple section names, and All points under Avg points. `gradlew.bat test` passed after that. Do not claim they have looked at the All points label in the client yet.

On the evening of 2026-09-23 the same account file held live CM solos through KC 221, timestamp `2026-09-23T20:04:39Z`, 1 death, no purple. KC 219 and KC 220 also have `deaths` 1. KC 214 and KC 215 still have 0. The client log that was read did not contain `Oh dear, you are dead!`, so which signal stored those later deaths is still unconfirmed.

That same evening the Purples tab stayed stale until a client restart. The cause was that a raid in progress only repainted Active. Target, Bests, and Purples now reload when the log file's modified time changes during a raid, and again when one of those tabs is opened. `gradlew.bat test` passed after that. Do not claim the owner has confirmed the live refresh.

The same client log showed why new purples were missing. KC 218 and KC 219 each received a Dexterous prayer scroll, and the file had no purple on either raid. The friends-chat text is two lines, `Special loot:` and then `Name - Item`. The plugin was only matching `Special loot: Item` on one line. Those two raids were patched in the JSON from that log. The parser now reads the two-line list and `Valuable drop: Item (coins)`. The clan line `received special loot from a raid:` stays ignored. `gradlew.bat test` passed on 2026-09-24 after that parser. Do not claim a later purple has been caught live by it. Do not import those two scrolls again.

On 2026-09-24 the owner looked at a card layout for the Purples tab and asked for it on `main`. That layout is `PurplePanel`. The label under Avg points is Total points. Do not put the monospace purple tab back.

The owner also asked for a faint line on the Active graph, the steady climb from the start to the personal best. It was drawn, they said it looked buggy, and it was removed on 2026-09-24. Dots are one per room again. There is no pace-angle line. Do not put that line back. Do not claim they have looked at the graph since it was removed.

On 2026-09-24 the owner looked at the drawn Active, Target, and Bests lists and said they look good. Those three tabs share `ActiveTimes`. The time and the comparison are two columns on the right. The section over the prep rooms is Rooms. A quiet line follows the upper floor and the middle floor. Mage hand shows as Mage P1, and Olm phase 1 shows as Olm P1. Hovering PPH says points per hour. The four tabs are equal width and fill the row. Do not put Active, Target, or Bests back on monospace. Do not add room icons.

`gradlew.bat test` passed on 2026-09-24 after pet, kit, and dust were stored on `extras`, and again after the README and this file stopped naming the owner and the character. Do not claim a live pet, kit, or dust has been caught.

Still not verified on a live raid:

- Which signal stored the deaths on KC 219, KC 220, and KC 221. The chat line and the death varbit are both still unconfirmed as the source.
- A pet, kit, or dust written to `extras` after 2026-09-24. Old raids do not have that field. Do not backfill it.
- Game object `29881`.
- A purple caught by the two-line reader after 2026-09-24.
- The owner looking at the graph after the pace-angle line was removed. `gradlew.bat test` covers the pace points. The points are still seconds ahead of the PB. `Start` is still `PB - average raid time`, and that value is where the average line sits. The graph draws the middle on that value and draws the PB at 0. Do not pin the axis back on the PB. A finish slower than the average is below the middle line, and the scale grows so that point stays in the plot.

If a finished raid does not appear, check the developer client log for `Logged CoX raid` and for `CoXGrind could not read a raid message`. Then compare the actual chat line and NPC ids with `RaidChat` and `OlmNpcs`.

## Runtime behavior

`CoxGrindPlugin` is the only class that touches the RuneLite client. Everything else is plain Java so tests do not need a client.

On startup the plugin adds a sidebar button: purple 16x16 icon, white "C", tooltip "CoXGrind", priority 7. The panel is `CoxGrindPanel`. There is no overlay manager registration.

### Raid lifecycle

1. Friends-chat or game message `The raid has begun!` writes a completed raid that is still unsaved, then calls `CoxRaidSession.startRaid()` and reads party size.
2. While the session is running and not complete, chat is parsed for room and floor lines. Olm mage-hand spawns, mage-hand deaths, melee-hand deaths, and game object `29881` update Olm phase timers. `Oh dear, you are dead!` increments the personal death count.
3. `Congratulations - your raid is complete!` reads `RAIDS_DIED` and freezes the clock, points, and team size. The raid is held in memory. It is not written yet.
4. The plugin waits up to 40 game ticks for the kill-count chat line and for personal points above 0. A kill-count line that arrives before the congratulations line is kept and applied when the raid completes. As soon as KC and points are both known, it writes one raid. If that chat line never arrives, it writes whatever it has when the wait ends, or when the next raid starts, or when the client returns to the login screen, or when the plugin shuts down.
5. A later personal purple, teammate purple, or your own pet, kit, or dust replaces that same raid in the file. There is no second line. The side reward is written on `extras`. It does not set `purple`.
6. Leaving the raid instance (`VarbitID.RAIDS_CLIENT_INDUNGEON` becomes 0) resets the session only when the raid has not been completed, and the Active tab goes back to the latest saved raid. A completed session stays alive so the KC and purple messages can still update it. The next `The raid has begun!` writes an unsaved raid first, then clears the session and the live tab.
7. Returning to the login screen writes an unsaved completed raid, resets an incomplete session, and leaves the side panel on the last account file. An incomplete session is cleared and the tab returns to the latest saved raid.
8. Plugin shutdown (closing the client, or turning the plugin off) writes a finished raid that is still unsaved, then clears the session. The account hash must be non-zero. A zero hash is skipped so the raid is not written to `account-0.json`. An unfinished raid is not written.

While a raid is running and not yet saved, the plugin pushes the in-memory raid to the panel on each game tick and after room, floor, and Olm events. The open segment is the raid-clock time since the last room or floor, or since the current Olm phase or head when one of those is running. That line is labeled `Current` in the list. It is not a point on the pace line. The panel keeps the saved raids in memory for those ticks. It reads the log file again when the account changes, the filter changes, a raid is saved, the tab leaves live mode, or the log file's modified time changes during a live raid. Opening Target, Bests, or Purples reads the file again. Active does not.

Chat is accepted only for `ChatMessageType.GAMEMESSAGE` and `ChatMessageType.FRIENDSCHATNOTIFICATION`, after `RaidChat.plain` strips color tags and `@mes_hl_mag@` style tokens. Public chat is ignored. A raid-start line is accepted even if the in-dungeon varbit has not flipped yet. Later lines are accepted while `inRaid` is true or the session is still running.

Kill count is applied when the session is complete. A kill-count line that arrives first is remembered and applied on the congratulations line. The usual game order is congratulations, then KC. The file is written after that KC line, so the saved raid should already have the real KC and Challenge Mode flag.

Challenge Mode is not read from a varbit. It is true only after this exact game message:

`Your completed Chambers of Xeric Challenge Mode count is: <number>.`

Regular KC:

`Your completed Chambers of Xeric count is: <number>.`

The CM sentence does not match the regular pattern, because the regular pattern expects `count is` immediately after `Chambers of Xeric`. There is a `RAIDS_CHALLENGE_MODE` varbit (`6385`). It is not used. Keep CM on the kill-count sentence unless a live raid proves that sentence is missed.

### Clock and points

| Value | How it is read |
| --- | --- |
| Raid clock | Varbit `6386`. 100 units = 1 minute. Leftover units convert at 0.6 seconds, matching the in-game raid clock: `minutes = units / 100`, `seconds = (units % 100) * 6 / 10`. |
| Personal points | `client.getVarpValue(VarPlayerID.RAIDS_PLAYERSCORE)` |
| Team points | `client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSCORE)` |
| Party size | Varbit `5424`, including the local player. Solo is 1. Values outside 1..100 are ignored. |
| In the raid instance | `VarbitID.RAIDS_CLIENT_INDUNGEON == 1` |
| Personal deaths | Chat `Oh dear, you are dead!` while the raid is running and not complete. `VarbitID.RAIDS_DIED` (`10055`) can only raise the count, and only for a value from 1 to 30, while the raid is still running and not complete. It is read again when the congratulations message arrives, before the save wait. |
| Account file id | `Long.toUnsignedString(client.getAccountHash())`. This is the file name only. It is not stored inside the raid. |
| Player name | Local player name, tags removed, NBSP replaced with a normal space. Used only to tell your purple from a teammate's. Not stored. Not shown in the panel or the report heading. |

Room and floor durations are deltas of that raid clock, then converted with `TimeFormat.unitsToSeconds`. Do not "correct" this to raw game-tick subtraction. A 1-unit delta can round down to 0 seconds, and those splits are discarded.

`TimeFormat.formatSeconds` prints `MM:SS` with zero-padded minutes (`03:00`, `22:54`). Minutes can exceed 99 (`100:00`). `parseClock` accepts `MM:SS` or whole seconds. A trailing decimal on the seconds is ignored.

### Rooms and floors

Floor-complete lines from this client start with the RuneLite highlight token `@mes_hl_mag@` and still contain `<col>` tags. `RaidChat.plain` strips both before matching. A start-anchored match misses the floor. Room lines do not have that token. Confirmed on the 2026-09-22 solo CM raid: upper 6:19, middle 13:31, lower 20:06 in the client log. That raid was saved before the strip existed, so its file has no Floor rows. Later raids should.

Room lines look like:

`Combat room `Tekton` complete! Duration: ...`
`Puzzle `Ice Demon` complete! Duration: ...`

The duration inside the message is ignored. The split is the raid-clock delta since the previous room or floor. The first room is measured from raid start (clock 0). Unknown room names are dropped. A second completion of the same room is ignored. `RoomNames.canonical` maps aliases onto the Coxparser names. `Ice Demon` becomes `Ice demon`.

Prep rooms, in report order:

Tekton, Crabs, Ice demon, Shamans, Vanguards, Thieving, Vespula, Tightrope, Guardians, Vasa, Mystics, Muttadiles.

Floor lines:

- `Upper level complete!` stores `Floor 1` as the absolute clock.
- `Middle level complete!` stores `Floor 2` as middle minus upper.
- `Lower level complete!` stores `Floor 3` when a middle floor exists, otherwise `Floor 2`. Lower completion also sets the Olm start used for the total `Olm` split.

Floor names are stored in the log. The report table does not print Floor rows. It prints prep rooms, derived `Pre-Olm`, Olm phases, `Olm`, `Raid Completed`, derived `Between room time`, `Total Points`, and `PPH`.

`Pre-Olm` is the sum of prep-room seconds on that raid. `Between room time` is `totalSeconds - prep - Olm`, when all three are positive.

A full layout is 11 or more prep rooms with a split over 0 seconds (`CoxRaidRecord.FULL_LAYOUT_PREP_ROOMS`). That matches Coxparser. Regular and Regular full are separate filters. CM is not split by layout.

### Olm

Expected hand phases: `3 + teamSize / 8`. Unknown team size uses scale 1, so 3 phases. Solo is 3 hand phases plus the head.

Phase start: mage-hand NPC spawn, or game object `29881`. A start is ignored if a phase is already open or the head phase has started. If Olm start was missed, the first phase start also sets it.

Mage hand down: NPC death of id `7550` (normal right claw) or `7553` (CM). The split `Olm mage hand phase N` is recorded only when `N < expectedHandPhases`. The last hand phase has no separate mage-hand row. That matches the owner's Coxparser logs, which show mage hand phases 1 and 2, then `Olm phase 3`, then `Olm head`.

Melee hand down: NPC death of id `7552` or `7555`. Records `Olm phase N` from the phase start, then increments the phase. When the finished phase number reaches the expected count, the head timer starts.

Raid complete: `Olm head` is head-start to the complete clock. `Olm` is Olm-start to the complete clock. `Raid Completed` is the absolute clock. In memory, `totalSeconds` is that same clock. Mage-hand splits are a slice of the phase, so they are not added into `Olm`. `Olm` can be longer than the phases plus the head because of the gap between one phase ending and the next phase starting.

The hand NPC ids worked on the 2026-09-22 solo CM raid. Object `29881` has not been confirmed. If Olm rows are missing after a real raid, log the NPC ids from `ActorDeath` / `NpcSpawned` and update `OlmNpcs` only. Do not add attack-cycle prediction.

### Purple

Your own unique stays on `purple`. Dust, twisted kit, and Olmlet are not purples. They are stored on `extras` when you receive one, and they do not change purple counts, dry streaks, expected ticks, or the purple square. The Items section shows them after a gap: Olmlet, twisted kit, metamorphic dust, as a count only. History paints a non-purple square dark green for the kit, white for the pet, and cyan for dust. A purple square stays purple. Tracked items lists them in those same colors. The full-report purple text does not include them.

Accepted lines, case-insensitive, from game or friends-chat messages. Confirmed on 2026-09-23, KC 218 and KC 219, both Dexterous prayer scroll. The friends-chat text is two messages, and the item is not on the first line:

- `<col=ef20ff>Special loot:</col>` opens the list
- `<col=ef20ff>Player name -</col> <col=ff0000>Dexterous prayer scroll</col>` is that player's purple. The name is the local player or a teammate. More than one of these can follow the header.
- `Special loot: Twisted bow` on one line is still yours. That is the older shape.
- `<name> received special loot: Dinh's bulwark` is yours when the name is the local player, and a teammate's when it is not
- the same with `received a special drop:`
- `Valuable drop: Dexterous prayer scroll (15,020,290 coins)` is yours. The coin note is ignored. Dust, kit, and Olmlet still do not count as purples. The same chat shapes store them on `extras`: `Olmlet`, `Metamorphic dust`, and `Twisted ancestral colour kit`. A teammate's pet, kit, or dust is ignored.

The clan broadcast `received special loot from a raid:` is ignored. It can arrive late, and on login the client repeats old clan lines. Those lines are not this raid's friends-chat list.

The item must be one of:

Dexterous prayer scroll, Arcane prayer scroll, Twisted buckler, Dragon hunter crossbow, Dinh's bulwark, Ancestral hat, Ancestral robe top, Ancestral robe bottom, Dragon claws, Elder maul, Kodai insignia, Twisted bow.

Stored display text uses that exact capitalization. An empty string means no personal purple. Teammate drops are `{playerName, item}` on `partyPurples`. The same name and item are stored once. The report and loot math use only your purple. Teammate drops are kept for later.

### Deaths

`deaths` on the raid is how many times you died in that raid. Teammate deaths are not stored.

The chat line increments by one. The varbit only raises the stored count, so one death that produces both the chat line and a varbit of 1 is stored as 1. A varbit outside 1..30 is ignored. A death after the raid is complete is ignored. A later file update writes the session's death count. It does not add a second line.

The report prints a **Deaths** section from this count, then the Coxparser points estimate. The estimate is still the proxy: solo full regular under 48000, other solo regular under 29000, CM solo under 60000, CM team under 40000. Regular team raids are left out of the estimate. Those four cutoffs are plugin config. The logged count includes regular team raids.

## Log format

Directory: `%USERPROFILE%\.runelite\cox-grind\`

File: `account-<unsigned hash>.json`. The hash is restricted to letters, digits, `_`, and `-`. The name and the hash are not repeated inside the raid. The account is the file name.

The file is a pretty JSON array. Each raid is one object. Each split is one line. A purple or other later update rewrites that raid. The write goes to a temp file in the same folder, then replaces the JSON file.

`id` stays so a later purple or side reward can find the raid. It is not shown in the panel. `timestamp` is UTC, trimmed to seconds. Empty purple, empty `extras`, and empty `partyPurples` are left out. `totalSeconds` is not written when the `Raid Completed` split is present. Load copies that split back into `totalSeconds` for points per hour and between-room time. If that split is missing, the old `totalSeconds` field is kept.

```json
[
  {
    "id": "<uuid>",
    "timestamp": "2026-09-22T16:38:15Z",
    "challengeMode": true,
    "kc": 1,
    "teamSize": 1,
    "deaths": 0,
    "personalPoints": 60000,
    "teamPoints": 60000,
    "splits": [
      {"room": "Tekton", "seconds": 68}
    ],
    "extras": ["Olmlet"],
    "partyPurples": [
      {"playerName": "Someone Else", "item": "Elder maul"}
    ]
  }
]
```

An older `account-<hash>.jsonl` file is still read if one appears. `RaidLogStore.load` applies its raid and patch lines, writes the JSON file, and deletes the `.jsonl`. The live account file is already JSON. As of 2026-09-24 it holds the post-update import, the live CM solos from KC 214 through KC 221, and the two Dexterous prayer scrolls patched onto KC 218 and KC 219. Do not regenerate that history from Coxparser unless the owner asks. Do not patch those two scrolls again. Do not write into `cox-analytics` or `raid-data tracker`. Pet, kit, and dust are not in that history. They are stored only when one is received after this change.

Comparison targets are plugin settings, not `targets.json`. `ComparisonTargetStore` is gone.

Gson 2.10.1 reads the file. It is `compileOnly` plus `testImplementation`. The RuneLite client already provides Gson at runtime, including `gradlew run`. Do not shade a second Gson into the plugin jar unless sideload becomes a real target.

## Side panel

`CoxGrindPanel` extends `PluginPanel(false)`.

The panel does not wait for login and does not show the player name. On startup it loads the newest `account-*.json` or `account-*.jsonl` in the log folder. After login it switches to the logged-in account hash. Logging out leaves the panel on that file.

- One row: mode filter, size filter, Full report, Log folder. The sidebar is narrow, so a label may clip. The hover text has the full name.
- Mode filter: All, Regular, Regular full, CM
- Size filter: All, Solo (team size 1), Team (team size >= 2). Team size 0 stays in All only
- Regular is not CM and not a full layout. Regular full is not CM and is a full layout. Both use the regular comparison sheet. CM is Challenge Mode at any layout and uses the CM sheet. All has no comparison column.
- Nothing sits between the filter row and the tabs. Do not put a status line back there.
- Tabs, one row, equal width, stretched across the full panel width so there is no empty gap on the right. Active, Target, Bests, and Purples stay fully visible. The layout is wrap, not a scrolling tab row. FlatLaf needs `JTabbedPane.tabAreaAlignment` set to `fill` as well as `tabWidthMode` `equal`. Hover text, on the tab itself: Active is `This raid vs your average` during a raid and `Recent raid vs your average` otherwise; Target is `Recent raid vs your targets`; Bests is `Fastest split, points, and PPH in this filter`; Purples is `Every logged raid`. Do not put those descriptions back as a line above the list. Target and Bests start on the first row.
- Active, Target, and Bests use the same drawn list, `ActiveTimes`. The time and the comparison sit in two columns on the right. Target says `vs target`. Bests says `fastest`, times are gold, and the right column is that raid's kill count. Hovering PPH says points per hour. Do not put these three tabs back on monospace. The Purples tab is not monospace either. Do not put it back on monospace. Horizontal scroll is off.
- Active, Target, and Bests draw a hairline on Pre-Olm, Olm, and Raid Completed, and a quiet floor line after Shamans and after Tightrope. The text methods still print a dashed rule after Pre-Olm, Raid Completed, and Between rooms for the tests. During a live raid, Pre-Olm is not its own row yet. Bests lists the fastest valid split for each room in the current filter, including Pre-Olm, Between rooms, Total Points, and PPH, with that raid's KC in the right column. Time values on Bests are gold. Points and PPH take the highest value. The Purples summary shows Regular and CM as their own cards. Regular is cyan. CM is gold.
- The mode and size filters are saved in the RuneLite profile (`panelMode`, `panelSize`) and restored on the next startup. They are not shown in the plugin settings.
- The Active list and the pace graph are one column in one scrollbar. The graph starts on the line after the last row. Do not put the graph in its own scroll pane, and do not leave a stretch gap between the list and the graph.
- The list clears when a raid starts and adds each split against the filtered average. `Current` ticks the open segment and has no difference. Leaving before the raid ends shows the latest saved raid again. Challenge Mode is not known until the kill-count line, so the filter the owner picked is the comparison set during the raid.
- Target is the latest raid in the filter against the matching target sheet. Bests is the fastest valid split per room in that same filter. Active, Target, and Bests follow the filters. Purples does not. The time tabs put the room, the time, and the colored difference on one row. On those tabs `Olm mage hand phase N` shows as `Mage PN`, and `Olm phase N` shows as `Olm PN`. `Between room time` shows as `Between rooms`. The text methods and the full report still use `mage hand pN` and the long Olm phase name.
- The Purples tab is cards, drawn by `PurplePanel` from `PurpleBoard`. It uses the whole log. The same inset sits on the left and the right, and the panel tracks the sidebar width so a row does not stick out. Section titles collapse. Do not put the word Purple back on those titles. The full report is still plain text and still says Purple Summary, Purple Items, Purple History, and Tracked purples. `purpleView` is that text shape. Tests still read it. The tab does not.
- Summary cards: Raids, Rate, Regular in cyan, CM in gold, Expected, Actual, Scrolls, Diff, then Dry now, Longest, and Average, then Avg points and Total points. Total points is the sum of personal points above 0. Avg points and Total points use an apostrophe for thousands, so 53999 is 53'999. Do not put the label All points back on this tab.
- Items are two to a row. Each cell is the RuneLite inventory sprite, the count, and the on-rate difference. Hover the sprite for the item name. Sprites come from `ItemManager`, passed in from `CoxGrindPlugin`. A count of 0 is dim. The difference is green when ahead of rate and red when behind.
- History is one square per logged raid, read left to right. Each row sits on a faint bar. Grey is a white light. Purple is a unique you received. A light dot marks the raid where one was expected, and that square stays grey unless that raid also has a pet, kit, or dust. If that raid was also a purple, the square stays purple and the dot sits on it. A raid with no purple paints the kit dark green, the pet white, and dust cyan. The last square is gold, with a small arrow pointing right, and it is the next raid. Hover a square for the raid number. Do not put the red expected fill or the split square back.
- Tracked items are rows, newest first. CM kill count is gold. Regular kill count is cyan. A purple name is green. Pet is white, kit is dark green, and dust is cyan. The full report history is still the wide `.` `+` `'` line and still ends with the gold `@`. Do not put that line back on the tab. The full report does not list pet, kit, or dust.

The panel reloads when a raid is written and when a later purple updates it. While a raid is running, the other tabs also reload when the log file changes, so Purples picks up a raid that was just saved. Opening Target, Bests, or Purples reads the log again. There is no Refresh button and no Copy report button.

### Pace line

`PaceGraph` draws one line, not one bar per room. Dots are one slot per room, from Start on the left to the latest point on the right. Do not space them by the raid clock, and do not add a line for the pace angle. The owner tried that line and said it looked buggy. The middle line is your average pace, labeled Avg. The personal best is a dashed line above it, labeled PB, the time still to gain. A point above the average is ahead of that average. The caption under the graph is two lines. The first is against the average, such as `Tekton  00:08 ahead of average`, colored with the average cuts. The second is against the PB, such as `Tekton  01:22 behind PB`. That line is gold when the point is past the PB. Behind the PB it uses the same orange and red cuts, measured from the PB. The dots follow the average cuts, and turn gold past the PB. Do not color the dots from the PB again. Hover a dot for both lines. The graph is 174 pixels tall so both lines fit under the plot. When the PB and the average are the same raid, the two lines sit together and the label is PB. A finish slower than the average drops below the middle line. The scale grows to fit that point, so it stays inside the plot. Orange is under 10 seconds behind the average. Red is further behind.

The PB is the fastest valid `Raid Completed` in the comparison set. The average is the average valid `Raid Completed` in that same set. Both use the outlier rules. The raid being drawn is not part of the set. For the latest saved raid, the set is the filtered raids before it. For a live raid, the set is the filtered saved raids. With no earlier raid, the graph says `Need another raid in this filter.`

`paceLine` in `RaidReportFormatter`:

- `Start` is `PB - average raid time`. An average raid begins on the middle line, that far behind the PB. The line then climbs toward the PB as rooms come in under the average. Do not put the axis back on the PB. The points themselves stay seconds ahead of the PB, so `Start` is unchanged.
- Each finished prep room adds `round(average for that room) - actual`. Pre-Olm is not a second point.
- Mage hand is not a point. That time is already inside the phase. It still appears in the list.
- `Olm phase N` and `Olm head` move the line during the fight.
- The `Olm` point replaces those phase gains with `round(average Olm) - actual Olm`, so the gaps between phases are included once and the phases are not added on top of Olm.
- Floors, Between rooms, Total Points, and PPH are not points. Between rooms still appears in the list.
- The open `Current` segment does not move the line. The room is not known to be fast or slow until it finishes.
- `Finish`, only when the raid is complete and the total is known, is `PB - actual finish`. That last point is the real result, not the sum of the earlier gains. The earlier points are the running estimate: average raid time, adjusted by the time gained or lost on the pieces above. A finish slower than the average is below the middle line.

Do not turn this back into a chart of each row's difference from the average. The list already shows that.

Full report opens a modeless window with the same text report, including colors. Log folder creates `cox-grind` if needed and calls `Desktop.open`. UI mutations hop to the Swing event thread.

Diff colors match Coxparser: cyan when a time is at least 20 seconds faster, green when it is faster, orange when it is under 10 seconds slower, red when it is slower than that. Higher points are green. Lower points are red. The text itself carries the console color codes. `ColoredText` paints them. A purple `+` is green. An expected-tick `'` is red. On the pace line the same cuts are measured from the average: 20 seconds or more ahead is cyan, any time ahead is green, under 10 seconds behind is orange, and further behind is red. Even with the average is the plain text color. A point past the PB line is gold, the same color as that line. The middle of the graph is the average, not zero.

Targets are the RuneLite plugin settings, in the collapsed Regular targets and CM targets sections. `@ConfigSection` in RuneLite 1.12.39 annotates a field, not a method. Time rows accept `MM:SS`. Total Points and PPH accept whole numbers. Blank means no target. The rows are prep rooms, Pre-Olm, mage hand phases 1 and 2, Olm phases 1 through 3, Olm head, Olm, Raid Completed, Between room time, Total Points, PPH. The Target tab asks for Regular, Regular full, or CM when the filter is All.

Regular targets default to blank. CM defaults, set by the owner:

| Row | CM |
| --- | --- |
| Tekton | 1:08 |
| Crabs | 0:57 |
| Ice demon | 2:19 |
| Shamans | 1:02 |
| Vanguards | 2:18 |
| Thieving | 1:21 |
| Vespula | 1:01 |
| Tightrope | 1:00 |
| Guardians | 1:52 |
| Vasa | 1:11 |
| Mystics | 1:46 |
| Muttadiles | 1:42 |
| Pre-Olm | 17:34 |
| Olm mage hand phase 1 and 2 | 0:56 |
| Olm phase 1, 2, and 3 | 2:00 |
| Olm head | 1:04 |
| Olm | 8:00 |
| Raid Completed | 27:04 |
| Between room time | 1:19 |
| Total Points | 63750 |
| PPH | 130000 |

Olm 8:00 is the three phase targets plus the head, plus a bit of time between phases. Mage-hand targets are not added again. These CM keys are not in the owner's RuneLite profile yet, so the code defaults apply until the owner edits a box. A saved blank would hide the new default. Do not clear these defaults.

## Plugin config

Config group `coxgrind`. The owner's existing RuneLite profile may still contain older keys from the sideloaded jar (`showPanel`, `panelPriority`, `trackMageHand`, `chatOnSave`). This code does not read them.

| Key | Default | Meaning |
| --- | --- | --- |
| `lastRaids` | 10 | Last N column. Settings spinner is 1..100. A saved value under 1 is treated as 10. |
| `reportRaids` | 0 | 0 uses every raid in the filter. Any other number uses that many newest raids, then Last N is inside that set. Spinner and the report both clamp to 0..10000. |
| `deathFullRegular` | 48000 | Points estimate cutoff |
| `deathRegular` | 29000 | Points estimate cutoff |
| `deathCmSolo` | 60000 | Points estimate cutoff. Coxparser's note said 59000; `Config.cpp` is 60000 and that is what this plugin uses. |
| `deathCmTeam` | 40000 | Points estimate cutoff |
| `showPurpleSummary` | true | Deaths, points estimate, purple summary, item table, history map |
| `showTrackedPurples` | true | Newest-first list of your purples. The purple tab titles that section Tracked items. The settings name stays Tracked purples. |
| `showRoomEfficiency` | false | Prep-room points per hour. Off unless the owner turns it on. |
| `showCommonRooms` | false | Most common prep rooms and the 5 / 6 / other count. Off unless the owner turns it on. |
| `showOutliers` | true | List of splits dropped from the averages |
| `useTbow` | true | Off shortens tbow room targets |
| `noTbowVanguards` | 15 | Seconds removed |
| `noTbowVasa` | 20 | Seconds removed |
| `noTbowMystics` | 25 | Seconds removed |
| `noTbowMuttadiles` | 20 | Seconds removed |
| `noTbowTightrope` | 10 | Removed only when twisted bow is off and killing rope is on |
| `noTbowOlmHead` | 30 | Also shortens Olm and Raid Completed when those targets exist |
| `iceMilking` | false | On adds the ice seconds to the Ice demon target, and includes Ice demon splits over 3:50, up to 4:30, in the averages |
| `panelMode` | | Hidden. Not a `@ConfigItem`. Saved when the mode box changes. Restored on startup. `ALL`, `REGULAR`, `REGULAR_FULL`, `CM` |
| `panelSize` | | Hidden. Same as `panelMode`. `ALL`, `SOLO`, `TEAM` |
| `iceMilkSeconds` | 70 | |
| `killRope` | false | On adds time to Tightrope |
| `killRopeSeconds` | 50 | |
| `milkVespula` | false | On adds time to Vespula |
| `milkVespulaSeconds` | 10 | |

Method-second boxes use `@Range` 0..600, and `TargetStyle` clamps again when it builds the comparison column. Killing rope defaults to 50 seconds in both the settings and `TargetStyle`. Adjustments change the comparison column only, except Ice milking, which also decides whether Ice demon splits over 3:50 are in the averages. The typed targets in plugin settings stay as entered. Pre-Olm and Raid Completed move by the net change of the prep rooms that had a target. A room with no typed target is left blank.

Code defaults do not override a value already saved in the owner's RuneLite profile under `%USERPROFILE%\.runelite\profiles2\`. As of 2026-09-22 that profile has `iceMilking=false`, `killRope=true`, `killRopeSeconds=50`, `milkVespula=true`, `showRoomEfficiency=false`, and `showCommonRooms=false`. The code defaults for those last two switches are false. The saved profile matches them. The owner asked for room efficiency and common rooms to stay off. Killing rope and milking Vespula stay as saved. Do not turn those two method boxes off unless the owner asks. Old unused keys in that profile (`showPanel`, `panelPriority`, `trackMageHand`, `chatOnSave`) are not read.

## Report

`RaidReportFormatter.format` filters first, optionally trims to `reportRaids`, then prints the report. Diffs, purple-item diffs, and the history marks carry Coxparser console color codes. `ColoredText` paints those codes in the sidebar and in the full-report window. `ReportOptions` carries the config and the target style. Room efficiency and common rooms default off in code. The owner's saved profile has both off.

Heading examples. The player name is not printed.

- `Analyzing all solo CM raids (12 raids)`
- `Analyzing all solo raids (4 raids)` for regular solo that is not a full layout
- `Analyzing all regular full raids (N raids)`
- `Analyzing all raids (N raids)` when both filters are All

Columns: Room, Best, Average, Recent, Last N, and `vs Target` when the filter is Regular, Regular full, or CM and at least one target is set. Recent is the last filtered raid only. Last N averages values present in the last N filtered raids. Time best is the minimum. Points and PPH best are the maximum. A missing recent value prints `-`.

`vs Target` shows the benchmark and how it differs from your average. `+` on a time means that target is slower than your average. `+` on points means the target is higher. Recent and Last N diffs use the same signs against your overall average.

Time diff is recent-or-window minus the overall average. The report says that under the table. When a method box changes the column, the report also says `Targets adjusted:` and names which boxes applied.

Outlier rules, copied as thresholds from the owner's Coxparser so one death does not dominate the average:

- Any split under 20 seconds is excluded.
- Prep rooms and `Between room time` also use min/max seconds in `RaidReportFormatter` (`Tekton` 30..240, `Ice demon` 90..230, and so on). Ice demon over 3:50 is milking: those splits stay out of the averages unless Ice milking is on, and then only up to 4:30. The outlier list marks them `ice milking`.
- Excluded splits still exist in the JSON file. They are only dropped from the table. The discarded list prints KC, room, time, and `<20s`, `below min`, or `above max`.

After the table, when the matching config switch is on:

- Room efficiency: each prep room's share of raid time times personal points, ranked by points per hour. Default off. Splits that fail the outlier gate, including ice milking, are left out.
- Most common prep rooms, using splits that pass the outlier gate, then raw prep-room counts of 5, 6, and other. Default off.

Purple summary, items, history, the tracked list, logged deaths, and the points-based death estimate use every raid in the account file. The mode filter, size filter, and raids-in-report limit do not apply to them. The full report says so above that section. The sidebar purple tab uses the same whole-log set.

Account breakdown is next. It counts logged regular solo and team raids, logged CM solo and team raids, missing KC between the first and last logged KC, CM equivalent raids (`CM personal points / average regular solo points`), effective raids, and total personal points. It does not estimate lifetime KC from before this log.

The discarded-outlier list is last. It uses the time filter.

`Total Points` and `PPH` use personal points. PPH for one raid is `personalPoints / (totalSeconds / 3600)`, truncated to int per raid, then averaged across raids. Solo personal and team points are equal. In a team, the table still uses personal points.

Purple pace: `867600 / average personal points`, printed as `1 in X.XX`. The same denominator is the per-raid unique chance. Item expectations use the post-weight-update tables from Coxparser: regular total weight 60, CM total weight 56. There is no pre-update era split. A mixed filter assigns each raid to its own table.

The purple summary includes logged raids, average personal points, rate `loggedRaids / expectedPurples`, expected count, actual count, prayer-scroll share, and difference in purples plus an approximate raid equivalent. In the full report that difference is wrapped with `ReportColor.signed`: more purples than expected is green, fewer is red. The Purples tab colors the same difference. Total points on that tab is the sum of personal points above 0. Regular is cyan and CM is gold. The full report On Rate column stays one decimal. The item table in the full report has Got, Expected, Diff, On Rate, Diff. Expected spreads the purples you actually got across the weight table. On Rate spreads the points-based expected count. Then the history map (`.` dry, `+` purple, `'` on the expected tick, gold `@` on the next raid's slot), current / longest / average dry streak. Average dry streak is a whole number of raids. The tracked list is newest first (`CM <kc>`, `KC <kc>`, or `--`). The full report keeps the single wide history line and the titles Purple Summary, Purple Items, Purple History, and Tracked purples. The tab draws squares instead of that line.

## Source map

| Path | Role |
| --- | --- |
| `src/main/java/com/coxgrind/CoxGrindPlugin.java` | Client events, sidebar, save and update |
| `src/main/java/com/coxgrind/CoxGrindConfig.java` | Report, death, section, and target-adjustment settings |
| `src/main/java/com/coxgrind/model/CoxRaidRecord.java` | One raid. `secondsFor` returns -1 when absent. `isFullLayout` is 11+ prep rooms. `extras` is pet, kit, and dust, separate from `purple` |
| `src/main/java/com/coxgrind/model/RoomSplit.java` | `{room, seconds}` |
| `src/main/java/com/coxgrind/model/PartyPurple.java` | Teammate unique `{playerName, item}` |
| `src/main/java/com/coxgrind/model/ComparisonTargets.java` | Regular sheet and CM sheet |
| `src/main/java/com/coxgrind/model/RaidFilter.java` | All / Regular / Regular full / CM and All / Solo / Team |
| `src/main/java/com/coxgrind/model/RaidModeFilter.java` | All, Regular, Regular full, CM |
| `src/main/java/com/coxgrind/model/RaidSizeFilter.java` | All, Solo, Team |
| `src/main/java/com/coxgrind/log/RaidLogStore.java` | Pretty JSON per account, replace one raid, read old `.jsonl` once |
| `src/main/java/com/coxgrind/report/TargetSettings.java` | Plugin-settings targets into `ComparisonTargets` |
| `src/main/java/com/coxgrind/report/ReportColor.java` | Coxparser diff colors, plus gold |
| `src/main/java/com/coxgrind/ui/ColoredText.java` | Paints those colors in the time tabs and the report window. `\u001B[#11m` switches the following text to size 11. The Purples tab does not use it. |
| `src/main/java/com/coxgrind/track/CoxRaidSession.java` | Testable raid state machine |
| `src/main/java/com/coxgrind/track/RaidChat.java` | Chat patterns, purple names, death line |
| `src/main/java/com/coxgrind/track/TimeFormat.java` | Raid-clock conversion and `MM:SS` parse |
| `src/main/java/com/coxgrind/track/RoomNames.java` | Prep-room canonical names |
| `src/main/java/com/coxgrind/track/OlmNpcs.java` | Hand and encounter ids |
| `src/main/java/com/coxgrind/report/RaidReportFormatter.java` | Text report, the PB pace line, and `purpleBoard` for the Purples tab |
| `src/main/java/com/coxgrind/report/PurpleBoard.java` | Numbers for the Purples tab. The text report still prints its own copy. |
| `src/main/java/com/coxgrind/report/ReportOptions.java` | One report's settings and which target sheet to use |
| `src/main/java/com/coxgrind/report/TargetStyle.java` | Twisted bow, ice, rope, and Vespula adjustments |
| `src/main/java/com/coxgrind/ui/ActiveTimes.java` | Drawn split list for Active, Target, and Bests. Times and the comparison are two columns on the right. Mage P1 and Olm P1. Quiet lines after the upper and middle floors. |
| `src/main/java/com/coxgrind/ui/PaceGraph.java` | Pace line under the Active tab. Centered on the average. The PB is the line above it. |
| `src/main/java/com/coxgrind/ui/PurplePanel.java` | Card layout for the Purples tab. Item sprites, a side count for pet, kit, and dust, history squares, Total points. |
| `src/main/java/com/coxgrind/ui/CoxGrindPanel.java` | Swing side panel. Filters, two buttons, four equal tabs. No status line. No target editor. Filters are remembered. |
| `src/test/java/com/coxgrind/CoxGrindPluginTest.java` | `main` for `gradlew run`, not a JUnit test |
| `src/test/java/com/coxgrind/track/*Test.java` | Clock, chat, session, teammate purple, deaths |
| `src/test/java/com/coxgrind/log/RaidLogStoreTest.java` | One raid replaced in place, legacy `.jsonl` folded in, `totalSeconds` omitted when Raid Completed is present, `extras` round-trip |
| `src/test/java/com/coxgrind/report/RaidReportFormatterTest.java` | Heading, filters, ice milking, unfiltered purples, bests, outliers, targets, On Rate, deaths |
| `runelite-plugin.properties` | Hub-style metadata. Not used by `gradlew run` |

Tests to keep green: clock units `100 -> 60` seconds and `150 -> 90`; a scripted solo raid with Tekton, Crabs, two floors, three Olm phases, no mage-hand row on phase 3, head, total, two deaths, and one teammate purple; the open segment follows the current phase and is `-1` after the raid ends; one JSON raid replaced in place, plus a legacy `.jsonl` raid-and-patch folded into one object; report heading `Analyzing all solo CM raids (4 raids)`; a no-tbow / ice / rope / Vespula target adjustment; a floor line that starts with `@mes_hl_mag@`; a live Tekton that is 20 seconds under the comparison average is `+20` ahead of PB when that comparison raid is its own PB; the pace line has no mage-hand point, an Olm phase point is replaced when Olm is applied, and `Finish` is PB minus the actual total; when the comparison set's PB is faster than its average, `Start` is that gap behind the PB, a room ahead of the average moves up from `Start` by that many seconds, and a finish on the PB time is 0; the purple tab colors the on-rate diff and uses `DHCB`; Ice demon over 3:50 is excluded unless ice milking is on, and over 4:30 stays excluded; a CM-solo time filter still counts a regular raid in the purple section and colors the full-report Difference line; the purple tab prints indented Regular and CM raid counts from the whole log, and its history says `Purples:` without the wide ` | Purples:` line; Bests shows the fastest valid room and its KC in gold, the time starts in the same column as Active and Target, includes Between rooms, Total Points, and PPH, has no title line, and a dash rule follows Pre-Olm, Raid Completed, and Between rooms; `purpleView` still prints Summary, Items, History, and Tracked items, Regular in cyan, CM in gold, All points under Avg points, a two-decimal Rate column, a whole-number average dry streak, and a gold `@` at the end of the history text; the tab itself is `PurplePanel` and labels that sum Total points; a friends-chat `Special loot:` header followed by `Name - Item` is that player's purple, `Valuable drop: Item (coins)` is yours, and `received special loot from a raid:` is not a purple; Olmlet, metamorphic dust, and the twisted kit are side rewards on `extras`, they stay out of the purple count, a purple square stays purple, and a dry raid with a kit is still a dry mark.

## Explicitly out of scope until the owner asks

- Another import from Cox Analytics or Raid Data Tracker. The 2026-09-23 post-update window is already in the account file. Do not read those logs again unless the owner asks, and do not write to them.
- A separate purple chart. The history squares on the Purples tab stay.
- Loading another player's Cox Analytics file for a comparison column. Comparison is the manual Regular and CM target sheets.
- Plugin Hub submission. The repo is public so that can happen later. Hub submission needs a public repo and a clean rejected-features pass.
- HUD overlays of any kind.
- Reading or copying `credentials.properties`.
- Showing teammate purples in the report. They are stored only.

## Working rules for the next agent

- Change this repo locally. Commit only when the owner asks. Do not force-push `main`.
- After Java changes, run `gradlew.bat test`. If client wiring changed, `gradlew.bat run` must reach `Plugin CoxGrindPlugin is now running` with no `Error instantiating plugin`.
- A real raid is still the acceptance test for tracking. Tell the owner what to click. Do not claim a raid was logged unless a new line exists under `.runelite\cox-grind\`.
- Keep the plugin readable as model / log / track / report / ui. Put client calls in `CoxGrindPlugin`.
- Do not add a second CoXGrind class on the classpath. One sidebar button, one log writer.
- README edits stay in plain Windows steps: File Explorer, double-click, Command Prompt. No Linux instructions.
