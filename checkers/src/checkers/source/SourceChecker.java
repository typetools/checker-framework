package checkers.source;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.*;
import java.util.regex.Pattern;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.GeneralAnnotatedTypeFactory;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

/*>>>
import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.nullness.quals.*;
*/

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacMessager;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

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
public abstract class SourceChecker extends AbstractTypeProcessor {

    // TODO checkers should export themselves through a separate interface,
    // and maybe have an interface for all the methods for which it's safe
    // to override

    /** file name of the localized messages */
    private static final String MSGS_FILE = "messages.properties";

    /** Maps error keys to localized/custom error messages. */
    protected Properties messages;

    /** Used to report error messages and warnings via the compiler. */
    protected Messager messager;

    /** Used as a helper for the {@link SourceVisitor}. */
    protected Trees trees;

    /** The source tree that's being scanned. */
    protected CompilationUnitTree currentRoot;
    public TreePath currentPath;

    /** issue errors as warnings */
    private boolean warns;

    /**
     * Regular expression pattern to specify Java classes that are not
     * annotated, so warnings about uses of them should be suppressed.
     *
     * It contains the pattern specified by the user, through the option
     * {@code checkers.skipUses}; otherwise it contains a pattern that can
     * match no class.
     */
    private Pattern skipUsesPattern;

    /**
     * Regular expression pattern to specify Java classes whose
     * definition should not be checked.
     *
     * It contains the pattern specified by the user, through the option
     * {@code checkers.skipDefs}; otherwise it contains a pattern that can
     * match no class.
     */
    private Pattern skipDefsPattern;

    /** The supported lint options */
    private Set<String> supportedLints;

    /** The chosen lint options that have been enabled by programmer */
    private Set<String> activeLints;

    /** The line separator */
    private final static String LINE_SEPARATOR = System.getProperty("line.separator").intern();

