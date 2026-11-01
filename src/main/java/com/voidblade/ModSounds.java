package com.voidblade;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Voidblade's own sounds (assets/voidblade/sounds/*.ogg, declared in sounds.json). */
public final class ModSounds {
	public static final SoundEvent RIFT_OPEN = reg("rift_open");
	public static final SoundEvent RIFT_ARRIVE = reg("rift_arrive");
	public static final SoundEvent ANCHOR_SET = reg("anchor_set");
	public static final SoundEvent MAGNET_HUM = reg("magnet_hum");
	public static final SoundEvent DENY = reg("deny");
	public static final SoundEvent READY = reg("ready");

	private static SoundEvent reg(String name) {
		Identifier id = VoidbladeMod.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void play(ServerLevel level, Vec3 p, SoundEvent s, float volume, float pitch) {
		level.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, volume, pitch);
	}

	public static void init() {
		// touching the class registers every field above
	}

	private ModSounds() {
	}
}
