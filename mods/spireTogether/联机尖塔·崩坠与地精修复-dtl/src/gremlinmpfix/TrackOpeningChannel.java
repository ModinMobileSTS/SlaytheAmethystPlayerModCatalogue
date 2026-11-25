package gremlinmpfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;

@SpirePatch2(clz = ChannelAction.class, method = "update", paramtypez = {})
public final class TrackOpeningChannel {
    private TrackOpeningChannel() {}

    @SpirePostfixPatch
    public static void Postfix(ChannelAction __instance) {
        GremlinBattleState.onChannelUpdated(__instance);
    }
}
