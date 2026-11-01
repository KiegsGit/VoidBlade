package com.voidblade.client;

import com.voidblade.Feature;
import com.voidblade.FxPayload;
import com.voidblade.Magnet;
import com.voidblade.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Client side: renders the server's effects as purple particles and runs the holder's smooth magnet motion. */
final class FxClient {
	private static final class Rift {
		double x, y, z;
		float yaw;
		int age;
		int life;
	}

	private static final List<Rift> RIFTS = new ArrayList<>();

	static void clear() {
		RIFTS.clear();
	}

	private static double rnd() {
		return ThreadLocalRandom.current().nextDouble();
	}

	private static double sym() {
		return rnd() * 2.0 - 1.0;
	}

	private static void add(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level != null) {
			level.addAlwaysVisibleParticle(o, x, y, z, vx, vy, vz);
		}
	}

	static void handle(FxPayload p) {
		switch (p.kind()) {
			case FxPayload.BURST -> burst(p.x(), p.y(), p.z(), p.a());
			case FxPayload.SAVE -> save(p.x(), p.y(), p.z());
			case FxPayload.RIFT -> {
				Rift r = new Rift();
				r.x = p.x();
				r.y = p.y();
				r.z = p.z();
				r.yaw = p.yaw();
				r.life = Math.max(10, p.a());
				RIFTS.add(r);
				add(ModParticles.FLARE, p.x(), p.y() + 1.0, p.z(), 0, 0, 0);
			}
			case FxPayload.MAGNET -> magnetPulse(p.x(), p.y(), p.z());
			default -> {
			}
		}
	}

	/** Activation burst: a sphere of violet motes and stars with a flash in the middle. */
	private static void burst(double x, double y, double z, int n) {
		add(ModParticles.FLARE, x, y + 1.0, z, 0, 0, 0);
		for (int i = 0; i < n; i++) {
			double vx = sym(), vy = sym(), vz = sym();
			double len = Math.max(0.001, Math.sqrt(vx * vx + vy * vy + vz * vz));
			double sp = 0.06 + rnd() * 0.18;
			add(i % 3 == 0 ? ModParticles.SPARK : ModParticles.MOTE, x, y + 1.0, z, vx / len * sp, vy / len * sp, vz / len * sp);
		}
	}

	/** Anchor saved: a rising spiral column plus a ground ring. */
	private static void save(double x, double y, double z) {
		for (int i = 0; i < 40; i++) {
			double a = i * 0.55;
			double h = i * 0.07;
			add(ModParticles.MOTE, x + Math.cos(a) * 0.7, y + h, z + Math.sin(a) * 0.7, 0, 0.04, 0);
			add(ModParticles.SPARK, x + Math.cos(a + Math.PI) * 0.7, y + h, z + Math.sin(a + Math.PI) * 0.7, 0, 0.05, 0);
		}
		for (int i = 0; i < 24; i++) {
			double a = i / 24.0 * Math.PI * 2.0;
			add(ModParticles.MOTE, x + Math.cos(a) * 1.1, y + 0.1, z + Math.sin(a) * 1.1, Math.cos(a) * 0.03, 0.01, Math.sin(a) * 0.03);
		}
		add(ModParticles.FLARE, x, y + 0.2, z, 0, 0, 0);
	}

	/** Magnet visuals: a contracting ring on the ground and a short violet trail on everything being pulled. */
	private static void magnetPulse(double x, double y, double z) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		double phase = (level.getGameTime() % 20) / 20.0;
		double r = Magnet.RADIUS - phase * (Magnet.RADIUS - 1.0);
		for (int i = 0; i < 18; i++) {
			double a = i / 18.0 * Math.PI * 2.0 + phase;
			add(ModParticles.SPARK, x + Math.cos(a) * r, y + 0.08, z + Math.sin(a) * r, 0, 0.01, 0);
		}
		AABB box = new AABB(x - Magnet.RADIUS, y - Magnet.RADIUS, z - Magnet.RADIUS, x + Magnet.RADIUS, y + Magnet.RADIUS, z + Magnet.RADIUS);
		Vec3 c = new Vec3(x, y + 0.6, z);
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
			trail(e, c);
		}
		for (ExperienceOrb e : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
			trail(e, c);
		}
	}

	private static void trail(Entity e, Vec3 c) {
		Vec3 d = c.subtract(e.position());
		double len = d.length();
		if (len < 0.5 || len > Magnet.RADIUS) {
			return;
		}
		Vec3 v = d.scale(0.05 / len);
		add(ModParticles.MOTE, e.getX(), e.getY() + 0.2, e.getZ(), v.x, v.y, v.z);
	}

	static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			RIFTS.clear();
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		// smooth magnet motion for the holder; the server runs the same pull and stays the authority
		if (Magnet.active(mc.player)) {
			Magnet.pull(level, mc.player);
		}
		if (!Feature.RIFT_VISUALS) {
			return;
		}
		Iterator<Rift> it = RIFTS.iterator();
		while (it.hasNext()) {
			Rift r = it.next();
			r.age++;
			if (r.age > r.life) {
				add(ModParticles.FLARE, r.x, r.y + 1.0, r.z, 0, 0, 0);
				it.remove();
				continue;
			}
			double e = Math.pow(Math.sin(Math.PI * r.age / r.life), 0.5); // opens, holds, closes
			double rx = 0.85 * e;
			double ry = 1.45 * e;
			double yawRad = Math.toRadians(r.yaw);
			double rightX = Math.cos(yawRad);
			double rightZ = Math.sin(yawRad);
			double cy = r.y + 1.0;
			// the rim
			for (int i = 0; i < 14; i++) {
				double a = rnd() * Math.PI * 2.0;
				double ox = Math.cos(a) * rx;
				double oy = Math.sin(a) * ry;
				double tx = -Math.sin(a) * rx * 0.03;
				double ty = Math.cos(a) * ry * 0.03;
				add(ModParticles.MOTE, r.x + rightX * ox, cy + oy, r.z + rightZ * ox, rightX * tx, ty, rightZ * tx);
			}
			// the swirling interior
			for (int k = 0; k < 3; k++) {
				double a = r.age * 0.4 + k * 2.094;
				double f = 0.15 + rnd() * 0.8;
				double ox = Math.cos(a) * rx * f;
				double oy = Math.sin(a) * ry * f;
				add(ModParticles.SPARK, r.x + rightX * ox, cy + oy, r.z + rightZ * ox, 0, 0, 0);
			}
			if (r.age % 3 == 0) {
				add(ModParticles.SPARK, r.x + sym() * 0.5, r.y + 0.2, r.z + sym() * 0.5, 0, 0.06, 0);
			}
		}
	}

	private FxClient() {
	}
}
