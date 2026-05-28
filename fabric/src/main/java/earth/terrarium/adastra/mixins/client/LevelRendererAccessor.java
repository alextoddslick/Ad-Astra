package earth.terrarium.adastra.mixins.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    // TODO 26.1.2: doesMobEffectBlockSky removed from LevelRenderer.

    @Accessor
    int getTicks();

    @Accessor
    SkyRenderer getSkyRenderer();

    @Accessor
    LevelTargetBundle getTargets();
}
