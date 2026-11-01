package com.voidblade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Per-world Rift Anchor storage: &lt;world&gt;/voidblade_anchors.json, one entry per player UUID. */
public final class AnchorStore {
	public static final class Anchor {
		public String dim;
		public double x;
		public double y;
		public double z;
		public float yaw;
		public float pitch;
	}

	public static final class Data {
		public int selected;
		public Anchor[] anchors = new Anchor[Feature.SLOTS];
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Map<String, Data> data = new HashMap<>();
	private static Path file;

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(AnchorStore::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> save());
	}

	private static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("voidblade_anchors.json");
		data = new HashMap<>();
		if (Files.exists(file)) {
			try (Reader r = Files.newBufferedReader(file)) {
				Map<String, Data> loaded = GSON.fromJson(r, new TypeToken<Map<String, Data>>() { }.getType());
				if (loaded != null) {
					data = loaded;
				}
			} catch (Exception e) {
				VoidbladeMod.LOGGER.warn("Could not read {}: {}", file, e.toString());
			}
		}
	}

	private static void save() {
		if (file == null) {
			return;
		}
		try (Writer w = Files.newBufferedWriter(file)) {
			GSON.toJson(data, w);
		} catch (IOException e) {
			VoidbladeMod.LOGGER.warn("Could not write {}: {}", file, e.toString());
		}
	}

	private static Data of(ServerPlayer p) {
		Data d = data.computeIfAbsent(p.getUUID().toString(), k -> new Data());
		if (d.anchors == null) {
			d.anchors = new Anchor[Feature.SLOTS];
		} else if (d.anchors.length < Feature.SLOTS) {
			d.anchors = Arrays.copyOf(d.anchors, Feature.SLOTS);
		}
		if (d.selected < 0 || d.selected >= Feature.SLOTS) {
			d.selected = 0;
		}
		return d;
	}

	public static int selected(ServerPlayer p) {
		return of(p).selected;
	}

	public static void select(ServerPlayer p, int slot) {
		of(p).selected = Math.max(0, Math.min(Feature.SLOTS - 1, slot));
		save();
	}

	public static Anchor get(ServerPlayer p, int slot) {
		return of(p).anchors[slot];
	}

	public static void set(ServerPlayer p, int slot, Anchor a) {
		of(p).anchors[slot] = a;
		save();
	}

	public static void clear(ServerPlayer p, int slot) {
		of(p).anchors[slot] = null;
		save();
	}

	/** "minecraft:overworld" from a dimension key (parsed from ResourceKey.toString(): "ResourceKey[registry / id]"). */
	public static String dimId(ResourceKey<Level> key) {
		String s = key.toString();
		int i = s.lastIndexOf("/ ");
		return i >= 0 ? s.substring(i + 2, s.length() - 1) : s;
	}

	public static String pretty(String dim) {
		return switch (dim) {
			case "minecraft:overworld" -> "Overworld";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "End";
			default -> dim;
		};
	}

	private AnchorStore() {
	}
}
