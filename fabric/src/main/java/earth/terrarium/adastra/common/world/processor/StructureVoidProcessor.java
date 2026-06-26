package earth.terrarium.adastra.common.world.processor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public class StructureVoidProcessor implements StructureProcessor {

    public static final MapCodec<StructureVoidProcessor> CODEC = MapCodec.unit(StructureVoidProcessor::new);

    private StructureVoidProcessor() {
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo blockInfo, StructurePlaceSettings data) {
        if (blockInfo.state().getBlock().equals(Blocks.STRUCTURE_VOID)) {
            return null;
        }
        if (level.getBlockState(blockInfo.pos()).isAir()) {
            return null;
        }

        return blockInfo;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }
}
