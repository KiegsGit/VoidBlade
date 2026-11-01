package com.voidblade.client;

import com.voidblade.FxPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class VoidbladeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleSetup.register();
		ClientPlayNetworking.registerGlobalReceiver(FxPayload.TYPE, (payload, context) -> FxClient.handle(payload));
		ClientTickEvents.END_CLIENT_TICK.register(FxClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FxClient.clear());
	}
}
