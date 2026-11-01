package com.voidblade;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Void Magnet. While a Voidblade is held (either hand) it pulls dropped items and XP orbs within 8 blocks toward the
 * holder. Only ItemEntity and ExperienceOrb are ever touched, never living entities. Crouching pauses it.
 * The same pull runs on the server (authority) and on the holder's client (smooth motion between server syncs).
 */
public final class Magnet {
	public static final double RADIUS = 8.0;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Magnet::tick);
	}

	public static boolean holding(Player p) {
		return p.getMainHandItem().is(ModItems.VOIDBLADE) || p.getOffhandItem().is(ModItems.VOIDBLADE);
	}

	public static boolean active(Player p) {
		return holding(p) && !p.isShiftKeyDown() && !p.isSpectator() && p.isAlive();
	}

	/** Pulls everything in range toward the player. Returns how many things moved. */
	public static int pull(Level level, Player p) {
		Vec3 center = p.position().add(0.0, 0.6, 0.0);
		AABB box = p.getBoundingBox().inflate(RADIUS);
		int n = 0;
		for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
			if (!e.getItem().isEmpty() && pullOne(e, center)) {
				n++;
			}
		}
		for (ExperienceOrb e : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
			if (pullOne(e, center)) {
				n++;
			}
		}
		return n;
	}

	private static boolean pullOne(Entity e, Vec3 center) {
		if (!e.isAlive() || e.tickCount < 15) {
			return false; // fresh drops get a moment, so you can still throw things away
		}
		Vec3 d = center.subtract(e.position());
		double dist = d.length();
		if (dist > RADIUS || dist < 0.35) {
			return false;
		}
		double speed = Mth.clamp(0.12 + (RADIUS - dist) * 0.045, 0.12, 0.48);
		Vec3 desired = d.normalize().scale(speed).add(0.0, 0.04, 0.0); // +0.04 cancels item gravity
		e.setDeltaMovement(e.getDeltaMovement().scale(0.55).add(desired.scale(0.45)));
		return true;
	}

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
			Level lvl = sp.level();
			if (!(lvl instanceof ServerLevel level)) {
				continue;
			}
			boolean holding = holding(sp);

			// cooldown indicator in the action bar (the item also shows the vanilla cooldown sweep)
			Long until = VoidbladeItem.READY_AT.get(sp.getUUID());
			if (until != null) {
				if (now < until) {
					if (holding && now % 4 == 0) {
						long left = until - now;
						int filled = (int) Math.max(0, Math.min(10, 10 - (left * 10 / Math.max(1, VoidbladeItem.lastCooldown(sp)))));
						StringBuilder bar = new StringBuilder();
						for (int i = 0; i < 10; i++) {
							bar.append(i < filled ? '\u25A0' : '\u25A1');
						}
						sp.sendOverlayMessage(Component.literal("\u00a75Rift recharging \u00a78[\u00a7d" + bar + "\u00a78] \u00a7d"
							+ String.format("%.1f", left / 20.0) + "s"));
					}
				} else {
					VoidbladeItem.READY_AT.remove(sp.getUUID());
					if (holding) {
						sp.sendOverlayMessage(Component.literal("\u00a7dRift ready"));
						ModSounds.play(level, sp.position(), ModSounds.READY, 0.5f, 1.0f);
					}
				}
			}

			if (!active(sp)) {
				continue;
			}
			int n = pull(level, sp);
			if (n > 0) {
				if (now % 30 == 0) {
					ModSounds.play(level, sp.position(), ModSounds.MAGNET_HUM, 0.35f, 0.9f + 0.2f * level.getRandom().nextFloat());
				}
			}
			if (Feature.RIFT_VISUALS && now % 4 == 0) {
				FxPayload.broadcast(level, new FxPayload(FxPayload.MAGNET, sp.getX(), sp.getY(), sp.getZ(), n, 0.0f));
			}
		}
	}

	private Magnet() {
	}
}
