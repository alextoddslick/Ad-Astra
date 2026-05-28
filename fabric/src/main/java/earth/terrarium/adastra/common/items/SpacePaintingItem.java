package earth.terrarium.adastra.common.items;

import earth.terrarium.adastra.common.registry.ModItems;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpacePaintingItem extends HangingEntityItem {

    private final ResourceKey<PaintingVariant> defaultVariantKey;
    private final TagKey<PaintingVariant> variants;

    public SpacePaintingItem(Properties settings, ResourceKey<PaintingVariant> defaultVariantKey, TagKey<PaintingVariant> variants) {
        super(EntityType.PAINTING, settings);
        this.defaultVariantKey = defaultVariantKey;
        this.variants = variants;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos pos2 = pos.relative(direction);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        if (player != null && !mayPlace(player, direction, stack, pos2)) {
            return InteractionResult.FAIL;
        }

        Optional<Painting> optional = create(level, pos2, direction);
        if (optional.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        Painting painting = optional.get();

        TypedEntityData<EntityType<?>> entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData != null) {
            entityData.loadInto(painting);
        }
        if (painting.survives()) {
            if (!level.isClientSide()) {
                painting.playPlacementSound();
                level.gameEvent(player, GameEvent.ENTITY_PLACE, painting.blockPosition());
                level.addFreshEntity(painting);
            }
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    public Optional<Painting> create(Level level, BlockPos pos, Direction direction) {
        var registry = level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT);
        Optional<Holder.Reference<PaintingVariant>> defaultHolder = registry.get(defaultVariantKey);
        if (defaultHolder.isEmpty()) return Optional.empty();

        List<Holder<PaintingVariant>> list = new ArrayList<>();
        registry.getTagOrEmpty(variants).forEach(list::add);
        if (list.isEmpty()) return Optional.empty();

        list.removeIf(holder -> {
            Painting test = new Painting(level, pos, direction, holder);
            return !test.survives();
        });
        if (list.isEmpty()) return Optional.empty();

        int max = list.stream().mapToInt(SpacePaintingItem::variantArea).max().orElse(0);
        list.removeIf(holder -> variantArea(holder) < max);

        return Util.getRandomSafe(list, level.getRandom()).map(holder -> {
            Painting painting = new Painting(level, pos, direction, holder) {
                @Override
                public ItemEntity spawnAtLocation(ServerLevel serverLevel, ItemLike item) {
                    return super.spawnAtLocation(serverLevel, ModItems.SPACE_PAINTING.get());
                }

                @Override
                public ItemStack getPickResult() {
                    return new ItemStack(ModItems.SPACE_PAINTING.get());
                }
            };
            return painting;
        });
    }

    private static int variantArea(Holder<PaintingVariant> variant) {
        return variant.value().width() * variant.value().height();
    }
}
