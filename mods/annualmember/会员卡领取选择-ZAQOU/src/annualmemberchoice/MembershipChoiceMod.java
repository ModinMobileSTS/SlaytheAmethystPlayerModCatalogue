package annualmemberchoice;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import org.apache.logging.log4j.LogManager;

@SpireInitializer
public class MembershipChoiceMod {

    public static void initialize() {
        LogManager.getLogger("AnnualMemberChoice").info(
                "[AnnualMemberChoice] loaded: the membership card is now claimed by choice at run start");
    }
}
