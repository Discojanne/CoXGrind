package com.coxgrind;

import com.coxgrind.log.RaidLogStore;
import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.track.CoxRaidSession;
import com.coxgrind.track.OlmNpcs;
import com.coxgrind.track.RaidChat;
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
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
	name = "CoXGrind",
	description = "Logs Chambers of Xeric raids locally and shows times, points, and purples in a side panel.",
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

	private CoxGrindPanel panel;
	private NavigationButton navButton;
	/** Ticks to wait for the kill-count chat line before writing the raid anyway. */
	private static final int SAVE_WAIT_TICKS = 40;

	private boolean inRaid;
	private boolean sawPlayer;
	private int saveWaitTicks;
	private RaidChat.KillCount earlyKillCount;
	/** The next friends-chat lines are {@code Name - Item} rows for this raid's purples. */
	private boolean purpleListOpen;

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
			.tooltip("CoXGrind")
			.icon(icon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		clientThread.invokeLater(this::refreshAccount);
	}

	@Override
	protected void shutDown()
	{
		try
		{
			// Same flush as the login screen. A zero hash would write account-0.json, so skip it.
			if (session.isComplete() && !session.isSaved() && client.getAccountHash() != 0)
			{
				writeRaid();
			}
		}
		finally
		{
			if (navButton != null)
			{
				clientToolbar.removeNavigation(navButton);
			}
			session.reset();
			inRaid = false;
			sawPlayer = false;
			saveWaitTicks = 0;
			earlyKillCount = null;
			purpleListOpen = false;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			refreshAccount();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			sawPlayer = false;
			if (session.isComplete() && !session.isSaved())
			{
				writeRaid();
			}
			if (!session.isComplete())
			{
				session.reset();
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
		if (!sawPlayer && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null)
		{
			sawPlayer = true;
			refreshAccount();
		}
		if (session.isRunning() && !session.isComplete() && !session.isSaved())
		{
			publishLive();
		}
		if (!session.isComplete() || session.isSaved() || saveWaitTicks <= 0)
		{
			return;
		}
		session.updateScore(personalPoints(), teamPoints(), partySize());
		if (!session.isSaved())
		{
			publishLive();
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
			session.reset();
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
		if (!trackingOlm())
		{
			return;
		}
		if (OlmNpcs.isMageHand(event.getNpc().getId()))
		{
			session.olmPhaseStarted(timerUnits());
			publishLive();
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
		publishLive();
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
			publishLive();
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
				earlyKillCount = null;
				saveWaitTicks = 0;
				purpleListOpen = false;
				session.startRaid();
				session.setTeamSize(partySize());
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
				session.recordDeath();
			}

			RaidChat.KillCount count = RaidChat.killCount(message);
			if (count != null)
			{
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
			log.warn("CoXGrind could not read a raid message", ex);
		}
		finally
		{
			if (session.isRunning() && !session.isSaved())
			{
				publishLive();
			}
		}
	}

	private boolean trackingOlm()
	{
		return session.isRunning() && !session.isComplete();
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
		if (record.getId() == null || record.getId().isEmpty())
		{
			record.setId(UUID.randomUUID().toString());
			record.setTimestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		}
		record.setPlayerName(null);
		record.setAccountHash(null);
		try
		{
			store.save(accountHash(), record);
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
			log.warn("Could not write the CoXGrind log", ex);
		}
	}

	private void updateSaved()
	{
		if (!session.isSaved())
		{
			return;
		}
		CoxRaidRecord live = session.snapshot();
		live.setPlayerName(null);
		live.setAccountHash(null);
		try
		{
			store.save(accountHash(), live);
			if (panel != null)
			{
				panel.reload();
			}
		}
		catch (IOException ex)
		{
			log.warn("Could not update the CoXGrind log", ex);
		}
	}

	private void publishLive()
	{
		if (panel == null || !session.isRunning() || session.isSaved())
		{
			return;
		}
		panel.showLive(session.snapshot(), session.openSegmentSeconds(timerUnits()), !session.isComplete());
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
		if (panel == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		panel.setAccount(accountHash());
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

	private String accountHash()
	{
		return Long.toUnsignedString(client.getAccountHash());
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
