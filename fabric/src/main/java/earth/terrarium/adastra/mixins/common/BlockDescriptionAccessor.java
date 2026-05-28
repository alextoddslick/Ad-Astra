package earth.terrarium.adastra.mixins.common;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface BlockDescriptionAccessor {

    @Accessor("descriptionId")
    String adastra$getDescriptionId();

    @Accessor("descriptionId")
    @Mutable
    @Final
    void adastra$setDescriptionId(String descriptionId);
}
