package earth.terrarium.adastra.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.bytecodecs.ExtraByteCodecs;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.api.planets.PlanetApi;
import earth.terrarium.adastra.common.config.AdAstraConfig;
import earth.terrarium.adastra.common.entities.vehicles.Rocket;
import earth.terrarium.adastra.common.handlers.LaunchingDimensionHandler;
import earth.terrarium.adastra.common.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public record ServerboundLandPacket(ResourceKey<Level> dimension,
                                    boolean tryPreviousLocation) implements Packet<ServerboundLandPacket> {

    public static final ServerboundPacketType<ServerboundLandPacket> TYPE = new Type();

    @Override
    public PacketType<ServerboundLandPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType.Server<ServerboundLandPacket> {

        public Type() {
            super(
                Identifier.fromNamespaceAndPath(AdAstra.MOD_ID, "land"),
                ObjectByteCodec.create(
                    ExtraByteCodecs.DIMENSION.fieldOf(ServerboundLandPacket::dimension),
                    ByteCodec.BOOLEAN.fieldOf(ServerboundLandPacket::tryPreviousLocation),
                    ServerboundLandPacket::new
                )
            );
        }

        @Override
        public Consumer<Player> handle(ServerboundLandPacket packet) {
            return player -> {
                AdAstra.LOGGER.info("[ad_astra] ServerboundLandPacket received dim={} for player={}", packet.dimension.identifier(), player.getName().getString());
                if (!(player.level() instanceof ServerLevel serverLevel)) {
                    AdAstra.LOGGER.warn("[ad_astra] Land: player.level not ServerLevel (level={})", player.level());
                    return;
                }
                var planet = PlanetApi.API.getPlanet(packet.dimension);
                if (planet == null) {
                    AdAstra.LOGGER.warn("[ad_astra] Land: no registered planet for dim={}", packet.dimension.identifier());
                    return; // Only allow teleporting to registered planets.
                }

                if (planet.isSpace()) {
                    AdAstra.LOGGER.warn("[ad_astra] Land: planet is space-only ({})", packet.dimension.identifier());
                    return;
                }
                if (!ModUtils.canTeleportToPlanet(player, planet)) {
                    AdAstra.LOGGER.warn("[ad_astra] Land: canTeleportToPlanet returned false (player={} dim={} container={} creative={} spectator={} vehicleY={} vehicleTier={} planetTier={})",
                        player.getName().getString(), packet.dimension.identifier(),
                        player.containerMenu == null ? "null" : player.containerMenu.getClass().getSimpleName(),
                        player.isCreative(), player.isSpectator(),
                        player.getVehicle() == null ? "null" : player.getVehicle().getY(),
                        player.getVehicle() instanceof Rocket r ? r.tier() : -1,
                        planet.tier());
                    return;
                }

                boolean landingNormally = packet.tryPreviousLocation() && player.getVehicle() instanceof Rocket;
                GlobalPos newPos = landingNormally ? LaunchingDimensionHandler.getSpawningLocation(player, serverLevel, planet)
                    .orElse(null) : null;

                var server = serverLevel.getServer();
                ServerLevel targetLevel = newPos == null ? server.getLevel(planet.dimension()) : server.getLevel(newPos.dimension());
                if (targetLevel == null) {
                    throw new IllegalStateException(String.format("Dimension %s does not exist! Try restarting your %s!",
                        planet.dimension(), server.isDedicatedServer() ? "server" : "singleplayer world"));
                }

                LaunchingDimensionHandler.addSpawnLocation(player, serverLevel);
                BlockPos targetPos = newPos != null ? newPos.pos() : player.blockPosition();
                AdAstra.LOGGER.info("[ad_astra] Land: teleporting player={} to level={} pos=({},{},{})",
                    player.getName().getString(), targetLevel.dimension().identifier(), targetPos.getX(), AdAstraConfig.atmosphereLeave, targetPos.getZ());
                ModUtils.land((ServerPlayer) player, targetLevel, new Vec3(targetPos.getX(), AdAstraConfig.atmosphereLeave, targetPos.getZ()));
            };
        }
    }
}