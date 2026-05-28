package earth.terrarium.adastra.common.compat.cadmus;

import com.teamresourceful.resourcefullib.common.utils.modinfo.ModInfoUtils;
// TODO: Cadmus 2.0-alpha.5 API has changed - TeamApi.API.getId() and ClaimApi.API.getClaimInfo()
// may not exist or have different signatures. Re-implement when Cadmus 2.0 API stabilizes.
// import earth.terrarium.cadmus.api.claims.ClaimApi;
// import earth.terrarium.cadmus.api.teams.TeamApi;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class CadmusIntegration {

    public static boolean cadmusLoaded() {
        return ModInfoUtils.isModLoaded("cadmus");
    }

    // TODO: Cadmus 2.0 API changed - TeamApi.API.getId() and ClaimApi.API.claim() may have
    // different signatures. Re-implement when Cadmus 2.0 API stabilizes.
    public static void claim(ServerPlayer player, ChunkPos pos) {
        // var teamId = TeamApi.API.getId(player);
        // if (teamId != null) {
        //     ClaimApi.API.claim(player.serverLevel(), teamId, pos, false);
        // }
    }

    // TODO: Cadmus 2.0 API changed - ClaimApi.API.getClaimInfo() may have a different signature.
    // Re-implement when Cadmus 2.0 API stabilizes.
    public static boolean isClaimed(ServerLevel level, ChunkPos pos) {
        // return ClaimApi.API.getClaimInfo(level, pos).isPresent();
        return false;
    }

    // TODO: Re-implement when ClientClaims is available in Cadmus
    public static void addClientListeners(ResourceKey<Level> dimension) {
    }

    // TODO: Re-implement when ClientClaims is available in Cadmus
    public static void removeClientListeners(ResourceKey<Level> dimension) {
    }
}
