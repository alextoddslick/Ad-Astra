package earth.terrarium.adastra.mixins.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.systems.RenderSystem;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import earth.terrarium.adastra.client.dimension.ModDimensionSpecialEffects;
import earth.terrarium.adastra.client.dimension.PlanetSkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces vanilla's sky pass for Ad Astra space dimensions.
 *
 * All Ad Astra space dim_types use {@code "skybox": "none"} so vanilla's
 * {@code addSkyPass} would early-return — drawing nothing. We mixin at HEAD: when the
 * current dimension has an Ad Astra renderer registered, we add our own framegraph pass
 * that draws stars (via the vanilla {@code SkyRenderer.renderStars} invoker) and the
 * Ad Astra planet / sun / moon discs, then cancel the vanilla method so it does not
 * proceed to its NONE-return.
 *
 * For non-Ad-Astra dimensions (overworld, nether, vanilla End, modded skies, etc.), we
 * fall through and let vanilla handle the sky as normal — there is zero behavior change.
 */
@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class LevelRendererSkyPassMixin {

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void adastra$replaceSkyPass(FrameGraphBuilder frameGraphBuilder, Camera camera,
                                        GpuBufferSlice shaderFog, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        ModDimensionSpecialEffects effects = ClientPlatformUtils.getPlanetRenderers().get(level.dimension());
        if (effects == null) return; // not an Ad Astra dim — let vanilla proceed.

        // Reuse vanilla's SkyRenderer for the star pass. If it isn't constructed yet (very
        // early frames), bail and let vanilla's null-check fire.
        LevelRendererAccessor accessor = (LevelRendererAccessor) (Object) this;
        SkyRenderer skyRenderer = accessor.getSkyRenderer();
        if (skyRenderer == null) return;

        LevelTargetBundle targets = accessor.getTargets();

        FramePass pass = frameGraphBuilder.addPass("ad_astra_sky");
        targets.main = pass.readsAndWrites(targets.main);
        pass.executes(() -> {
            RenderSystem.setShaderFog(shaderFog);
            // Stars first (background), then planet discs on top.
            // 1.0 brightness — we are in space, no atmospheric dimming.
            ((SkyRendererAccessor) skyRenderer).adastra$renderStars(1.0f, new com.mojang.blaze3d.vertex.PoseStack());
            // Day fraction drives any TIME_OF_DAY rotations in planet renderables.
            float timeOfDayFraction = (level.getDayTime() % 24000L) / 24000.0f;
            PlanetSkyRenderer.render(new com.mojang.blaze3d.vertex.PoseStack(), timeOfDayFraction);
        });

        ci.cancel();
    }
}
