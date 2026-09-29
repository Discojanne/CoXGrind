package com.coxgrind.log;

import com.coxgrind.model.CoxRaidRecord;
import com.coxgrind.model.PartyPurple;
import com.coxgrind.model.RaidDeath;
import com.coxgrind.model.RoomSplit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One pretty JSON file per account key. Each raid is stored once.
 * A leagues world uses {@code account-<hash>-league.json} so it stays out of the main file.
 * A later kill count or purple replaces that raid instead of adding a second line.
 * Older append-only {@code .jsonl} files are read once and rewritten.
 */
public final class RaidLogStore
{
	private static final Logger log = LoggerFactory.getLogger(RaidLogStore.class);
	private static final Gson GSON = new GsonBuilder().create();
	private static final Type RAID_LIST = new TypeToken<List<CoxRaidRecord>>()
	{
	}.getType();

	private final Path root;
	private final Map<String, List<CoxRaidRecord>> memory = new HashMap<>();
	private final Object ioLock = new Object();
	private final Object writes = new Object();
	private ExecutorService writer;
	private int pending;

	public RaidLogStore(Path root)
	{
		this.root = root;
	}

	public static Path defaultRoot()
	{
		return Paths.get(System.getProperty("user.home"), ".runelite", "cox-grind");
	}

	public Path getRoot()
	{
		return root;
	}

	public Path accountFile(String accountHash)
	{
		return root.resolve(jsonName(accountHash));
	}

	public static String jsonName(String accountHash)
	{
		return "account-" + safe(accountHash) + ".json";
	}

	public static String safe(String accountHash)
	{
		String raw = accountHash == null ? "" : accountHash;
		String cleaned = raw.replaceAll("[^0-9A-Za-z_-]", "");
		if (cleaned.isEmpty())
		{
			cleaned = "unknown";
		}
		return cleaned;
	}

	/**
	 * Insert or replace one raid in memory, then rewrite the account file before returning.
	 */
	public void save(String accountHash, CoxRaidRecord record) throws IOException
	{
		write(accountHash, remember(accountHash, record));
	}

