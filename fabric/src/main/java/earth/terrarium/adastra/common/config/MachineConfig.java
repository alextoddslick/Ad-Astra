package earth.terrarium.adastra.common.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import earth.terrarium.adastra.common.config.machines.MachineTypeConfigObject;

@Category("config.ad_astra.machines")
@ConfigInfo(
    icon = "zap",
    title = "Machine Config"
)
public final class MachineConfig {

    @ConfigEntry(
        id = "ironTier",
        translation = "config.ad_astra.ironTier"
    )
    public static final MachineTypeConfigObject IRON = new MachineTypeConfigObject(
        100, 10_000, 0
    );

    @ConfigEntry(
        id = "steelTier",
        translation = "config.ad_astra.steelTier"
    )
    public static final MachineTypeConfigObject STEEL = new MachineTypeConfigObject(
        150, 20_000, 3000
    );

    @ConfigEntry(
        id = "deshTier",
        translation = "config.ad_astra.deshTier"
    )
    public static final MachineTypeConfigObject DESH = new MachineTypeConfigObject(
        250, 50_000, 5000
    );

    @ConfigEntry(
        id = "ostrumTier",
        translation = "config.ad_astra.ostrumTier"
    )
    public static final MachineTypeConfigObject OSTRUM = new MachineTypeConfigObject(
        500, 100_000, 10_000
    );

    @ConfigEntry(
        id = "coalGeneratorEnergyGenerationPerTick",
        translation = "config.ad_astra.coalGeneratorEnergyGenerationPerTick"
    )
    public static long coalGeneratorEnergyGenerationPerTick = 20;

    @ConfigEntry(
        id = "etrionicBlastFurnaceBlastingEnergyPerItem",
        translation = "config.ad_astra.etrionicBlastFurnaceBlastingEnergyPerItem"
    )
    public static long etrionicBlastFurnaceBlastingEnergyPerItem = 10;

    @ConfigEntry(
        id = "waterPumpEnergyPerTick",
        translation = "config.ad_astra.waterPumpEnergyPerTick"
    )
    public static long waterPumpEnergyPerTick = 20;

    @ConfigEntry(
        id = "waterPumpFluidGenerationPerTick",
        translation = "config.ad_astra.waterPumpFluidGenerationPerTick"
    )
    public static long waterPumpFluidGenerationPerTick = 50;

    @ConfigEntry(
        id = "energizerEnergyCapacity",
        translation = "config.ad_astra.energizerEnergyCapacity"
    )
    public static long energizerEnergyCapacity = 2_000_000;

    @ConfigEntry(
        id = "maxDistributionBlocks",
        translation = "config.ad_astra.maxDistributionBlocks"
    )
    @Comment("The maximum number of blocks that an oxygen distributor and gravity normalizer can distribute to.")
    public static int maxDistributionBlocks = 6000;

    @ConfigEntry(
        id = "distributionRefreshRate",
        translation = "config.ad_astra.distributionRefreshRate"
    )
    @Comment("The tick rate (20 ticks = 1 second) at which the oxygen distributor and gravity normalizer will recalculate the distribution area.")
    public static int distributionRefreshRate = 100;

    @ConfigEntry(
        id = "pipeRefreshRate",
        translation = "config.ad_astra.pipeRefreshRate"
    )
    @Comment("The tick rate (20 ticks = 1 second) at which cables and fluid pipes will recalculate their connections.")
    public static int pipeRefreshRate = 50;
}
