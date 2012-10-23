package checkers.eclipse.actions;

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

/**
 * Stores information that describes a particular checker such as its label, the
 * class to run, or the quals path that is associate with it.
 * 
 * @author asumu
 * 
 */
public class CheckerInfo
{
    private final String label;
    private final Class<?> processor;
    private final String qualsPath;

    // Static information about built-in checkers
    public static final String NULLNESS_LABEL = "Nullness checker";
    public static final Class<?> NULLNESS_CLASS = NullnessChecker.class;
    public static final String NULLNESS_QUALS = "checkers.nullness.quals.*";
    public static final CheckerInfo NULLNESS_INFO = new CheckerInfo(
            NULLNESS_LABEL, NULLNESS_CLASS, NULLNESS_QUALS);
        
    public static final String JAVARI_LABEL = "Javari checker";
    public static final Class<?> JAVARI_CLASS = JavariChecker.class;
    public static final String JAVARI_QUALS = "checkers.javari.quals.*";
    public static final CheckerInfo JAVARI_INFO = new CheckerInfo(JAVARI_LABEL,
            JAVARI_CLASS, JAVARI_QUALS);
    
    public static final String INTERNING_LABEL = "Interning checker";
    public static final Class<?> INTERNING_CLASS = InterningChecker.class;
    public static final String INTERNING_QUALS = "checkers.interning.quals.*";
    public static final CheckerInfo INTERNING_INFO = new CheckerInfo(
            INTERNING_LABEL, INTERNING_CLASS, INTERNING_QUALS);
    
    public static final String IGJ_LABEL = "IGJ checker";
    public static final Class<?> IGJ_CLASS = IGJChecker.class;
    public static final String IGJ_QUALS = "checkers.igj.quals.*";
    public static final CheckerInfo IGJ_INFO = new CheckerInfo(IGJ_LABEL,
            IGJ_CLASS, IGJ_QUALS);
    
    public static final String FENUM_LABEL = "Fenum checker";
    public static final Class<?> FENUM_CLASS = FenumChecker.class;
    public static final String FENUM_QUALS = "checkers.fenum.quals.*";
    public static final CheckerInfo FENUM_INFO = new CheckerInfo(FENUM_LABEL,
            FENUM_CLASS, FENUM_QUALS);
        
    public static final Class<?> LINEAR_CLASS = LinearChecker.class;
    public static final String LINEAR_LABEL = "Linear checker";
    public static final String LINEAR_QUALS = "checkers.linear.quals.*";
    public static final CheckerInfo LINEAR_INFO = new CheckerInfo(LINEAR_LABEL,
            LINEAR_CLASS, LINEAR_QUALS);
    
    public static final Class<?> LOCK_CLASS = LockChecker.class;
    public static final String LOCK_LABEL = "Lock checker";
    public static final String LOCK_QUALS = "checkers.lock.quals.*";
    public static final CheckerInfo LOCK_INFO = new CheckerInfo(LOCK_LABEL,
            LOCK_CLASS, LOCK_QUALS);
    
    public static final Class<?> REGEX_CLASS = RegexChecker.class;
    public static final String REGEX_LABEL = "Regex checker";
    public static final String REGEX_QUALS = "checkers.regex.quals.*";
    public static final CheckerInfo REGEX_INFO = new CheckerInfo(REGEX_LABEL,
            REGEX_CLASS, REGEX_QUALS);
    
    public static final Class<?> TAINTING_CLASS = TaintingChecker.class;
    public static final String TAINTING_LABEL = "Tainting checker";
    public static final String TAINTING_QUALS = "checkers.tainting.quals.*";
    public static final CheckerInfo TAINTING_INFO = new CheckerInfo(
            TAINTING_LABEL, TAINTING_CLASS, TAINTING_QUALS);
    
    public static final Class<?> I18N_CLASS = I18nChecker.class;
    public static final String I18N_LABEL = "I18n checker";
    public static final String I18N_QUALS = "checkers.i18n.quals.*";
    public static final CheckerInfo I18N_INFO = new CheckerInfo(I18N_LABEL,
            I18N_CLASS, I18N_QUALS);


    /**
     * Sets the name and processor accordingly.
     * 
     * @param label
     * @param processor
     */
    CheckerInfo(String label, Class<?> processor, String qualsPath)
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
    String getClassName()
    {
        return this.processor.getCanonicalName();
    }

    /**
     * Get the label for this checker.
     * 
     * @return the label name
     */
    String getLabel()
    {
        return this.label;
    }

    /**
     * Get the quals path for this checker.
     * 
     * @return the quals path
     */
    String getQualsPath()
    {
        return this.qualsPath;
    }
}
