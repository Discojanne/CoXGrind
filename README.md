# CoXGrind

Personal Chambers of Xeric logger for RuneLite.

It writes each finished raid to a file on your PC and shows a side panel with recent raids, filters, and a text report. It does not draw anything on the game screen.

## Run it

1. Install Java 17 or newer if you do not have it: https://adoptium.net/
   During setup, turn on "Add to PATH" if you see that option.
2. Open this project folder in File Explorer.
3. Double-click `Run-CoXGrind.bat`.
4. Leave the black window open. The first time can take a few minutes. It downloads RuneLite, then a RuneLite window opens with CoXGrind loaded.
5. If Windows asks whether to allow Java to use your network, choose Yes.
6. Log in and play. Closing the black window closes the game.

You can also open Command Prompt in this folder and run:

```
gradlew.bat run
```

Use this client to play when you want CoXGrind. The normal Jagex Launcher client does not load this personal plugin.

An older copy of this plugin may still sit in `%USERPROFILE%\.runelite\sideloaded-plugins\`. If that jar is named `cox-grind-1.0.0.jar`, rename it so the name does not end in `.jar`. The launcher loads every jar in that folder, and two copies of this plugin will fight. Do not put a jar back there while you use `Run-CoXGrind.bat`.

## Log in with a Jagex account

The first time, the developer client needs a saved login from the official launcher. Do this once:

1. Press the Windows key and search for `RuneLite (configure)`.
2. In Client arguments, add:

   `--insecure-write-credentials`

3. Click Save.
4. Start Old School RuneScape from the Jagex Launcher as you normally do, and log in once.
5. That writes `%USERPROFILE%\.runelite\credentials.properties`.
6. Close that client.
7. Double-click `Run-CoXGrind.bat` again. It should log into the same account.

Do not share `credentials.properties`. You can delete that file later if you want the normal launcher to stop using it. Full notes: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

## What to click after one raid

1. Start `Run-CoXGrind.bat`. You can open the CoXGrind panel before you log in. It shows the raids already saved on this PC.
2. On the RuneLite sidebar (the vertical strip of icons), click the purple square with a white C. The tooltip says CoXGrind.
3. At the top of the side panel, pick the filters. Mode is All / Regular / Regular full / CM. Size is All / Solo / Team.
4. The tabs are Active, Target, Bests, and Purples. Active follows the raid you are in. It clears when the raid starts, then adds each split against your average. The graph under that list is your pace against your average, with your personal best as a line above it. If you leave before the raid ends, it goes back to your latest saved raid. Target is the latest raid against your targets. Bests is the fastest split for each room in the current filter. Purples is every logged raid. It updates when a raid is saved, including while you are still on that tab, and again when you open the tab.
5. Enter Chambers of Xeric, start the raid, and finish it (kill the Great Olm).
6. Wait until you see the raid complete message and your kill count in chat. The panel updates itself.
7. Click Full report to open the full text in its own window.
8. Click Log folder to see the save file.
9. Target times are in the plugin settings (the RuneLite wrench, CoXGrind). Regular targets and CM targets are collapsed sections. Leave a box blank when you have no target for that row.

A normal raid and a Challenge Mode raid both count. Solo is a team of 1. Team is 2 or more players.

## Where raids are saved

```
%USERPROFILE%\.runelite\cox-grind\
```

Each account gets its own file, named `account-<hash>.json`. One raid is one block in that file, with each room split on its own line. A purple that arrives a moment later updates that same raid. Pet, kit, and dust are stored on that same raid, apart from the purple. The plugin waits for the kill-count message before it writes, so the saved raid has the real KC and Challenge Mode flag.

The plugin does not import Cox Analytics or Raid Data Tracker again.

## Check the project from Command Prompt

Open Command Prompt in this folder, then:

```
gradlew.bat test
gradlew.bat jar
```

`test` runs the log, timer, and report checks. `jar` builds `build\libs\cox-grind-1.0.0.jar`. You do not need that jar for daily play. `Run-CoXGrind.bat` is the way to play.

## What this plugin does not do

No boss overlays, prayer helpers, tile markers, attack timers, or anything that plays the raid for you. It only reads raid chat, the raid clock, points, and when Olm's hands go down, then saves the result.
