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
import java.util.List;

/**
 * Study log for Challenge Mode Vanguards. Separate from {@code account-<hash>.json}.
 */
public final class VanguardLogStore
{
	private static final Gson GSON = new GsonBuilder().create();
	private static final Type SAMPLE_LIST = new TypeToken<List<VanguardSample>>()
	{
	}.getType();

	private final Path root;

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

	public synchronized void save(String accountHash, VanguardSample sample) throws IOException
	{
		if (accountHash == null || accountHash.isEmpty())
		{
			throw new IOException("Vanguard sample is missing an account hash.");
		}
		if (sample == null || sample.getId() == null || sample.getId().isEmpty())
		{
			throw new IOException("Vanguard sample is missing an id.");
		}
		List<VanguardSample> samples = load(accountHash);
		boolean replaced = false;
		for (int i = 0; i < samples.size(); i++)
		{
			if (sample.getId().equals(samples.get(i).getId()))
			{
				samples.set(i, sample);
				replaced = true;
				break;
			}
		}
		if (!replaced)
		{
			samples.add(sample);
		}
		write(accountHash, samples);
	}

	public synchronized void remove(String accountHash, String id) throws IOException
	{
		if (id == null || id.isEmpty())
		{
			return;
		}
		List<VanguardSample> samples = load(accountHash);
		boolean removed = false;
		for (int i = samples.size() - 1; i >= 0; i--)
		{
			if (id.equals(samples.get(i).getId()))
			{
				samples.remove(i);
				removed = true;
			}
		}
		if (removed)
		{
			write(accountHash, samples);
		}
	}

	public synchronized List<VanguardSample> load(String accountHash) throws IOException
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
		return samples == null ? new ArrayList<>() : samples;
	}

	private void write(String accountHash, List<VanguardSample> samples) throws IOException
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
