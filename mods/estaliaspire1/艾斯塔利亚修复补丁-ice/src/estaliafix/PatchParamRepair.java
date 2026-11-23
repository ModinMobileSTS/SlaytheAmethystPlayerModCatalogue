package estaliafix;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Instanceof;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 艾斯塔利亚模组里大量老式 @SpirePatch 钩子是按“名字”写的（例如
 * after(AbstractCard card)，只写想要的参数，不写目标实例），而 ModTheSpire
 * 对老式 @SpirePatch 是按位置匹配参数的：第 1 个参数总是目标实例 $0。
 * 于是注入代码变成 after($0)，与该类里真实存在的方法签名对不上，
 * 启动时抛 PatchingException: ... not found in ...，游戏无法进入主菜单。
 *
 * 本类用 @SpireInstrumentPatch（应用顺序 -1，早于 Postfix 的 2）在补丁真正
 * 被应用之前、遍历父模组的全部类，逐个修正这类钩子：
 *   1) 如果参数本来就对得上（例如 after(boolean __result, AbstractCard __instance)），
 *      不做任何改动；
 *   2) 如果整体前面补一个“目标实例”参数后就能对上，则把原方法复制成
 *      after__estaliafix(...)，再生成 after(目标类, 原参数...) 委托给它，
 *      同时改写原方法的描述符，让 ModTheSpire 按新参数列表生成调用。
 *      这样行为完全保留；
 *   3) 如果补上实例参数也对不上（参数顺序本来就是错的），则补一个参数类型与
 *      生成调用完全一致的“空实现”重载，保证注入代码能编译、游戏能启动。
 */
public class PatchParamRepair {

    private static final String PARENT_MOD_ID = "estaliaspire1";
    private static final String SPIRE_PATCH = "com.evacipated.cardcrawl.modthespire.lib.SpirePatch";
    private static final String SPIRE_PATCHES = "com.evacipated.cardcrawl.modthespire.lib.SpirePatches";
    private static final String HOOK_POSTFIX = "com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch";
    private static final String HOOK_PREFIX = "com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch";
    private static final String BY_REF = "com.evacipated.cardcrawl.modthespire.lib.ByRef";
    private static final String SPIRE_RETURN = "com.evacipated.cardcrawl.modthespire.lib.SpireReturn";
    private static final String SUFFIX = "__estaliafix";

    private static boolean repaired = false;

    /**
     * 触发点：这个钩子本身是坏的（after(AbstractCard) 被用在
     * CardGroup.moveToExhaustPile 上），用它来启动一次全局修复。
     */
    @SpirePatch(cls = "estalia.Patches$Exhausted", method = "after", paramtypez = {AbstractCard.class})
    public static class Trigger {

        @SpireInstrumentPatch
        public static ExprEditor instrument() {
            return new ExprEditor() {
                private boolean once = false;

                private void run(CtClass ct) {
                    if (once || ct == null) {
                        return;
                    }
                    once = true;
                    try {
                        repairAll(ct.getClassPool());
                    } catch (Throwable t) {
                        log("global repair failed: " + t);
                    }
                }

                @Override
                public void edit(MethodCall m) {
                    run(m.getEnclosingClass());
                }

                @Override
                public void edit(FieldAccess f) {
                    run(f.getEnclosingClass());
                }

                @Override
                public void edit(NewExpr e) {
                    run(e.getEnclosingClass());
                }

                @Override
                public void edit(Instanceof i) {
                    run(i.getEnclosingClass());
                }
            };
        }
    }

    private static void log(String msg) {
        System.out.println("[EstaliaFix] " + msg);
    }

    private static void repairAll(ClassPool pool) {
        if (repaired) {
            return;
        }
        repaired = true;
        List<String> names = parentClassNames();
        for (String name : names) {
            try {
                repairClass(pool, pool.get(name));
            } catch (Throwable t) {
                log("skip " + name + ": " + t);
            }
        }
        log("parameter repair pass done, scanned " + names.size() + " classes");
    }

