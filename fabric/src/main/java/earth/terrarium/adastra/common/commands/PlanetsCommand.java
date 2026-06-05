package earth.terrarium.adastra.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.teamresourceful.resourcefullib.common.menu.MenuContentHelper;
import earth.terrarium.adastra.common.menus.base.PlanetsMenuProvider;
import earth.terrarium.adastra.common.utils.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PlanetsCommand {

    /** Test terrain variant: flat-ish icy Europa with large caves. Reachable via /adastra planets europa-t. */
    private static final ResourceKey<Level> EUROPA_T = ResourceKey.create(
        Registries.DIMENSION, Identifier.fromNamespaceAndPath("ad_astra", "europa_t"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adastra")
            .then(Commands.literal("planets")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    MenuContentHelper.open(player, new PlanetsMenuProvider());
                    return 1;
                })
                .then(Commands.literal("europa-t")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ServerLevel level = context.getSource().getServer().getLevel(EUROPA_T);
                        if (level == null) {
                            context.getSource().sendFailure(Component.literal("Europa-T dimension is not loaded."));
                            return 0;
                        }
                        // Surface dim: land() uses this explicit Y. ~100 drops onto the flat ice in low gravity.
                        ModUtils.land(player, level, new Vec3(player.getX(), 100.0, player.getZ()));
                        return 1;
                    })
                )
            )
        );
    }
}
