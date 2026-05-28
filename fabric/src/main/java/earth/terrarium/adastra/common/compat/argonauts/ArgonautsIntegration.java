package earth.terrarium.adastra.common.compat.argonauts;

import com.mojang.authlib.GameProfile;
import com.teamresourceful.resourcefullib.common.utils.modinfo.ModInfoUtils;
// TODO: Argonauts dependency was removed. The following imports are commented out:
// import earth.terrarium.argonauts.api.client.guild.GuildClientApi;
// import earth.terrarium.argonauts.api.client.party.PartyClientApi;
// import earth.terrarium.argonauts.api.guild.Guild;
// import earth.terrarium.argonauts.api.party.Party;
// import earth.terrarium.argonauts.common.handlers.base.members.Member;

import java.util.List;
import java.util.UUID;

public class ArgonautsIntegration {

    public static boolean argonautsLoaded() {
        return ModInfoUtils.isModLoaded("argonauts");
    }

    // TODO: Re-implement when Argonauts dependency is restored
    public static List<GameProfile> getClientPartyMembers(UUID player) {
        return List.of();
    }

    // TODO: Re-implement when Argonauts dependency is restored
    public static List<GameProfile> getClientGuildMembers(UUID player) {
        return List.of();
    }
}
