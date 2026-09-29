package com.coxgrind.log;

import com.coxgrind.model.VanguardSample;
import com.coxgrind.model.VanguardUptime;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Study log for Challenge Mode Vanguards. Separate from the raid file.
 * The key matches the raid log, so a leagues world writes {@code vanguards-<hash>-league.json}.
 */
public final class VanguardLogStore
{
	private static final Logger log = LoggerFactory.getLogger(VanguardLogStore.class);
	private static final Gson GSON = new GsonBuilder().create();
	private static final Type SAMPLE_LIST = new TypeToken<List<VanguardSample>>()
	{
	}.getType();

	private final Path root;
	private final Map<String, List<VanguardSample>> memory = new HashMap<>();
	private final Object ioLock = new Object();
	private final Object writes = new Object();
	private ExecutorService writer;
	private int pending;

	public VanguardLogStore(Path root)
	{
		this.root = root;
	}

	public Path file(String accountHash)
	{
		return root.resolve(jsonName(accountHash));
	}

	public static String jsonName(String accountHash)
	{
		return "vanguards-" + RaidLogStore.safe(accountHash) + ".json";
	}

	public void save(String accountHash, VanguardSample sample) throws IOException
	{
		write(accountHash, remember(accountHash, sample));
	}

	/**
	 * Insert or replace one room in memory before returning. The file is written on a background thread.
	 * {@link #flush()} waits for that write.
	 */
	public void saveLater(String accountHash, VanguardSample sample) throws IOException
	{
		queue(accountHash, remember(accountHash, sample));
	}

	public void remove(String accountHash, String id) throws IOException
	{
		List<VanguardSample> next = forget(accountHash, id);
		if (next != null)
		{
			write(accountHash, next);
		}
	}

	/** Same replacement as {@link #remove}, written on the background thread. */
	public void removeLater(String accountHash, String id) throws IOException
	{
		List<VanguardSample> next = forget(accountHash, id);
		if (next != null)
		{
			queue(accountHash, next);
		}
	}

	/** Wait until queued vanguard-file writes have finished. */
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

	public synchronized List<VanguardSample> load(String accountHash) throws IOException
	{
		return new ArrayList<>(samples(accountHash));
	}

	private synchronized List<VanguardSample> remember(String accountHash, VanguardSample sample) throws IOException
	{
		if (accountHash == null || accountHash.isEmpty())
		{
			throw new IOException("Vanguard sample is missing an account hash.");
		}
		if (sample == null || sample.getId() == null || sample.getId().isEmpty())
		{
			throw new IOException("Vanguard sample is missing an id.");
		}
		List<VanguardSample> current = samples(accountHash);
		List<VanguardSample> next = new ArrayList<>(current.size() + 1);
		boolean replaced = false;
		for (int i = 0; i < current.size(); i++)
		{
			VanguardSample existing = current.get(i);
			if (!replaced && sample.getId().equals(existing.getId()))
			{
				next.add(sample);
				replaced = true;
			}
			else
			{
				next.add(existing);
			}
		}
		if (!replaced)
		{
			next.add(sample);
		}
		memory.put(accountHash, next);
		return next;
	}

	/** @return the list to write, or null when the id was not stored */
	private synchronized List<VanguardSample> forget(String accountHash, String id) throws IOException
	{
		if (accountHash == null || accountHash.isEmpty() || id == null || id.isEmpty())
		{
			return null;
		}
		List<VanguardSample> current = samples(accountHash);
		List<VanguardSample> next = new ArrayList<>(current.size());
		boolean removed = false;
		for (int i = 0; i < current.size(); i++)
		{
			VanguardSample existing = current.get(i);
			if (id.equals(existing.getId()))
			{
				removed = true;
			}
			else
			{
				next.add(existing);
			}
		}
		if (!removed)
		{
			return null;
		}
		memory.put(accountHash, next);
		return next;
	}

	private synchronized List<VanguardSample> samples(String accountHash) throws IOException
	{
		List<VanguardSample> cached = memory.get(accountHash);
		if (cached != null)
		{
			return cached;
		}
		List<VanguardSample> loaded = readFile(accountHash);
		memory.put(accountHash, loaded);
		return loaded;
	}

	private List<VanguardSample> readFile(String accountHash) throws IOException
	{
		Path json = file(accountHash);
		if (!Files.exists(json))
		{
			return new ArrayList<>();
		}
		String text = new String(Files.readAllBytes(json), StandardCharsets.UTF_8).trim();
		if (text.isEmpty())
		{
			return new ArrayList<>();
		}
		List<VanguardSample> samples = GSON.fromJson(text, SAMPLE_LIST);
		return samples == null ? new ArrayList<VanguardSample>() : samples;
	}

