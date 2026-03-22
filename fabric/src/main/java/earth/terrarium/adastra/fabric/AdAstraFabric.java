package earth.terrarium.adastra.fabric;

import earth.terrarium.adastra.AdAstra;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.common.commands.AdAstraCommands;
import earth.terrarium.adastra.common.registry.ModEntityTypes;
import earth.terrarium.adastra.common.items.armor.JetSuitItem;
import earth.terrarium.adastra.common.tags.ModItemTags;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class AdAstraFabric {

    public static void init() {
        AdAstra.init();
        onAddReloadListener();
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> AdAstra.onDatapackSync(player));
        ServerTickEvents.END_SERVER_TICK.register(AdAstra::onServerTick);
        ServerLifecycleEvents.SERVER_STARTED.register(AdAstra::onServerStarted);
        ModEntityTypes.registerAttributes((type, builder) -> FabricDefaultAttributeRegistry.register(type.get(), builder.get()));
        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, environment) -> AdAstraCommands.register(dispatcher));

        // Register custom elytra behavior for JetSuit (replaces FabricElytraItem which was removed)
        EntityElytraEvents.CUSTOM.register((entity, tickElytra) -> {
            var chestStack = entity.getItemBySlot(EquipmentSlot.CHEST);
            if (chestStack.getItem() instanceof JetSuitItem jetSuit) {
                if (!jetSuit.canElytraFly(chestStack, entity)) return false;
                if (tickElytra) {
                    jetSuit.elytraFlightTick(chestStack, entity, entity.getFallFlyingTicks());
                }
                return true;
            }
            return false;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            var stack = player.getItemInHand(hand);
            if (!level.isClientSide()
                && stack.is(ModItemTags.DESTROYED_IN_SPACE)
                && !OxygenApi.API.hasOxygen(level, hitResult.getBlockPos().immutable().relative(hitResult.getDirection()))
            ) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    public static void onAddReloadListener() {
        AdAstra.onAddReloadListener((id, listener) -> ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id;
            }

            @Override
            public @NotNull CompletableFuture<Void> reload(@NotNull PreparableReloadListener.SharedState sharedState, @NotNull Executor prepareExecutor, PreparableReloadListener.@NotNull PreparationBarrier synchronizer, @NotNull Executor applyExecutor) {
                return listener.reload(sharedState, prepareExecutor, synchronizer, applyExecutor);
            }
        }));
    }
}
