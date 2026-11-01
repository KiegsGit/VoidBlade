package com.voidblade;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Voidblade: a non-combat dimensional tool.
 * Right-click = return to your selected Rift Anchor. Sneak + right-click = save the anchor here.
 * Holding it also runs the Void Magnet (see Magnet).
 */
public class VoidbladeItem extends Item {
	public static final int SAVE_COOLDOWN = 20;
	public static final int RETURN_COOLDOWN = 120;

	/** Game tick at which each player's Voidblade is ready again. */
	static final Map<UUID, Long> READY_AT = new HashMap<>();
	private static final Map<UUID, Integer> LAST_COOLDOWN = new HashMap<>();

	public VoidbladeItem(Properties properties) {
		super(properties);
	}

	static int lastCooldown(ServerPlayer p) {
		return LAST_COOLDOWN.getOrDefault(p.getUUID(), RETURN_COOLDOWN);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS; // client: just swing; the server decides everything
		}
		ItemStack stack = sp.getItemInHand(hand);
		boolean ok = sp.isShiftKeyDown() ? save(server, sp, stack) : returnToAnchor(server, sp, stack);
		return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	private static String label(int slot) {
		return Feature.MULTI_SLOT ? "Anchor " + (slot + 1) + "/" + Feature.SLOTS : "Anchor";
	}

	private static void cooldown(ServerLevel server, ServerPlayer sp, ItemStack stack, int ticks) {
		READY_AT.put(sp.getUUID(), server.getGameTime() + ticks);
		LAST_COOLDOWN.put(sp.getUUID(), ticks);
		sp.getCooldowns().addCooldown(stack, ticks);
	}

	private static void fail(ServerLevel server, ServerPlayer sp, String msg) {
		sp.sendOverlayMessage(Component.literal("\u00a7c" + msg));
		ModSounds.play(server, sp.position(), ModSounds.DENY, 0.6f, 1.0f);
	}

	private static boolean save(ServerLevel server, ServerPlayer sp, ItemStack stack) {
		int slot = AnchorStore.selected(sp);
		AnchorStore.Anchor a = new AnchorStore.Anchor();
		a.dim = AnchorStore.dimId(server.dimension());
		a.x = sp.getX();
		a.y = sp.getY();
		a.z = sp.getZ();
		a.yaw = sp.getYRot();
		a.pitch = sp.getXRot();
		AnchorStore.set(sp, slot, a);

		FxPayload.broadcast(server, new FxPayload(FxPayload.SAVE, a.x, a.y, a.z, 0, a.yaw));
		ModSounds.play(server, sp.position(), ModSounds.ANCHOR_SET, 0.8f, 1.0f);
		sp.sendOverlayMessage(Component.literal("\u00a7d" + label(slot) + " set \u00a77at \u00a7f" + (int) a.x + " " + (int) a.y + " " + (int) a.z
			+ " \u00a77(" + AnchorStore.pretty(a.dim) + ")"));
		cooldown(server, sp, stack, SAVE_COOLDOWN);
		return true;
	}

	private static boolean returnToAnchor(ServerLevel server, ServerPlayer sp, ItemStack stack) {
		int slot = AnchorStore.selected(sp);
		AnchorStore.Anchor a = AnchorStore.get(sp, slot);
		if (a == null) {
			fail(server, sp, "No " + label(slot) + " saved. Sneak + right-click to set one.");
			return false;
		}
		Identifier dimId = Identifier.tryParse(a.dim);
		ServerLevel dest = dimId == null ? null : server.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
		if (dest == null) {
			fail(server, sp, "That dimension is not available.");
			return false;
		}
		if (!Feature.MULTI_SLOT && dest != server) {
			fail(server, sp, "Your anchor is in another dimension.");
			return false;
		}
		Vec3 safe = SafeTeleport.find(dest, new Vec3(a.x, a.y, a.z));
		if (safe == null) {
			fail(server, sp, "Your " + label(slot) + " is blocked or unsafe. Nothing safe to land on.");
			return false;
		}

		Vec3 from = sp.position();
		FxPayload.broadcast(server, new FxPayload(Feature.RIFT_VISUALS ? FxPayload.RIFT : FxPayload.BURST, from.x, from.y, from.z, Feature.RIFT_VISUALS ? 50 : 48, sp.getYRot()));
		ModSounds.play(server, from, ModSounds.RIFT_OPEN, 0.9f, 1.0f);

		if (sp.isPassenger()) {
			sp.stopRiding();
		}
		if (dest == server) {
			sp.connection.teleport(safe.x, safe.y, safe.z, a.yaw, a.pitch);
		} else {
			sp.teleport(new TeleportTransition(dest, safe, Vec3.ZERO, a.yaw, a.pitch, TeleportTransition.DO_NOTHING));
		}
		sp.resetFallDistance();

		FxPayload.broadcast(dest, new FxPayload(Feature.RIFT_VISUALS ? FxPayload.RIFT : FxPayload.BURST, safe.x, safe.y, safe.z, Feature.RIFT_VISUALS ? 50 : 48, a.yaw));
		ModSounds.play(dest, safe, ModSounds.RIFT_ARRIVE, 0.9f, 1.0f);
		sp.sendOverlayMessage(Component.literal("\u00a7dReturned to " + label(slot)));
		cooldown(server, sp, stack, RETURN_COOLDOWN);
		return true;
	}
}
