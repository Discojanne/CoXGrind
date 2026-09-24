package com.coxgrind.track;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the raid's own chat lines. No combat advice is produced from these.
 */
public final class RaidChat
{
	public static final String RAID_START = "The raid has begun!";
	public static final String RAID_COMPLETE = "Congratulations - your raid is complete!";
	public static final String PLAYER_DEATH = "Oh dear, you are dead!";

	private static final Pattern HIGHLIGHT = Pattern.compile("@[A-Za-z0-9_]+@");
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	private static final Pattern ROOM = Pattern.compile(
		"(?:Combat room|Puzzle)\\s+`([^`]+)`\\s+complete",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern LEVEL = Pattern.compile(
		"(Upper|Middle|Lower)\\s+level complete",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern KC_CM = Pattern.compile(
		"Your completed Chambers of Xeric Challenge Mode count is:\\s*([0-9]+)");
	private static final Pattern KC_REGULAR = Pattern.compile(
		"Your completed Chambers of Xeric count is:\\s*([0-9]+)");
	private static final Pattern SPECIAL_LOOT = Pattern.compile(
		"(?i)special loot:\\s*(.+)");
	private static final Pattern NAMED_LOOT = Pattern.compile(
		"(?i)^(.+?)\\s+received (?:a )?special (?:loot|drop):\\s*(.+)");
	private static final Pattern RECIPIENT_LOOT = Pattern.compile(
		"^(.+?)\\s+-\\s+(.+)$");
	private static final Pattern VALUABLE_DROP = Pattern.compile(
		"(?i)valuable drop:\\s*(.+)");

	private static final Map<String, String> PURPLE_ITEMS = new LinkedHashMap<>();
	private static final Map<String, String> SIDE_ITEMS = new LinkedHashMap<>();

	static
	{
		registerPurple("Dexterous prayer scroll");
		registerPurple("Arcane prayer scroll");
		registerPurple("Twisted buckler");
		registerPurple("Dragon hunter crossbow");
		registerPurple("Dinh's bulwark");
		registerPurple("Ancestral hat");
		registerPurple("Ancestral robe top");
		registerPurple("Ancestral robe bottom");
		registerPurple("Dragon claws");
		registerPurple("Elder maul");
		registerPurple("Kodai insignia");
		registerPurple("Twisted bow");
		registerSide("Olmlet");
		registerSide("Metamorphic dust");
		registerSide("Twisted ancestral colour kit");
	}

	private static void registerPurple(String name)
	{
		PURPLE_ITEMS.put(name.toLowerCase(Locale.ENGLISH), name);
	}

	private static void registerSide(String name)
	{
		SIDE_ITEMS.put(name.toLowerCase(Locale.ENGLISH), name);
	}

	/** {@code pet}, {@code kit}, or {@code dust}. Empty when the name is not one of those. */
	public static String sideKind(String name)
	{
		if ("Olmlet".equals(name))
		{
			return "pet";
		}
		if ("Twisted ancestral colour kit".equals(name))
		{
			return "kit";
		}
		if ("Metamorphic dust".equals(name))
		{
			return "dust";
		}
		return "";
	}

	private RaidChat()
	{
	}

	/**
	 * Drops color tags and RuneLite highlight tokens such as {@code @mes_hl_mag@}.
	 * Floor-complete lines arrive with that token, so a start-anchored match misses them.
	 */
	public static String plain(String message)
	{
		if (message == null)
		{
			return "";
		}
		String text = HIGHLIGHT.matcher(message).replaceAll("");
		text = TAG.matcher(text).replaceAll("");
		return text.replace('\u00A0', ' ').trim();
	}

	public static boolean isRaidStart(String message)
	{
		return plain(message).startsWith(RAID_START);
	}

	public static boolean isRaidComplete(String message)
	{
		return plain(message).startsWith(RAID_COMPLETE);
	}

	public static boolean isPlayerDeath(String message)
	{
		return plain(message).startsWith(PLAYER_DEATH);
	}

	public static String roomName(String message)
	{
		message = plain(message);
		if (message.isEmpty())
		{
			return null;
		}
		Matcher matcher = ROOM.matcher(message);
		if (!matcher.find())
		{
			return null;
		}
		return RoomNames.canonical(matcher.group(1));
	}

	/**
	 * @return Upper, Middle, or Lower
	 */
	public static String levelName(String message)
	{
		message = plain(message);
		if (message.isEmpty())
		{
			return null;
		}
		Matcher matcher = LEVEL.matcher(message);
		if (!matcher.find())
		{
			return null;
		}
		String raw = matcher.group(1);
		return raw.substring(0, 1).toUpperCase(Locale.ENGLISH) + raw.substring(1).toLowerCase(Locale.ENGLISH);
	}

	public static KillCount killCount(String message)
	{
		message = plain(message);
		if (message.isEmpty())
		{
			return null;
		}
		Matcher cm = KC_CM.matcher(message);
		if (cm.find())
		{
			return new KillCount(Integer.parseInt(cm.group(1)), true);
		}
		Matcher regular = KC_REGULAR.matcher(message);
		if (regular.find())
		{
			return new KillCount(Integer.parseInt(regular.group(1)), false);
		}
		return null;
	}

	/**
	 * The friends-chat line that introduces the raid's purple list.
	 * The item is on the following line, {@code Name - Item}.
	 */
	public static boolean isPurpleHeader(String message)
	{
		String text = plain(message);
		return "special loot:".equals(text.toLowerCase(Locale.ENGLISH))
			|| "special loot".equals(text.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * A purple chat line. Unnamed "Special loot" and "Valuable drop" belong to the local player.
	 * A named line keeps that player's name, including teammates.
	 * The live friends-chat pair is a bare {@code Special loot:} header, then {@code Name - Item}.
	 * That second line is {@link #purpleRecipient(String)}, and only counts after the header.
	 */
	public static LootDrop lootDrop(String message)
	{
		return namedDrop(message, true);
	}

	/**
	 * One row of the friends-chat purple list: {@code Player name - Twisted bow}.
	 */
	public static LootDrop purpleRecipient(String message)
	{
		message = plain(message);
		if (message.isEmpty())
		{
			return null;
		}
		Matcher recipient = RECIPIENT_LOOT.matcher(message);
		if (!recipient.find())
		{
			return null;
		}
		String item = canonicalPurple(recipient.group(2));
		if (item == null)
		{
			return null;
		}
		return new LootDrop(clean(recipient.group(1)), item);
	}

	/**
	 * Pet, kit, or dust. The same chat shapes as a purple, and never a purple item.
	 */
	public static LootDrop sideReward(String message)
	{
		return namedDrop(message, false);
	}

	/**
	 * One row of the friends-chat list when the item is pet, kit, or dust.
	 */
	public static LootDrop sideRecipient(String message)
	{
		message = plain(message);
		if (message.isEmpty())
		{
			return null;
		}
		Matcher recipient = RECIPIENT_LOOT.matcher(message);
		if (!recipient.find())
		{
			return null;
		}
		String item = canonicalSide(recipient.group(2));
		if (item == null)
		{
			return null;
		}
		return new LootDrop(clean(recipient.group(1)), item);
	}

	private static LootDrop namedDrop(String message, boolean purple)
	{
		message = plain(message);
		if (message.isEmpty() || isPurpleHeader(message))
		{
			return null;
		}
		Matcher named = NAMED_LOOT.matcher(message);
		if (named.find())
		{
			String item = purple ? canonicalPurple(named.group(2)) : canonicalSide(named.group(2));
			if (item == null)
			{
				return null;
			}
			return new LootDrop(clean(named.group(1)), item);
		}
		Matcher special = SPECIAL_LOOT.matcher(message.trim());
		if (special.find())
		{
			String item = purple ? canonicalPurple(special.group(1)) : canonicalSide(special.group(1));
			if (item == null)
			{
				return null;
			}
			return new LootDrop(null, item);
		}
		Matcher valuable = VALUABLE_DROP.matcher(message.trim());
		if (valuable.find())
		{
			String item = purple ? canonicalPurple(valuable.group(1)) : canonicalSide(valuable.group(1));
			if (item == null)
			{
				return null;
			}
			return new LootDrop(null, item);
		}
		return null;
	}

	private static String canonicalPurple(String raw)
	{
		return lookup(PURPLE_ITEMS, raw);
	}

	private static String canonicalSide(String raw)
	{
		return lookup(SIDE_ITEMS, raw);
	}

	private static String lookup(Map<String, String> items, String raw)
	{
		if (raw == null)
		{
			return null;
		}
		String item = raw.trim();
		if (item.endsWith("."))
		{
			item = item.substring(0, item.length() - 1).trim();
		}
		int paren = item.indexOf(" (");
		if (paren > 0)
		{
			item = item.substring(0, paren).trim();
		}
		return items.get(item.toLowerCase(Locale.ENGLISH));
	}

	static boolean samePlayer(String mentioned, String local)
	{
		if (mentioned == null || local == null)
		{
			return false;
		}
		return clean(mentioned).equalsIgnoreCase(clean(local));
	}

	private static String clean(String name)
	{
		return name.replace('\u00A0', ' ').trim();
	}

	public static final class LootDrop
	{
		private final String playerName;
		private final String item;

		public LootDrop(String playerName, String item)
		{
			this.playerName = playerName;
			this.item = item;
		}

		public String getPlayerName()
		{
			return playerName;
		}

		public String getItem()
		{
			return item;
		}

		public boolean isOwnDrop(String localPlayerName)
		{
			return playerName == null || playerName.length() == 0 || samePlayer(playerName, localPlayerName);
		}
	}

	public static final class KillCount
	{
		private final int kc;
		private final boolean challengeMode;

		public KillCount(int kc, boolean challengeMode)
		{
			this.kc = kc;
			this.challengeMode = challengeMode;
		}

		public int getKc()
		{
			return kc;
		}

		public boolean isChallengeMode()
		{
			return challengeMode;
		}
	}
}
