package com.coxgrind;

import com.coxgrind.log.AccountLog;
import com.coxgrind.log.RaidLogStore;
import com.coxgrind.log.VanguardLogStore;
import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.TargetSheet;
import com.coxgrind.model.VanguardSample;
import com.coxgrind.track.CoxRaidSession;
import com.coxgrind.track.OlmNpcs;
import com.coxgrind.track.RaidChat;
import com.coxgrind.report.TargetSettings;
import com.coxgrind.track.VanguardNpcs;
import com.coxgrind.track.VanguardTracker;
import com.coxgrind.ui.ConfigItemIcons;
import com.coxgrind.ui.CoxGrindPanel;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Cox Grind",
	description = "Store and visualize Chambers of Xeric raid data. View live splits, compare them to your average or target times, and see statistics for points, items, and purples.",
	tags = {"cox", "chambers", "xeric", "raids", "analytics"},
	enabledByDefault = true
)
public class CoxGrindPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CoxGrindPlugin.class);

	/** Raid clock. 100 units is one minute on the in-game raid timer. */
	static final int RAID_TIMER = 6386;
	/** Players in the current raid party, including you. */
	static final int PARTY_SIZE = 5424;
	/** Live Challenge Mode flag. The saved raid still uses the kill-count sentence. */
	static final int CHALLENGE_MODE = 6385;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private CoxGrindConfig config;

	@Inject
	private net.runelite.client.game.ItemManager itemManager;

	private final CoxRaidSession session = new CoxRaidSession();
	private final RaidLogStore store = new RaidLogStore(RaidLogStore.defaultRoot());
	private final VanguardTracker vanguards = new VanguardTracker();
	private final VanguardLogStore vanguardStore = new VanguardLogStore(RaidLogStore.defaultRoot());

	private CoxGrindPanel panel;
	private NavigationButton navButton;
	private ConfigItemIcons configIcons;
	/** Ticks to wait for the kill-count chat line before writing the raid anyway. */
	private static final int SAVE_WAIT_TICKS = 40;

	private boolean inRaid;
	private int saveWaitTicks;
	private RaidChat.KillCount earlyKillCount;
	/** The next friends-chat lines are {@code Name - Item} rows for this raid's purples. */
	private boolean purpleListOpen;
	/** File key of the account currently logged in. Stays set on the login screen. */
	private String loggedInKey;
	/** File key captured when this raid started. Later writes keep using it. */
	private String raidKey;
	/** File key captured for the vanguard room open now. */
	private String vanguardKey;
	private boolean missingAccountWarned;
	private boolean resettingTargets;
	/** Ticks after login to catch an account hash that is not ready on the first tick. */
	private int accountLookTicks;
	/** Last live list pushed to the panel. Skips a redraw when nothing on screen changed. */
	private int publishedGeneration = -1;
	private int publishedOpen = Integer.MIN_VALUE;

	@Provides
	CoxGrindConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoxGrindConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new CoxGrindPanel(store, config, configManager, itemManager);
		navButton = NavigationButton.builder()
			.tooltip("Cox Grind")
			.icon(icon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		TargetSettings.forgetTotals(configManager);
		configIcons = new ConfigItemIcons(config, this::resetTargetSheet);
		configIcons.start();
		accountLookTicks = 20;
		clientThread.invokeLater(this::refreshAccount);
	}

	@Override
	protected void shutDown()
	{
		try
		{
			// A finished raid that is still unsaved is written, then the file write is allowed to finish.
			saveVanguard(vanguards.finish(false, timerUnits()));
			if (session.isComplete() && !session.isSaved())
			{
				writeRaid();
			}
			vanguardStore.flush();
			store.flush();
		}
		finally
		{
			if (configIcons != null)
			{
				configIcons.stop();
				configIcons = null;
			}
			if (navButton != null)
			{
				clientToolbar.removeNavigation(navButton);
			}
			session.reset();
			inRaid = false;
			saveWaitTicks = 0;
			earlyKillCount = null;
			purpleListOpen = false;
			loggedInKey = null;
			raidKey = null;
			vanguardKey = null;
			missingAccountWarned = false;
			accountLookTicks = 0;
			publishedGeneration = -1;
			publishedOpen = Integer.MIN_VALUE;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (resettingTargets || event.getGroup() == null || !"coxgrind".equals(event.getGroup()))
		{
			return;
		}
		if (TargetSettings.isTotalKey(event.getKey()))
		{
			return;
		}
		resettingTargets = true;
		try
		{
			TargetSettings.forgetTotals(configManager);
		}
		finally
		{
			resettingTargets = false;
		}
		if (panel != null)
		{
			panel.reload();
		}
		if (configIcons != null)
		{
			configIcons.refreshTotals();
		}
	}

	private void resetTargetSheet(TargetSheet sheet)
	{
		resettingTargets = true;
		try
		{
			TargetSettings.reset(configManager, sheet);
		}
		finally
		{
			resettingTargets = false;
		}
		if (panel != null)
		{
			panel.reload();
		}
		if (configIcons != null)
		{
			configIcons.showSheet(sheet);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			accountLookTicks = 20;
			refreshAccount();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			saveVanguard(vanguards.finish(false, timerUnits()));
			vanguardKey = null;
			if (session.isComplete() && !session.isSaved())
			{
				writeRaid();
			}
			if (!session.isComplete())
			{
				session.reset();
				raidKey = null;
				saveWaitTicks = 0;
				earlyKillCount = null;
				purpleListOpen = false;
				publishRecent();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (accountLookTicks > 0)
		{
			accountLookTicks--;
			refreshAccount();
		}
		if (session.isRunning() && !session.isComplete())
		{
			session.notePoints(personalPoints());
			if (!session.isSaved())
			{
				publishLiveIfNeeded();
			}
		}
		if (config.trackVanguards() && session.isRunning() && !session.isComplete())
		{
			if (client.getVarbitValue(CHALLENGE_MODE) != 0)
			{
				vanguards.allowChallengeMode();
			}
			if (vanguards.isTiming())
			{
				vanguards.tick();
				if (vanguards.needsHealth())
				{
					for (NPC npc : client.getNpcs())
					{
						String style = VanguardNpcs.style(npc.getId());
						if (style != null && npc.getHealthScale() > 0)
						{
							vanguards.seen(style, npc.getHealthRatio(), npc.getHealthScale());
						}
					}
				}
			}
		}
		if (!session.isComplete() || session.isSaved() || saveWaitTicks <= 0)
		{
			return;
		}
		session.updateScore(personalPoints(), teamPoints(), partySize());
		if (!session.isSaved())
		{
			publishLiveIfNeeded();
		}
		saveWaitTicks--;
		tryWrite(saveWaitTicks <= 0);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.RAIDS_DIED)
		{
			session.raiseDeathCount(event.getValue());
			return;
		}
		if (event.getVarbitId() != VarbitID.RAIDS_CLIENT_INDUNGEON)
		{
			return;
		}
		inRaid = event.getValue() == 1;
		if (!inRaid && session.isRunning() && !session.isComplete())
		{
			saveVanguard(vanguards.finish(false, timerUnits()));
			session.reset();
			raidKey = null;
			vanguardKey = null;
			saveWaitTicks = 0;
			earlyKillCount = null;
			purpleListOpen = false;
			publishRecent();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.FRIENDSCHATNOTIFICATION)
		{
			return;
		}
		String message = RaidChat.plain(event.getMessage());
		if (!inRaid && !session.isRunning() && !RaidChat.isRaidStart(message))
		{
			return;
		}
		handleMessage(message);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (trackingOlm() && OlmNpcs.isMageHand(npc.getId()))
		{
			session.olmPhaseStarted(timerUnits());
			publishLiveIfNeeded();
		}
		vanguardSpawned(npc);
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		String style = VanguardNpcs.style(npc.getId());
		if (style != null)
		{
			vanguardSpawned(npc);
			return;
		}
		if (npc.getId() == VanguardNpcs.WALKING && event.getOld() != null)
		{
			String from = VanguardNpcs.style(event.getOld().getId());
			if (from != null)
			{
				vanguardDig(from, npc);
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!watchingVanguards())
		{
			return;
		}
		if (VanguardNpcs.style(event.getNpc().getId()) != null)
		{
			vanguards.combatGone();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!watchingVanguards() || !(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		if (VanguardNpcs.isVanguard(npc.getId()) && event.getHitsplat().getHitsplatType() == HitsplatID.HEAL)
		{
			vanguards.heal();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!trackingOlm())
		{
			return;
		}
		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}
		int id = ((NPC) actor).getId();
		int timer = timerUnits();
		if (OlmNpcs.isMageHand(id))
		{
			session.mageHandDown(timer);
		}
		else if (OlmNpcs.isMeleeHand(id))
		{
			session.meleeHandDown(timer);
		}
		else
		{
			return;
		}
		publishLiveIfNeeded();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!trackingOlm())
		{
			return;
		}
		if (event.getGameObject().getId() == OlmNpcs.ENCOUNTER_OBJECT)
		{
			session.olmPhaseStarted(timerUnits());
			publishLiveIfNeeded();
		}
	}

	private void handleMessage(String message)
	{
		try
		{
			if (RaidChat.isRaidStart(message))
			{
				if (session.isComplete() && !session.isSaved())
				{
					writeRaid();
				}
				saveVanguard(vanguards.finish(false, timerUnits()));
				earlyKillCount = null;
				saveWaitTicks = 0;
				purpleListOpen = false;
				missingAccountWarned = false;
				session.startRaid();
				session.setTeamSize(partySize());
				bindRaidAccount();
				if (config.trackVanguards())
				{
					vanguards.startRaid(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(), partySize());
					if (client.getVarbitValue(CHALLENGE_MODE) != 0)
					{
						vanguards.allowChallengeMode();
					}
				}
				else
				{
					vanguards.discard();
				}
				return;
			}

			if (RaidChat.isPurpleHeader(message))
			{
				purpleListOpen = true;
				return;
			}

			RaidChat.LootDrop drop = RaidChat.lootDrop(message);
			RaidChat.LootDrop side = RaidChat.sideReward(message);
			if (drop == null && side == null && purpleListOpen)
			{
				drop = RaidChat.purpleRecipient(message);
				if (drop == null)
				{
					side = RaidChat.sideRecipient(message);
				}
				if (drop == null && side == null)
				{
					purpleListOpen = false;
				}
			}
			if (drop != null && session.isRunning())
			{
				if (drop.isOwnDrop(playerName()))
				{
					session.setPurple(drop.getItem());
				}
				else
				{
					session.addPartyPurple(drop.getPlayerName(), drop.getItem());
				}
				if (session.isSaved())
				{
					updateSaved();
				}
			}
			else if (side != null && session.isRunning() && side.isOwnDrop(playerName()))
			{
				session.addExtra(side.getItem());
				if (session.isSaved())
				{
					updateSaved();
				}
			}

			if (RaidChat.isPlayerDeath(message))
			{
				session.recordDeath(personalPoints());
				vanguards.noteDeaths(session.snapshot().getDeaths());
			}

			RaidChat.KillCount count = RaidChat.killCount(message);
			if (count != null)
			{
				if (count.isChallengeMode())
				{
					saveVanguard(vanguards.confirm(count.getKc()));
				}
				else
				{
					dropVanguard(vanguards.discard());
				}
				if (session.isComplete())
				{
					session.setKillCount(count.getKc(), count.isChallengeMode());
					if (session.isSaved())
					{
						updateSaved();
					}
					else
					{
						tryWrite(false);
					}
				}
				else if (session.isRunning())
				{
					earlyKillCount = count;
				}
			}

			if (!session.isRunning() || session.isComplete())
			{
				return;
			}

			String room = RaidChat.roomName(message);
			if (room != null)
			{
				if ("Vanguards".equals(room))
				{
					vanguards.noteDeaths(session.snapshot().getDeaths());
					saveVanguard(vanguards.finish(true, timerUnits()));
				}
				session.completeRoom(room, timerUnits());
				return;
			}
			String level = RaidChat.levelName(message);
			if (level != null)
			{
				session.completeLevel(level, timerUnits());
				return;
			}
			if (RaidChat.isRaidComplete(message))
			{
				session.raiseDeathCount(client.getVarbitValue(VarbitID.RAIDS_DIED));
				session.raidCompleted(timerUnits(), personalPoints(), teamPoints(), partySize());
				if (earlyKillCount != null)
				{
					session.setKillCount(earlyKillCount.getKc(), earlyKillCount.isChallengeMode());
					earlyKillCount = null;
				}
				saveWaitTicks = SAVE_WAIT_TICKS;
				tryWrite(false);
			}
		}
		catch (Exception ex)
		{
			log.warn("Cox Grind could not read a raid message", ex);
		}
		finally
		{
			if (session.isRunning() && !session.isSaved())
			{
				publishLiveIfNeeded();
			}
		}
	}

	private boolean trackingOlm()
	{
		return session.isRunning() && !session.isComplete();
	}

	private boolean watchingVanguards()
	{
		if (!config.trackVanguards() || !session.isRunning() || session.isComplete())
		{
			return false;
		}
		if (client.getVarbitValue(CHALLENGE_MODE) != 0)
		{
			vanguards.allowChallengeMode();
		}
		return vanguards.isChallengeMode();
	}

	private void vanguardSpawned(NPC npc)
	{
		String style = VanguardNpcs.style(npc.getId());
		if (style == null || !watchingVanguards())
		{
			return;
		}
		int[] at = center(npc);
		if (at == null)
		{
			return;
		}
		if (vanguards.needsContact())
		{
			vanguards.contact(
				Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
				client.getWorld(),
				partySize(),
				timerUnits(),
				session.snapshot().getDeaths());
		}
		vanguards.emerge(style, at[0], at[1]);
	}

	private void vanguardDig(String style, NPC npc)
	{
		if (!watchingVanguards())
		{
			return;
		}
		int[] at = center(npc);
		if (at == null)
		{
			return;
		}
		int ratio = npc.getHealthRatio();
		int scale = npc.getHealthScale();
		vanguards.dig(style, at[0], at[1], ratio, scale, npc.getAnimation());
	}

	/** Southwest tile plus the center of a 3x3, or whatever size the composition reports. */
	private static int[] center(NPC npc)
	{
		WorldPoint southwest = npc.getWorldLocation();
		if (southwest == null)
		{
			return null;
		}
		int size = 3;
		if (npc.getComposition() != null && npc.getComposition().getSize() > 0)
		{
			size = npc.getComposition().getSize();
		}
		int nudge = Math.max(0, (size - 1) / 2);
		return new int[]{southwest.getX() + nudge, southwest.getY() + nudge};
	}

	private void saveVanguard(VanguardSample sample)
	{
		if (sample == null || vanguardKey == null)
		{
			return;
		}
		try
		{
			vanguardStore.saveLater(vanguardKey, sample);
			log.info("Logged CoX vanguards world {} kc {} uptimes {}", sample.getWorld(), sample.getKc(), sample.getUptimes().size());
		}
		catch (IOException ex)
		{
			log.warn("Could not write the Cox Grind vanguard log", ex);
		}
	}

	private void dropVanguard(String id)
	{
		if (id == null || vanguardKey == null)
		{
			return;
		}
		try
		{
			vanguardStore.removeLater(vanguardKey, id);
		}
		catch (IOException ex)
		{
			log.warn("Could not update the Cox Grind vanguard log", ex);
		}
	}

	/**
	 * Write once the kill count and points are known. {@code force} writes whatever we have,
	 * so a missing kill-count line cannot drop the raid.
	 */
	private void tryWrite(boolean force)
	{
		if (!session.isComplete() || session.isSaved())
		{
			return;
		}
		CoxRaidRecord live = session.snapshot();
		boolean ready = live.getKc() > 0 && live.getPersonalPoints() > 0;
		if (ready || force)
		{
			writeRaid();
		}
	}

	private void writeRaid()
	{
		if (!session.isComplete() || session.isSaved())
		{
			return;
		}
		CoxRaidRecord record = session.snapshot();
		record.settleDeathRooms();
		if (record.getId() == null || record.getId().isEmpty())
		{
			record.setId(UUID.randomUUID().toString());
			record.setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		}
		record.setPlayerName(null);
		record.setAccountHash(null);
		if (raidKey == null)
		{
			if (!missingAccountWarned)
			{
				missingAccountWarned = true;
				log.warn("Cox Grind skipped a raid because the account was not logged in");
			}
			return;
		}
		try
		{
			store.saveLater(raidKey, record);
			session.markSaved(record.getId(), record.getTimestamp());
			saveWaitTicks = 0;
			log.info("Logged CoX raid {} ({}s, {} personal points, kc {})", record.getId(), record.getTotalSeconds(), record.getPersonalPoints(), record.getKc());
			if (panel != null)
			{
				panel.showRecent();
			}
		}
		catch (IOException ex)
		{
			log.warn("Could not write the Cox Grind log", ex);
		}
	}

	private void updateSaved()
	{
		if (!session.isSaved())
		{
			return;
		}
		if (raidKey == null || (loggedInKey != null && !raidKey.equals(loggedInKey)))
		{
			return;
		}
		CoxRaidRecord live = session.snapshot();
		live.settleDeathRooms();
		live.setPlayerName(null);
		live.setAccountHash(null);
		try
		{
			store.saveLater(raidKey, live);
			if (panel != null)
			{
				panel.reload();
			}
		}
		catch (IOException ex)
		{
			log.warn("Could not update the Cox Grind log", ex);
		}
	}

	/**
	 * Push the raid on screen only when the panel is open and the list or the current second changed.
	 * Chat lines and game ticks that do not move the clock stay off the Swing thread.
	 */
	private void publishLiveIfNeeded()
	{
		if (panel == null || !session.isRunning() || session.isSaved())
		{
			return;
		}
		boolean reveal = panel.consumeReveal();
		if (!reveal && !panel.isShown())
		{
			return;
		}
		int open = session.openSegmentSeconds(timerUnits());
		int generation = session.getGeneration();
		if (!reveal && generation == publishedGeneration && open == publishedOpen)
		{
			return;
		}
		publishedGeneration = generation;
		publishedOpen = open;
		panel.showLive(session.snapshot(), open, !session.isComplete());
	}

	private void publishRecent()
	{
		if (panel != null)
		{
			panel.showRecent();
		}
	}

	private void refreshAccount()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return;
		}
		String key = currentAccountKey();
		if (key == null || key.equals(loggedInKey))
		{
			return;
		}
		if (loggedInKey != null)
		{
			detachPreviousAccount();
		}
		loggedInKey = key;
		if (panel != null)
		{
			panel.setAccount(key);
		}
	}

	/** Point the open raid at the account and world that just started it. */
	private void bindRaidAccount()
	{
		String key = currentAccountKey();
		if (key == null)
		{
			key = loggedInKey;
		}
		raidKey = key;
		vanguardKey = key;
		if (key == null)
		{
			return;
		}
		loggedInKey = key;
		if (panel != null)
		{
			panel.setAccount(key);
		}
	}

	/**
	 * The logged-in account or world kind changed. Finish the previous raid into its own file
	 * and drop it, so the next account cannot append to that log.
	 */
	private void detachPreviousAccount()
	{
		if (session.isComplete() && !session.isSaved())
		{
			writeRaid();
		}
		saveVanguard(vanguards.finish(false, timerUnits()));
		session.reset();
		raidKey = null;
		vanguardKey = null;
		saveWaitTicks = 0;
		earlyKillCount = null;
		purpleListOpen = false;
		publishRecent();
	}

	private String currentAccountKey()
	{
		return AccountLog.key(client.getAccountHash(), client.getWorldType());
	}

	private int timerUnits()
	{
		return client.getVarbitValue(RAID_TIMER);
	}

	private int personalPoints()
	{
		return client.getVarpValue(VarPlayerID.RAIDS_PLAYERSCORE);
	}

	private int teamPoints()
	{
		return client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSCORE);
	}

	private int partySize()
	{
		return client.getVarbitValue(PARTY_SIZE);
	}

	private String playerName()
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return "";
		}
		return Text.removeTags(client.getLocalPlayer().getName()).replace('\u00A0', ' ').trim();
	}

	private static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(126, 56, 196));
		g.fillRoundRect(0, 0, 16, 16, 4, 4);
		g.setColor(Color.WHITE);
		g.drawString("C", 4, 12);
		g.dispose();
		return image;
	}
}