	private void queue(final String accountHash, final List<VanguardSample> snapshot)
	{
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
						log.warn("Could not write the Cox Grind vanguard log", ex);
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
						Thread thread = new Thread(runnable, "coxgrind-vanguards");
						thread.setDaemon(true);
						return thread;
					}
				});
			}
			return writer;
		}
	}

	private void write(String accountHash, List<VanguardSample> samples) throws IOException
	{
		synchronized (ioLock)
		{
			writeFile(accountHash, samples);
		}
	}

	private void writeFile(String accountHash, List<VanguardSample> samples) throws IOException
	{
		Files.createDirectories(root);
		Path file = file(accountHash);
		Path temp = file.resolveSibling(file.getFileName().toString() + ".tmp");
		Files.write(temp, toPretty(samples).getBytes(StandardCharsets.UTF_8));
		try
		{
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException ex)
		{
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	static String toPretty(List<VanguardSample> samples)
	{
		StringBuilder out = new StringBuilder();
		out.append("[\n");
		if (samples != null)
		{
			for (int i = 0; i < samples.size(); i++)
			{
				out.append(sampleJson(samples.get(i)));
				if (i + 1 < samples.size())
				{
					out.append(',');
				}
				out.append('\n');
			}
		}
		out.append("]\n");
		return out.toString();
	}

	private static String sampleJson(VanguardSample sample)
	{
		List<String> lines = new ArrayList<>();
		lines.add("    \"id\": " + quote(sample.getId()));
		lines.add("    \"startedAt\": " + quote(sample.getStartedAt()));
		lines.add("    \"emergedAt\": " + quote(sample.getEmergedAt()));
		lines.add("    \"world\": " + sample.getWorld());
		lines.add("    \"teamSize\": " + sample.getTeamSize());
		lines.add("    \"kc\": " + (sample.getKc() == null ? "null" : sample.getKc().toString()));
		lines.add("    \"confirmed\": " + sample.isConfirmed());
		lines.add("    \"raidClockEmerge\": " + sample.getRaidClockEmerge());
		lines.add("    \"raidClockComplete\": " + clock(sample.getRaidClockComplete()));
		lines.add("    \"finished\": " + sample.isFinished());
		lines.add("    \"deaths\": " + sample.getDeaths());
		lines.add("    \"uptimes\": " + uptimesJson(sample.getUptimes()));
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

	private static String uptimesJson(List<VanguardUptime> uptimes)
	{
		if (uptimes == null || uptimes.isEmpty())
		{
			return "[]";
		}
		StringBuilder out = new StringBuilder("[\n");
		for (int i = 0; i < uptimes.size(); i++)
		{
			VanguardUptime uptime = uptimes.get(i);
			out.append("      {\"index\": ").append(uptime.getIndex())
				.append(", \"melee\": ").append(quote(uptime.getMelee()))
				.append(", \"ranged\": ").append(quote(uptime.getRanged()))
				.append(", \"magic\": ").append(quote(uptime.getMagic()))
				.append(", \"durationTicks\": ").append(uptime.getDurationTicks())
				.append(", \"gapTicks\": ").append(uptime.getGapTicks() == null ? "null" : uptime.getGapTicks().toString())
				.append(", \"meleeRatio\": ").append(uptime.getMeleeRatio())
				.append(", \"meleeScale\": ").append(uptime.getMeleeScale())
				.append(", \"rangedRatio\": ").append(uptime.getRangedRatio())
				.append(", \"rangedScale\": ").append(uptime.getRangedScale())
				.append(", \"magicRatio\": ").append(uptime.getMagicRatio())
				.append(", \"magicScale\": ").append(uptime.getMagicScale())
				.append(", \"heal\": ").append(uptime.isHeal())
				.append(", \"forced\": ").append(uptime.isForced())
				.append(", \"meleeOffHole\": ").append(uptime.isMeleeOffHole())
				.append(", \"animation\": ").append(uptime.getAnimation())
				.append(", \"killed\": ").append(uptime.isKilled())
				.append(", \"interrupted\": ").append(uptime.isInterrupted())
				.append('}');
			if (i + 1 < uptimes.size())
			{
				out.append(',');
			}
			out.append('\n');
		}
		out.append("    ]");
		return out.toString();
	}

	private static String clock(int units)
	{
		return units < 0 ? "null" : Integer.toString(units);
	}

	private static String quote(String value)
	{
		return GSON.toJson(value == null ? "" : value);
	}
}
