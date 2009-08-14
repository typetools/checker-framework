package checkers.source;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.*;
import checkers.quals.DefaultQualifier;
import checkers.quals.TypeQualifiers;
import checkers.types.*;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import com.sun.tools.javac.processing.*;

/**
 * An abstract annotation processor designed for implementing a
 * source-file checker for a JSR-308 conforming compiler plug-in. It provides an
 * interface to {@code javac}'s annotation processing API, routines for error
 * reporting via the JSR 199 compiler API, and an implementation for using a
 * {@link SourceVisitor} to perform the type-checking.
 *
 * <p>
 *
 * Subclasses must implement the following methods:
 *
 * <ul>
 *  <li>{@link SourceChecker#getMessages} (for type-qualifier specific error messages)
 *  <li>{@link SourceChecker#createSourceVisitor(CompilationUnitTree)} (for a custom {@link SourceVisitor})
 *  <li>{@link SourceChecker#createFactory} (for a custom {@link AnnotatedTypeFactory})
 *  <li>{@link SourceChecker#getSuppressWarningsKey} (for honoring
 *      {@link SuppressWarnings} annotations)
 * </ul>
 *
 * Most type-checker plug-ins will want to extend {@link BaseTypeChecker},
 * instead of this class.  Only checkers which require annotated types but not
 * subtype checking (e.g. for testing purposes) should extend this.
 * Non-type checkers (e.g. for enforcing coding styles) should extend
 * {@link AbstractProcessor} (or even this class) as the Checker Framework is
 * not designed for such checkers.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public abstract class SourceChecker extends AbstractTypeProcessor {

    // TODO checkers should export themselves through a separate interface,
    // and maybe have an interface for all the methods for which it's safe
    // to override

    /** Provides access to compiler helpers/internals. */
    protected ProcessingEnvironment env;

    /** file name of the localized messages */
    private static final String MSGS_FILE = "messages.properties";

    /** Maps error keys to localized/custom error messages. */
    protected Properties messages;

    /** Used to report error messages and warnings via the compiler. */
    protected JavacMessager messager;

    /** Used as a helper for the {@link SourceVisitor}. */
    protected Trees trees;

    /** The source tree that's being scanned. */
    protected CompilationUnitTree currentRoot;
    public TreePath currentPath;

    /** issue errors as warnings */
    private boolean warns;

    /** A regular expression for classes that should be skipped. */
    protected Pattern skipPattern;

    /** The line separator */
    private final static String LINE_SEPARATOR = System.getProperty("line.separator").intern();

    /**
     * @return the {@link ProcessingEnvironment} that was supplied to this
     *         checker
     */
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.env;
    }

    /**
     * @param root the AST root for the factory
     * @return an {@link AnnotatedTypeFactory} for use by typecheckers
     */
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new AnnotatedTypeFactory(this, root);
    }

    /**
     * Provides the {@link SourceVisitor} that the checker should use to scan
     * input source trees.
     *
     * @param root
     *            the AST root
     * @return a {@link SourceVisitor} to use to scan source trees
     */
    protected abstract SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root);

    /**
     * Provides a mapping of error keys to custom error messages.
     *
     * As a default, this implementation builds a {@link Properties} out of
     * file {@code messages.properties}.  It accumulates all the properties files
     * in the Java class hierarchy from the checker up to {@code SourceChecker}.
     * This permits subclasses to inherit default messages while being able to
     * override them.
     *
     * @return a {@link Properties} that maps error keys to error message text
     */
    public Properties getMessages() {
        if (this.messages != null)
            return this.messages;

        this.messages = new Properties();
        Stack<Class<?>> checkers = new Stack<Class<?>>();

        Class<?> currClass = this.getClass();
        while (currClass != SourceChecker.class) {
            checkers.push(currClass);
            currClass = currClass.getSuperclass();
        }
        checkers.push(SourceChecker.class);

        while (!checkers.empty())
            messages.putAll(getProperties(checkers.pop(), MSGS_FILE));
        return this.messages;
    }

    private String getSkipPattern(Map<String, String> options) {
        if (options.containsKey("skipClasses"))
            return options.get("skipClasses");

        if (System.getProperty("checkers.skipClasses") != null)
            return System.getProperty("checkers.skipClasses");

        if (System.getenv("skipClasses") != null)
            return System.getenv("skipClasses");

        return "";
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractProcessor#init(ProcessingEnvironment)
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.env = processingEnv;

        this.skipPattern = Pattern.compile(getSkipPattern(processingEnv.getOptions()));

        // Grab the Trees and Messager instances now; other utilities
        // (like Types and Elements) can be retrieved by subclasses.
        @Nullable Trees trees = Trees.instance(processingEnv);
        assert trees != null; /*nninvariant*/
        this.trees = trees;

        this.messager = (JavacMessager) processingEnv.getMessager();
        this.messages = getMessages();
        this.warns = processingEnv.getOptions().containsKey("warns");
    }

    /**
     * Type-check the code with Java specifications and then runs the Checker
     * Rule Checking visitor on the processed source.
     *
     * The {@link Processor} is invoked in the annotation processing phase,
     * before the code is type-checked by the compiler.  This method ensures
     * that only Java valid code is processed by the Rule Checking visitor.
     *
     * @see Processor#process(Set, RoundEnvironment)
     */
    @Override
    public void typeProcess(TypeElement e, TreePath p) {
        currentRoot = p.getCompilationUnit();
        currentPath = p;
        // Visit the attributed tree.
        try {
            SourceVisitor<?,?> visitor = createSourceVisitor(currentRoot);
            visitor.scan(p, null);
        } catch (Throwable exception) {
            String message = getClass().getSimpleName().replaceAll("Checker", "")
            + " processor threw unexpected exception when processing "
            + currentRoot.getSourceFile().getName();

            Error err = new Error(message, exception);
            err.printStackTrace();
            throw err;
        }
    }

    /**
     * Prints a message (error, warning, note, etc.) via JSR-269.
     *
     * @param kind
     *            the type of message to print
     * @param source
     *            the object from which to obtain source position information
     * @param msgKey
     *            the message key to print
     * @param args
     *            arguments for interpolation in the string corresponding to the
     *            given message key
     * @see Diagnostic
     * @throws IllegalArgumentException
     *             if {@code source} is neither a {@link Tree} nor an
     *             {@link Element}
     */
    protected void message(Diagnostic.Kind kind, Object source, Object msgKey,
            Object... args) {

        assert messages != null : "null messages";

        for (Object arg : args) {
            arg = (arg == null) ? null :
                messages.getProperty(arg.toString(), arg.toString());
        }

        if (kind == Diagnostic.Kind.NOTE) {
            System.err.println("(NOTE) " + String.format(msgKey.toString(), args));
            return;
        }

        final String defaultFormat = String.format("(%s)", msgKey);
        String fmtString;
        if (this.env.getOptions() != null /*nnbug*/
                && this.env.getOptions().containsKey("nomsgtext"))
            fmtString = defaultFormat;
        else
            fmtString = messages.getProperty(msgKey.toString(), defaultFormat);
        String messageText = String.format(fmtString, args);

        // Replace '\n' with the proper line separator
        if (LINE_SEPARATOR != "\n")
            messageText = messageText.replaceAll("\n", LINE_SEPARATOR);

        if (source instanceof Element)
            messager.printMessage(kind, messageText, (Element) source);
        else if (source instanceof Tree)
            Trees.instance(env).printMessage(kind, messageText, (Tree) source,
                    currentRoot);
        else
            throw new IllegalArgumentException("invalid position source: "
                    + source.getClass().getName());
    }

    /**
     * Determines whether one of the annotations in the given set of {@link
     * AnnotationMirror}s is a {@link SuppressWarnings} annotation with the
     * SuppressWarnings key corresponding to this checker.
     *
     * @param annos the annotations to search
     * @return true if one of {@code annos} is a {@link SuppressWarnings}
     *         annotation with the key returned by {@link
     *         SourceChecker#getSuppressWarningsKey}
     */
    private boolean checkSuppressWarnings(List<? extends AnnotationMirror> annos, String err) {

        Collection<String> swkeys = this.getSuppressWarningsKey();

        // For all the method's annotations, check for a @SuppressWarnings
        // annotation. If one is found, check its values for this checker's
        // SuppressWarnings key.
        for (AnnotationMirror am : annos) {

            Element elt = am.getAnnotationType().asElement();
            assert elt instanceof TypeElement;

            String annoName = ((TypeElement)elt).getQualifiedName().toString();
            if (!("java.lang.SuppressWarnings".equals(annoName)))
                continue;

            // Parse the value of the @SuppressWarnings annotation. Return
            // true if it contains the SuppressWarnings key.
            @Nullable List<String> vals = AnnotationUtils.parseStringArrayValue(am, "value");
            if (vals != null)
                for (String swkey : swkeys) {
                    if (vals.contains(swkey) || vals.contains(swkey + ":" + err))
                        return true;
                }
        }

        return false;
    }

    /**
     * Determines whether the warnings pertaining to a given tree should be
     * suppressed (namely, if its containing method has a @SuppressWarnings
     * annotation for which one of the values is the key provided by the {@link
     * SourceChecker#getSuppressWarningsKey} method).
     *
     * @param tree the tree that might be a source of a warning
     * @return true if no warning should be emitted for the given tree because
     *         it is contained by a method with an appropriately-valued
     *         @SuppressWarnings annotation; false otherwise
     */
    private boolean shouldSuppressWarnings(Tree tree, String err) {

        // Don't suppress warnings if there's no key.
        Collection<String> swKeys = this.getSuppressWarningsKey();
        if (swKeys.isEmpty())
            return false;

        @Nullable TreePath path = trees.getPath(this.currentRoot, tree);
        if (path == null)
            return false;

        @Nullable VariableTree var = TreeUtils.enclosingVariable(path);
        if (var != null && shouldSuppressWarnings(InternalUtils.symbol(var), err))
            return true;

        @Nullable MethodTree method = TreeUtils.enclosingMethod(path);
        if (method != null && shouldSuppressWarnings(InternalUtils.symbol(method), err))
            return true;

        @Nullable ClassTree cls = TreeUtils.enclosingClass(path);
        if (cls != null && shouldSuppressWarnings(InternalUtils.symbol(cls), err))
            return true;

        return false;
    }

    private boolean shouldSuppressWarnings(@Nullable Element elt, String err) {

        if (elt == null)
            return false;

        return checkSuppressWarnings(elt.getAnnotationMirrors(), err) ||
            shouldSuppressWarnings(elt.getEnclosingElement(), err);
    }

    /**
     * Reports a result. By default, it prints it to the screen via the
     * compiler's internal messeger if the result is non-success; otherwise,
     * the method returns with no side-effects.
     *
     * @param r
     *            the result to report
     * @param src
     *            the position object associated with the result
     */
    public void report(final Result r, final Object src) {

        String err = r.getMessageKeys().iterator().next();
        // TODO: SuppressWarnings checking for Elements
        if (src instanceof Tree && shouldSuppressWarnings((Tree)src, err))
            return;
        if (src instanceof Element && shouldSuppressWarnings((Element)src, err))
            return;

        if (r.isSuccess())
            return;

        for (Result.DiagMessage msg : r.getDiagMessages()) {
            if (r.isFailure())
                this.message(warns ? Diagnostic.Kind.MANDATORY_WARNING : Diagnostic.Kind.ERROR,
                        src, msg.getMessageKey(), msg.getArgs());
            else if (r.isWarning())
                this.message(Diagnostic.Kind.MANDATORY_WARNING, src, msg.getMessageKey(), msg.getArgs());
            else
                this.message(Diagnostic.Kind.NOTE, src, msg.getMessageKey(), msg.getArgs());
        }
    }

    /**
     * Determines whether checking against members of the class with the given
     * name should be performed. This method will return true only when the
     * given class name matches the regular expression specified by the
     * {@code checkers.skipClasses} property.
     *
     * @param className the fully qualified name of the class to check
     * @return true if the members of the class named {@code className} should
     *         not be checked against
     */
    public boolean shouldSkip(String className) {
        Matcher m = skipPattern.matcher(className);
        return m.matches();
    }

    /**
     * Determines the value of the lint option with the given name.  Just
     * as <a
     * href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/solaris/javac.html">javac</a>
     * uses "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx,
     * annotation-related lint options are enabled with "-Alint:xxx" and
     * disabled with "-Alint:-xxx".
     *
     * @throws IllegalArgumentException if the option name is not recognized
     *         via the {@link SupportedLintOptions} annotation or the {@link
     *         SourceChecker#getSupportedLintOptions} method
     * @param name the name of the lint option to check for
     * @return true if the lint option was given, false if it was not given or
     * was given prepended with a "-"
     *
     * @see SourceChecker#getLintOption(String,boolean)
     */
    public final boolean getLintOption(String name) {
        return getLintOption(name, false);
    }

    /**
     * Determines the value of the lint option with the given name.  Just
     * as <a
     * href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/solaris/javac.html">javac</a>
     * uses "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx,
     * annotation-related lint options are enabled with "-Alint=xxx" and
     * disabled with "-Alint=-xxx".
     *
     * @throws IllegalArgumentException if the option name is not recognized
     *         via the {@link SupportedLintOptions} annotation or the {@link
     *         SourceChecker#getSupportedLintOptions} method
     * @param name the name of the lint option to check for
     * @param def the default option value, returned if the option was not given
     * @return true if the lint option was given, false if it was given
     *         prepended with a "-", or {@code def} if it was not given at all
     *
     * @see SourceChecker#getLintOption(String)
     */
    public final boolean getLintOption(String name, boolean def) {

        if (!this.getSupportedLintOptions().contains(name))
            throw new IllegalArgumentException("illegal lint option: " + name);

        Map<String, /*@Nullable*/ String> options = this.env.getOptions();

        @Nullable String lintString = options.get("lint");
        if (lintString == null)
            return def;

        Set<String> lintOptions = new HashSet<String>();
        for (String s : lintString.split(","))
            lintOptions.add(s);

        if (lintOptions.contains(String.format("-%s", name)))
            return false;
        else if (lintOptions.contains(name))
            return true;
        else return def;
    }

    /**
     * Returns the lint options recognized by this checker. Lint options are
     * those which can be checked for via {@link SourceChecker#getLintOption}.
     *
     * @return an unmodifiable {@link Set} of the lint options recognized by
     *         this checker
     */
    public Set<String> getSupportedLintOptions() {
        @Nullable SupportedLintOptions sl =
            this.getClass().getAnnotation(SupportedLintOptions.class);

        if (sl == null)
            return Collections.</*@NonNull*/ String>emptySet();

        @Nullable String /*@Nullable*/ [] slValue = sl.value();
        assert slValue != null; /*nninvariant*/

        @Nullable String [] lintArray = slValue;
        Set<String> lintSet = new HashSet<String>(lintArray.length);
        for (String s : lintArray)
            lintSet.add(s);
        return Collections.</*@NonNull*/ String>unmodifiableSet(lintSet);

    }

    /*
     * Force "-Alint" as a recognized processor option for all subtypes of
     * SourceChecker.
     *
     * [This method is provided here, rather than via the @SupportedOptions
     * annotation, so that it may be inherited by subclasses.]
     */
    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>();
        options.add("skipClasses");
        options.add("lint");
        options.add("nomsgtext");
        options.add("filenames");
        options.add("showchecks");
        options.add("stubs");
        options.add("warns");
        options.addAll(super.getSupportedOptions());
        return Collections.</*@NonNull*/ String>unmodifiableSet(options);
    }

    /**
     * Always returns a singleton set containing only "*".
     *
     * This method returns the argument to the {@link
     * SupportedAnnotationTypes} annotation, so the effect of returning "*"
     * is as if the checker were annotated by
     * {@code @SupportedAnnotationTypes("*")}:
     * javac runs the checker on every
     * class mentioned on the javac command line.  This method also checks
     * that subclasses do not contain a {@link SupportedAnnotationTypes}
     * annotation.  <p>
     *
     * To specify the annotations that a checker recognizes as type qualifiers,
     * use the {@link TypeQualifiers} annotation on the declaration of
     * subclasses of this class or override the
     * {@link BaseTypeChecker#getSupportedTypeQualifiers()} method.
     *
     * @throws Error if a subclass is annotated with
     *         {@link SupportedAnnotationTypes}
     *
     * @see TypeQualifiers
     * @see BaseTypeChecker#getSupportedAnnotationTypes()
     */
    @Override
    public final Set<String> getSupportedAnnotationTypes() {

        SupportedAnnotationTypes supported = this.getClass().getAnnotation(
                SupportedAnnotationTypes.class);
        if (supported != null)
            throw new Error("@SupportedAnnotationTypes should not be written on any checker;"
                            + " supported annotation types are inherited from SourceChecker");
        return Collections.singleton("*");
    }

    /**
     * @return String keys that a checker honors for suppressing warnings
     *         and errors that it issues
     *
     * @see SuppressWarningsKey
     */
    public Collection<String> getSuppressWarningsKey() {
        SuppressWarningsKey annotation =
            this.getClass().getAnnotation(SuppressWarningsKey.class);

        if (annotation != null)
            return Collections.singleton(annotation.value());

        // Inferring key from class name
        String className = this.getClass().getSimpleName();
        int indexOfChecker = className.lastIndexOf("Checker");
        if (indexOfChecker == -1)
        	indexOfChecker = className.lastIndexOf("Subchecker");
        String key = (indexOfChecker == -1) ? className : className.substring(0, indexOfChecker);
        return Collections.singleton(key.trim().toLowerCase());
    }

    /**
     * Returns a regular expression pattern to specify java classes that are not
     * annotated, and thus whose warnings and should be surpressed.
     *
     * It returns the pattern specified by the user, through the option
     * {@code checkers.skipClasses}; otherwise it returns a pattern that can
     * match no class.
     *
     * @return pattern of un-annotated classes that should be skipped
     */
    public Pattern getShouldSkip() {
        String unannotatedClasses = System.getProperty("checkers.skipClasses");
        if (unannotatedClasses != null && !unannotatedClasses.trim().equals(""))
            return Pattern.compile(unannotatedClasses);
        return Pattern.compile("_");
    }

    /**
     * A helper function to parse a Properties file
     *
     * @param cls   the class whose location is the base of the file path
     * @param filePath the name/path of the file to be read
     * @return  the properties
     */
    private Properties getProperties(Class<?> cls, String filePath) {
        Properties prop = new Properties();
        try {
            InputStream base = cls.getResourceAsStream(filePath);

            if (base == null)
                // No message customization file was given
                return prop;

            prop.load(base);
        } catch (IOException e) {
            System.err.println("Couldn't parse " + filePath + " file");
            e.printStackTrace();
            // ignore the possible customization file
        }
        return prop;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }
}
