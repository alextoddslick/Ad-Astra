package earth.terrarium.adastra.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * /adastra debug velocity
 *
 * Toggles a per-player action-bar overlay that prints the player's current
 * deltaMovement (vx, vy, vz, |v|) every server tick. Used to diagnose stuck
 * velocity (e.g. drifting against a wall in space). The actual ticker is in
 * {@link earth.terrarium.adastra.common.events.VelocityDebugTicker}.
 */
public class VelocityDebugCommand {

    private static final Set<UUID> WATCHED = new HashSet<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adastra")
            .then(Commands.literal("debug")
                .then(Commands.literal("velocity")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        UUID id = player.getUUID();
                        boolean nowWatching;
                        if (WATCHED.contains(id)) {
                            WATCHED.remove(id);
                            nowWatching = false;
                        } else {
                            WATCHED.add(id);
                            nowWatching = true;
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "Velocity overlay: " + (nowWatching ? "ON" : "OFF")
                        ).withStyle(nowWatching ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
                        return 1;
                    })
                )
            )
        );
    }

    public static boolean isWatching(UUID id) {
        return WATCHED.contains(id);
    }

    public static Component formatVelocity(ServerPlayer player) {
        Vec3 v = player.getDeltaMovement();
        double len = v.length();
        boolean h = player.horizontalCollision;
        boolean ve = player.verticalCollision;
        return Component.literal(String.format(
            "v = (%+.3f, %+.3f, %+.3f)  |v| = %.3f%s%s",
            v.x, v.y, v.z, len,
            h  ? "  [H-COL]" : "",
            ve ? "  [V-COL]" : ""
        )).withStyle(ChatFormatting.AQUA);
    }
}
