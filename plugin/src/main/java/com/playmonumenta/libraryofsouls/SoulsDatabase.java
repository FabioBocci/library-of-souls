package com.playmonumenta.libraryofsouls;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.libraryofsouls.utils.FileUtils;

import net.md_5.bungee.api.ChatColor;

public class SoulsDatabase {
	private static SoulsDatabase INSTANCE = null;

	private static final Comparator<String> COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String e1, String e2) {
			return e1.toLowerCase().compareTo(e2.toLowerCase());
		}
	};
	private TreeMap<String, SoulEntry> mSouls = new TreeMap<String, SoulEntry>(COMPARATOR);

	public SoulsDatabase(Plugin plugin) throws Exception {
		reload(plugin);

		INSTANCE = this;
	}

	public List<SoulEntry> getSouls(int offset, int count) {
		List<SoulEntry> souls = new ArrayList<SoulEntry>(count);

		for (int i = offset; i < offset + count; i++) {
			SoulEntry bos = getSoul(i);
			if (bos != null) {
				souls.add(bos);
			}
		}

		return souls;
	}

	public SoulEntry getSoul(int index) {
		if (index >= mSouls.size()) {
			return null;
		}

		return (SoulEntry)mSouls.values().toArray()[index];
	}

	public SoulEntry getSoul(String name) {
		SoulEntry soul = mSouls.get(name);
		if (soul != null) {
			return soul;
		}
		return null;
	}

	/* TODO: File watcher */
	public void reload(Plugin plugin) throws Exception {
		plugin.getLogger().info("Parsing souls library...");
		mSouls = new TreeMap<String, SoulEntry>(COMPARATOR);

		File directory = plugin.getDataFolder();
		if (!directory.exists()) {
			directory.mkdirs();
		}

		String content = FileUtils.readFile(Paths.get(plugin.getDataFolder().getPath(), "souls_database.json").toString());
		if (content == null || content.isEmpty()) {
			throw new Exception("Failed to parse file as JSON object");
		}

		Gson gson = new Gson();
		JsonArray array = gson.fromJson(content, JsonArray.class);
		if (array == null) {
			throw new Exception("Failed to parse file as JSON array");
		}

		int count = 0;
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();

			JsonObject obj = entry.getAsJsonObject();

			SoulEntry soul = new SoulEntry(obj);
			String label = soul.getLabel();

			if (mSouls.get(label) != null) {
				plugin.getLogger().severe("Refused to load Library of Souls duplicate mob '" + label + "'");
				continue;
			}

			plugin.getLogger().info("  " + label);

			mSouls.put(label, soul);
			count++;
		}
		plugin.getLogger().info("Finished parsing souls library");
		plugin.getLogger().info("Loaded " + Integer.toString(count) + " mob souls");
	}

	public static SoulsDatabase getInstance() {
		return INSTANCE;
	}

	/*
	 * Valid examples:
	 *   §6Master Scavenger
	 *   "§6Master Scavenger"
	 *   "{\"text\":\"§6Master Scavenger\"}"
	 */
	public static String stripColorsAndJSON(Gson gson, String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}

		JsonElement element = gson.fromJson(str, JsonElement.class);
		return stripColorsAndJSON(element);
	}

	public static String stripColorsAndJSON(JsonElement element) {
		String str = "";
		if (element.isJsonObject()) {
			JsonElement textElement = element.getAsJsonObject().get("text");
			if (textElement != null) {
				str = textElement.getAsString();
			}
		} else if (element.isJsonArray()) {
			str = "";
			for (JsonElement arrayElement : element.getAsJsonArray()) {
				str += stripColorsAndJSON(arrayElement);
			}
		} else {
			str = element.getAsString();
		}
		return ChatColor.stripColor(str);
	}

	public Set<String> listMobNames() {
		return mSouls.keySet();
	}
}
