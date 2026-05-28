package earth.terrarium.adastra.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import earth.terrarium.adastra.common.menus.base.PlanetsMenuProvider;
import com.teamresourceful.resourcefullib.common.menu.MenuContentHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class PlanetsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adastra")
            .then(Commands.literal("planets")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    MenuContentHelper.open(player, new PlanetsMenuProvider());
                    return 1;
                })
            )
        );
    }
}
