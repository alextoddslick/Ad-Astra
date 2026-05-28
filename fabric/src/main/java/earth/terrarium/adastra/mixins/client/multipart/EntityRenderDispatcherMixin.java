package earth.terrarium.adastra.mixins.client.multipart;

import org.spongepowered.asm.mixin.Mixin;

// TODO: 1.21.11 - EntityRenderDispatcher.renderHitbox and LevelRenderer.renderLineBox no longer exist.
// The hitbox rendering system has been fundamentally changed. The multipart entity hitbox rendering
// needs to be reimplemented using the new rendering API (ShapeRenderer.renderShape or similar).
// Disabled for now as the debug hitbox rendering is non-essential functionality.
@Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderDispatcher")
public class EntityRenderDispatcherMixin {
    // Intentionally empty - multipart hitbox debug rendering disabled pending API migration
}
