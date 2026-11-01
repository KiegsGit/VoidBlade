package com.voidblade;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Voidblade's particle types; textures live in assets/voidblade/textures/particle, sizes are set client-side. */
public final class ModParticles {
	/** Soft violet orb. */
	public static final SimpleParticleType MOTE = reg("mote");
	/** Four-point neon star. */
	public static final SimpleParticleType SPARK = reg("spark");
	/** Big pulsing violet glow. */
	public static final SimpleParticleType FLARE = reg("flare");

	private static SimpleParticleType reg(String name) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, VoidbladeMod.id(name), FabricParticleTypes.simple());
	}

	public static void init() {
		// touching the class registers every field above
	}

	private ModParticles() {
	}
}
