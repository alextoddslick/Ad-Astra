package earth.terrarium.adastra.client.dimension;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import earth.terrarium.adastra.client.utils.DimensionRenderingUtils;
import earth.terrarium.adastra.mixins.client.LevelRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
// WeightedList is used transitively via PlanetRenderer.starColors()
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Custom sky renderer for Ad Astra dimensions.
 *
 * In 1.21.11, the rendering pipeline was completely overhauled:
 * - VertexBuffer was replaced by GpuBuffer
 * - RenderSystem.enableBlend/disableBlend/setShader/setShaderTexture etc. are all removed
 * - BufferUploader.drawWithShader() is removed
 * - FogRenderer.levelFogColor()/setupNoFog() are removed
 * - Rendering now uses a frame graph with RenderPipeline objects (RenderPipelines.STARS, .CELESTIAL, .SKY, etc.)
 * - Draw calls go through RenderPass objects obtained from the frame graph
 *
 * TODO: 1.21.11 - Fully reimplement sky rendering using the new pipeline:
 * - Stars: Build a GpuBuffer of star vertices, draw via RenderPipelines.STARS
 * - Celestial bodies (sun, moon, planets): Draw via RenderPipelines.CELESTIAL
 * - Sunrise: Draw via RenderPipelines.SUNRISE_SUNSET
 * - Sky dome: Draw via RenderPipelines.SKY
 * - All rendering must go through a RenderPass from the frame graph
 *
 * For now, this class retains the data model (renderer config, star generation)
 * but the actual rendering methods are stubbed out.
 */
public class ModSkyRenderer {

    private final PlanetRenderer renderer;

    // 26.2: com.mojang.blaze3d.vertex.Tesselator was removed. Tesselator.getInstance() used to
    // hand out a BufferBuilder backed by a shared, reusable ByteBufferBuilder; we now keep that
    // backing buffer ourselves and build BufferBuilders against it directly.
    private static final ByteBufferBuilder STAR_BYTE_BUFFER = new ByteBufferBuilder(1536);

    public ModSkyRenderer(PlanetRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Called to render the custom sky for this dimension.
     * TODO: 1.21.11 - This method needs to be completely reimplemented using the new rendering pipeline.
     * The old approach used RenderSystem state calls and VertexBuffer/BufferUploader which no longer exist.
     * The new approach should use RenderPass with appropriate RenderPipelines.
     */
    public void render(ClientLevel level, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        // Stubbed - rendering pipeline completely changed in 1.21.11
        // The old implementation used:
        // - RenderSystem.enableBlend/disableBlend (removed)
        // - RenderSystem.setShader/setShaderTexture (removed)
        // - VertexBuffer (removed, replaced by GpuBuffer)
        // - BufferUploader.drawWithShader (removed)
        // - FogRenderer.levelFogColor/setupNoFog (removed)
        // - GlStateManager.SourceFactor/DestFactor for blend modes (moved to RenderPipeline config)
    }

    public boolean inFog(Camera camera) {
        // TODO 26.1.2: doesMobEffectBlockSky removed; only fluid-based fog detection remains.
        var fogType = camera.getFluidInCamera();
        return fogType == FogType.POWDER_SNOW
            || fogType == FogType.LAVA;
    }

    /**
     * Generates star vertex data for the custom sky.
     * This can be used to populate a GpuBuffer for rendering through the new pipeline.
     */
    public MeshData generateStarMesh() {
        var random = RandomSource.create(10842);
        BufferBuilder builder = new BufferBuilder(STAR_BYTE_BUFFER, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < renderer.stars(); i++) {
            double x = random.nextFloat() * 2 - 1;
            double y = random.nextFloat() * 2 - 1;
            double z = random.nextFloat() * 2 - 1;
            double scale = 0.15 + random.nextFloat() * 0.1;

            double distance = x * x + y * y + z * z;

            if (distance >= 1 || distance <= 0.01) continue;

            distance = 1 / Math.sqrt(distance);
            x *= distance;
            y *= distance;
            z *= distance;

            double xScale = x * 100;
            double yScale = y * 100;
            double zScale = z * 100;

            double theta = Math.atan2(x, z);
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            double phi = Math.atan2(Math.sqrt(x * x + z * z), y);
            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);

            double rot = random.nextDouble() * Math.PI * 2;
            double sinRot = Math.sin(rot);
            double cosRot = Math.cos(rot);

            int color = renderer.starColors().getRandom(random).orElse(0xffffffff);

            for (int j = 0; j < 4; j++) {
                double xOffset = ((j & 2) - 1) * scale;
                double yOffset = ((j + 1 & 2) - 1) * scale;

                double rotatedX = xOffset * cosRot - yOffset * sinRot;
                double rotatedY = yOffset * cosRot + xOffset * sinRot;

                double transformedX = rotatedX * sinPhi;
                double transformedY = -rotatedX * cosPhi;

                builder.addVertex(
                        (float) (xScale + transformedY * sinTheta - rotatedY * cosTheta),
                        (float) (yScale + transformedX),
                        (float) (zScale + rotatedY * sinTheta + transformedY * cosTheta))
                    .setColor(color);
            }
        }

        return builder.buildOrThrow();
    }

    public void setSkyRenderableColor(ClientLevel level, float partialTick, int color) {
        // In 1.21.11, RenderSystem.setShaderColor() is removed.
        // Color tinting is now handled through the rendering pipeline or vertex colors.
        // TODO: 1.21.11 - Implement color application through the new rendering pipeline
    }

    public PlanetRenderer getRenderer() {
        return renderer;
    }
}
