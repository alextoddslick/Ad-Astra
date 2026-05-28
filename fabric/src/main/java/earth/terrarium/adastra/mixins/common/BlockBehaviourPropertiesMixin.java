package earth.terrarium.adastra.mixins.common;

import earth.terrarium.adastra.common.registry.RegistryIdContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BlockBehaviour.Properties.class)
public abstract class BlockBehaviourPropertiesMixin {

    @Shadow
    private ResourceKey<Block> id;

    @Inject(method = "effectiveDrops", at = @At("HEAD"), cancellable = true)
    private void adastra$effectiveDrops(CallbackInfoReturnable<Optional<ResourceKey<LootTable>>> cir) {
        if (this.id == null) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "effectiveDescriptionId", at = @At("HEAD"), cancellable = true)
    private void adastra$effectiveDescriptionId(CallbackInfoReturnable<String> cir) {
        if (this.id == null) {
            Identifier contextId = RegistryIdContext.CURRENT_ID.get();
            if (contextId != null) {
                cir.setReturnValue("block." + contextId.getNamespace() + "." + contextId.getPath());
            } else {
                cir.setReturnValue("block.unknown.unknown");
            }
        }
    }
}
