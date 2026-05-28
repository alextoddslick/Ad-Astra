package earth.terrarium.adastra.mixins.common;

import earth.terrarium.adastra.common.registry.RegistryIdContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.Properties.class)
public abstract class ItemPropertiesMixin {

    @Shadow
    private ResourceKey<Item> id;

    @Shadow
    private DependantName<Item, String> descriptionId;

    @Shadow
    private static DependantName<Item, String> BLOCK_DESCRIPTION_ID;

    @Shadow
    private static DependantName<Item, String> ITEM_DESCRIPTION_ID;

    @Inject(method = "itemIdOrThrow", at = @At("HEAD"), cancellable = true)
    private void adastra$itemIdOrThrow(CallbackInfoReturnable<ResourceKey<Item>> cir) {
        if (this.id == null) {
            Identifier contextId = RegistryIdContext.CURRENT_ID.get();
            if (contextId != null) {
                cir.setReturnValue(ResourceKey.create(Registries.ITEM, contextId));
            }
        }
    }

    @Inject(method = "effectiveDescriptionId", at = @At("HEAD"), cancellable = true)
    private void adastra$effectiveDescriptionId(CallbackInfoReturnable<String> cir) {
        if (this.id == null) {
            Identifier contextId = RegistryIdContext.CURRENT_ID.get();
            if (contextId != null) {
                // Use the DependantName to compute the correct prefix (block. or item.)
                String prefix = (this.descriptionId == BLOCK_DESCRIPTION_ID) ? "block" : "item";
                cir.setReturnValue(prefix + "." + contextId.getNamespace() + "." + contextId.getPath());
            } else {
                cir.setReturnValue("item.unknown.unknown");
            }
        }
    }

    @Inject(method = "effectiveModel", at = @At("HEAD"), cancellable = true)
    private void adastra$effectiveModel(CallbackInfoReturnable<Identifier> cir) {
        if (this.id == null) {
            Identifier contextId = RegistryIdContext.CURRENT_ID.get();
            if (contextId != null) {
                cir.setReturnValue(contextId);
            } else {
                cir.setReturnValue(Identifier.fromNamespaceAndPath("ad_astra", "unknown"));
            }
        }
    }
}
