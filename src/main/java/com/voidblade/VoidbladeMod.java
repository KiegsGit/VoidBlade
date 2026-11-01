package com.voidblade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoidbladeMod implements ModInitializer {
	public static final String MOD_ID = "voidblade";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(FxPayload.TYPE, FxPayload.CODEC);
		ModSounds.init();
		ModParticles.init();
		ModItems.init();
		AnchorStore.init();
		Magnet.init();
		if (Feature.MULTI_SLOT) {
			VoidCommands.init();
		}
		LOGGER.info("Voidblade loaded.");
	}
}
