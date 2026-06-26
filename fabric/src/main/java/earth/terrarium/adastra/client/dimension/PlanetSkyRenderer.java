package earth.terrarium.adastra.client.dimension;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import earth.terrarium.adastra.client.ClientPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Draws Ad Astra planet/sun/moon discs on top of the vanilla sky pipeline.
 *
 * Geometry mirrors the legacy 1.20 implementation: a unit textured quad is built once
 * (POSITION_TEX, QUADS) and stored in a single shared GpuBuffer. Each renderable is drawn
 * with the {@link RenderPipelines#CELESTIAL} pipeline (BlendFunction.OVERLAY, no depth write),
 * with its translation/rotation baked into the model-view uniform transform.
 *
 * The quad sits at y=100 in sky-space and is scaled by {@code SkyRenderable#scale()} on X/Z.
 * {@code globalRotation} rotates the camera-aligned axes; the quad is then translated +100
 * along Y; {@code localRotation} then spins the disc in place.
 */
public final class PlanetSkyRenderer {

    // 26.2: the blaze3d RenderPipeline.Builder was overhauled — withSampler / withUniform /
    // withVertexFormat were all replaced by bind-group-layout + vertex-binding + primitive-topology
    // builders, and the global-uniform snippet that the core/position_tex shader needs
    // (RenderPipelines.GLOBALS_SNIPPET) is private. The old custom PLANET_DISC_PIPELINE only
    // differed from vanilla CELESTIAL by a depth-write flag that had already been stubbed out in
    // the 26.1.2 port (see the removed TODO), so it was functionally identical to CELESTIAL. We now
    // draw the body discs with vanilla RenderPipelines.CELESTIAL — exactly the pipeline vanilla uses
    // for the sun/moon discs (POSITION_TEX, core/position_tex, OVERLAY blend, QUADS).

    @Nullable
    private static GpuBuffer quadBuffer;

    private PlanetSkyRenderer() {}

    private static GpuBuffer getOrCreateQuadBuffer() {
        if (quadBuffer != null) return quadBuffer;
        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(4 * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytebufferbuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
            Matrix4f m = new Matrix4f();
            // Match vanilla sun/moon orientation: quad lies in the X-Z plane at y=0, normal up.
            // Texture flipped vs the legacy renderer to compensate for the model-view rotation we apply.
            builder.addVertex(m, -1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F);
            builder.addVertex(m, 1.0F, 0.0F, -1.0F).setUv(1.0F, 0.0F);
            builder.addVertex(m, 1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F);
            builder.addVertex(m, -1.0F, 0.0F, 1.0F).setUv(0.0F, 1.0F);
            try (MeshData meshData = builder.buildOrThrow()) {
                quadBuffer = RenderSystem.getDevice().createBuffer(() -> "Ad Astra planet quad", 40, meshData.vertexBuffer());
            }
        }
        return quadBuffer;
    }

    /**
     * Render all planet/sun/moon disc renderables for the current dimension, if any.
     * Called from {@link earth.terrarium.adastra.mixins.client.SkyRendererMixin} at the
     * tail of {@code SkyRenderer.renderSunMoonAndStars}.
     */
    public static void render(PoseStack poseStack, float timeOfDay) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        ModDimensionSpecialEffects effects = ClientPlatformUtils.getPlanetRenderers().get(level.dimension());
        if (effects == null) return;

        PlanetRenderer renderer = effects.renderer();
        if (renderer.skyRenderables().isEmpty()) return;

        // In orbit/space dimensions the camera is in free space — there's no ground horizon, so
        // bodies positioned BELOW the local up axis (e.g. Earth seen looking down from a space
        // station in earth_orbit) should still render. Suppress the horizon cull only there.
        boolean inSpace = earth.terrarium.adastra.common.planets.AdAstraData.isSpace(level.dimension());

        // We are injected at the TAIL of renderSunMoonAndStars, AFTER its popPose, so poseStack is in
        // the caller-provided sky frame (no time-of-day rotation applied). renderable globalRotation
        // is interpreted as absolute sky-space rotation.
        poseStack.pushPose();

        for (SkyRenderable renderable : renderer.skyRenderables()) {
            Vector3f globalRot = switch (renderable.movementType()) {
                case STATIC -> new Vector3f(
                    (float) renderable.globalRotation().x,
                    (float) renderable.globalRotation().y,
                    (float) renderable.globalRotation().z);
                case TIME_OF_DAY -> new Vector3f(
                    (float) renderable.globalRotation().x,
                    (float) renderable.globalRotation().y,
                    (float) renderable.globalRotation().z + timeOfDay * 360.0F);
                case TIME_OF_DAY_REVERSED -> new Vector3f(
                    (float) renderable.globalRotation().x,
                    (float) renderable.globalRotation().y,
                    (float) renderable.globalRotation().z - timeOfDay * 360.0F);
            };

            // Horizon cull: skip any renderable whose disc center (origin translated +100 Y, then
            // rotated by globalRot) sits at or below the local horizon plane. Otherwise small moons
            // (Phobos, Deimos, etc.) clip into surface terrain — the player sees them rendered
            // "through" the ground.
            if (!inSpace && skyY(globalRot) <= 0.05F) continue;

            // Optional back-light glow first (drawn larger/below on the additive CELESTIAL
            // pipeline so it leaks softly around the disc edges).
            if (renderable.backLightScale() > 0) {
                renderQuad(poseStack, globalRot, renderable.localRotation(),
                    renderable.backLightScale(),
                    earth.terrarium.adastra.client.utils.DimensionRenderingUtils.BACKLIGHT,
                    renderable.backLightColor(),
                    RenderPipelines.CELESTIAL);
            }

            // Body disc on the opaque, depth-writing pipeline so it occludes stars (drawn
            // before with no depth write) and any later-drawn celestial body that would
            // otherwise overpaint it (sun/moon/etc.).
            renderQuad(poseStack, globalRot, renderable.localRotation(),
                renderable.scale(), renderable.texture(), 0xFFFFFFFF,
                RenderPipelines.CELESTIAL);
        }

        poseStack.popPose();
    }

    /**
     * Returns the disc-center Y coordinate after applying the global rotation to the up vector
     * (0,1,0) — i.e. the unit-sphere Y of where the disc would sit before the +100 translate
     * and per-disc local rotation. Range [-1, +1]; <= 0 means at or below horizon.
     */
    private static float skyY(Vector3f globalRot) {
        // Match the X→Y→Z multiplication order applied by mulPose calls in renderQuad.
        // Rotate (0,1,0) by R_x then R_y then R_z, return the resulting Y.
        float rx = (float) Math.toRadians(globalRot.x);
        float ry = (float) Math.toRadians(globalRot.y);
        float rz = (float) Math.toRadians(globalRot.z);
        // Start (0,1,0). After R_x: (0, cos(rx), sin(rx)).
        float y1 = (float) Math.cos(rx);
        float z1 = (float) Math.sin(rx);
        // After R_y on (0, y1, z1): (sin(ry)*z1, y1, cos(ry)*z1). Y unchanged.
        // After R_z on (x2, y1, z2): y' = x2*sin(rz) + y1*cos(rz). x2 = sin(ry)*z1.
        float x2 = (float) Math.sin(ry) * z1;
        return x2 * (float) Math.sin(rz) + y1 * (float) Math.cos(rz);
    }

    private static void renderQuad(PoseStack poseStack, Vector3f globalRotation,
                                   net.minecraft.world.phys.Vec3 localRotation,
                                   float scale, Identifier texture, int colorArgb,
                                   RenderPipeline pipeline) {
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture tex = mc.getTextureManager().getTexture(texture);
        if (tex == null) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(globalRotation.x));
        poseStack.mulPose(Axis.YP.rotationDegrees(globalRotation.y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(globalRotation.z));
        // Translate up to the celestial radius (matches legacy y=100).
        poseStack.translate(0.0F, 100.0F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) localRotation.x));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) localRotation.y));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) localRotation.z));

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushMatrix();
        mvStack.mul(poseStack.last().pose());
        mvStack.scale(scale, 1.0F, scale);

        float r = ARGB.redFloat(colorArgb);
        float g = ARGB.greenFloat(colorArgb);
        float b = ARGB.blueFloat(colorArgb);
        float a = ARGB.alphaFloat(colorArgb);

        GpuBufferSlice transform = RenderSystem.getDynamicUniforms()
            .writeTransform(mvStack, new Vector4f(r, g, b, a), new Vector3f(), new Matrix4f());

        GpuTextureView color = mc.gameRenderer.mainRenderTarget().getColorTextureView();
        GpuTextureView depth = mc.gameRenderer.mainRenderTarget().getDepthTextureView();
        var quadIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = quadIndices.getBuffer(6);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "Ad Astra planet disc", color, Optional.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transform);
            pass.bindTexture("Sampler0", tex.getTextureView(), tex.getSampler());
            pass.setVertexBuffer(0, getOrCreateQuadBuffer().slice());
            pass.setIndexBuffer(indexBuffer, quadIndices.type());
            pass.drawIndexed(6, 1, 0, 0, 0);
        }

        mvStack.popMatrix();
        poseStack.popPose();
    }
}
