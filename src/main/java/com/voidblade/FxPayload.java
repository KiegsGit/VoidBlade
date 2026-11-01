package com.voidblade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server -> client "draw this effect". All gameplay stays on the server; the client only renders particles. */
public record FxPayload(int kind, double x, double y, double z, int a, float yaw) implements CustomPacketPayload {
	/** Purple activation burst (a = particle count). */
	public static final int BURST = 1;
	/** Anchor saved: rising spiral column. */
	public static final int SAVE = 2;
	/** Decorative rift (a = lifetime in ticks, yaw = facing). */
	public static final int RIFT = 3;
	/** Magnet pulse around a player (a = how many things are being pulled). */
	public static final int MAGNET = 4;

	public static final CustomPacketPayload.Type<FxPayload> TYPE = new CustomPacketPayload.Type<>(VoidbladeMod.id("fx"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = CustomPacketPayload.codec(FxPayload::write, FxPayload::new);

	public FxPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readFloat());
	}

	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(kind);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeVarInt(a);
		buf.writeFloat(yaw);
	}

	/** Sends to every player within 96 blocks of the effect. */
	public static void broadcast(ServerLevel level, FxPayload payload) {
		for (ServerPlayer p : level.players()) {
			if (p.distanceToSqr(payload.x, payload.y, payload.z) <= 96.0 * 96.0) {
				ServerPlayNetworking.send(p, payload);
			}
		}
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
