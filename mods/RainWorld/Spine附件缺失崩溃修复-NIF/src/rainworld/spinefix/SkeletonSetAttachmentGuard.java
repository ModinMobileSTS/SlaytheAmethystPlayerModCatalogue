package rainworld.spinefix;

import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * Vanilla Spine 3.8 throws IllegalArgumentException from Skeleton.setAttachment(String, String) when the
 * requested attachment is missing from both the current skin and the skeleton's default skin. RainWorld
 * merges skins from its own SKIN_NAMES tables, and the merged skin can lack attachments that the loaded
 * skeleton still animates (for example "ear2" of Monk/yellowplus.json), which crashed the game.
 *
 * The guard keeps vanilla behaviour whenever the slot and attachment really exist, and instead of throwing
 * it skips the call (the slot keeps its previous attachment) for broken/unfinished Spine data. Each
 * distinct slot/attachment pair is logged once so the problem stays visible in the log.
 */
@SpirePatch2(clz = Skeleton.class, method = "setAttachment", paramtypez = {String.class, String.class})
public class SkeletonSetAttachmentGuard {

    private static final Set<String> REPORTED = new HashSet<>();

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Skeleton __instance, Object[] __args) {
        try {
            if (__instance == null || __args == null || __args.length < 2) {
                return SpireReturn.Continue();
            }

            String slotName = (String) __args[0];
            String attachmentName = (String) __args[1];

            if (slotName == null) {
                // Vanilla throws "slotName cannot be null"; leave that sanity check untouched.
                return SpireReturn.Continue();
            }

            Slot slot = __instance.findSlot(slotName);
            if (slot == null) {
                // Vanilla throws "Slot not found"; skipping is just as harmless.
                report(slotName, "<slot missing>");
                return SpireReturn.Return();
            }

            if (attachmentName == null) {
                // Vanilla clears the slot attachment in this case.
                return SpireReturn.Continue();
            }

            Attachment attachment = __instance.getAttachment(slot.getData().getIndex(), attachmentName);
            if (attachment != null) {
                return SpireReturn.Continue();
            }

            // Attachment genuinely missing from current skin and default skin: skip the vanilla throw.
            report(slotName, attachmentName);
            return SpireReturn.Return();
        } catch (Exception e) {
            // Never let the guard itself break the game.
            return SpireReturn.Continue();
        }
    }

    private static void report(String slotName, String attachmentName) {
        String key = slotName + "|" + attachmentName;
        if (REPORTED.size() < 64 && REPORTED.add(key)) {
            System.out.println("[RainWorld SpineFix] skip Skeleton.setAttachment: attachment \"" + attachmentName
                    + "\" not found for slot \"" + slotName + "\"");
        }
    }
}
