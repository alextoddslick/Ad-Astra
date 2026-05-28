package earth.terrarium.adastra.common.blocks.machines;

import earth.terrarium.adastra.common.blockentities.machines.EnergizerBlockEntity;
import earth.terrarium.adastra.common.blocks.base.MachineBlock;
import earth.terrarium.adastra.common.registry.ModItems;
import earth.terrarium.adastra.common.utils.EnergyUtils;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import earth.terrarium.common_storage_lib.energy.impl.SimpleValueStorage;
import earth.terrarium.common_storage_lib.storage.base.ValueStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

@SuppressWarnings("deprecation")
public class EnergizerBlock extends MachineBlock {

    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 5);

    public EnergizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(LIT, false)
            .setValue(POWER, 0));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EnergizerBlockEntity entity)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (entity.getItem(0).isEmpty() && stack.getCount() == 1) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            entity.setItem(0, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EnergizerBlockEntity entity)) {
            return InteractionResult.PASS;
        }

        if (!entity.getItem(0).isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, entity.getItem(0));
            entity.clearContent();
        } else {
            long stored = entity.getEnergyStorage().getStoredAmount();
            long capacity = entity.getEnergyStorage().getCapacity();
            player.sendSystemMessage(TooltipUtils.getEnergyComponent(stored, capacity));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnergizerBlockEntity entity) {
            return (int) (entity.getEnergyStorage().getStoredAmount() / (float) entity.getEnergyStorage().getCapacity() * 15);
        }
        return 0;
    }

    @Override
    public List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof EnergizerBlockEntity entity)) return super.getDrops(blockState, builder);
        ItemStack stack = ModItems.ENERGIZER.get().getDefaultInstance();
        SimpleValueStorage itemEnergyContainer = EnergyUtils.getItemEnergyStorageOrNull(stack);
        if (itemEnergyContainer == null) return super.getDrops(blockState, builder);
        itemEnergyContainer.set(entity.getEnergyStorage().getStoredAmount());
        EnergyUtils.saveItemEnergyStorage(stack, itemEnergyContainer);
        return List.of(stack);
    }
}