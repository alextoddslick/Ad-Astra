package earth.terrarium.adastra.mixins.client;

import earth.terrarium.adastra.common.registry.ModParticleTypes;
import earth.terrarium.adastra.common.tags.ModBiomeTags;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = WeatherEffectRenderer.class, priority = 2000)
public abstract class WeatherEffectRendererMixin {

    @Shadow
    private int rainSoundTime;

    @Inject(
        method = "tickRainParticles",
        at = @At("HEAD"),
        cancellable = true)
    public void adastra$tickRainParticles(ClientLevel level, Camera camera, int ticks, ParticleStatus particleStatus, int rainSoundTime, CallbackInfo ci) {
        if (!adastra$hasAcidRain()) return;
        ci.cancel();

        var minecraft = Minecraft.getInstance();
        float f = Objects.requireNonNull(minecraft.level).getRainLevel(1.0F);
        if (!(f <= 0.0F)) {
            RandomSource randomSource = RandomSource.create((long) ticks * 312987231L);
            LevelReader levelReader = minecraft.level;
            BlockPos blockPos = BlockPos.containing(camera.position());
            BlockPos blockPos2 = null;
            int i = (int) (100.0F * f * f) / (particleStatus == ParticleStatus.DECREASED ? 2 : 1);

            for (int j = 0; j < i; ++j) {
                int k = randomSource.nextInt(21) - 10;
                int l = randomSource.nextInt(21) - 10;
                BlockPos blockPos3 = levelReader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos.offset(k, 0, l));
                if (blockPos3.getY() > levelReader.getMinY() && blockPos3.getY() <= blockPos.getY() + 10 && blockPos3.getY() >= blockPos.getY() - 10) {
                    Biome biome = levelReader.getBiome(blockPos3).value();
                    if (biome.getPrecipitationAt(blockPos3, blockPos3.getY()) == Biome.Precipitation.RAIN) {
                        blockPos2 = blockPos3.below();
                        if (particleStatus == ParticleStatus.MINIMAL) {
                            break;
                        }

                        double d = randomSource.nextDouble();
                        double e = randomSource.nextDouble();
                        BlockState blockState = levelReader.getBlockState(blockPos2);
                        FluidState fluidState = levelReader.getFluidState(blockPos2);
                        VoxelShape voxelShape = blockState.getCollisionShape(levelReader, blockPos2);
                        double g = voxelShape.max(Direction.Axis.Y, d, e);
                        double h = fluidState.getHeight(levelReader, blockPos2);
                        double m = Math.max(g, h);
                        ParticleOptions particleOptions = !fluidState.is(FluidTags.LAVA) && !blockState.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(blockState)
                            ? ModParticleTypes.ACID_RAIN.get()
                            : ParticleTypes.SMOKE;
                        minecraft.level.addParticle(particleOptions, (double) blockPos2.getX() + d, (double) blockPos2.getY() + m, (double) blockPos2.getZ() + e, 0.0, 0.0, 0.0);
                    }
                }
            }

            if (blockPos2 != null && randomSource.nextInt(3) < this.rainSoundTime++) {
                this.rainSoundTime = 0;
                if (blockPos2.getY() > blockPos.getY() + 1
                    && levelReader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).getY() > Mth.floor((float) blockPos.getY())) {
                    minecraft.level.playLocalSound(blockPos2, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
                } else {
                    minecraft.level.playLocalSound(blockPos2, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
                }
            }
        }
    }

    @Unique
    private boolean adastra$hasAcidRain() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        return player.level().getBiome(player.blockPosition()).is(ModBiomeTags.HAS_ACID_RAIN);
    }
}