    /**
     * @return the {@link ProcessingEnvironment} that was supplied to this
     *         checker
     */
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.processingEnv;
    }

    /* This method is package visible only to allow the AggregateChecker. */
    /* package-visible */
    void setProcessingEnvironment(ProcessingEnvironment env) {
        this.processingEnv = env;
    }

    /**
     * @param root the AST root for the factory
     * @return an {@link AnnotatedTypeFactory} for use by typecheckers
     */
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new GeneralAnnotatedTypeFactory(this, root);
    }

    /**
     * Provides the {@link SourceVisitor} that the checker should use to scan
     * input source trees.
     *
     * @param root the AST root
     * @return a {@link SourceVisitor} to use to scan source trees
     */
    protected abstract SourceVisitor<?, ?> createSourceVisitor(CompilationUnitTree root);

    /**
     * Provides a mapping of error keys to custom error messages.
     * <p>
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

    private Pattern getSkipPattern(String patternName, Map<String, String> options) {
        String pattern = "";

        if (options.containsKey(patternName))
            pattern = options.get(patternName);
        else if (System.getProperty("checkers." + patternName) != null)
            pattern = System.getProperty("checkers." + patternName);
        else if (System.getenv(patternName) != null)
            pattern = System.getenv(patternName);

        if (pattern.indexOf("/") != -1) {
            getProcessingEnvironment().getMessager().printMessage(Kind.WARNING,
              "The " + patternName + " property contains \"/\", which will never match a class name: " + pattern);
        }

        // return a pattern of an illegal Java identifier character
        // so that it won't match anything
        if (pattern.equals(""))
            pattern = "\\(";

        return Pattern.compile(pattern);
    }

    private Pattern getSkipUsesPattern(Map<String, String> options) {
        return getSkipPattern("skipUses", options);
    }

    private Pattern getSkipDefsPattern(Map<String, String> options) {
        return getSkipPattern("skipDefs", options);
    }

    // TODO: do we want this?
    // Cache the keys that we already warned about to prevent repetitions.
    // private Set<String> warnedOnLint = new HashSet<String>();

    private Set<String> createActiveLints(Map<String, String> options) {
        if (!options.containsKey("lint"))
            return Collections.emptySet();

        String lintString = options.get("lint");
        if (lintString == null) {
            return Collections.singleton("all");
        }

        Set<String> activeLint = new HashSet<String>();
        for (String s : lintString.split(",")) {
            if (!this.getSupportedLintOptions().contains(s) &&
                    !(s.charAt(0) == '-' && this.getSupportedLintOptions().contains(s.substring(1))) &&
                    !s.equals("all") &&
                    !s.equals("none") /*&&
                    !warnedOnLint.contains(s)*/) {
                this.messager.printMessage(javax.tools.Diagnostic.Kind.WARNING,
                        "Unsupported lint option: " + s + "; All options: " + this.getSupportedLintOptions());
                // warnedOnLint.add(s);
            }

            activeLint.add(s);
            if (s.equals("none"))
                activeLint.add("-all");
        }

        return Collections.unmodifiableSet(activeLint);
    }

    /**
     * Exception type used only internally to abort
     * processing.
     * Only public to allow tests.AnnotationBuilderTest;
     * this class should be private. TODO: nicer way?
     */
    @SuppressWarnings("serial")
	public static class CheckerError extends RuntimeException {
        public CheckerError(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * Log an error message and abort processing.
     * Call this method instead of raising an exception.
     *
     * @param msg The error message to log.
     */
    public static void errorAbort(String msg) {
        throw new CheckerError(msg, new Throwable());
    }

    public static void errorAbort(String msg, Throwable cause) {
        throw new CheckerError(msg, cause);
    }

    private void logCheckerError(CheckerError ce) {
        StringBuilder msg = new StringBuilder(ce.getMessage());
        if ((processingEnv == null ||
                processingEnv.getOptions() == null ||
                processingEnv.getOptions().containsKey("printErrorStack")) &&
                ce.getCause() != null) {
            msg.append("\nException: " +
                            ce.getCause().toString() + ": " + formatStackTrace(ce.getCause().getStackTrace()));
            Throwable cause = ce.getCause().getCause();
            while (cause!=null) {
                msg.append("\nUnderlying Exception: " +
                                (cause.toString() + ": " +
                                        formatStackTrace(cause.getStackTrace())));
                cause = cause.getCause();
            }
        }
        if (this.messager != null) {
            this.messager.printMessage(javax.tools.Diagnostic.Kind.ERROR, msg);
        } else {
            System.err.println("Exception before having a messager set up: " + msg);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Type checkers are not supposed to override this.
     * Instead use initChecker.
     * This allows us to handle CheckerError only here and doesn't
     * require all overriding implementations to be aware of CheckerError.
     *
     * @see AbstractProcessor#init(ProcessingEnvironment)
     * @see SourceChecker#initChecker()
     */
    @Override
    public void typeProcessingStart() {
        try {
            super.typeProcessingStart();
            initChecker();
            if (this.messager == null) {
                messager = (JavacMessager) processingEnv.getMessager();
                messager.printMessage(
                        javax.tools.Diagnostic.Kind.WARNING,
                        "You have forgotten to call super.initChecker in your "
                                + "subclass of SourceChecker! Please ensure your checker is properly initialized.");
            }
            if (shouldAddShutdownHook()) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        shutdownHook();
                    }
                });
            }
        } catch (CheckerError ce) {
            logCheckerError(ce);
        } catch (Throwable t) {
            logCheckerError(new CheckerError("SourceChecker.init: unexpected Throwable (" +
                    t.getClass().getSimpleName() + "); message: " + t.getMessage() +
                    "; invoke the compiler with -AprintErrorStack to see the stack trace.", t));
        }
    }

    /**
     * Initialize the checker.
     *
     * @see AbstractProcessor#init(ProcessingEnvironment)
     */
    public void initChecker() {
        this.skipUsesPattern = getSkipUsesPattern(processingEnv.getOptions());
        this.skipDefsPattern = getSkipDefsPattern(processingEnv.getOptions());

        // Grab the Trees and Messager instances now; other utilities
        // (like Types and Elements) can be retrieved by subclasses.
        /*@Nullable*/ Trees trees = Trees.instance(processingEnv);
        assert trees != null; /*nninvariant*/
        this.trees = trees;

        this.messager = processingEnv.getMessager();
        this.messages = getMessages();
        this.warns = processingEnv.getOptions().containsKey("warns");
        this.activeLints = createActiveLints(processingEnv.getOptions());
    }

    /**
     * Return true to indicate that method {@link #shutdownHook} should be
     * added as a shutdownHook of the JVM.
     */
    protected boolean shouldAddShutdownHook() {
        return processingEnv.getOptions().containsKey("resourceStats");
    }

    /**
     * Method that gets called exactly once at shutdown time of the JVM.
     * Checkers can override this method to customize the behavior.
     */
    protected void shutdownHook() {
        if (processingEnv.getOptions().containsKey("resourceStats")) {
            printStats();
        }
    }

    /** Print resource usage statistics */
    protected void printStats() {
        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPool : memoryPools) {
            System.out.println("Memory pool " + memoryPool.getName() + " statistics");
            System.out.println("  Pool type: " + memoryPool.getType());
            System.out.println("  Peak usage: " + memoryPool.getPeakUsage());
        }
    }

    // Output the warning about source level at most once.
    private boolean warnedAboutSourceLevel = false;

    // The number of errors at the last exit of the type processor.
    // At entry to the type processor we check whether the current error count is
    // higher and then don't process the file, as it contains some Java errors.
    // Needs to be package-visible to allow access from AggregateChecker.
    /* package-visible */
    int errsOnLastExit = 0;

    /**
     * Type-check the code with Java specifications and then runs the Checker
     * Rule Checking visitor on the processed source.
     *
     * @see Processor#process(Set, RoundEnvironment)
     */
    @Override
    public void typeProcess(TypeElement e, TreePath p) {
        if (e == null) {
            messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
                    "Refusing to process empty TypeElement");
            return;
        }
        if (p == null) {
            messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
                    "Refusing to process empty TreePath in TypeElement: " + e);
            return;
        }

        Context context = ((JavacProcessingEnvironment)processingEnv).getContext();
        com.sun.tools.javac.code.Source source = com.sun.tools.javac.code.Source.instance(context);
        if ((! warnedAboutSourceLevel) && (! source.allowTypeAnnotations())) {
            messager.printMessage(javax.tools.Diagnostic.Kind.WARNING,
                                  "-source " + source.name + " does not support type annotations");
            warnedAboutSourceLevel = true;
        }

        Log log = Log.instance(context);
        if (log.nerrors > this.errsOnLastExit) {
            this.errsOnLastExit = log.nerrors;
            return;
        }

        currentRoot = p.getCompilationUnit();
        currentPath = p;
        // Visit the attributed tree.
        SourceVisitor<?, ?> visitor = null;
        try {
            visitor = createSourceVisitor(currentRoot);
            visitor.scan(p, null);
        } catch (CheckerError ce) {
            logCheckerError(ce);
        } catch (Throwable t) {
            logCheckerError(new CheckerError("SourceChecker.typeProcess: unexpected Throwable (" +
                    t.getClass().getSimpleName() + ")  when processing "
                    + currentRoot.getSourceFile().getName() +
                    (t.getMessage() != null ? "; message: " + t.getMessage() : "") +
                    "; invoke the compiler with -AprintErrorStack to see the stack trace.", t));
        } finally {
            // Also add possibly deferred diagnostics, which will get published back in
            // AbstractTypeProcessor.
            this.errsOnLastExit = log.nerrors;
        }
    }

    /**
     * Format a list of {@link StackTraceElement}s to be printed out as an error
     * message.
     */
    protected String formatStackTrace(StackTraceElement[] stackTrace) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : stackTrace) {
            if (!first) {
                sb.append("\n");
            }
            first = false;
            sb.append(ste.toString());
        }
        return sb.toString();
    }

    // Uses private fields, need to rewrite.
    // public void dumpState() {
    //     System.out.printf("SourceChecker = %s%n", this);
    //     System.out.printf("  env = %s%n", env);
    //     System.out.printf("    env.elementUtils = %s%n", ((JavacProcessingEnvironment) env).elementUtils);
    //     System.out.printf("      env.elementUtils.types = %s%n", ((JavacProcessingEnvironment) env).elementUtils.types);
    //     System.out.printf("      env.elementUtils.enter = %s%n", ((JavacProcessingEnvironment) env).elementUtils.enter);
    //     System.out.printf("    env.typeUtils = %s%n", ((JavacProcessingEnvironment) env).typeUtils);
    //     System.out.printf("  trees = %s%n", trees);
    //     System.out.printf("    trees.enter = %s%n", ((com.sun.tools.javac.api.JavacTrees) trees).enter);
    //     System.out.printf("    trees.elements = %s%n", ((com.sun.tools.javac.api.JavacTrees) trees).elements);
    //     System.out.printf("      trees.elements.types = %s%n", ((com.sun.tools.javac.api.JavacTrees) trees).elements.types);
    //     System.out.printf("      trees.elements.enter = %s%n", ((com.sun.tools.javac.api.JavacTrees) trees).elements.enter);
    // }

    /**
     * Returns the localized long message corresponding for this key, and
     * returns the defValue if no localized message is found.
     *
     */
    protected String fullMessageOf(String messageKey, String defValue) {
        String key = messageKey;

        do {
            if (messages.containsKey(key)) {
                return messages.getProperty(key);
            }

            int dot = key.indexOf('.');
            if (dot < 0) return defValue;
            key = key.substring(dot + 1);
        } while (true);
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
    protected void message(Diagnostic.Kind kind, Object source, /*@CompilerMessageKey*/ String msgKey,
            Object... args) {

        assert messages != null : "null messages";

        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                if (args[i] == null)
                    continue;

                // look whether we can expand the arguments, too.
                args[i] = messages.getProperty(args[i].toString(), args[i].toString());
            }
        }

        if (kind == Diagnostic.Kind.NOTE) {
            System.err.println("(NOTE) " + String.format(msgKey, args));
            return;
        }

        final String defaultFormat = String.format("(%s)", msgKey);
        String fmtString;
        if (this.processingEnv.getOptions() != null /*nnbug*/
                && this.processingEnv.getOptions().containsKey("nomsgtext")) {
            fmtString = defaultFormat;
        } else if (this.processingEnv.getOptions() != null /*nnbug*/
                && this.processingEnv.getOptions().containsKey("detailedmsgtext")) {
            StringBuilder sb = new StringBuilder();
            sb.append(defaultFormat);
            sb.append(DETAILS_SEPARATOR);
            if (args != null) {
                sb.append(args.length);
                sb.append(DETAILS_SEPARATOR);
                for (Object arg : args) {
                    sb.append(arg);
                    sb.append(DETAILS_SEPARATOR);
                }
            } else {
                // Output 0 for null arguments.
                sb.append(0);
                sb.append(DETAILS_SEPARATOR);
            }
            sb.append(fullMessageOf(msgKey, defaultFormat));
            fmtString = sb.toString();
        } else {
            fmtString = fullMessageOf(msgKey, defaultFormat);
        }
        String messageText = String.format(fmtString, args);

        // Replace '\n' with the proper line separator
        if (LINE_SEPARATOR != "\n") // interned
            messageText = messageText.replaceAll("\n", LINE_SEPARATOR);

        if (source instanceof Element)
            messager.printMessage(kind, messageText, (Element) source);
        else if (source instanceof Tree)
            Trees.instance(processingEnv).printMessage(kind, messageText, (Tree) source,
                    currentRoot);
        else
            SourceChecker.errorAbort("invalid position source: "
                    + source.getClass().getName());
    }

    public static final String DETAILS_SEPARATOR = " $$ ";

    /**
     * Determines if an error (whose error key is {@code err}), should
     * be suppressed according to the user explicitly written
     * {@code anno} Suppress annotation.
     * <p>
     *
     * A suppress warnings value may be of the following pattern:
     *
     * <ol>
     * <li>{@code "suppress-key"}, where suppress-key is a supported warnings key, as
     * specified by {@link #getSuppressWarningsKey()},
     * e.g. {@code "nullness"} for nullness, {@code "igj"} for igj
     * test</li>
     *
     * <li>{@code "suppress-key:error-key}, where the suppress-key
     * is as above, and error-key is a prefix of the errors
     * that it may suppress.  So "nullness:generic.argument", would
     * suppress any errors in nullness checker related to
     * generic.argument.
     *
     * @param annos the annotations to search
     * @param err   the error key the checker is emitting
     * @return true if one of {@code annos} is a {@link SuppressWarnings}
     *         annotation with the key returned by {@link
     *         SourceChecker#getSuppressWarningsKey}
     */
    private boolean checkSuppressWarnings(SuppressWarnings anno, String err) {

        if (anno == null)
            return false;

        Collection<String> swkeys = this.getSuppressWarningsKey();

        // For all the method's annotations, check for a @SuppressWarnings
        // annotation. If one is found, check its values for this checker's
        // SuppressWarnings key.
        for (String suppressWarningValue : anno.value()) {
            for (String swKey : swkeys) {
                if (suppressWarningValue.equalsIgnoreCase(swKey))
                    return true;

                String expected = swKey + ":" + err;
                if (expected.toLowerCase().contains(suppressWarningValue.toLowerCase()))
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

        /*@Nullable*/ TreePath path = trees.getPath(this.currentRoot, tree);
        if (path == null)
            return false;

        /*@Nullable*/ VariableTree var = TreeUtils.enclosingVariable(path);
        if (var != null && shouldSuppressWarnings(InternalUtils.symbol(var), err))
            return true;

        /*@Nullable*/ MethodTree method = TreeUtils.enclosingMethod(path);
        if (method != null && shouldSuppressWarnings(InternalUtils.symbol(method), err))
            return true;

        /*@Nullable*/ ClassTree cls = TreeUtils.enclosingClass(path);
        if (cls != null && shouldSuppressWarnings(InternalUtils.symbol(cls), err))
            return true;

        return false;
    }

    private boolean shouldSuppressWarnings(/*@Nullable*/ Element elt, String err) {

        if (elt == null)
            return false;

        return checkSuppressWarnings(elt.getAnnotation(SuppressWarnings.class), err)
                || shouldSuppressWarnings(elt.getEnclosingElement(), err);
    }

    /**
     * Reports a result. By default, it prints it to the screen via the
     * compiler's internal messenger if the result is non-success; otherwise,
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
     * Determines the value of the lint option with the given name.  Just
     * as <a
     * href="http://docs.oracle.com/javase/7/docs/technotes/guides/javac/index.html">javac</a>
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
     * href="http://docs.oracle.com/javase/1.5.0/docs/tooldocs/solaris/javac.html">javac</a>
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

        if (!this.getSupportedLintOptions().contains(name)) {
            errorAbort("Illegal lint option: " + name);
        }

        if (activeLints.isEmpty())
            return def;

        String tofind = name;
        while (tofind != null) {
            if (activeLints.contains(tofind))
                return true;
            else if (activeLints.contains(String.format("-%s", tofind)))
                return false;

            tofind = parentOfOption(tofind);
        }

        return def;
    }

    /**
     * Set the value of the lint option with the given name.  Just
     * as <a
     * href="http://docs.oracle.com/javase/1.5.0/docs/tooldocs/solaris/javac.html">javac</a>
     * uses "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx,
     * annotation-related lint options are enabled with "-Alint=xxx" and
     * disabled with "-Alint=-xxx".
     * This method can be used by subclasses to enforce having certain lint
     * options enabled/disabled.
     *
     * @throws IllegalArgumentException if the option name is not recognized
     *         via the {@link SupportedLintOptions} annotation or the {@link
     *         SourceChecker#getSupportedLintOptions} method
     * @param name the name of the lint option to set
     * @param val the option value
     *
     * @see SourceChecker#getLintOption(String)
     * @see SourceChecker#getLintOption(String,boolean)
     */
    protected final void setLintOption(String name, boolean val) {
        if (!this.getSupportedLintOptions().contains(name)) {
            errorAbort("Illegal lint option: " + name);
        }

        /* TODO: warn if the option is also provided on the command line(?)
        boolean exists = false;
        if (!activeLints.isEmpty()) {
            String tofind = name;
            while (tofind != null) {
                if (activeLints.contains(tofind) || // direct
                        activeLints.contains(String.format("-%s", tofind)) || // negation
                        activeLints.contains(tofind.substring(1))) { // name was negation
                    exists = true;
                }
                tofind = parentOfOption(tofind);
            }
        }

        if (exists) {
            // TODO: Issue warning?
        }
        TODO: assert that name doesn't start with '-'
        */

        Set<String> newlints = new HashSet<String>();
        newlints.addAll(activeLints);
        if (val) {
            newlints.add(name);
        } else {
            newlints.add(String.format("-%s", name));
        }
        activeLints = Collections.unmodifiableSet(newlints);
    }

    /**
     * Helper method to find the parent of a lint key.  The lint hierarchy
     * level is donated by a colon ':'.  'all' is the root for all hierarchy.
     *
     * Example
     *    cast:unsafe --> cast
     *    cast        --> all
     *    all         --> {@code null}
     */
    private String parentOfOption(String name) {
        if (name.equals("all"))
            return null;
        else if (name.contains(":")) {
            return name.substring(0, name.lastIndexOf(':'));
        } else {
            return "all";
        }
    }

    /**
     * Returns the lint options recognized by this checker. Lint options are
     * those which can be checked for via {@link SourceChecker#getLintOption}.
     *
     * @return an unmodifiable {@link Set} of the lint options recognized by
     *         this checker
     */
    public Set<String> getSupportedLintOptions() {
        if (supportedLints == null) {
            supportedLints = createSupportedLintOptions();
        }
        return supportedLints;
    }

    /**
     * Compute the set of supported lint options.
     */
    protected Set<String> createSupportedLintOptions() {
        /*@Nullable*/ SupportedLintOptions sl =
            this.getClass().getAnnotation(SupportedLintOptions.class);

        if (sl == null)
            return Collections.</*@NonNull*/ String>emptySet();

        /*@Nullable*/ String /*@Nullable*/ [] slValue = sl.value();
        assert slValue != null; /*nninvariant*/

        /*@Nullable*/ String [] lintArray = slValue;
        Set<String> lintSet = new HashSet<String>(lintArray.length);
        for (String s : lintArray)
            lintSet.add(s);
        return Collections.</*@NonNull*/ String>unmodifiableSet(lintSet);

    }

    /**
     * Set the supported lint options.
     * Use of this method should be limited to the AggregateChecker,
     * who needs to set the lint options to the union of all subcheckers.
     * Also, e.g. the NullnessSubchecker/RawnessSubchecker need to
     * use this method, as one is created by the other.
     */
    protected void setSupportedLintOptions(Set<String> newlints) {
        supportedLints = newlints;
    }

    /*
     * Force "-Alint" as a recognized processor option for all subtypes of
     * SourceChecker.
     * TODO: Options other than "lint" are also added here. Many of them could
     * be lint options.
     *
     * [This method is provided here, rather than via the @SupportedOptions
     * annotation, so that it may be inherited by subclasses.]
     */
    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>();
        options.add("skipUses");
        options.add("skipDefs");
        options.add("lint");
        options.add("nomsgtext");
        options.add("detailedmsgtext");
        options.add("filenames");
        options.add("showchecks");
        options.add("stubs");
        options.add("ignorejdkastub");
        options.add("nocheckjdk");
        options.add("warns");
        options.add("annotatedTypeParams");
        options.add("printErrorStack");
        options.add("printAllQualifiers");
        options.add("resourceStats");
        options.add("stubWarnIfNotFound");
        options.add("stubDebug");
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
            errorAbort("@SupportedAnnotationTypes should not be written on any checker;"
                            + " supported annotation types are inherited from SourceChecker.");
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
     * Tests whether the class owner of the passed element is an unannotated
     * class and matches the pattern specified in the
     * {@code checker.skipUses} property.
     *
     * @param element   an element
     * @return  true iff the enclosing class of element should be skipped
     */
    public final boolean shouldSkipUses(Element element) {
        if (element == null)
            return false;
        TypeElement typeElement = ElementUtils.enclosingClass(element);
        String name = typeElement.getQualifiedName().toString();
        return skipUsesPattern.matcher(name).find();
    }

    /**
     * Tests whether the class definition should not be checked because it
     * matches the {@code checker.skipDefs} property.
     *
     * @param node class to potentially skip
     * @return true if checker should not test node
     */
    public final boolean shouldSkipDefs(ClassTree node) {
        String qualifiedName = InternalUtils.typeOf(node).toString();
        return skipDefsPattern.matcher(qualifiedName).find();
    }

    /**
     * Tests whether the method definition should not be checked because it
     * matches the {@code checker.skipDefs} property.
     *
     * TODO: currently only uses the class definition. Refine pattern. Same for skipUses.
     *
     * @param cls class to potentially skip
     * @param meth method to potentially skip
     * @return true if checker should not test node
     */
    public final boolean shouldSkipDefs(ClassTree cls, MethodTree meth) {
        return shouldSkipDefs(cls);
    }


    /**
     * A helper function to parse a Properties file
     *
     * @param cls   the class whose location is the base of the file path
     * @param filePath the name/path of the file to be read
     * @return  the properties
     */
    protected Properties getProperties(Class<?> cls, String filePath) {
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
    public final SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }
}
