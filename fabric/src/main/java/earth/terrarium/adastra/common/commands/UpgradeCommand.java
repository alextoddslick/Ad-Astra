package earth.terrarium.adastra.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import earth.terrarium.adastra.common.utils.UpgradeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /adastra upgrade <visor|boost|clear>} — applies (or clears) a jet-suit upgrade flag on the
 * item in the player's main hand. A creative/testing convenience so upgrades can be granted without
 * crafting them at the NASA Workbench. Effects:
 * <ul><li>visor → analysis_visor (helmet sees through storms)</li>
 *     <li>boost → boost_mode (boots boost on high-gravity worlds)</li></ul>
 */
public class UpgradeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adastra")
            .then(Commands.literal("upgrade")
                .then(Commands.literal("visor").executes(ctx ->
                    apply(ctx.getSource(), UpgradeUtils.ANALYSIS_VISOR, "Analysis Visor")))
                .then(Commands.literal("boost").executes(ctx ->
                    apply(ctx.getSource(), UpgradeUtils.BOOST_MODE, "Jet Boots Boost")))
                .then(Commands.literal("clear").executes(ctx ->
                    clear(ctx.getSource())))));
    }

    private static int apply(CommandSourceStack source, String key, String label) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.literal("Hold the item to upgrade in your main hand."));
            return 0;
        }
        UpgradeUtils.set(held, key, true);
        source.sendSuccess(() -> Component.literal("Applied " + label + " to " + held.getHoverName().getString())
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand."));
            return 0;
        }
        UpgradeUtils.set(held, UpgradeUtils.ANALYSIS_VISOR, false);
        UpgradeUtils.set(held, UpgradeUtils.BOOST_MODE, false);
        source.sendSuccess(() -> Component.literal("Cleared suit upgrades.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
