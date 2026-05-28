package earth.terrarium.adastra.common.events;

import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.common.commands.VelocityDebugCommand;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import earth.terrarium.adastra.common.tags.ModItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/**
 * Per-tick server-side handler for jet-suit related physics quality-of-life:
 *  1. Shows a /adastra debug velocity overlay in the action bar for opted-in players.
 *  2. If a player is in space wearing a jet suit and slams into a block at speed,
 *     deals momentum damage and instantly zeros the velocity. Avoids the "stuck
 *     against a wall but still drifting" effect.
 */
public class VelocityDebugTicker {

    /** Minimum speed (blocks/tick) before a wall hit causes damage. */
    private static final double IMPACT_DAMAGE_THRESHOLD = 0.5;

    /** Damage per (speed - threshold). 1.5 b/t → 4 dmg, 2.0 b/t → 6 dmg. */
    private static final float IMPACT_DAMAGE_MULTIPLIER = 4.0f;

    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        // Velocity overlay (action bar) for /adastra debug velocity opt-ins.
        if (VelocityDebugCommand.isWatching(player.getUUID())) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                VelocityDebugCommand.formatVelocity(player)
            ));
        }

        // Wall-impact damage in space, jet-suit only.
        if (!PlanetApi.API.isSpace(player.level())) return;
        if (!hasJetSuitChest(player)) return;

        Vec3 v = player.getDeltaMovement();
        double speed = v.length();
        if (speed < IMPACT_DAMAGE_THRESHOLD) return;
        if (!player.horizontalCollision && !player.verticalCollision) return;

        // Took an impact. Deal damage scaled with how fast we were going,
        // then zero velocity so the player isn't pinned against the wall
        // accumulating phantom momentum.
        float damage = Math.min(20.0f, (float)(speed - IMPACT_DAMAGE_THRESHOLD) * IMPACT_DAMAGE_MULTIPLIER);
        if (damage > 0.5f) {
            DamageSource src = player.damageSources().flyIntoWall();
            player.hurt(src, damage);
        }

        // Zero the components that were blocked. Easier: zero entirely so
        // the player visibly stops; they can re-thrust to get moving again.
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private static boolean hasJetSuitChest(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItemTags.JET_SUITS);
    }
}
