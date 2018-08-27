package org.checkerframework.framework.source;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.CFContext;
import org.checkerframework.framework.util.CheckerMain;
import org.checkerframework.framework.util.OptionConfiguration;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.PluginUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

/**
 * An abstract annotation processor designed for implementing a source-file checker as an annotation
 * processor (a compiler plug-in). It provides an interface to {@code javac}'s annotation processing
 * API, routines for error reporting via the JSR 199 compiler API, and an implementation for using a
 * {@link SourceVisitor} to perform the type-checking.
 *
 * <p>Subclasses must implement the following methods: (TODO: update the list)
 *
 * <ul>
 *   <li>{@link SourceChecker#getMessages} (for type-qualifier specific error messages)
 *   <li>{@link SourceChecker#createSourceVisitor} (for a custom {@link SourceVisitor})
 *   <li>{@link SourceChecker#getSuppressWarningsKeys} (for honoring {@literal @}{link
 *       SuppressWarnings} annotations)
 * </ul>
 *
 * Most type-checker plug-ins will want to extend {@link BaseTypeChecker}, instead of this class.
 * Only checkers that require annotated types but not subtype checking (e.g. for testing purposes)
 * should extend this. Non-type checkers (e.g. for enforcing coding styles) should extend {@link
 * AbstractProcessor} (or even this class) as the Checker Framework is not designed for such
 * checkers.
 */