    private static List<String> parentClassNames() {
        List<String> names = new ArrayList<String>();
        ModInfo[] infos = Loader.MODINFOS;
        if (infos == null) {
            return names;
        }
        for (int i = 0; i < infos.length; i++) {
            ModInfo info = infos[i];
            if (info == null || !PARENT_MOD_ID.equals(info.ID) || info.jarURL == null) {
                continue;
            }
            JarFile jar = null;
            try {
                jar = new JarFile(new File(info.jarURL.toURI()));
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.endsWith(".class")) {
                        names.add(entry.substring(0, entry.length() - 6).replace('/', '.'));
                    }
                }
            } catch (Throwable t) {
                log("cannot read parent jar: " + t);
            } finally {
                if (jar != null) {
                    try {
                        jar.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return names;
    }

    private static final class RawPatch {
        String targetClass;
        String method;
        List<String> paramtypez;
    }

    private static void repairClass(ClassPool pool, CtClass ct) {
        List<RawPatch> patches = readPatches(ct);
        if (patches.isEmpty()) {
            return;
        }
        for (int p = 0; p < patches.size(); p++) {
            RawPatch patch = patches.get(p);
            if (patch.targetClass == null || patch.targetClass.isEmpty()) {
                continue;
            }
            CtClass target;
            try {
                target = pool.get(patch.targetClass);
            } catch (Throwable t) {
                continue;
            }
            CtBehavior toPatch = resolveTarget(pool, target, patch);
            if (toPatch == null) {
                continue;
            }
            CtMethod[] hooks = ct.getDeclaredMethods();
            for (int i = 0; i < hooks.length; i++) {
                CtMethod hook = hooks[i];
                try {
                    if (!Modifier.isStatic(hook.getModifiers())) {
                        continue;
                    }
                    boolean postfix = hook.hasAnnotation(HOOK_POSTFIX) || "Postfix".equals(hook.getName());
                    boolean prefix = hook.hasAnnotation(HOOK_PREFIX) || "Prefix".equals(hook.getName());
                    if (!postfix && !prefix) {
                        continue;
                    }
                    if (hasByRef(hook)) {
                        continue;
                    }
                    fixHook(ct, hook, toPatch, postfix);
                } catch (Throwable t) {
                    log("hook " + ct.getName() + "#" + hook.getName() + " failed: " + t);
                }
            }
        }
    }

    private static List<RawPatch> readPatches(CtClass ct) {
        List<RawPatch> out = new ArrayList<RawPatch>();
        try {
            AnnotationsAttribute attrs = (AnnotationsAttribute)
                    ct.getClassFile2().getAttribute(AnnotationsAttribute.visibleTag);
            if (attrs == null) {
                attrs = (AnnotationsAttribute) ct.getClassFile2().getAttribute(AnnotationsAttribute.invisibleTag);
            }
            if (attrs == null) {
                return out;
            }
            Annotation[] anns = attrs.getAnnotations();
            for (int i = 0; i < anns.length; i++) {
                Annotation an = anns[i];
                if (an == null) {
                    continue;
                }
                String type = an.getTypeName();
                if (SPIRE_PATCH.equals(type)) {
                    out.add(toRawPatch(an));
                } else if (SPIRE_PATCHES.equals(type)) {
                    MemberValue value = an.getMemberValue("value");
                    if (value instanceof ArrayMemberValue) {
                        MemberValue[] items = ((ArrayMemberValue) value).getValue();
                        for (int k = 0; k < items.length; k++) {
                            if (items[k] instanceof AnnotationMemberValue) {
                                Annotation inner = ((AnnotationMemberValue) items[k]).getValue();
                                if (inner != null) {
                                    out.add(toRawPatch(inner));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log("cannot read annotations of " + ct.getName() + ": " + t);
        }
        return out;
    }

    private static RawPatch toRawPatch(Annotation an) {
        RawPatch patch = new RawPatch();
        patch.method = stringMember(an, "method");
        String clz = classNameMember(an, "clz");
        String cls = stringMember(an, "cls");
        if (clz != null && !clz.isEmpty() && !"void".equals(clz)) {
            patch.targetClass = clz;
        } else if (cls != null && !cls.isEmpty()) {
            patch.targetClass = cls;
        }
        List<String> types = classListMember(an, "paramtypez");
        if (types != null && types.size() == 1 && "void".equals(types.get(0))) {
            types = null;
        }
        patch.paramtypez = types;
        return patch;
    }

    private static String normalizeClassName(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.startsWith("L") && s.endsWith(";")) {
            s = s.substring(1, s.length() - 1);
        }
        s = s.replace('/', '.');
        if ("V".equals(s) || "void".equals(s)) {
            return "void";
        }
        return s;
    }

    private static String stringMember(Annotation an, String name) {
        MemberValue value = an.getMemberValue(name);
        if (value instanceof StringMemberValue) {
            return ((StringMemberValue) value).getValue();
        }
        return null;
    }

    private static String classNameMember(Annotation an, String name) {
        MemberValue value = an.getMemberValue(name);
        if (value instanceof ClassMemberValue) {
            return normalizeClassName(((ClassMemberValue) value).getValue());
        }
        return null;
    }

    private static List<String> classListMember(Annotation an, String name) {
        MemberValue value = an.getMemberValue(name);
        if (!(value instanceof ArrayMemberValue)) {
            return null;
        }
        MemberValue[] items = ((ArrayMemberValue) value).getValue();
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof ClassMemberValue) {
                String n = normalizeClassName(((ClassMemberValue) items[i]).getValue());
                if (n != null) {
                    out.add(n);
                }
            }
        }
        return out;
    }

    private static CtBehavior resolveTarget(ClassPool pool, CtClass target, RawPatch patch) {
        String method = patch.method;
        if (method == null || method.isEmpty() || "<class>".equals(method) || "<staticinit>".equals(method)) {
            return null;
        }
        try {
            if ("<ctor>".equals(method)) {
                CtConstructor[] ctors = target.getDeclaredConstructors();
                if (patch.paramtypez != null && !patch.paramtypez.isEmpty()) {
                    CtClass[] types = toTypes(pool, patch.paramtypez);
                    for (int i = 0; i < ctors.length; i++) {
                        if (sameTypes(ctors[i].getParameterTypes(), types)) {
                            return ctors[i];
                        }
                    }
                    return null;
                }
                return ctors.length == 1 ? ctors[0] : null;
            }
            if (patch.paramtypez != null && !patch.paramtypez.isEmpty()) {
                return target.getDeclaredMethod(method, toTypes(pool, patch.paramtypez));
            }
            CtMethod[] methods = target.getDeclaredMethods(method);
            return methods.length == 1 ? methods[0] : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static CtClass[] toTypes(ClassPool pool, List<String> names) throws Exception {
        CtClass[] types = new CtClass[names.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = pool.get(names.get(i));
        }
        return types;
    }

    private static boolean sameTypes(CtClass[] a, CtClass[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasByRef(CtBehavior hook) {
        try {
            MethodInfo info = hook.getMethodInfo2();
            ParameterAnnotationsAttribute attrs = (ParameterAnnotationsAttribute)
                    info.getAttribute(ParameterAnnotationsAttribute.visibleTag);
            if (attrs == null) {
                attrs = (ParameterAnnotationsAttribute) info.getAttribute(ParameterAnnotationsAttribute.invisibleTag);
            }
            if (attrs == null) {
                return false;
            }
            Annotation[][] all = attrs.getAnnotations();
            for (int i = 0; i < all.length; i++) {
                if (all[i] == null) {
                    continue;
                }
                for (int k = 0; k < all[i].length; k++) {
                    if (BY_REF.equals(all[i][k].getTypeName())) {
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    private static final class Mapping {
        boolean ok = true;
        final List<CtClass> argTypes = new ArrayList<CtClass>();
    }

    private static Mapping analyse(CtClass[] declared, String[] names, CtClass targetClass,
                                   CtClass[] targetArgs, boolean targetStatic, CtClass ret, boolean postfix) {
        Mapping mapping = new Mapping();
        int src = 0;
        for (int j = 0; j < declared.length; j++) {
            if (postfix && j == 0 && !ret.equals(CtClass.voidType) && declared[0].equals(ret)) {
                // ModTheSpire 的 $_ 结果参数，不消耗目标参数位置
                mapping.argTypes.add(ret);
                continue;
            }
            CtClass actual;
            if (targetStatic) {
                // 静态目标方法没有 $0，ModTheSpire 会把位置整体后移一位：第 j 个参数对应 $j+1
                int position = src + 1;
                actual = position <= targetArgs.length
                        ? targetArgs[position - 1]
                        : fieldType(targetClass, names != null && j < names.length ? names[j] : null);
            } else if (src == 0) {
                actual = targetClass;
            } else if (src <= targetArgs.length) {
                actual = targetArgs[src - 1];
            } else {
                actual = fieldType(targetClass, names != null && j < names.length ? names[j] : null);
            }
            mapping.argTypes.add(actual);
            if (!compatible(declared[j], actual)) {
                mapping.ok = false;
            }
            src++;
        }
        return mapping;
    }

    private static boolean compatible(CtClass declared, CtClass actual) {
        if (declared == null || actual == null) {
            return false;
        }
        if (declared.isPrimitive() || actual.isPrimitive()) {
            return declared.equals(actual);
        }
        try {
            return actual.subtypeOf(declared);
        } catch (Throwable t) {
            return false;
        }
    }

    private static CtClass fieldType(CtClass targetClass, String paramName) {
        if (paramName == null || !paramName.startsWith("___")) {
            return null;
        }
        try {
            return targetClass.getDeclaredField(paramName.substring(3)).getType();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String[] paramNames(CtBehavior behavior, int count) {
        String[] out = new String[count];
        try {
            CodeAttribute code = behavior.getMethodInfo2().getCodeAttribute();
            if (code == null) {
                return out;
            }
            LocalVariableAttribute table = (LocalVariableAttribute) code.getAttribute(LocalVariableAttribute.tag);
            if (table == null) {
                return out;
            }
            int offset = Modifier.isStatic(behavior.getModifiers()) ? 0 : 1;
            int index = 0;
            for (int i = 0; i < table.tableLength() && index < count; i++) {
                if (table.startPc(i) != 0) {
                    continue;
                }
                if (table.index(i) < offset) {
                    continue;
                }
                out[index++] = table.variableName(i);
            }
        } catch (Throwable t) {
            // 没有 LocalVariableTable 时返回空名字
        }
        return out;
    }

    private static void fixHook(CtClass ct, CtMethod hook, CtBehavior toPatch, boolean postfix) throws Exception {
        CtClass targetClass = toPatch.getDeclaringClass();
        CtClass[] targetArgs = toPatch.getParameterTypes();
        CtClass[] declared = hook.getParameterTypes();
        String[] names = paramNames(hook, declared.length);
        CtClass ret = hook.getReturnType();

        boolean targetStatic = Modifier.isStatic(toPatch.getModifiers());
        Mapping plain = analyse(declared, names, targetClass, targetArgs, targetStatic, ret, postfix);
        if (plain.ok) {
            return;
        }

        if (!targetStatic) {
            CtClass[] shifted = new CtClass[declared.length + 1];
            shifted[0] = targetClass;
            System.arraycopy(declared, 0, shifted, 1, declared.length);
            String[] shiftedNames = new String[names.length + 1];
            shiftedNames[0] = "__instance";
            System.arraycopy(names, 0, shiftedNames, 1, names.length);

            if (analyse(shifted, shiftedNames, targetClass, targetArgs, false, ret, postfix).ok) {
                prependFix(ct, hook, targetClass, declared, ret);
                return;
            }
        }
        neutralise(ct, hook, plain.argTypes, ret);
    }

    private static void prependFix(CtClass ct, CtMethod hook, CtClass targetClass,
                                   CtClass[] declared, CtClass ret) throws Exception {
        String name = hook.getName();
        String original = name + SUFFIX;
        try {
            ct.removeMethod(ct.getDeclaredMethod(original, declared));
        } catch (Throwable ignored) {
            // 不存在旧副本
        }
        CtMethod copy = CtNewMethod.copy(hook, original, ct, null);
        ct.addMethod(copy);
        ct.removeMethod(hook);

        StringBuilder signature = new StringBuilder(declare(targetClass, "p0"));
        StringBuilder call = new StringBuilder();
        for (int i = 0; i < declared.length; i++) {
            signature.append(", ").append(declare(declared[i], "p" + (i + 1)));
            if (i > 0) {
                call.append(", ");
            }
            call.append('p').append(i + 1);
        }
        StringBuilder src = new StringBuilder("public static ");
        src.append(ret.getName()).append(' ').append(name).append('(').append(signature).append(") { ");
        if (!ret.equals(CtClass.voidType)) {
            src.append("return ");
        }
        src.append(original).append('(').append(call).append("); }");
        ct.addMethod(CtNewMethod.make(src.toString(), ct));

        CtClass[] newParams = new CtClass[declared.length + 1];
        newParams[0] = targetClass;
        System.arraycopy(declared, 0, newParams, 1, declared.length);
        hook.getMethodInfo2().setDescriptor(Descriptor.ofMethod(ret, newParams));
        log("repaired " + ct.getName() + "#" + name + "(" + signature + ")");
    }

    private static void neutralise(CtClass ct, CtMethod hook, List<CtClass> argTypes, CtClass ret) throws Exception {
        String name = hook.getName();
        CtClass[] types = new CtClass[argTypes.size()];
        StringBuilder signature = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            types[i] = argTypes.get(i);
            if (types[i] == null) {
                log("cannot neutralise " + ct.getName() + "#" + name + ": unknown parameter type");
                return;
            }
            if (i > 0) {
                signature.append(", ");
            }
            signature.append(declare(types[i], "a" + i));
        }
        try {
            ct.getDeclaredMethod(name, types);
            return;
        } catch (Throwable ignored) {
            // 需要的重载还不存在
        }
        StringBuilder src = new StringBuilder("public static ");
        src.append(ret.getName()).append(' ').append(name).append('(').append(signature).append(") { ")
                .append(defaultReturn(ret)).append(" }");
        ct.addMethod(CtNewMethod.make(src.toString(), ct));
        log("neutralised " + ct.getName() + "#" + name + "(" + signature + ")");
    }

    private static String declare(CtClass type, String name) {
        return type.getName() + " " + name;
    }

    private static String defaultReturn(CtClass ret) {
        if (ret.equals(CtClass.voidType)) {
            return "";
        }
        String name = ret.getName();
        if (SPIRE_RETURN.equals(name)) {
            return "return com.evacipated.cardcrawl.modthespire.lib.SpireReturn.Continue();";
        }
        if ("boolean".equals(name)) {
            return "return false;";
        }
        if ("char".equals(name)) {
            return "return (char) 0;";
        }
        if ("long".equals(name)) {
            return "return 0L;";
        }
        if ("float".equals(name)) {
            return "return 0.0f;";
        }
        if ("double".equals(name)) {
            return "return 0.0d;";
        }
        if ("byte".equals(name) || "short".equals(name) || "int".equals(name)) {
            return "return 0;";
        }
        return "return null;";
    }
}