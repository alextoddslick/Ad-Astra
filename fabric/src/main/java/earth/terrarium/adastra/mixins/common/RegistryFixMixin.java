package earth.terrarium.adastra.mixins.common;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ResourcefulLib creates items/blocks before they're registered, so their
 * descriptionId gets the fallback value. This mixin fixes it after registration
 * by calling the accessor mixins.
 */
@Mixin(Registry.class)
public interface RegistryFixMixin {

    @Inject(method = "register(Lnet/minecraft/core/Registry;Lnet/minecraft/resources/Identifier;Ljava/lang/Object;)Ljava/lang/Object;", at = @At("RETURN"))
    private static <V, T extends V> void adastra$fixDescriptionId(Registry<V> registry, Identifier id, T value, CallbackInfoReturnable<T> cir) {
        if (value instanceof ItemDescriptionAccessor item) {
            String descId = item.adastra$getDescriptionId();
            if (descId != null && descId.startsWith("item.unknown")) {
                item.adastra$setDescriptionId("item." + id.getNamespace() + "." + id.getPath());
            }
        }
        if (value instanceof BlockDescriptionAccessor block) {
            String descId = block.adastra$getDescriptionId();
            if (descId != null && descId.startsWith("block.unknown")) {
                block.adastra$setDescriptionId("block." + id.getNamespace() + "." + id.getPath());
            }
        }
    }
}