@SupportedOptions({
    // When adding a new standard option:
    // 1. Add a brief blurb here about the use case
    //    and a pointer to one prominent use of the option.
    // 2. Update the Checker Framework manual:
    //     * docs/manual/introduction.tex contains a list of all options,
    //       which should be in the same order as this source code file.
    //     * a specific section should contain a detailed discussion.

    ///
    /// Unsound checking: ignore some errors
    ///

    // A comma-separated list of warnings to suppress
    // org.checkerframework.framework.source.SourceChecker.createSuppressWarnings
    "suppressWarnings",

    // Set inclusion/exclusion of type uses or definitions
    // org.checkerframework.framework.source.SourceChecker.shouldSkipUses and similar
    "skipUses",
    "onlyUses",
    "skipDefs",
    "onlyDefs",

    // Whether to ignore all subtype tests for type arguments that
    // were inferred for a raw type
    // org.checkerframework.framework.type.TypeHierarchy.isSubtypeTypeArguments
    "ignoreRawTypeArguments",

    // Unsoundly ignore side effects
    "assumeSideEffectFree",

    // Whether to assume that assertions are enabled or disabled
    // org.checkerframework.framework.flow.CFCFGBuilder.CFCFGBuilder
    "assumeAssertionsAreEnabled",
    "assumeAssertionsAreDisabled",

    // Treat checker errors as warnings
    // org.checkerframework.framework.source.SourceChecker.report
    "warns",

    ///
    /// More sound (strict checking): enable errors that are disabled by default
    ///

    // The next ones *increase* rather than *decrease* soundness.
    // They will eventually be replaced by their complements
    // (except -AconcurrentSemantics) and moved into the above section.

    // TODO: Checking of bodies of @SideEffectFree, @Deterministic, and
    // @Pure methods is temporarily disabled unless -AcheckPurityAnnotations is
    // supplied on the command line.
    // Re-enable it after making the analysis more precise.
    // org.checkerframework.common.basetype.BaseTypeVisitor.visitMethod(MethodTree, Void)
    "checkPurityAnnotations",

    // TODO: Temporary option to make array subtyping invariant,
    // which will be the new default soon.
    "invariantArrays",

    // TODO:  Temporary option to make casts stricter, in particular when
    // casting to an array or generic type. This will be the new default soon.
    "checkCastElementType",

    // Whether to use unchecked code defaults for bytecode and/or source code; these are configured
    // by the specific type checker using @Default[QualifierInHierarchy]InUncheckedCode[For].
    // This option takes arguments "source" and/or "bytecode".
    // The default is "-source,-bytecode" (eventually this will be changed to "-source,bytecode").
    // Note, if unchecked code defaults are turned on for source code, the unchecked
    // defaults are not applied to code in scope of an @AnnotatedFor.
    // See the "Compiling partially-annotated libraries" and
    // "Default qualifiers for \<.class> files (conservative library defaults)"
    // sections in the manual for more details
    // org.checkerframework.framework.source.SourceChecker.useUncheckedCodeDefault
    "useDefaultsForUncheckedCode",

    // Whether to assume sound concurrent semantics or
    // simplified sequential semantics
    // org.checkerframework.framework.flow.CFAbstractTransfer.sequentialSemantics
    "concurrentSemantics",

    // Whether to use a conservative value for type arguments that could not be inferred.
    // See Issue 979.
    "conservativeUninferredTypeArguments",

    ///
    /// Type-checking modes:  enable/disable functionality
    ///

    // Lint options
    // org.checkerframework.framework.source.SourceChecker.getSupportedLintOptions() and similar
    "lint",

    // Whether to suggest methods that could be marked @SideEffectFree,
    // @Deterministic, or @Pure
    // org.checkerframework.common.basetype.BaseTypeVisitor.visitMethod(MethodTree, Void)
    "suggestPureMethods",

    // Whether to resolve reflective method invocations.
    // "-AresolveReflection=debug" causes debugging information
    // to be output.
    "resolveReflection",

    // Whether to use .jaif files whole-program inference
    "infer",

    // With each warning, in addition to the concrete error key,
    // output the suppress warning keys that can be used to
    // suppress that warning.
    "showSuppressWarningKeys",

    // Warn about @SuppressWarnings annotations that do not suppress any warnings.
    // org.checkerframework.common.basetype.BaseTypeChecker.warnUnneededSuppressions
    // org.checkerframework.framework.source.SourceChecker.warnUnneededSuppressions
    // org.checkerframework.framework.source.SourceChecker.shouldSuppressWarnings(javax.lang.model.element.Element, java.lang.String)
    // org.checkerframework.framework.source.SourceeVisitor.checkForSuppressWarningsAnno
    "warnUnneededSuppressions",

    ///
    /// Partially-annotated libraries
    ///

    // Additional stub files to use
    // org.checkerframework.framework.type.AnnotatedTypeFactory.parseStubFiles()
    "stubs",
    // Whether to print warnings about types/members in a stub file
    // that were not found on the class path
    // org.checkerframework.framework.stub.StubParser.warnIfNotFound
    "stubWarnIfNotFound",
    // Whether to ignore missing classes even when warnIfNotFound is set to true and
    // other classes from the same package are present (useful if a package spans more than one
    // jar).
    // org.checkerframework.framework.stub.StubParser.warnIfNotFoundIgnoresClasses
    "stubWarnIfNotFoundIgnoresClasses",
    // Whether to print warnings about stub files that overwrite annotations
    // from bytecode.
    "stubWarnIfOverwritesBytecode",
    // Already listed above, but worth noting again in this section:
    // "useDefaultsForUncheckedCode"

    ///
    /// Debugging
    ///

    /// Amount of detail in messages

    // Whether to print @InvisibleQualifier marked annotations
    // org.checkerframework.framework.type.AnnotatedTypeMirror.toString()
    "printAllQualifiers",

    // Whether to print [] around a set of type parameters in order to clearly see where they end
    // e.g.  <E extends F, F extends Object>
    // without this option the E is printed: E extends F extends Object
    // with this option:                    E [ extends F [ extends Object super Void ] super Void ]
    // when multiple type variables are used this becomes useful very quickly
    "printVerboseGenerics",

    // Output detailed message in simple-to-parse format, useful
    // for tools parsing Checker Framework output.
    // org.checkerframework.framework.source.SourceChecker.message(Kind, Object, String, Object...)
    "detailedmsgtext",

    // Whether to NOT output a stack trace for each framework error.
    // org.checkerframework.framework.source.SourceChecker.logBugInCF
    "noPrintErrorStack",

    // Only output error code, useful for testing framework
    // org.checkerframework.framework.source.SourceChecker.message(Kind, Object, String, Object...)
    "nomsgtext",

    /// Stub and JDK libraries

    // Ignore the standard jdk.astub file; primarily for testing or debugging.
    // org.checkerframework.framework.type.AnnotatedTypeFactory.parseStubFiles()
    "ignorejdkastub",

    // Whether to check that the annotated JDK is correctly provided
    // org.checkerframework.common.basetype.BaseTypeVisitor.checkForAnnotatedJdk()
    "nocheckjdk",

    // Whether to print debugging messages while processing the stub files
    // org.checkerframework.framework.stub.StubParser.debugStubParser
    "stubDebug",

    /// Progress tracing

    // Output file names before checking
    // org.checkerframework.framework.source.SourceChecker.typeProcess()
    "filenames",

    // Output all subtyping checks
    // org.checkerframework.common.basetype.BaseTypeVisitor
    "showchecks",

    // Output information about intermediate steps in method type argument inference
    // org.checkerframework.framework.util.typeinference.DefaultTypeArgumentInference
    "showInferenceSteps",

    /// Visualizing the CFG

    // Implemented in the wrapper rather than this file, but worth noting here.
    // -AoutputArgsToFile

    // Mechanism to visualize the control flow graph (CFG).
    // The argument is a sequence of values or key-value pairs.
    // The first argument has to be the fully-qualified name of the
    // org.checkerframework.dataflow.cfg.CFGVisualizer implementation
    // that should be used. The remaining values or key-value pairs are
    // passed to CFGVisualizer.init.
    // For example:
    //    -Acfgviz=MyViz,a,b=c,d
    // instantiates class MyViz and calls CFGVisualizer.init
    // with {"a" -> true, "b" -> "c", "d" -> true}.
    "cfgviz",

    // Directory for .dot files generated from the CFG visualization in
    // org.checkerframework.dataflow.cfg.DOTCFGVisualizer
    // as initialized by
    // org.checkerframework.framework.type.GenericAnnotatedTypeFactory.createCFGVisualizer()
    // -Aflowdotdir=xyz
    // is short-hand for
    // -Acfgviz=org.checkerframework.dataflow.cfg.DOTCFGVisualizer,outdir=xyz
    "flowdotdir",

    // Enable additional output in the CFG visualization.
    // -Averbosecfg
    // is short-hand for
    // -Acfgviz=MyClass,verbose
    "verbosecfg",

    /// Miscellaneous debugging options

    // Whether to output resource statistics at JVM shutdown
    // org.checkerframework.framework.source.SourceChecker.shutdownHook()
    "resourceStats",

    // Set the cache size for caches in AnnotatedTypeFactory
    "atfCacheSize",

    // Sets AnnotatedTypeFactory shouldCache to false
    "atfDoNotCache"
})
public abstract class SourceChecker extends AbstractTypeProcessor
        implements CFContext, OptionConfiguration {

    // TODO A checker should export itself through a separate interface,
    // and maybe have an interface for all the methods for which it's safe
    // to override.

    /** The @SuppressWarnings key that will suppress warnings for all checkers. */
    public static final String SUPPRESS_ALL_KEY = "all";

    /** The @SuppressWarnings key emitted when an unused warning suppression is found. */
    public static final @CompilerMessageKey String UNNEEDED_SUPPRESSION_KEY =
            "unneeded.suppression";

    /** File name of the localized messages. */
    protected static final String MSGS_FILE = "messages.properties";

    /** Maps error keys to localized/custom error messages. */
    protected Properties messages;

    /** Used to report error messages and warnings via the compiler. */
    protected Messager messager;

    /** Used as a helper for the {@link SourceVisitor}. */
    protected Trees trees;

    /** The source tree that is being scanned. */
    protected CompilationUnitTree currentRoot;

    /**
     * If an error is detected in a CompilationUnitTree, skip all future calls of typeProcess with
     * that same CompilationUnitTree.
     */
    private CompilationUnitTree previousErrorCompilationUnit;

    /** The visitor to use. */
    protected SourceVisitor<?, ?> visitor;

    /** Keys for warning suppressions specified on the command line. */
    private String @Nullable [] suppressWarnings;

    /**
     * Regular expression pattern to specify Java classes that are not annotated, so warnings about
     * uses of them should be suppressed.
     *
     * <p>It contains the pattern specified by the user, through the option {@code
     * checkers.skipUses}; otherwise it contains a pattern that can match no class.
     */
    private Pattern skipUsesPattern;

    /**
     * Regular expression pattern to specify Java classes that are annotated, so warnings about them
     * should be issued but warnings about all other classes should be suppressed.
     *
     * <p>It contains the pattern specified by the user, through the option {@code
     * checkers.onlyUses}; otherwise it contains a pattern matches every class.
     */
    private Pattern onlyUsesPattern;

    /**
     * Regular expression pattern to specify Java classes whose definition should not be checked.
     *
     * <p>It contains the pattern specified by the user, through the option {@code
     * checkers.skipDefs}; otherwise it contains a pattern that can match no class.
     */
    private Pattern skipDefsPattern;

    /**
     * Regular expression pattern to specify Java classes whose definition should be checked.
     *
     * <p>It contains the pattern specified by the user, through the option {@code
     * checkers.onlyDefs}; otherwise it contains a pattern that matches every class.
     */
    private Pattern onlyDefsPattern;

    /** The supported lint options. */
    private Set<String> supportedLints;

    /** The enabled lint options. */
    private Set<String> activeLints;

    /**
     * The active options for this checker. This is a processed version of {@link
     * ProcessingEnvironment#getOptions()}: If the option is of the form "-ACheckerName@key=value"
     * and the current checker class, or one of its superclasses is named "CheckerName", then add
     * key &rarr; value. If the option is of the form "-ACheckerName@key=value" and the current
     * checker class, and none of its superclasses is named "CheckerName", then do not add key
     * &rarr; value. If the option is of the form "-Akey=value", then add key &rarr; value.
     *
     * <p>Both the simple and the canonical name of the checker can be used. Superclasses of the
     * current checker are also considered.
     */
    private Map<String, String> activeOptions;

    /**
     * The string that separates the checker name from the option name. This string may only consist
     * of valid Java identifier part characters, because it will be used within the key of an
     * option.
     */
    private static final String OPTION_SEPARATOR = "_";

    /** The line separator. */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator").intern();

    /**
     * The checker that called this one, whether that be a BaseTypeChecker (used as a compound
     * checker) or an AggregateChecker. Null if this is the checker that calls all others. Note that
     * in the case of a compound checker, the compound checker is the parent, not the checker that
     * was run prior to this one by the compound checker.
     */
    protected SourceChecker parentChecker = null;

    /** List of upstream checker names. Includes the current checker. */
    protected List<String> upstreamCheckerNames = null;

    @Override
    public final synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        // The processingEnvironment field will also be set by the superclass' init method.
        // This is used to trigger AggregateChecker's setProcessingEnvironment.
        setProcessingEnvironment(env);

        double jreVersion = PluginUtil.getJreVersion();
        if (jreVersion != 1.8) {
            throw new UserError(
                    String.format(
                            "The Checker Framework must be run under JDK 1.8.  You are using version %f.",
                            jreVersion));
        }
    }

    /** @return the {@link ProcessingEnvironment} that was supplied to this checker */
    @Override // from CFChecker
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.processingEnv;
    }

    /* This method is protected only to allow the AggregateChecker and BaseTypeChecker to call it. */
    protected void setProcessingEnvironment(ProcessingEnvironment env) {
        this.processingEnv = env;
    }

    protected void setParentChecker(SourceChecker parentChecker) {
        this.parentChecker = parentChecker;
    }

    /**
     * Return a list containing this checker name and all checkers it is a part of (that is,
     * checkers that called it).
     */
    public List<String> getUpstreamCheckerNames() {
        if (upstreamCheckerNames == null) {
            upstreamCheckerNames = new ArrayList<>();

            SourceChecker checker = this;

            while (checker != null) {
                upstreamCheckerNames.add(checker.getClass().getName());
                checker = checker.parentChecker;
            }
        }

        return upstreamCheckerNames;
    }

    /** @return the {@link CFContext} used by this checker */
    public CFContext getContext() {
        return this;
    }

    @Override
    public SourceChecker getChecker() {
        return this;
    }

    @Override
    public OptionConfiguration getOptionConfiguration() {
        return this;
    }

    @Override
    public Elements getElementUtils() {
        return getProcessingEnvironment().getElementUtils();
    }

    @Override
    public Types getTypeUtils() {
        return getProcessingEnvironment().getTypeUtils();
    }

    @Override
    public Trees getTreeUtils() {
        return Trees.instance(getProcessingEnvironment());
    }

    @Override
    public SourceVisitor<?, ?> getVisitor() {
        return this.visitor;
    }

    /**
     * Provides the {@link SourceVisitor} that the checker should use to scan input source trees.
     *
     * @return a {@link SourceVisitor} to use to scan source trees
     */
    protected abstract SourceVisitor<?, ?> createSourceVisitor();

    @Override
    public AnnotationProvider getAnnotationProvider() {
        throw new UnsupportedOperationException(
                "getAnnotationProvider is not implemented for this class.");
    }

    /**
     * Provides a mapping of error keys to custom error messages.
     *
     * <p>As a default, this implementation builds a {@link Properties} out of file {@code
     * messages.properties}. It accumulates all the properties files in the Java class hierarchy
     * from the checker up to {@code SourceChecker}. This permits subclasses to inherit default
     * messages while being able to override them.
     *
     * @return a {@link Properties} that maps error keys to error message text
     */
    public Properties getMessages() {
        if (this.messages != null) {
            return this.messages;
        }

        this.messages = new Properties();
        ArrayDeque<Class<?>> checkers = new ArrayDeque<>();

        Class<?> currClass = this.getClass();
        while (currClass != SourceChecker.class) {
            checkers.addFirst(currClass);
            currClass = currClass.getSuperclass();
        }
        checkers.addFirst(SourceChecker.class);

        while (!checkers.isEmpty()) {
            messages.putAll(getProperties(checkers.removeFirst(), MSGS_FILE));
        }
        return this.messages;
    }

    private Pattern getSkipPattern(String patternName, Map<String, String> options) {
        // Default is an illegal Java identifier substring
        // so that it won't match anything.
        // Note that AnnotatedType's toString output format contains characters such as "():{}".
        return getPattern(patternName, options, "\\]'\"\\]");
    }

    private Pattern getOnlyPattern(String patternName, Map<String, String> options) {
        // default matches everything
        return getPattern(patternName, options, ".");
    }

    private Pattern getPattern(
            String patternName, Map<String, String> options, String defaultPattern) {
        String pattern = "";

        if (options.containsKey(patternName)) {
            pattern = options.get(patternName);
            if (pattern == null) {
                message(
                        Kind.WARNING,
                        "The " + patternName + " property is empty; please fix your command line");
                pattern = "";
            }
        } else if (System.getProperty("checkers." + patternName) != null) {
            pattern = System.getProperty("checkers." + patternName);
        } else if (System.getenv(patternName) != null) {
            pattern = System.getenv(patternName);
        }

        if (pattern.indexOf("/") != -1) {
            message(
                    Kind.WARNING,
                    "The "
                            + patternName
                            + " property contains \"/\", which will never match a class name: "
                            + pattern);
        }

        if (pattern.equals("")) {
            pattern = defaultPattern;
        }

        return Pattern.compile(pattern);
    }

    private Pattern getSkipUsesPattern(Map<String, String> options) {
        return getSkipPattern("skipUses", options);
    }

    private Pattern getOnlyUsesPattern(Map<String, String> options) {
        return getOnlyPattern("onlyUses", options);
    }

    private Pattern getSkipDefsPattern(Map<String, String> options) {
        return getSkipPattern("skipDefs", options);
    }

    private Pattern getOnlyDefsPattern(Map<String, String> options) {
        return getOnlyPattern("onlyDefs", options);
    }

    // TODO: do we want this?
    // Cache the keys that we already warned about to prevent repetitions.
    // private Set<String> warnedOnLint = new HashSet<String>();

    private Set<String> createActiveLints(Map<String, String> options) {
        if (!options.containsKey("lint")) {
            return Collections.emptySet();
        }

        String lintString = options.get("lint");
        if (lintString == null) {
            return Collections.singleton("all");
        }

        Set<String> activeLint = new HashSet<>();
        for (String s : lintString.split(",")) {
            if (!this.getSupportedLintOptions().contains(s)
                    && !(s.charAt(0) == '-'
                            && this.getSupportedLintOptions().contains(s.substring(1)))
                    && !s.equals("all")
                    && !s.equals("none") /*&&
                    !warnedOnLint.contains(s)*/) {
                this.messager.printMessage(
                        javax.tools.Diagnostic.Kind.WARNING,
                        "Unsupported lint option: "
                                + s
                                + "; All options: "
                                + this.getSupportedLintOptions());
                // warnedOnLint.add(s);
            }

            activeLint.add(s);
            if (s.equals("none")) {
                activeLint.add("-all");
            }
        }

        return Collections.unmodifiableSet(activeLint);
    }

    private Map<String, String> createActiveOptions(Map<String, String> options) {
        if (options.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> activeOpts = new HashMap<>();

        for (Map.Entry<String, String> opt : options.entrySet()) {
            String key = opt.getKey();
            String value = opt.getValue();

            String[] split = key.split(OPTION_SEPARATOR);

            switch (split.length) {
                case 1:
                    // No separator, option always active
                    activeOpts.put(key, value);
                    break;
                case 2:
                    // Valid class-option pair
                    Class<?> clazz = this.getClass();

                    do {
                        if (clazz.getCanonicalName().equals(split[0])
                                || clazz.getSimpleName().equals(split[0])) {
                            activeOpts.put(split[1], value);
                        }

                        clazz = clazz.getSuperclass();
                    } while (clazz != null
                            && !clazz.getName()
                                    .equals(AbstractTypeProcessor.class.getCanonicalName()));
                    break;
                default:
                    throw new UserError(
                            "Invalid option name: "
                                    + key
                                    + " At most one separator "
                                    + OPTION_SEPARATOR
                                    + " expected, but found "
                                    + split.length);
            }
        }
        return Collections.unmodifiableMap(activeOpts);
    }

    private String @Nullable [] createSuppressWarnings(Map<String, String> options) {
        if (!options.containsKey("suppressWarnings")) {
            return null;
        }

        String swString = options.get("suppressWarnings");
        if (swString == null) {
            return null;
        }

        return swString.split(",");
    }

    /** Log a user error. */
    private void logUserError(UserError ce) {
        StringBuilder msg = new StringBuilder(ce.getMessage());
        printMessage(msg + ".");
    }

    /** Log an internal error in the framework or a checker. */
    private void logBugInCF(BugInCF ce) {
        // TODO: do this at construction time.
        if (ce.getMessage() == null) {
            final String stackTrace = formatStackTrace(ce.getStackTrace());
            throw new BugInCF(
                    "Null error message while logging Checker error.\nStack Trace:\n" + stackTrace);
        }

        StringBuilder msg = new StringBuilder(ce.getMessage());
        boolean noPrintErrorStack =
                (processingEnv != null
                        && processingEnv.getOptions() != null
                        && processingEnv.getOptions().containsKey("noPrintErrorStack"));
        if (ce.getCause() == null) {
            msg.append("; The Checker Framework crashed.  Please report the crash.");
        } else if (noPrintErrorStack) {
            msg.append(
                    "; The Checker Framework crashed.  Please report the crash.  To see "
                            + "the full stack trace, don't invoke the compiler with -AnoPrintErrorStack");
        } else {
            if (this.currentRoot != null && this.currentRoot.getSourceFile() != null) {
                msg.append("\nCompilation unit: " + this.currentRoot.getSourceFile().getName());
            }
            if (this.visitor != null) {
                DiagnosticPosition pos = (DiagnosticPosition) this.visitor.lastVisited;
                DiagnosticSource source =
                        new DiagnosticSource(this.currentRoot.getSourceFile(), null);
                int linenr = source.getLineNumber(pos.getStartPosition());
                int col = source.getColumnNumber(pos.getStartPosition(), true);
                String line = source.getLine(pos.getStartPosition());

                msg.append(
                        "\nLast visited tree at line " + linenr + " column " + col + ":\n" + line);
            }

            msg.append(
                    "\nException: "
                            + ce.getCause().toString()
                            + "; "
                            + formatStackTrace(ce.getCause().getStackTrace()));
            Throwable cause = ce.getCause().getCause();
            while (cause != null) {
                msg.append(
                        "\nUnderlying Exception: "
                                + (cause.toString()
                                        + "; "
                                        + formatStackTrace(cause.getStackTrace())));
                cause = cause.getCause();
            }
        }

        printMessage(msg.toString());
    }

    /** Print the given message. */
    private void printMessage(String msg) {
        if (messager == null) {
            messager = processingEnv.getMessager();
        }
        messager.printMessage(javax.tools.Diagnostic.Kind.ERROR, msg);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Type-checkers are not supposed to override this. Instead use initChecker. This allows us
     * to handle BugInCF only here and doesn't require all overriding implementations to be aware of
     * BugInCF.
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
                messager = processingEnv.getMessager();
                messager.printMessage(
                        javax.tools.Diagnostic.Kind.WARNING,
                        "You have forgotten to call super.initChecker in your "
                                + "subclass of SourceChecker, "
                                + this.getClass()
                                + "! Please ensure your checker is properly initialized.");
            }
            if (shouldAddShutdownHook()) {
                Runtime.getRuntime()
                        .addShutdownHook(
                                new Thread() {
                                    @Override
                                    public void run() {
                                        shutdownHook();
                                    }
                                });
            }
        } catch (UserError ce) {
            logUserError(ce);
        } catch (BugInCF ce) {
            logBugInCF(ce);
        } catch (Throwable t) {
            logBugInCF(wrapThrowableAsBugInCF("SourceChecker.typeProcessingStart", t, null));
        }
    }

    /**
     * Initialize the checker.
     *
     * @see AbstractProcessor#init(ProcessingEnvironment)
     */
    public void initChecker() {
        // Grab the Trees and Messager instances now; other utilities
        // (like Types and Elements) can be retrieved by subclasses.
        @Nullable Trees trees = Trees.instance(processingEnv);
        assert trees != null; /*nninvariant*/
        this.trees = trees;

        this.messager = processingEnv.getMessager();
        this.messages = getMessages();

        this.visitor = createSourceVisitor();

        // TODO: hack to clear out static caches.
        AnnotationUtils.clear();
    }

    /**
     * Return true to indicate that method {@link #shutdownHook} should be added as a shutdownHook
     * of the JVM.
     */
    protected boolean shouldAddShutdownHook() {
        return hasOption("resourceStats");
    }

    /**
     * Method that gets called exactly once at shutdown time of the JVM. Checkers can override this
     * method to customize the behavior.
     */
    protected void shutdownHook() {
        if (hasOption("resourceStats")) {
            // Check for the "resourceStats" option and don't call shouldAddShutdownHook
            // to allow subclasses to override shouldXXX and shutdownHook and simply
            // call the super implementations.
            printStats();
        }
    }

    /** Print resource usage statistics. */
    protected void printStats() {
        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPool : memoryPools) {
            System.out.println("Memory pool " + memoryPool.getName() + " statistics");
            System.out.println("  Pool type: " + memoryPool.getType());
            System.out.println("  Peak usage: " + memoryPool.getPeakUsage());
        }
    }

    /** Output the warning about source level at most once. */
    private boolean warnedAboutSourceLevel = false;

    /**
     * The number of errors at the last exit of the type processor. At entry to the type processor
     * we check whether the current error count is higher and then don't process the file, as it
     * contains some Java errors. Needs to be protected to allow access from AggregateChecker and
     * BaseTypeChecker.
     */
    protected int errsOnLastExit = 0;

    /**
     * Type-check the code with Java specifications and then runs the Checker Rule Checking visitor
     * on the processed source.
     *
     * @see Processor#process(Set, RoundEnvironment)
     */
    @Override
    public void typeProcess(TypeElement e, TreePath p) {
        if (e == null) {
            messager.printMessage(
                    javax.tools.Diagnostic.Kind.ERROR, "Refusing to process empty TypeElement");
            return;
        }
        if (p == null) {
            messager.printMessage(
                    javax.tools.Diagnostic.Kind.ERROR,
                    "Refusing to process empty TreePath in TypeElement: " + e);
            return;
        }

        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        com.sun.tools.javac.code.Source source = com.sun.tools.javac.code.Source.instance(context);
        if ((!warnedAboutSourceLevel) && (!source.allowTypeAnnotations())) {
            messager.printMessage(
                    javax.tools.Diagnostic.Kind.WARNING,
                    "-source " + source.name + " does not support type annotations");
            warnedAboutSourceLevel = true;
        }

        Log log = Log.instance(context);
        if (log.nerrors > this.errsOnLastExit) {
            this.errsOnLastExit = log.nerrors;
            previousErrorCompilationUnit = p.getCompilationUnit();
            return;
        }
        if (p.getCompilationUnit() == previousErrorCompilationUnit) {
            // If the same compilation unit was seen with an error before,
            // skip it. This is in particular necessary for Java errors, which
            // show up once, but further calls to typeProcess will happen.
            // See Issue 346.
            return;
        } else {
            previousErrorCompilationUnit = null;
        }
        if (visitor == null) {
            // typeProcessingStart invokes initChecker, which should
            // have set the visitor. If the field is still null, an
            // exception occured during initialization, which was already
            // logged there. Don't also cause a NPE here.
            return;
        }
        if (p.getCompilationUnit() != currentRoot) {
            currentRoot = p.getCompilationUnit();
            if (hasOption("filenames")) {
                message(
                        Kind.NOTE,
                        "Checker: %s is type-checking: %s",
                        (Object) this.getClass().getSimpleName(),
                        currentRoot.getSourceFile().getName());
            }
            visitor.setRoot(currentRoot);
        }

        // Visit the attributed tree.
        try {
            visitor.visit(p);
            warnUnneededSuppressions();
        } catch (UserError ce) {
            logUserError(ce);
        } catch (BugInCF ce) {
            logBugInCF(ce);
        } catch (Throwable t) {
            logBugInCF(wrapThrowableAsBugInCF("SourceChecker.typeProcess", t, p));
        } finally {
            // Also add possibly deferred diagnostics, which will get published back in
            // AbstractTypeProcessor.
            this.errsOnLastExit = log.nerrors;
        }
    }

    /**
     * Issues a warning about any {@code @SuppressWarnings} that isn't used by this checker, but
     * contains a key that would suppress a warning from this checker.
     */
    protected void warnUnneededSuppressions() {
        if (!hasOption("warnUnneededSuppressions")) {
            return;
        }

        Set<Element> elementsSuppress = new HashSet<>(this.elementsWithSuppressedWarnings);
        this.elementsWithSuppressedWarnings.clear();
        Set<String> checkerKeys = new HashSet<>(getSuppressWarningsKeys());
        Set<String> errorKeys = new HashSet<>(messages.stringPropertyNames());
        warnUnneedSuppressions(elementsSuppress, checkerKeys, errorKeys);
        getVisitor().treesWithSuppressWarnings.clear();
    }

    /**
     * Issues a warning about any {@code @SuppressWarnings} that isn't used by this checker, but
     * contains a key that would suppress a warning from this checker.
     *
     * @param elementsSuppress elements with a {@code @SuppressWarnings} that actually suppressed a
     *     warning
     * @param checkerKeys suppress warning keys that suppress any warning from this checker
     * @param errorKeys error keys that can be issued by this checker
     */
    protected void warnUnneedSuppressions(
            Set<Element> elementsSuppress, Set<String> checkerKeys, Set<String> errorKeys) {
        // It's not clear for which checker this suppression is intended,
        // so never report it as unused.
        checkerKeys.remove(SourceChecker.SUPPRESS_ALL_KEY);

        for (Tree tree : getVisitor().treesWithSuppressWarnings) {
            Element elt = TreeUtils.elementFromTree(tree);
            SuppressWarnings suppressAnno = elt.getAnnotation(SuppressWarnings.class);
            if (suppressAnno == null || elementsSuppress.contains(elt)) {
                continue;
            }
            for (String keyFromAnno : suppressAnno.value()) {
                for (String checkerKey : checkerKeys) {
                    // KeyFromAnno may contain a checker key, but may not be equal to it in cases
                    // where the checker key if followed by a more precise warning.
                    // For example if keyFromAnno is "nullness:assignment.type.incompatible"
                    if (keyFromAnno.contains(checkerKey)) {
                        reportUnneededSuppression(tree, keyFromAnno);
                    }
                }
                if (keyFromAnno.contains(":")) {
                    // The key starts with a checker name, if that is this checker, then the warning
                    // was issued above.  For example, if this is the Nullness Checker and the
                    // keyForAnno is "index:override.return.invalid", then don't issue a warning.
                    continue;
                }

                for (String errorKey : errorKeys) {
                    // The keyFromAnno may only be a part of an error key.
                    // For example, @SuppressWarnings("purity") suppresses errors with keys:
                    // purity.deterministic.void.method, purity.deterministic.constructor, etc..
                    if (errorKey.contains(keyFromAnno)) {
                        reportUnneededSuppression(tree, keyFromAnno);
                    }
                }
            }
        }
    }

    /**
     * Issues a warning that the key in a {@code @SuppressWarnings} on {@code tree} isn't needed.
     *
     * @param tree has unneeded {@code @SuppressWarnings}
     * @param key suppress warning key that isn't needed
     */
    private void reportUnneededSuppression(Tree tree, String key) {
        Tree swTree = findSuppressWarningsTree(tree);
        report(
                Result.warning(
                        SourceChecker.UNNEEDED_SUPPRESSION_KEY,
                        getClass().getSimpleName(),
                        "\"" + key + "\""),
                swTree);
    }

    /**
     * Finds the tree that is a {@code @SuppressWarnings} annotation.
     *
     * @param tree a class, method, or variable tree annotated with {@code @SuppressWarnings}
     * @return tree for {@code @SuppressWarnings} or {@code default} if one isn't found
     */
    private Tree findSuppressWarningsTree(Tree tree) {
        List<? extends AnnotationTree> annotations;
        if (TreeUtils.isClassTree(tree)) {
            annotations = ((ClassTree) tree).getModifiers().getAnnotations();
        } else if (tree.getKind() == Tree.Kind.METHOD) {
            annotations = ((MethodTree) tree).getModifiers().getAnnotations();
        } else {
            annotations = ((VariableTree) tree).getModifiers().getAnnotations();
        }

        for (AnnotationTree annotationTree : annotations) {
            if (AnnotationUtils.areSameByClass(
                    TreeUtils.annotationFromAnnotationTree(annotationTree),
                    SuppressWarnings.class)) {
                return annotationTree;
            }
        }
        throw new BugInCF("Did not find @SuppressWarnings: " + tree);
    }

    private BugInCF wrapThrowableAsBugInCF(String where, Throwable t, @Nullable TreePath p) {
        return new BugInCF(
                where
                        + ": unexpected Throwable ("
                        + t.getClass().getSimpleName()
                        + ")"
                        + ((p == null)
                                ? ""
                                : " while processing "
                                        + p.getCompilationUnit().getSourceFile().getName())
                        + (t.getMessage() == null ? "" : "; message: " + t.getMessage()),
                t);
    }

    /** Format a list of {@link StackTraceElement}s to be printed out as an error message. */
    protected String formatStackTrace(StackTraceElement[] stackTrace) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        if (stackTrace.length == 0) {
            sb.append("no stack trace available.");
        } else {
            sb.append("Stack trace: ");
        }
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
    //     System.out.printf(
    //             "    env.elementUtils = %s%n", ((JavacProcessingEnvironment) env).elementUtils);
    //     System.out.printf(
    //             "      env.elementUtils.types = %s%n",
    //             ((JavacProcessingEnvironment) env).elementUtils.types);
    //     System.out.printf(
    //             "      env.elementUtils.enter = %s%n",
    //             ((JavacProcessingEnvironment) env).elementUtils.enter);
    //     System.out.printf(
    //             "    env.typeUtils = %s%n", ((JavacProcessingEnvironment) env).typeUtils);
    //     System.out.printf("  trees = %s%n", trees);
    //     System.out.printf(
    //             "    trees.enter = %s%n", ((com.sun.tools.javac.api.JavacTrees) trees).enter);
    //     System.out.printf(
    //             "    trees.elements = %s%n",
    //             ((com.sun.tools.javac.api.JavacTrees) trees).elements);
    //     System.out.printf(
    //             "      trees.elements.types = %s%n",
    //             ((com.sun.tools.javac.api.JavacTrees) trees).elements.types);
    //     System.out.printf(
    //             "      trees.elements.enter = %s%n",
    //             ((com.sun.tools.javac.api.JavacTrees) trees).elements.enter);
    // }

    /**
     * Returns the localized long message corresponding for this key, and returns the defValue if no
     * localized message is found.
     */
    protected String fullMessageOf(String messageKey, String defValue) {
        String key = messageKey;

        do {
            if (messages.containsKey(key)) {
                return messages.getProperty(key);
            }

            int dot = key.indexOf('.');
            if (dot < 0) {
                return defValue;
            }
            key = key.substring(dot + 1);
        } while (true);
    }

    /**
     * Prints a message (error, warning, note, etc.) via JSR-269.
     *
     * @param kind the type of message to print
     * @param source the object from which to obtain source position information
     * @param msgKey the message key to print
     * @param args arguments for interpolation in the string corresponding to the given message key
     * @see Diagnostic
     * @throws IllegalArgumentException if {@code source} is neither a {@link Tree} nor an {@link
     *     Element}
     */
    private void message(
            Diagnostic.Kind kind,
            Object source,
            @CompilerMessageKey String msgKey,
            Object... args) {

        assert messages != null : "null messages";

        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                if (args[i] == null) {
                    continue;
                }

                // Try to process the arguments
                args[i] = processArg(args[i]);
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
            // The -Adetailedmsgtext command-line option was given, so output
            // a stylized error message for easy parsing by a tool.

            StringBuilder sb = new StringBuilder();

            // The parts, separated by " $$ " (DETAILS_SEPARATOR), are:

            // (1) error key
            // TODO: should we also have some type system identifier here?
            // E.g. Which subclass of SourceChecker we are? Or also the SuppressWarnings keys?
            sb.append(defaultFormat);
            sb.append(DETAILS_SEPARATOR);

            // (2) number of additional tokens, and those tokens; this
            // depends on the error message, and an example is the found
            // and expected types
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

            // (3) The error position, as starting and ending characters in
            // the source file.
            final Tree tree;
            if (source instanceof Element) {
                tree = trees.getTree((Element) source);
            } else if (source instanceof Tree) {
                tree = (Tree) source;
            } else {
                tree = null;
            }
            sb.append(treeToFilePositionString(tree, currentRoot, processingEnv));
            sb.append(DETAILS_SEPARATOR);

            // (4) The human-readable error message.
            sb.append(fullMessageOf(msgKey, defaultFormat));

            fmtString = sb.toString();

        } else {
            final String suppressing;
            if (this.processingEnv.getOptions().containsKey("showSuppressWarningKeys")) {
                suppressing = String.format("[%s:%s] ", this.getSuppressWarningsKeys(), msgKey);
            } else {
                suppressing = String.format("[%s] ", msgKey);
            }
            fmtString = suppressing + fullMessageOf(msgKey, defaultFormat);
        }
        String messageText;
        try {
            messageText = String.format(fmtString, args);
        } catch (Exception e) {
            messageText =
                    "Invalid format string: \"" + fmtString + "\" args: " + Arrays.toString(args);
        }

        if (LINE_SEPARATOR != "\n") { // interned
            // Replace '\n' with the proper line separator
            messageText = messageText.replaceAll("\n", LINE_SEPARATOR);
        }

        if (source instanceof Element) {
            messager.printMessage(kind, messageText, (Element) source);
        } else if (source instanceof Tree) {
            printMessage(kind, messageText, (Tree) source, currentRoot);
        } else {
            throw new BugInCF("invalid position source: " + source.getClass().getName());
        }
    }

    /**
     * Do not call this method directly. Call {@link #report(Result, Object)} instead. (This method
     * exists so that the BaseTypeChecker can override it and treat messages from compound checkers
     * differently.)
     */
    protected void printMessage(
            Diagnostic.Kind kind, String message, Tree source, CompilationUnitTree root) {
        Trees.instance(processingEnv).printMessage(kind, message, source, root);
    }

    /**
     * Process an argument to an error message before it is passed to String.format.
     *
     * @param arg the argument
     * @return the result after processing
     */
    protected Object processArg(Object arg) {
        // Check to see if the argument itself is a property to be expanded
        return messages.getProperty(arg.toString(), arg.toString());
    }

    /**
     * Print a non-localized message using the javac messager. This is preferable to using
     * System.out or System.err, but should only be used for exceptional cases that don't happen in
     * correct usage. Localized messages should be raised using {@link SourceChecker#report(Result,
     * Object)}.
     *
     * @param kind the kind of message to print
     * @param msg the message text
     * @param args optional arguments to substitute in the message
     * @see SourceChecker#report(Result, Object)
     */
    public void message(Diagnostic.Kind kind, String msg, Object... args) {
        String ftdmsg = String.format(msg, args);
        if (messager != null) {
            messager.printMessage(kind, ftdmsg);
        } else {
            System.err.println(kind + ": " + ftdmsg);
        }
    }

    /**
     * For the given tree, compute the source positions for that tree. Return a "tuple" like string
     * (e.g. "( 1, 200 )" ) that contains the start and end position of the tree in the current
     * compilation unit.
     *
     * @param tree tree to locate within the current compilation unit
     * @param currentRoot the current compilation unit
     * @param processingEnv the current processing environment
     * @return a tuple string representing the range of characters that tree occupies in the source
     *     file
     */
    public String treeToFilePositionString(
            Tree tree, CompilationUnitTree currentRoot, ProcessingEnvironment processingEnv) {
        if (tree == null) {
            return null;
        }

        SourcePositions sourcePositions = trees.getSourcePositions();
        long start = sourcePositions.getStartPosition(currentRoot, tree);
        long end = sourcePositions.getEndPosition(currentRoot, tree);

        return "( " + start + ", " + end + " )";
    }

    public static final String DETAILS_SEPARATOR = " $$ ";

    /**
     * Determines whether an error (whose error key is {@code errKey}) should be suppressed,
     * according to the user's explicitly-written SuppressWarnings annotation {@code anno} or the
     * {@code -AsuppressWarnings} command-line argument.
     *
     * <p>A @SuppressWarnings value may be of the following pattern:
     *
     * <ol>
     *   <li>{@code "suppress-key"}, where suppress-key is a supported warnings key, as specified by
     *       {@link #getSuppressWarningsKeys()} (e.g., {@code "nullness"} for Nullness, {@code
     *       "regex"} for Regex)
     *   <li>{@code "suppress-key:error-key}, where the suppress-key is as above, and error-key is a
     *       prefix or suffix of the errors that it may suppress. So "nullness:generic.argument",
     *       would suppress any errors in the Nullness Checker related to generic.argument.
     * </ol>
     *
     * @param anno the @SuppressWarnings annotation written by the user
     * @param errKey the error key the checker is emitting
     * @return true if one of {@code anno}'s keys is returned by {@link
     *     SourceChecker#getSuppressWarningsKeys}; also accounts for errKey
     */
    private boolean checkSuppressWarnings(@Nullable SuppressWarnings anno, String errKey) {

        // Don't suppress warnings if this checker provides no key to do so.
        Collection<String> checkerSwKeys = this.getSuppressWarningsKeys();
        if (checkerSwKeys.isEmpty()) {
            return false;
        }

        String[] userSwKeys = (anno == null ? null : anno.value());
        if (this.suppressWarnings == null) {
            this.suppressWarnings = createSuppressWarnings(getOptions());
        }
        String[] cmdLineSwKeys = this.suppressWarnings;

        return checkSuppressWarnings(userSwKeys, errKey)
                || checkSuppressWarnings(cmdLineSwKeys, errKey);
    }

    /**
     * Return true if the given error should be suppressed, based on the given @SuppressWarnings
     * keys.
     *
     * @param userSwKeys the @SuppressWarnings keys supplied by the user
     * @param errKey the error key the checker is emitting
     * @return true if one of the {@code userSwKeys} is returned by {@link
     *     SourceChecker#getSuppressWarningsKeys}; also accounts for errKey
     */
    private boolean checkSuppressWarnings(String @Nullable [] userSwKeys, String errKey) {
        if (userSwKeys == null) {
            return false;
        }

        Collection<String> checkerSwKeys = this.getSuppressWarningsKeys();

        // Check each value of the user-written @SuppressWarnings annotation.
        for (String suppressWarningValue : userSwKeys) {
            for (String checkerKey : checkerSwKeys) {
                if (suppressWarningValue.equalsIgnoreCase(checkerKey)) {
                    return true;
                }

                String expected = checkerKey + ":" + errKey;
                if (expected.toLowerCase().contains(suppressWarningValue.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines whether all the warnings pertaining to a given tree should be suppressed. Returns
     * true if the tree is within the scope of a @SuppressWarnings annotation, one of whose values
     * suppresses the checker's warnings. The list of keys that suppress a checker's warnings is
     * provided by the {@link SourceChecker#getSuppressWarningsKeys} method.
     *
     * @param tree the tree that might be a source of a warning
     * @param errKey the error key the checker is emitting
     * @return true if no warning should be emitted for the given tree because it is contained by a
     *     declaration with an appropriately-valued {@literal @}SuppressWarnings annotation; false
     *     otherwise
     */
    // Public so it can be called from a few places in
    // org.checkerframework.framework.flow.CFAbstractTransfer
    public boolean shouldSuppressWarnings(Tree tree, String errKey) {
        // Don't suppress warnings if this checker provides no key to do so.
        Collection<String> checkerKeys = this.getSuppressWarningsKeys();
        if (checkerKeys.isEmpty()) {
            return false;
        }

        // trees.getPath might be slow, but this is only used in error reporting
        // TODO: #1586 this might return null within a cloned finally block and
        // then a warning that should be suppressed isn't. Fix this when fixing #1586.
        @Nullable TreePath path = trees.getPath(this.currentRoot, tree);
        if (path == null) {
            return false;
        }

        @Nullable VariableTree var = TreeUtils.enclosingVariable(path);
        if (var != null && shouldSuppressWarnings(TreeUtils.elementFromTree(var), errKey)) {
            return true;
        }

        @Nullable MethodTree method = TreeUtils.enclosingMethod(path);
        if (method != null) {
            @Nullable Element elt = TreeUtils.elementFromTree(method);

            if (shouldSuppressWarnings(elt, errKey)) {
                return true;
            }

            if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
                // Return false immediately. Do NOT check for AnnotatedFor in
                // the enclosing elements, because they may not have an
                // @AnnotatedFor.
                return false;
            }
        }

        @Nullable ClassTree cls = TreeUtils.enclosingClass(path);
        if (cls != null) {
            @Nullable Element elt = TreeUtils.elementFromTree(cls);

            if (shouldSuppressWarnings(elt, errKey)) {
                return true;
            }

            if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
                // Return false immediately. Do NOT check for AnnotatedFor in
                // the enclosing elements, because they may not have an
                // @AnnotatedFor.
                return false;
            }
        }

        if (useUncheckedCodeDefault("source")) {
            // If we got this far without hitting an @AnnotatedFor and returning
            // false, we DO suppress the warning.
            return true;
        }

        return false;
    }

    /**
     * Should unchecked code defaults be used for the kind of code indicated by the parameter.
     *
     * @param kindOfCode source or bytecode
     * @return whether unchecked code defaults should be used
     */
    public boolean useUncheckedCodeDefault(String kindOfCode) {
        final boolean useUncheckedDefaultsForSource = false;
        final boolean useUncheckedDefaultsForByteCode = false;
        String option = this.getOption("useDefaultsForUncheckedCode");

        String[] args = option != null ? option.split(",") : new String[0];
        for (String arg : args) {
            boolean value = arg.indexOf("-") != 0;
            arg = value ? arg : arg.substring(1);
            if (arg.equals(kindOfCode)) {
                return value;
            }
        }
        if (kindOfCode.equals("source")) {
            return useUncheckedDefaultsForSource;
        } else if (kindOfCode.equals("bytecode")) {
            return useUncheckedDefaultsForByteCode;
        } else {
            throw new UserError(
                    "SourceChecker: unexpected argument to useUncheckedCodeDefault: " + kindOfCode);
        }
    }

    /**
     * Elements with a {@code @SuppressWarnings} that actually suppressed a warning for this
     * checker.
     */
    protected final Set<Element> elementsWithSuppressedWarnings = new HashSet<>();

    /**
     * Determines whether all the warnings pertaining to a given tree should be suppressed. Returns
     * true if the element is within the scope of a @SuppressWarnings annotation, one of whose
     * values suppresses the checker's warnings. The list of keys that suppress a checker's warnings
     * is provided by the {@link SourceChecker#getSuppressWarningsKeys} method.
     *
     * @param elt the Element that might be a source of, or related to, a warning
     * @param errKey the error key the checker is emitting
     * @return true if no warning should be emitted for the given Element because it is contained by
     *     a declaration with an appropriately-valued {@code @SuppressWarnings} annotation; false
     *     otherwise
     */
    // Public so it can be called from InitializationVisitor.checkerFieldsInitialized
    public boolean shouldSuppressWarnings(@Nullable Element elt, String errKey) {
        if (UNNEEDED_SUPPRESSION_KEY.equals(errKey)) {
            // never suppress an unneeded suppression key warning.
            return false;
        }

        if (elt == null) {
            return false;
        }

        if (checkSuppressWarnings(elt.getAnnotation(SuppressWarnings.class), errKey)) {
            if (hasOption("warnUnneededSuppressions")) {
                elementsWithSuppressedWarnings.add(elt);
            }
            return true;
        }

        if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
            // Return false immediately. Do NOT check for AnnotatedFor in the
            // enclosing elements, because they may not have an @AnnotatedFor.
            return false;
        }

        return shouldSuppressWarnings(elt.getEnclosingElement(), errKey);
    }

    private boolean isAnnotatedForThisCheckerOrUpstreamChecker(@Nullable Element elt) {

        if (elt == null || !useUncheckedCodeDefault("source")) {
            return false;
        }

        @Nullable AnnotatedFor anno = elt.getAnnotation(AnnotatedFor.class);

        String[] userAnnotatedFors = (anno == null ? null : anno.value());

        if (userAnnotatedFors != null) {
            List<String> upstreamCheckerNames = getUpstreamCheckerNames();

            for (String userAnnotatedFor : userAnnotatedFors) {
                if (CheckerMain.matchesCheckerOrSubcheckerFromList(
                        userAnnotatedFor, upstreamCheckerNames)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Reports a result. By default, it prints it to the screen via the compiler's internal
     * messenger if the result is non-success; otherwise, the method returns with no side effects.
     *
     * @param r the result to report
     * @param src the position object associated with the result
     */
    public void report(final Result r, final Object src) {

        String errKey = r.getMessageKeys().iterator().next();
        if (src instanceof Tree && shouldSuppressWarnings((Tree) src, errKey)) {
            return;
        }
        if (src instanceof Element && shouldSuppressWarnings((Element) src, errKey)) {
            return;
        }

        if (r.isSuccess()) {
            return;
        }

        for (Result.DiagMessage msg : r.getDiagMessages()) {
            if (r.isFailure()) {
                this.message(
                        hasOption("warns")
                                ? Diagnostic.Kind.MANDATORY_WARNING
                                : Diagnostic.Kind.ERROR,
                        src,
                        msg.getMessageKey(),
                        msg.getArgs());
            } else if (r.isWarning()) {
                this.message(
                        Diagnostic.Kind.MANDATORY_WARNING, src, msg.getMessageKey(), msg.getArgs());
            } else {
                this.message(Diagnostic.Kind.NOTE, src, msg.getMessageKey(), msg.getArgs());
            }
        }
    }

    /**
     * Determines the value of the lint option with the given name. Just as <a
     * href="https://docs.oracle.com/javase/7/docs/technotes/guides/javac/index.html">javac</a> uses
     * "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx, annotation-related lint
     * options are enabled with "-Alint:xxx" and disabled with "-Alint:-xxx".
     *
     * @throws IllegalArgumentException if the option name is not recognized via the {@link
     *     SupportedLintOptions} annotation or the {@link SourceChecker#getSupportedLintOptions}
     *     method
     * @param name the name of the lint option to check for
     * @return true if the lint option was given, false if it was not given or was given prepended
     *     with a "-"
     * @see SourceChecker#getLintOption(String, boolean)
     */
    public final boolean getLintOption(String name) {
        return getLintOption(name, false);
    }

    /**
     * Determines the value of the lint option with the given name. Just as <a
     * href="https://docs.oracle.com/javase/1.5.0/docs/tooldocs/solaris/javac.html">javac</a> uses
     * "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx, annotation-related lint
     * options are enabled with "-Alint=xxx" and disabled with "-Alint=-xxx".
     *
     * @throws IllegalArgumentException if the option name is not recognized via the {@link
     *     SupportedLintOptions} annotation or the {@link SourceChecker#getSupportedLintOptions}
     *     method
     * @param name the name of the lint option to check for
     * @param def the default option value, returned if the option was not given
     * @return true if the lint option was given, false if it was given prepended with a "-", or
     *     {@code def} if it was not given at all
     * @see SourceChecker#getLintOption(String)
     * @see SourceChecker#getOption(String)
     */
    public final boolean getLintOption(String name, boolean def) {

        if (!this.getSupportedLintOptions().contains(name)) {
            throw new UserError("Illegal lint option: " + name);
        }

        if (activeLints == null) {
            activeLints = createActiveLints(processingEnv.getOptions());
        }

        if (activeLints.isEmpty()) {
            return def;
        }

        String tofind = name;
        while (tofind != null) {
            if (activeLints.contains(tofind)) {
                return true;
            } else if (activeLints.contains(String.format("-%s", tofind))) {
                return false;
            }

            tofind = parentOfOption(tofind);
        }

        return def;
    }

    /**
     * Set the value of the lint option with the given name. Just as <a
     * href="https://docs.oracle.com/javase/1.5.0/docs/tooldocs/solaris/javac.html">javac</a> uses
     * "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx, annotation-related lint
     * options are enabled with "-Alint=xxx" and disabled with "-Alint=-xxx". This method can be
     * used by subclasses to enforce having certain lint options enabled/disabled.
     *
     * @throws IllegalArgumentException if the option name is not recognized via the {@link
     *     SupportedLintOptions} annotation or the {@link SourceChecker#getSupportedLintOptions}
     *     method
     * @param name the name of the lint option to set
     * @param val the option value
     * @see SourceChecker#getLintOption(String)
     * @see SourceChecker#getLintOption(String,boolean)
     */
    protected final void setLintOption(String name, boolean val) {
        if (!this.getSupportedLintOptions().contains(name)) {
            throw new UserError("Illegal lint option: " + name);
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

        Set<String> newlints = new HashSet<>();
        newlints.addAll(activeLints);
        if (val) {
            newlints.add(name);
        } else {
            newlints.add(String.format("-%s", name));
        }
        activeLints = Collections.unmodifiableSet(newlints);
    }

    /**
     * Helper method to find the parent of a lint key. The lint hierarchy level is donated by a
     * colon ':'. 'all' is the root for all hierarchy.
     *
     * <pre>
     * Example
     *    cast:unsafe &rarr; cast
     *    cast        &rarr; all
     *    all         &rarr; {@code null}
     * </pre>
     */
    private String parentOfOption(String name) {
        if (name.equals("all")) {
            return null;
        } else if (name.contains(":")) {
            return name.substring(0, name.lastIndexOf(':'));
        } else {
            return "all";
        }
    }

    /**
     * Returns the lint options recognized by this checker. Lint options are those which can be
     * checked for via {@link SourceChecker#getLintOption}.
     *
     * @return an unmodifiable {@link Set} of the lint options recognized by this checker
     */
    public Set<String> getSupportedLintOptions() {
        if (supportedLints == null) {
            supportedLints = createSupportedLintOptions();
        }
        return supportedLints;
    }

    /** Compute the set of supported lint options. */
    protected Set<String> createSupportedLintOptions() {
        @Nullable SupportedLintOptions sl = this.getClass().getAnnotation(SupportedLintOptions.class);

        if (sl == null) {
            return Collections.emptySet();
        }

        @Nullable String @Nullable [] slValue = sl.value();
        assert slValue != null; /*nninvariant*/

        @Nullable String[] lintArray = slValue;
        Set<String> lintSet = new HashSet<>(lintArray.length);
        for (String s : lintArray) {
            lintSet.add(s);
        }
        return Collections.unmodifiableSet(lintSet);
    }

    /**
     * Set the supported lint options. Use of this method should be limited to the AggregateChecker,
     * who needs to set the lint options to the union of all subcheckers. Also, e.g. the
     * NullnessSubchecker/RawnessSubchecker need to use this method, as one is created by the other.
     */
    protected void setSupportedLintOptions(Set<String> newlints) {
        supportedLints = newlints;
    }

    /**
     * Add additional active options. Use of this method should be limited to the AggregateChecker,
     * who needs to set the active options to the union of all subcheckers.
     */
    protected void addOptions(Map<String, String> moreopts) {
        Map<String, String> activeOpts = new HashMap<>(getOptions());
        activeOpts.putAll(moreopts);
        activeOptions = Collections.unmodifiableMap(activeOpts);
    }

    /**
     * Check whether the given option is provided.
     *
     * <p>Note that {@link #getOption(String)} can still return null even if {@code hasOption}
     * returns true: this happens e.g. for {@code -Amyopt}
     *
     * @param name the name of the option to check
     * @return true if the option name was provided, false otherwise
     */
    @Override
    public final boolean hasOption(String name) {
        return getOptions().containsKey(name);
    }

    /**
     * Determines the value of the option with the given name.
     *
     * @param name the name of the option to check
     * @see SourceChecker#getLintOption(String,boolean)
     */
    @Override
    public final String getOption(String name) {
        return getOption(name, null);
    }

    /**
     * Determines the boolean value of the option with the given name. Returns false if the option
     * is not set.
     *
     * @param name the name of the option to check
     * @see SourceChecker#getLintOption(String,boolean)
     */
    @Override
    public final boolean getBooleanOption(String name) {
        return getBooleanOption(name, false);
    }

    /**
     * Determines the boolean value of the option with the given name. Returns the given default
     * value if the option is not set.
     *
     * @param name the name of the option to check
     * @param defaultValue the default value to use if the option is not set
     * @see SourceChecker#getLintOption(String,boolean)
     */
    @Override
    public final boolean getBooleanOption(String name, boolean defaultValue) {
        String value = getOption(name);
        if (value == null) {
            return defaultValue;
        }
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        throw new UserError(
                String.format(
                        "Value of %s option should be a boolean, but is \"%s\".", name, value));
    }

    /**
     * Return all active options for this checker.
     *
     * @return all active options for this checker
     */
    @Override
    public Map<String, String> getOptions() {
        if (activeOptions == null) {
            activeOptions = createActiveOptions(processingEnv.getOptions());
        }
        return activeOptions;
    }

    /**
     * Determines the value of the lint option with the given name and returns the default value if
     * nothing is specified.
     *
     * @param name the name of the option to check
     * @param defaultValue the default value to use if the option is not set
     * @see SourceChecker#getOption(String)
     * @see SourceChecker#getLintOption(String)
     */
    @Override
    public final String getOption(String name, String defaultValue) {

        if (!this.getSupportedOptions().contains(name)) {
            throw new UserError("Illegal option: " + name);
        }

        if (activeOptions == null) {
            activeOptions = createActiveOptions(processingEnv.getOptions());
        }

        if (activeOptions.isEmpty()) {
            return defaultValue;
        }

        if (activeOptions.containsKey(name)) {
            return activeOptions.get(name);
        } else {
            return defaultValue;
        }
    }

    /**
     * Map the Checker Framework version of {@link SupportedOptions} to the standard annotation
     * provided version {@link javax.annotation.processing.SupportedOptions}.
     */
    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<>();

        // Support all options provided with the standard
        // {@link javax.annotation.processing.SupportedOptions}
        // annotation.
        options.addAll(super.getSupportedOptions());

        // For the Checker Framework annotation
        // {@link org.checkerframework.framework.source.SupportedOptions}
        // we additionally add
        Class<?> clazz = this.getClass();
        List<Class<?>> clazzPrefixes = new ArrayList<>();

        do {
            clazzPrefixes.add(clazz);

            SupportedOptions so = clazz.getAnnotation(SupportedOptions.class);
            if (so != null) {
                options.addAll(expandCFOptions(clazzPrefixes, so.value()));
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null
                && !clazz.getName().equals(AbstractTypeProcessor.class.getCanonicalName()));

        return Collections.unmodifiableSet(options);
    }

    /**
     * Generate the possible command-line option names by prefixing each class name from {@code
     * classPrefixes} to {@code options}, separated by {@code OPTION_SEPARATOR}.
     *
     * @param clazzPrefixes the classes to prefix
     * @param options the option names
     * @return the possible combinations that should be supported
     */
    protected Collection<String> expandCFOptions(
            List<? extends Class<?>> clazzPrefixes, String[] options) {
        Set<String> res = new HashSet<>();

        for (String option : options) {
            res.add(option);
            for (Class<?> clazz : clazzPrefixes) {
                res.add(clazz.getCanonicalName() + OPTION_SEPARATOR + option);
                res.add(clazz.getSimpleName() + OPTION_SEPARATOR + option);
            }
        }
        return res;
    }

    /**
     * Overrides the default implementation to always return a singleton set containing only "*".
     *
     * <p>javac uses this list to determine which classes process; javac only runs an annotation
     * processor on classes that contain at least one of the mentioned annotations. Thus, the effect
     * of returning "*" is as if the checker were annotated by
     * {@code @SupportedAnnotationTypes("*")}: javac runs the checker on every class mentioned on
     * the javac command line. This method also checks that subclasses do not contain a {@link
     * SupportedAnnotationTypes} annotation.
     *
     * <p>To specify the annotations that a checker recognizes as type qualifiers, see {@link
     * AnnotatedTypeFactory#createSupportedTypeQualifiers()}.
     *
     * @throws Error if a subclass is annotated with {@link SupportedAnnotationTypes}
     */
    @Override
    public final Set<String> getSupportedAnnotationTypes() {

        SupportedAnnotationTypes supported =
                this.getClass().getAnnotation(SupportedAnnotationTypes.class);
        if (supported != null) {
            throw new BugInCF(
                    "@SupportedAnnotationTypes should not be written on any checker;"
                            + " supported annotation types are inherited from SourceChecker.");
        }
        return Collections.singleton("*");
    }

    /**
     * @return string keys that a checker honors for suppressing warnings and errors that it issues.
     *     Each such key suppresses all warnings issued by the checker.
     * @see SuppressWarningsKeys
     */
    public Collection<String> getSuppressWarningsKeys() {
        return getStandardSuppressWarningsKeys();
    }

    /**
     * Determine the standard set of suppress warning keys usable for any checker.
     *
     * @see #getSuppressWarningsKeys()
     * @return collection of warning keys
     */
    protected final Collection<String> getStandardSuppressWarningsKeys() {
        SuppressWarningsKeys annotation = this.getClass().getAnnotation(SuppressWarningsKeys.class);

        // TreeSet ensures keys are returned in a consistent order.
        Set<String> result = new TreeSet<>();
        result.add(SUPPRESS_ALL_KEY);

        if (annotation != null) {
            // Add from annotation
            for (String key : annotation.value()) {
                result.add(key);
            }

        } else {
            // No annotation, by default infer key from class name
            String className = this.getClass().getSimpleName();
            int indexOfChecker = className.lastIndexOf("Checker");
            if (indexOfChecker == -1) {
                indexOfChecker = className.lastIndexOf("Subchecker");
            }
            String key =
                    (indexOfChecker == -1) ? className : className.substring(0, indexOfChecker);
            result.add(key.trim().toLowerCase());
        }

        return result;
    }

    /**
     * Tests whether the class owner of the passed element is an unannotated class and matches the
     * pattern specified in the {@code checker.skipUses} property.
     *
     * @param element an element
     * @return true iff the enclosing class of element should be skipped
     */
    public final boolean shouldSkipUses(Element element) {
        if (element == null) {
            return false;
        }
        TypeElement typeElement = ElementUtils.enclosingClass(element);
        String name = typeElement.toString();
        return shouldSkipUses(name);
    }

    /**
     * Tests whether the class owner of the passed type matches the pattern specified in the {@code
     * checker.skipUses} property. In contrast to {@link #shouldSkipUses(Element)} this version can
     * also be used from primitive types, which don't have an element.
     *
     * <p>Checkers that require their annotations not to be checked on certain JDK classes may
     * override this method to skip them. They shall call {@code super.shouldSkipUses(typerName)} to
     * also skip the classes matching the pattern.
     *
     * @param typeName the fully-qualified name of a type
     * @return true iff the enclosing class of element should be skipped
     */
    public boolean shouldSkipUses(String typeName) {
        // System.out.printf("shouldSkipUses(%s) %s%nskipUses %s%nonlyUses %s%nresult %s%n",
        //                   element,
        //                   name,
        //                   skipUsesPattern.matcher(name).find(),
        //                   onlyUsesPattern.matcher(name).find(),
        //                   (skipUsesPattern.matcher(name).find()
        //                    || ! onlyUsesPattern.matcher(name).find()));
        // StackTraceElement[] stea = new Throwable().getStackTrace();
        // for (int i=0; i<3; i++) {
        //     System.out.println("  " + stea[i]);
        // }
        // System.out.println();
        if (skipUsesPattern == null) {
            skipUsesPattern = getSkipUsesPattern(getOptions());
        }
        if (onlyUsesPattern == null) {
            onlyUsesPattern = getOnlyUsesPattern(getOptions());
        }
        return skipUsesPattern.matcher(typeName).find()
                || !onlyUsesPattern.matcher(typeName).find();
    }

    /**
     * Tests whether the class definition should not be checked because it matches the {@code
     * checker.skipDefs} property.
     *
     * @param node class to potentially skip
     * @return true if checker should not test node
     */
    public final boolean shouldSkipDefs(ClassTree node) {
        String qualifiedName = TreeUtils.typeOf(node).toString();
        // System.out.printf("shouldSkipDefs(%s) %s%nskipDefs %s%nonlyDefs %s%nresult %s%n%n",
        //                   node,
        //                   qualifiedName,
        //                   skipDefsPattern.matcher(qualifiedName).find(),
        //                   onlyDefsPattern.matcher(qualifiedName).find(),
        //                   (skipDefsPattern.matcher(qualifiedName).find()
        //                    || ! onlyDefsPattern.matcher(qualifiedName).find()));
        if (skipDefsPattern == null) {
            skipDefsPattern = getSkipDefsPattern(getOptions());
        }
        if (onlyDefsPattern == null) {
            onlyDefsPattern = getOnlyDefsPattern(getOptions());
        }

        return skipDefsPattern.matcher(qualifiedName).find()
                || !onlyDefsPattern.matcher(qualifiedName).find();
    }

    /**
     * Tests whether the method definition should not be checked because it matches the {@code
     * checker.skipDefs} property.
     *
     * <p>TODO: currently only uses the class definition. Refine pattern. Same for skipUses.
     *
     * @param cls class to potentially skip
     * @param meth method to potentially skip
     * @return true if checker should not test node
     */
    public final boolean shouldSkipDefs(ClassTree cls, MethodTree meth) {
        return shouldSkipDefs(cls);
    }

    /**
     * A helper function to parse a Properties file.
     *
     * @param cls the class whose location is the base of the file path
     * @param filePath the name/path of the file to be read
     * @return the properties
     */
    protected Properties getProperties(Class<?> cls, String filePath) {
        Properties prop = new Properties();
        try {
            InputStream base = cls.getResourceAsStream(filePath);

            if (base == null) {
                // No message customization file was given
                return prop;
            }

            prop.load(base);
        } catch (IOException e) {
            message(Kind.WARNING, "Couldn't parse properties file: " + filePath);
            // e.printStackTrace();
            // ignore the possible customization file
        }
        return prop;
    }

    @Override
    public final SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
