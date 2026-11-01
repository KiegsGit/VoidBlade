package com.voidblade;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /voidblade (alias /vb) slot &lt;n&gt; | list | clear [n]. No permission needed, so it works in survival. */
public final class VoidCommands {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralCommandNode<CommandSourceStack> node = dispatcher.register(Commands.literal("voidblade")
				.then(Commands.literal("slot")
					.then(Commands.argument("number", IntegerArgumentType.integer(1, Feature.SLOTS))
						.executes(ctx -> slot(ctx, IntegerArgumentType.getInteger(ctx, "number")))))
				.then(Commands.literal("list").executes(VoidCommands::list))
				.then(Commands.literal("clear")
					.executes(ctx -> clear(ctx, -1))
					.then(Commands.argument("number", IntegerArgumentType.integer(1, Feature.SLOTS))
						.executes(ctx -> clear(ctx, IntegerArgumentType.getInteger(ctx, "number") - 1)))));
			dispatcher.register(Commands.literal("vb").redirect(node));
		});
	}

	private static int slot(CommandContext<CommandSourceStack> ctx, int n) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		AnchorStore.select(p, n - 1);
		AnchorStore.Anchor a = AnchorStore.get(p, n - 1);
		String info = a == null ? "empty" : (int) a.x + " " + (int) a.y + " " + (int) a.z + " (" + AnchorStore.pretty(a.dim) + ")";
		ctx.getSource().sendSuccess(() -> Component.literal("\u00a7dRift Anchor slot " + n + " selected \u00a77- " + info), false);
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		int sel = AnchorStore.selected(p);
		StringBuilder sb = new StringBuilder("\u00a7dRift Anchors:");
		for (int i = 0; i < Feature.SLOTS; i++) {
			AnchorStore.Anchor a = AnchorStore.get(p, i);
			sb.append("\n").append(i == sel ? "\u00a75> " : "\u00a78  ").append("\u00a7d").append(i + 1).append(": \u00a7f")
				.append(a == null ? "\u00a78empty" : (int) a.x + " " + (int) a.y + " " + (int) a.z + " \u00a77(" + AnchorStore.pretty(a.dim) + ")");
		}
		String text = sb.toString();
		ctx.getSource().sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	private static int clear(CommandContext<CommandSourceStack> ctx, int slot) throws CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		int s = slot < 0 ? AnchorStore.selected(p) : slot;
		AnchorStore.clear(p, s);
		ctx.getSource().sendSuccess(() -> Component.literal("\u00a7dRift Anchor slot " + (s + 1) + " cleared."), false);
		return 1;
	}

	private VoidCommands() {
	}
}
