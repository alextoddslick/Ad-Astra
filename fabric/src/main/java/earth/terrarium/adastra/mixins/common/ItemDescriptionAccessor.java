package earth.terrarium.adastra.mixins.common;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemDescriptionAccessor {

    @Accessor("descriptionId")
    String adastra$getDescriptionId();

    @Accessor("descriptionId")
    @Mutable
    @Final
    void adastra$setDescriptionId(String descriptionId);
}
