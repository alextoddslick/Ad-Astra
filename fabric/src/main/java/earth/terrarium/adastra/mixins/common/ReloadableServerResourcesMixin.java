package earth.terrarium.adastra.mixins.common;

import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * In Minecraft 26.1, items have a per-{@link net.minecraft.core.Holder.Reference}
 * {@code DataComponentMap} that must be bound by
 * {@link DataComponentInitializers.PendingComponents#apply()} before
 * {@code Item.CODEC_WITH_BOUND_COMPONENTS} (used by {@code ItemStack.CODEC})
 * will accept the item. Without this, recipe deserialization fails with
 * "Item ad_astra:foo does not have components yet" for every Ad Astra item
 * referenced as a recipe result.
 *
 * <p>The vanilla flow is:
 * <ol>
 *   <li>{@code ReloadableServerResources.loadResources(...)} calls the
 *       constructor with a freshly-built {@code newComponents} list.</li>
 *   <li>{@code SimpleReloadInstance} runs reload listeners (incl. recipes) —
 *       <b>components are NOT yet bound here</b>.</li>
 *   <li>{@code WorldLoader.lambda$load$4} (or
 *       {@code MinecraftServer#reloadResources}) calls
 *       {@code updateComponentsAndStaticRegistryTags()} which finally binds
 *       components.</li>
 * </ol>
 *
 * <p>Vanilla recipes (e.g. {@code ShapedRecipe}) work despite this ordering
 * because they store results as {@code ItemStackTemplate}, whose codec uses
 * {@code Item.CODEC} (no bound-components check) and defers
 * {@code ItemStack} construction. Ad Astra's recipes use
 * {@code ItemStackCodec.CODEC} from Resourceful Lib, which goes through
 * {@code ItemStack.CODEC} → {@code Item.CODEC_WITH_BOUND_COMPONENTS} and fails.</p>
 *
 * <p>Fix: at the tail of the {@code ReloadableServerResources} constructor —
 * AFTER {@code newComponents} has been assigned but BEFORE the constructor
 * returns and reload listeners begin running — eagerly apply each
 * {@link DataComponentInitializers.PendingComponents}. This binds every item's
 * components (the {@code BakedEntry} list includes fallback EMPTY entries for
 * items with no initializer plus the real maps for items that registered one
 * via {@code Item.<init>}), so subsequent recipe parsing sees a fully bound
 * registry.</p>
 *
 * <p>This is idempotent — {@code Holder.Reference#bindComponents} is just a
 * {@code putfield}, and the same map is computed deterministically on every
 * reload. The vanilla {@code updateComponentsAndStaticRegistryTags()} call
 * later in the reload flow rebinds the same maps to the same references with
 * no observable difference.</p>
 */
@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin {

    @Shadow
    @Final
    private List<DataComponentInitializers.PendingComponents<?>> newComponents;

    @Inject(
        method = "<init>(Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/world/flag/FeatureFlagSet;Lnet/minecraft/commands/Commands$CommandSelection;Ljava/util/List;Lnet/minecraft/server/permissions/PermissionSet;Ljava/util/List;)V",
        at = @At("TAIL")
    )
    private void adastra$bindComponentsEarly(
        LayeredRegistryAccess<RegistryLayer> layers,
        HolderLookup.Provider lookup,
        FeatureFlagSet flags,
        Commands.CommandSelection selection,
        List<Registry.PendingTags<?>> postponedTags,
        PermissionSet permissions,
        List<DataComponentInitializers.PendingComponents<?>> components,
        CallbackInfo ci
    ) {
        // Bind component maps to every item Holder.Reference BEFORE recipe
        // parsing runs, so Item.CODEC_WITH_BOUND_COMPONENTS doesn't reject
        // Ad Astra items registered via FabricResourcefulRegistry.
        for (DataComponentInitializers.PendingComponents<?> pending : this.newComponents) {
            pending.apply();
        }
    }
}
