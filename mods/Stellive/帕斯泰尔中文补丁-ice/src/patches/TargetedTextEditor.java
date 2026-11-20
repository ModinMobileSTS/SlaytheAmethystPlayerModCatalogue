package patches;

import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

final class TargetedTextEditor extends ExprEditor {
    private final boolean quizOptions;

    TargetedTextEditor(boolean quizOptions) {
        this.quizOptions = quizOptions;
    }

    @Override
    public void edit(MethodCall call) throws CannotCompileException {
        String owner = call.getClassName();
        String method = call.getMethodName();
        String signature = call.getSignature();
        if (owner.equals("com.megacrit.cardcrawl.events.GenericEventDialog")
                && (method.equals("updateBodyText") || method.equals("setDialogOption"))
                && signature.equals("(Ljava/lang/String;)V")) {
            call.replace("{ $1 = patches.EventTextTranslation.translate($1); $proceed($$); }");
        } else if (quizOptions && owner.equals("com.megacrit.cardcrawl.events.GenericEventDialog")
                && method.equals("setDialogOption") && signature.equals("(Ljava/lang/String;Z)V")) {
            call.replace("{ $1 = patches.EventTextTranslation.quizOption($1); $proceed($$); }");
        } else if (owner.equals("com.megacrit.cardcrawl.screens.select.GridCardSelectScreen")
                && method.equals("open")
                && signature.equals("(Lcom/megacrit/cardcrawl/cards/CardGroup;ILjava/lang/String;Z)V")) {
            call.replace("{ $3 = patches.EventTextTranslation.translate($3); $proceed($$); }");
        }
    }
}
