package earth.terrarium.adastra.mixins.fabric.common.multipart;

import earth.terrarium.adastra.common.entities.multipart.MultipartEntity;
import earth.terrarium.adastra.common.entities.multipart.MultipartPartEntity;
import earth.terrarium.adastra.common.entities.multipart.MultipartPartsHolder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public class LevelMixin implements MultipartPartsHolder {

    @Unique
    private final Int2ObjectMap<Entity> adastra$multipartEntityParts = new Int2ObjectOpenHashMap<>();

    @Override
    public Int2ObjectMap<Entity> adastra$getParts() {
        return adastra$multipartEntityParts;
    }

    // TODO 26.1.2: multipart entity hit detection injects stubbed — the original
    // mixin targeted intermediary lambda names (method_31593, method_47576) which
    // don't resolve under 26.1's no-remap toolchain. Vehicle multipart hitboxes
    // will not be returned by Level.getEntities until this is reworked against
    // the new method signatures.
}