	/**
	 * Insert or replace one raid in memory before returning. The file is written on a background thread.
	 * {@link #flush()} waits for that write.
	 */
	public void saveLater(String accountHash, CoxRaidRecord record) throws IOException
	{
		final List<CoxRaidRecord> snapshot = remember(accountHash, record);
		synchronized (writes)
		{
			pending++;
		}
		try
		{
			writer().execute(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						write(accountHash, snapshot);
					}
					catch (IOException ex)
					{
						log.warn("Could not write the Cox Grind log", ex);
					}
					finally
					{
						synchronized (writes)
						{
							pending--;
							writes.notifyAll();
						}
					}
				}
			});
		}
		catch (RuntimeException ex)
		{
			synchronized (writes)
			{
				pending--;
				writes.notifyAll();
			}
			throw ex;
		}
	}

	/** Wait until queued account-file writes have finished. */
	public void flush()
	{
		boolean interrupted = false;
		synchronized (writes)
		{
			while (pending > 0)
			{
				try
				{
					writes.wait();
				}
				catch (InterruptedException ex)
				{
					interrupted = true;
				}
			}
		}
		if (interrupted)
		{
			Thread.currentThread().interrupt();
		}
	}

	public synchronized List<CoxRaidRecord> load(String accountHash) throws IOException
	{
		List<CoxRaidRecord> cached = memory.get(accountHash);
		if (cached == null)
		{
			cached = readAccount(accountHash);
			memory.put(accountHash, cached);
		}
		return Collections.unmodifiableList(new ArrayList<>(cached));
	}

	private synchronized List<CoxRaidRecord> remember(String accountHash, CoxRaidRecord record) throws IOException
	{
		if (accountHash == null || accountHash.isEmpty())
		{
			throw new IOException("Raid is missing an account hash.");
		}
		if (record == null || record.getId() == null || record.getId().isEmpty())
		{
			throw new IOException("Raid is missing an id.");
		}
		List<CoxRaidRecord> current = memory.get(accountHash);
		if (current == null)
		{
			current = readAccount(accountHash);
		}
		List<CoxRaidRecord> next = new ArrayList<>(current.size() + 1);
		boolean replaced = false;
		for (int i = 0; i < current.size(); i++)
		{
			CoxRaidRecord existing = current.get(i);
			if (!replaced && record.getId().equals(existing.getId()))
			{
				next.add(record);
				replaced = true;
			}
			else
			{
				next.add(existing);
			}
		}
		if (!replaced)
		{
			next.add(record);
		}
		memory.put(accountHash, next);
		return next;
	}

	private List<CoxRaidRecord> readAccount(String accountHash) throws IOException
	{
		Path json = accountFile(accountHash);
		Path jsonl = root.resolve("account-" + safe(accountHash) + ".jsonl");
		if (Files.exists(json))
		{
			List<CoxRaidRecord> raids = readJson(json);
			if (Files.exists(jsonl))
			{
				Files.delete(jsonl);
			}
			return raids;
		}
		if (!Files.exists(jsonl))
		{
			return new ArrayList<>();
		}
		List<CoxRaidRecord> raids = readJsonl(jsonl);
		write(accountHash, raids);
		Files.delete(jsonl);
		return raids;
	}

	private ExecutorService writer()
	{
		synchronized (writes)
		{
			if (writer == null)
			{
				writer = Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory()
				{
					@Override
					public Thread newThread(Runnable runnable)
					{
						Thread thread = new Thread(runnable, "coxgrind-log");
						thread.setDaemon(true);
						return thread;
					}
				});
			}
			return writer;
		}
	}

	/**
	 * Account hash of the newest raid file, or null when the folder is empty.
	 */
	public synchronized String latestAccountHash()
	{
		if (!Files.isDirectory(root))
		{
			return null;
		}
		Path newest = null;
		FileTime newestTime = null;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(root, "account-*"))
		{
			for (Path path : stream)
			{
				String name = path.getFileName().toString();
				if (!name.endsWith(".json") && !name.endsWith(".jsonl"))
				{
					continue;
				}
				FileTime modified = Files.getLastModifiedTime(path);
				if (newest == null || modified.compareTo(newestTime) > 0)
				{
					newest = path;
					newestTime = modified;
				}
			}
		}
		catch (IOException ex)
		{
			return null;
		}
		return hashFromName(newest);
	}

	static String hashFromName(Path path)
	{
		if (path == null)
		{
			return null;
		}
		String name = path.getFileName().toString();
		String suffix = name.endsWith(".jsonl") ? ".jsonl" : ".json";
		if (!name.startsWith("account-") || !name.endsWith(suffix))
		{
			return null;
		}
		String hash = name.substring("account-".length(), name.length() - suffix.length());
		return hash.isEmpty() ? null : hash;
	}

	private static List<CoxRaidRecord> readJson(Path file) throws IOException
	{
		String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
		if (text.isEmpty())
		{
			return new ArrayList<>();
		}
		List<CoxRaidRecord> raids = GSON.fromJson(text, RAID_LIST);
		if (raids == null)
		{
			return new ArrayList<>();
		}
		for (int i = 0; i < raids.size(); i++)
		{
			tidy(raids.get(i));
		}
		return raids;
	}

	private static List<CoxRaidRecord> readJsonl(Path file) throws IOException
	{
		List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		Map<String, CoxRaidRecord> byId = new LinkedHashMap<>();
		List<CoxRaidRecord> order = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++)
		{
			String text = lines.get(i).trim();
			if (text.isEmpty())
			{
				continue;
			}
			LegacyLine line = GSON.fromJson(text, LegacyLine.class);
			if (line == null || line.type == null)
			{
				continue;
			}
			if ("raid".equals(line.type) && line.raid != null)
			{
				tidy(line.raid);
				order.add(line.raid);
				if (line.raid.getId() != null)
				{
					byId.put(line.raid.getId(), line.raid);
				}
			}
			else if ("patch".equals(line.type))
			{
				CoxRaidRecord raid = line.id == null ? null : byId.get(line.id);
				if (raid != null)
				{
					applyPatch(raid, line);
				}
			}
		}
		return order;
	}

	private static void tidy(CoxRaidRecord raid)
	{
		if (raid == null)
		{
			return;
		}
		raid.setTimestamp(normalizeTime(raid.getTimestamp()));
		raid.setPlayerName(null);
		raid.setAccountHash(null);
		int completed = raid.secondsFor("Raid Completed");
		if (completed >= 0)
		{
			raid.setTotalSeconds(completed);
		}
	}

	static String normalizeTime(String timestamp)
	{
		if (timestamp == null || timestamp.isEmpty())
		{
			return timestamp;
		}
		try
		{
			return Instant.parse(timestamp).truncatedTo(ChronoUnit.SECONDS).toString();
		}
		catch (Exception ex)
		{
			return timestamp;
		}
	}

	private static void applyPatch(CoxRaidRecord raid, LegacyLine line)
	{
		if (line.kc != null && line.kc > 0)
		{
			raid.setKc(line.kc);
		}
		if (line.challengeMode != null)
		{
			raid.setChallengeMode(line.challengeMode);
		}
		if (line.purple != null && line.purple.length() > 0)
		{
			raid.setPurple(line.purple);
		}
		if (line.personalPoints != null && line.personalPoints > 0)
		{
			raid.setPersonalPoints(line.personalPoints);
		}
		if (line.teamPoints != null && line.teamPoints > 0)
		{
			raid.setTeamPoints(line.teamPoints);
		}
		if (line.teamSize != null && line.teamSize > 0)
		{
			raid.setTeamSize(line.teamSize);
		}
		if (line.partyPurples != null)
		{
			for (int i = 0; i < line.partyPurples.size(); i++)
			{
				PartyPurple party = line.partyPurples.get(i);
				if (party != null)
				{
					raid.addPartyPurple(party.getPlayerName(), party.getItem());
				}
			}
		}
		if (line.deaths != null && line.deaths > raid.getDeaths())
		{
			raid.setDeaths(line.deaths);
		}
	}

	private void write(String accountHash, List<CoxRaidRecord> raids) throws IOException
	{
		synchronized (ioLock)
		{
			writeFile(accountHash, raids);
		}
	}

	private void writeFile(String accountHash, List<CoxRaidRecord> raids) throws IOException
	{
		Files.createDirectories(root);
		Path file = accountFile(accountHash);
		Path temp = file.resolveSibling(file.getFileName().toString() + ".tmp");
		Files.write(temp, toPretty(raids).getBytes(StandardCharsets.UTF_8));
		try
		{
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException ex)
		{
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	static String toPretty(List<CoxRaidRecord> raids)
	{
		StringBuilder out = new StringBuilder();
		out.append("[\n");
		if (raids != null)
		{
			for (int i = 0; i < raids.size(); i++)
			{
				out.append(raidJson(raids.get(i)));
				if (i + 1 < raids.size())
				{
					out.append(',');
				}
				out.append('\n');
			}
		}
		out.append("]\n");
		return out.toString();
	}

	private static String raidJson(CoxRaidRecord raid)
	{
		List<String> lines = new ArrayList<>();
		lines.add("    \"id\": " + quote(raid.getId()));
		lines.add("    \"timestamp\": " + quote(normalizeTime(raid.getTimestamp())));
		lines.add("    \"challengeMode\": " + raid.isChallengeMode());
		lines.add("    \"kc\": " + raid.getKc());
		lines.add("    \"teamSize\": " + raid.getTeamSize());
		lines.add("    \"deaths\": " + raid.getDeaths());
		if (!raid.getDeathList().isEmpty())
		{
			lines.add("    \"deathList\": " + deathsJson(raid.getDeathList()));
		}
		lines.add("    \"personalPoints\": " + raid.getPersonalPoints());
		lines.add("    \"teamPoints\": " + raid.getTeamPoints());
		if (raid.secondsFor("Raid Completed") < 0)
		{
			lines.add("    \"totalSeconds\": " + raid.getTotalSeconds());
		}
		lines.add("    \"splits\": " + splitsJson(raid.getSplits()));
		if (raid.hasPurple())
		{
			lines.add("    \"purple\": " + quote(raid.getPurple()));
		}
		if (!raid.getExtras().isEmpty())
		{
			lines.add("    \"extras\": " + extrasJson(raid.getExtras()));
		}
		if (!raid.getPartyPurples().isEmpty())
		{
			lines.add("    \"partyPurples\": " + partyJson(raid.getPartyPurples()));
		}
		StringBuilder out = new StringBuilder();
		out.append("  {\n");
		for (int i = 0; i < lines.size(); i++)
		{
			out.append(lines.get(i));
			if (i + 1 < lines.size())
			{
				out.append(',');
			}
			out.append('\n');
		}
		out.append("  }");
		return out.toString();
	}

	private static String splitsJson(List<RoomSplit> splits)
	{
		if (splits == null || splits.isEmpty())
		{
			return "[]";
		}
		StringBuilder out = new StringBuilder("[\n");
		for (int i = 0; i < splits.size(); i++)
		{
			RoomSplit split = splits.get(i);
			out.append("      {\"room\": ").append(quote(split.getRoom()))
				.append(", \"seconds\": ").append(split.getSeconds()).append('}');
			if (i + 1 < splits.size())
			{
				out.append(',');
			}
			out.append('\n');
		}
		out.append("    ]");
		return out.toString();
	}

	private static String deathsJson(List<RaidDeath> deaths)
	{
		StringBuilder out = new StringBuilder("[\n");
		for (int i = 0; i < deaths.size(); i++)
		{
			RaidDeath death = deaths.get(i);
			out.append("      {");
			if (death.getRoom().length() > 0)
			{
				out.append("\"room\": ").append(quote(death.getRoom())).append(", ");
			}
			out.append("\"pointsLost\": ").append(death.getPointsLost()).append('}');
			if (i + 1 < deaths.size())
			{
				out.append(',');
			}
			out.append('\n');
		}
		out.append("    ]");
		return out.toString();
	}

	private static String extrasJson(List<String> extras)
	{
		StringBuilder out = new StringBuilder("[");
		for (int i = 0; i < extras.size(); i++)
		{
			if (i > 0)
			{
				out.append(", ");
			}
			out.append(quote(extras.get(i)));
		}
		out.append(']');
		return out.toString();
	}

	private static String partyJson(List<PartyPurple> parties)
	{
		StringBuilder out = new StringBuilder("[\n");
		for (int i = 0; i < parties.size(); i++)
		{
			PartyPurple party = parties.get(i);
			out.append("      {\"playerName\": ").append(quote(party.getPlayerName()))
				.append(", \"item\": ").append(quote(party.getItem())).append('}');
			if (i + 1 < parties.size())
			{
				out.append(',');
			}
			out.append('\n');
		}
		out.append("    ]");
		return out.toString();
	}

	private static String quote(String value)
	{
		return GSON.toJson(value == null ? "" : value);
	}

	private static final class LegacyLine
	{
		private String type;
		private CoxRaidRecord raid;
		private String id;
		private Integer kc;
		private Boolean challengeMode;
		private String purple;
		private Integer personalPoints;
		private Integer teamPoints;
		private Integer teamSize;
		private List<PartyPurple> partyPurples;
		private Integer deaths;
	}
}
