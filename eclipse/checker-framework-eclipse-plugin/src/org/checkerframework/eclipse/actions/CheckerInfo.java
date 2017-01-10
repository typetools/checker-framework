package org.checkerframework.eclipse.actions;

import java.util.*;
import org.checkerframework.checker.fenum.FenumChecker;
import org.checkerframework.checker.formatter.FormatterChecker;
import org.checkerframework.checker.guieffect.GuiEffectChecker;
import org.checkerframework.checker.i18n.I18nChecker;
import org.checkerframework.checker.i18nformatter.I18nFormatterChecker;
import org.checkerframework.checker.index.IndexChecker;
import org.checkerframework.checker.interning.InterningChecker;
import org.checkerframework.checker.linear.LinearChecker;
import org.checkerframework.checker.lock.LockChecker;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.propkey.PropertyKeyChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.checker.signature.SignatureChecker;
import org.checkerframework.checker.signedness.SignednessChecker;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.checker.units.UnitsChecker;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.subtyping.SubtypingChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.eclipse.util.PluginUtil;
import org.checkerframework.framework.source.SourceChecker;

/**
 * Stores information that describes a particular checker such as its label, the class to run, or
 * the quals path that is associate with it.
 *
 * @author asumu
 */
public class CheckerInfo {

    private static Map<String, CheckerInfo> checkers;

    private final String label;
    private final String processor;
    private final String qualsPath;

    public static String[] splitAtUppercase(final String toSplit) {
        List<String> tokens = new ArrayList<String>();

        int length = toSplit.length();

        int start = 0;
        for (int i = 0; i < length; i++) {
            if ((Character.isUpperCase(toSplit.charAt(i)) && i != 0)) {
                tokens.add(toSplit.substring(start, i));
                start = i;
            }
        }

        tokens.add(toSplit.substring(start, length));
        return tokens.toArray(new String[tokens.size()]);
    }

    private static void initCheckers() {
        if (checkers == null) {
            final List<CheckerInfo> checkerList =
                    Arrays.asList(
                            new CheckerInfo("Nullness Checker", NullnessChecker.class),
                            new CheckerInfo("Interning Checker", InterningChecker.class),
                            new CheckerInfo("Lock Checker", LockChecker.class),
                            new CheckerInfo("Fenum Checker", FenumChecker.class),
                            new CheckerInfo("Index Checker", IndexChecker.class),
                            new CheckerInfo("Tainting Checker", TaintingChecker.class),
                            new CheckerInfo("Regex Checker", RegexChecker.class),
                            new CheckerInfo("Format String Checker", FormatterChecker.class),
                            new CheckerInfo(
                                    "I18n Format String Checker", I18nFormatterChecker.class),
                            new CheckerInfo("Property File Checker", PropertyKeyChecker.class),
                            new CheckerInfo("I18n Checker", I18nChecker.class),
                            new CheckerInfo("Signature Checker", SignatureChecker.class),
                            new CheckerInfo("GUI Effect Checker", GuiEffectChecker.class),
                            new CheckerInfo("Units Checker", UnitsChecker.class),
                            new CheckerInfo("Signedness Checker", SignednessChecker.class),
                            new CheckerInfo("Constant Value Checker", ValueChecker.class),
                            new CheckerInfo("Aliasing Checker", AliasingChecker.class),
                            new CheckerInfo("Linear Checker", LinearChecker.class),
                            new CheckerInfo("Subtyping Checker", SubtypingChecker.class, null));

            final Map<String, CheckerInfo> modifiableCheckers =
                    new LinkedHashMap<String, CheckerInfo>();
            for (final CheckerInfo checkerInfo : checkerList) {
                modifiableCheckers.put(checkerInfo.getClassPath(), checkerInfo);
            }
            checkers = Collections.unmodifiableMap(modifiableCheckers);
        }
    }

    public static String checkerToQuals(Class<? extends SourceChecker> checker) {
        return checker.getPackage().getName() + ".qual.*";
    }

    public static List<CheckerInfo> getCheckers() {
        initCheckers();
        return new ArrayList<CheckerInfo>(checkers.values());
    }

    public static Map<String, CheckerInfo> getPathToCheckerInfo() {
        initCheckers();
        return checkers;
    }

    public static CheckerInfo fromClassPath(final String classPath, final String qualsPath) {
        String str = "fromClassPath(" + classPath + ", " + qualsPath + ")";
        try {
            final String[] pathTokens = classPath.split("\\.");
            str += "[" + PluginUtil.join(" ", pathTokens) + "]";
            final String className =
                    PluginUtil.join(" ", splitAtUppercase(pathTokens[pathTokens.length - 1]));
            str += "className = " + className;
            return new CheckerInfo(className, classPath, qualsPath);
        } catch (Exception e) {
            throw new RuntimeException(str, e);
        }
    }

    /**
     * Sets the name and processor accordingly.
     *
     * @param label
     * @param checker
     */
    public CheckerInfo(final String label, final Class<? extends SourceChecker> checker) {
        this(label, checker.getCanonicalName(), checkerToQuals(checker));
    }
    /**
     * Sets the name and processor accordingly.
     *
     * @param label
     * @param checker
     */
    public CheckerInfo(
            final String label,
            final Class<? extends SourceChecker> checker,
            final String qualsPath) {
        this(label, checker.getCanonicalName(), qualsPath);
    }

    public CheckerInfo(final String label, final String processor, final String qualsPath) {
        this.label = label;
        this.processor = processor;
        this.qualsPath = qualsPath;
    }

    /**
     * Gets the canonical class name for running the checker.
     *
     * @return the class name
     */
    public String getClassPath() {
        return this.processor;
    }

    /**
     * Get the label for this checker.
     *
     * @return the label name
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Get the quals path for this checker.
     *
     * @return the quals path
     */
    public String getQualsPath() {
        return this.qualsPath;
    }
}
