package earth.terrarium.adastra.client.utils;

import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
// TODO: 1.21.11 - ClampedItemPropertyFunction no longer exists. Item properties are now
// data-driven via item model definitions. Item property registration needs reworking.
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * Replacement for botarium's ClientHooks.
 * Uses vanilla Minecraft registration methods directly.
 * NOTE: EntityRenderers.register() and ItemProperties.register() are platform-specific
 * (private/restricted in the common Architectury module). Entity renderer and item property
 * registrations must be handled by platform-specific code (Fabric/NeoForge modules).
 */
public final class ClientRegistrationHooks {

    private ClientRegistrationHooks() {}

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderers(BlockEntityType<T> type, BlockEntityRendererProvider<T, S> provider) {
        BlockEntityRenderers.register(type, provider);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void registerEntityRenderer(RegistryEntry<EntityType<T>> type, EntityRendererProvider<T> provider) {
        // TODO: Platform-specific registration - EntityRenderers.register() is not accessible in common module.
        // This must be called from Fabric/NeoForge platform code instead.
        // On Fabric: EntityRendererRegistry.register(type.get(), provider)
        // On NeoForge: handled via EntityRenderersEvent.RegisterRenderers event
    }

    // TODO: 1.21.11 - ClampedItemPropertyFunction no longer exists.
    // Item properties are now data-driven via item model definitions.
    // This interface provides a temporary replacement for the function signature.
    @FunctionalInterface
    public interface ItemPropertyFunction {
        float call(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.client.multiplayer.ClientLevel level, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity entity, int seed);
    }

    public static void registerItemProperty(Item item, Identifier id, ItemPropertyFunction function) {
        // TODO: 1.21.11 - Item properties are now data-driven. This is a no-op.
        // Define item property overrides in item model JSON files instead.
    }

    public static void setRenderLayer(Block block, Object renderType) {
        // TODO: In 1.21.1, render layers are determined by the block model JSON, not set programmatically.
        // No-op - ensure block models specify the correct render type in their JSON definitions.
    }
}
