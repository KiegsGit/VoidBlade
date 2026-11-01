package com.voidblade;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	public static final Item VOIDBLADE = register("voidblade", VoidbladeItem::new, lore());

	private static String[] lore() {
		List<String> l = new ArrayList<>();
		l.add("A dimensional tool carved from the void.");
		l.add("Right-click: return to your Rift Anchor.");
		l.add("Sneak + right-click: save the anchor here.");
		if (Feature.MULTI_SLOT) {
			l.add("/voidblade slot <1-5> picks the anchor slot.");
		}
		l.add("Hold it: pulls items and XP within 8 blocks.");
		l.add("Crouch to pause the magnet.");
		return l.toArray(new String[0]);
	}

	private static <T extends Item> T register(String name, java.util.function.Function<Item.Properties, T> factory, String... lore) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, VoidbladeMod.id(name));
		Item.Properties props = new Item.Properties()
			.setId(key)
			.stacksTo(1)
			.rarity(Rarity.EPIC)
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			.component(DataComponents.LORE, new ItemLore(java.util.Arrays.stream(lore)
				.<Component>map(l -> Component.literal(l).withStyle(st -> st.withColor(0xC58BFF).withItalic(false))).toList()));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props));
	}

	private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities"));
	private static final ResourceKey<CreativeModeTab> VOIDBLADE_TAB =
		ResourceKey.create(Registries.CREATIVE_MODE_TAB, VoidbladeMod.id("voidblade"));

	/** Fabric's tab-builder class has been renamed between versions, so it is looked up by name (same approach as Skybreaker). */
	private static CreativeModeTab.Builder fabricTabBuilder() {
		String[] classes = {
			"net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab",
			"net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup"
		};
		for (String name : classes) {
			try {
				return (CreativeModeTab.Builder) Class.forName(name).getMethod("builder").invoke(null);
			} catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
				// try the next name
			}
		}
		return null;
	}

	public static void init() {
		CreativeModeTab.Builder builder = fabricTabBuilder();
		if (builder != null) {
			CreativeModeTab tab = builder
				.title(Component.translatable("itemGroup.voidblade"))
				.icon(() -> new ItemStack(VOIDBLADE))
				.displayItems((params, output) -> output.accept(VOIDBLADE))
				.build();
			Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, VOIDBLADE_TAB, tab);
		} else {
			VoidbladeMod.LOGGER.warn("Fabric creative-tab builder not found, adding the Voidblade to the Tools tab instead.");
			CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> output.accept(VOIDBLADE));
		}
	}

	private ModItems() {
	}
}
