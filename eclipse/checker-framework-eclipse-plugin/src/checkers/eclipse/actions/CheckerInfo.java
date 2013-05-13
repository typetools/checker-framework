package checkers.eclipse.actions;

import checkers.eclipse.util.JavaUtils;
import checkers.fenum.FenumChecker;
import checkers.i18n.I18nChecker;
import checkers.igj.IGJChecker;
import checkers.interning.InterningChecker;
import checkers.javari.JavariChecker;
import checkers.linear.LinearChecker;
import checkers.lock.LockChecker;
import checkers.nullness.NullnessChecker;
import checkers.regex.RegexChecker;
import checkers.tainting.TaintingChecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores information that describes a particular checker such as its label, the
 * class to run, or the quals path that is associate with it.
 * 
 * @author asumu
 * 
 */
public class CheckerInfo
{

    private static List<CheckerInfo> checkers = null;

    private final String label;
    private final String processor;
    private final String qualsPath;

    public static String [] splitAtUppercase(final String toSplit) {
        List<String> tokens = new ArrayList<String>();

        int length = toSplit.length();

        int start = 0;
        for(int i = 0; i < length; i++) {
            if((Character.isUpperCase(toSplit.charAt(i)) && i != 0)) {
                tokens.add(toSplit.substring(start, i));
                start = i;
            }
        }

        tokens.add(toSplit.substring(start, length));
        return tokens.toArray(new String[tokens.size()]);
    }

    public static List<CheckerInfo> getCheckers() {
        if(checkers == null) {
            checkers = Arrays.asList(
                    new CheckerInfo("Nullness checker",  NullnessChecker.class.getCanonicalName(),  "checkers.nullness.quals.*"),
                    new CheckerInfo("Javari checker",    JavariChecker.class.getCanonicalName(),    "checkers.javari.quals.*"),
                    new CheckerInfo("Interning checker", InterningChecker.class.getCanonicalName(), "checkers.interning.quals.*"),
                    new CheckerInfo("Fenum checker",     FenumChecker.class.getCanonicalName(),     "checkers.fenum.quals.*"),

                    new CheckerInfo("Linear checker",    LinearChecker.class.getCanonicalName(),    "checkers.linear.quals.*"),
                    new CheckerInfo("Lock checker",      LockChecker.class.getCanonicalName(),      "checkers.lock.quals.*"),
                    new CheckerInfo("Regex checker",     RegexChecker.class.getCanonicalName(),     "checkers.regex.quals.*"),
                    new CheckerInfo("Tainting checker",  TaintingChecker.class.getCanonicalName(),  "checkers.tainting.quals.*"),

                    new CheckerInfo("I18n checker",      I18nChecker.class.getCanonicalName(),      "checkers.i18n.quals.*")
            );
        }

        return checkers;
    }

    public static CheckerInfo fromClassPath(final String classPath, final String qualsPath) {
        String str = "fromClassPath(" + classPath + ", " + qualsPath + ")";
        try  {
        final String [] pathTokens = classPath.split("\\.");
            str += "[" + JavaUtils.join(" ", pathTokens) + "]";
        final String className = JavaUtils.join(" ", splitAtUppercase(pathTokens[pathTokens.length - 1]));
            str += "className = " + className;
            return new CheckerInfo(className, classPath, qualsPath);
        } catch(Exception e) {
            throw new RuntimeException(str, e);
        }

    }

    /**
     * Sets the name and processor accordingly.
     * 
     * @param label
     * @param processor
     * @param qualsPath
     */
    public CheckerInfo(final String label, final String processor, final String qualsPath)
    {
        this.label = label;
        this.processor = processor;
        this.qualsPath = qualsPath;
    }

    /**
     * Gets the canonical class name for running the checker.
     * 
     * @return the class name
     */
    public String getClassPath()
    {
        return this.processor;
    }

    /**
     * Get the label for this checker.
     * 
     * @return the label name
     */
    public String getLabel()
    {
        return this.label;
    }

    /**
     * Get the quals path for this checker.
     * 
     * @return the quals path
     */
    public String getQualsPath()
    {
        return this.qualsPath;
    }
}
