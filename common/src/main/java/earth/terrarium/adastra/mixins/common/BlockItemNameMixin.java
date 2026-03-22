package earth.terrarium.adastra.mixins.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes item names in 1.21.11:
 * 1. BlockItems: redirects to block description ID (block. prefix)
 * 2. Items with "unknown" description: recomputes from registry key
 */
@Mixin(Item.class)
public abstract class BlockItemNameMixin {

    @Shadow
    protected abstract String getDescriptionId();

    @Inject(method = "getName()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void adastra$getName(CallbackInfoReturnable<Component> cir) {
        Component fixed = fixName();
        if (fixed != null) cir.setReturnValue(fixed);
    }

    @Inject(method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void adastra$getNameStack(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        Component fixed = fixName();
        if (fixed != null) cir.setReturnValue(fixed);
    }

    private Component fixName() {
        Item self = (Item) (Object) this;

        // Fix BlockItems that lost their block name
        if (self instanceof BlockItem blockItem) {
            String blockDescId = blockItem.getBlock().getDescriptionId();
            String itemDescId = getDescriptionId();
            if (!itemDescId.equals(blockDescId) && blockDescId.startsWith("block.")) {
                return Component.translatable(blockDescId);
            }
        }

        // Fix items with unknown description ID (deferred registration issue)
        String descId = getDescriptionId();
        if (descId.contains("unknown")) {
            Identifier id = BuiltInRegistries.ITEM.getKey(self);
            if (id != null && !id.getPath().equals("air")) {
                return Component.translatable("item." + id.getNamespace() + "." + id.getPath());
            }
        }

        return null;
    }
}
