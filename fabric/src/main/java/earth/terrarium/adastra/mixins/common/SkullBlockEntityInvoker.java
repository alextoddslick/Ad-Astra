package earth.terrarium.adastra.mixins.common;

import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

// In 1.21.11, SkullBlockEntity.fetchGameProfile was removed.
// Profile resolution is now handled through ResolvableProfile.resolveProfile().
// This invoker is kept as a placeholder but no longer exposes any methods.
@Mixin(SkullBlockEntity.class)
public interface SkullBlockEntityInvoker {
}
