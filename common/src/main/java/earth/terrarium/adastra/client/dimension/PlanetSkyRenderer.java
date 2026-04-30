package earth.terrarium.adastra.client.dimension;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
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

import java.util.OptionalDouble;
import java.util.OptionalInt;

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

    /**
     * Opaque depth-writing pipeline for the planet/sun/moon body discs.
     *
     * Mirrors vanilla {@link RenderPipelines#CELESTIAL} (POSITION_TEX + core/position_tex
     * shader) but enables depth write and uses TRANSLUCENT blend so the texture's alpha
     * channel still gives soft edges. With {@code withDepthWrite(true)} the disc occludes
     * any later-drawn celestial geometry at greater depth, which fixes the bug where stars
     * (drawn first by vanilla, no depth write) and other discs were visible through Earth.
     *
     * Stays inside the existing custom sky pass — that pass attaches the main render
     * target's depth view directly via createRenderPass, so depth write works without
     * touching the FrameGraph wiring.
     */
    private static final RenderPipeline PLANET_DISC_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("adastra", "pipeline/planet_disc"))
        .withVertexShader("core/position_tex")
        .withFragmentShader("core/position_tex")
        .withSampler("Sampler0")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(true)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .build();

    @Nullable
    private static GpuBuffer quadBuffer;

    private PlanetSkyRenderer() {}

    private static GpuBuffer getOrCreateQuadBuffer() {
        if (quadBuffer != null) return quadBuffer;
        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(4 * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
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
                PLANET_DISC_PIPELINE);
        }

        poseStack.popPose();
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

        GpuTextureView color = mc.getMainRenderTarget().getColorTextureView();
        GpuTextureView depth = mc.getMainRenderTarget().getDepthTextureView();
        var quadIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = quadIndices.getBuffer(6);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "Ad Astra planet disc", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transform);
            pass.bindTexture("Sampler0", tex.getTextureView(), tex.getSampler());
            pass.setVertexBuffer(0, getOrCreateQuadBuffer());
            pass.setIndexBuffer(indexBuffer, quadIndices.type());
            pass.drawIndexed(0, 0, 6, 1);
        }

        mvStack.popMatrix();
        poseStack.popPose();
    }
}
