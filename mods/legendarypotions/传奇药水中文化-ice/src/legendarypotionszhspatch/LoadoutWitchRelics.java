package legendarypotionszhspatch;

import com.megacrit.cardcrawl.relics.AbstractRelic;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class LoadoutWitchRelics {
    private static final Set<AbstractRelic> LOADOUT_RELICS =
        Collections.newSetFromMap(new IdentityHashMap<AbstractRelic, Boolean>());

    private LoadoutWitchRelics() {
    }

    static void markIfLoadout(AbstractRelic relic) {
        if (relic == null || !isWitchRelic(relic) || !calledFromLoadout()) {
            return;
        }
        synchronized (LOADOUT_RELICS) {
            LOADOUT_RELICS.add(relic);
        }
    }

    static boolean consume(AbstractRelic relic) {
        if (relic == null) {
            return false;
        }
        synchronized (LOADOUT_RELICS) {
            return LOADOUT_RELICS.remove(relic);
        }
    }

    private static boolean isWitchRelic(AbstractRelic relic) {
        String id = relic.relicId;
        return id != null && (id.endsWith(":WitchsTrunk")
            || id.endsWith(":WitchsDoll")
            || id.endsWith(":WitchsWorkbench"));
    }

    private static boolean calledFromLoadout() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if ("loadout.LoadoutMod".equals(element.getClassName())
                    && "modifyPlayerRelics".equals(element.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
