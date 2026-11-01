package com.voidblade.client;

import com.voidblade.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Same technique as Skybreaker: end-rod movement, but an exact size and lifetime and our own texture. */
final class GlowProvider implements ParticleProvider<SimpleParticleType> {
	private final EndRodParticle.Provider base;
	private final float size;
	private final float jitter;
	private final int life;

	GlowProvider(SpriteSet sprites, float size, float jitter, int life) {
		this.base = new EndRodParticle.Provider(sprites);
		this.size = size;
		this.jitter = jitter;
		this.life = life;
	}

	@Override
	public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
	                               double xd, double yd, double zd, RandomSource random) {
		Particle p = this.base.createParticle(type, level, x, y, z, xd, yd, zd, random);
		if (p == null) {
			return null;
		}
		p.setLifetime(Math.max(1, this.life + random.nextInt(Math.max(1, this.life / 4))));
		if (p instanceof SingleQuadParticle quad) {
			float wanted = this.size * (1.0f + this.jitter * (random.nextFloat() * 2.0f - 1.0f));
			quad.scale(wanted / Math.max(0.0001f, quad.getQuadSize(0.0f)));
		}
		return p;
	}
}

final class ParticleSetup {
	static void register() {
		//   type                  size   jitter life
		reg(ModParticles.MOTE,   0.11f, 0.30f, 16);
		reg(ModParticles.SPARK,  0.17f, 0.25f, 10);
		reg(ModParticles.FLARE,  1.10f, 0.00f, 5);
	}

	private static void reg(SimpleParticleType type, float size, float jitter, int life) {
		ParticleProviderRegistry.getInstance().register(type, sprites -> new GlowProvider(sprites, size, jitter, life));
	}

	private ParticleSetup() {
	}
}
