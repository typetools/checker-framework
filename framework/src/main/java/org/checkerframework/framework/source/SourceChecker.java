package org.checkerframework.framework.source;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import io.github.classgraph.ClassGraph;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
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
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.reflection.MethodValChecker;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.CheckerMain;
import org.checkerframework.framework.util.OptionConfiguration;
import org.checkerframework.framework.util.TreePathCacher;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
import org.plumelib.util.ArrayMap;
import org.plumelib.util.ArraySet;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.SystemPlume;
import org.plumelib.util.UtilPlume;

/**
 * An abstract annotation processor designed for implementing a source-file checker as an annotation
 * processor (a compiler plug-in). It provides an interface to {@code javac}'s annotation processing
 * API, routines for error reporting via the JSR 199 compiler API, and an implementation for using a
 * {@link SourceVisitor} to perform the type-checking.
 *
 * <p>Most type-checker plug-ins should extend {@link BaseTypeChecker}, instead of this class. Only
 * checkers that require annotated types but not subtype checking (e.g. for testing purposes) should
 * extend this. Non-type checkers (e.g. for enforcing coding styles) may extend {@link
 * AbstractProcessor} (or even this class).
 */
@SupportedOptions({
  // When adding a new standard option:
  // 1. Add a brief blurb here about the use case
  //    and a pointer to one prominent use of the option.
  // 2. Update the Checker Framework manual:
  //     * docs/manual/introduction.tex contains a list of all options,
  //       which should be in the same order as this source code file.
  //     * a specific section should contain a detailed discussion.

  //
  // Unsound checking: ignore some errors
  //

  // A comma-separated list of warnings to suppress
  // org.checkerframework.framework.source.SourceChecker.createSuppressWarnings
  "suppressWarnings",

  // Set inclusion/exclusion of type uses, definitions, or files.
  // org.checkerframework.framework.source.SourceChecker.shouldSkipUses and similar
  "skipUses",
  "onlyUses",
  "skipDefs",
  "onlyDefs",
  "skipFiles",
  "onlyFiles",
  "skipDirs", // Obsolete as of 2024-03-15, replaced by "skipFiles".

  // Unsoundly assume all methods have no side effects, are deterministic, or both.
  "assumeSideEffectFree",
  "assumeDeterministic",
  "assumePure",
  // Unsoundly assume getter methods have no side effects and are deterministic.
  "assumePureGetters",

  // Whether to assume that assertions are enabled or disabled
  // org.checkerframework.framework.flow.CFCFGBuilder.CFCFGBuilder
  "assumeAssertionsAreEnabled",
  "assumeAssertionsAreDisabled",

  // Treat checker errors as warnings
  // org.checkerframework.framework.source.SourceChecker.report
  "warns",

  //
  // More sound (strict checking): enable errors that are disabled by default
  //

  // The next ones *increase* rather than *decrease* soundness.  They will eventually be replaced
  // by their complements (except -AconcurrentSemantics) and moved into the above section.

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

  // Whether to use conservative defaults for bytecode and/or source code.
  // This option takes arguments "source" and/or "bytecode".
  // The default is "-source,-bytecode" (eventually this will be changed to "-source,bytecode").
  // Note, in source code, conservative defaults are never
  // applied to code in the scope of an @AnnotatedFor.
  // See the "Compiling partially-annotated libraries" and
  // "Default qualifiers for \<.class> files (conservative library defaults)"
  // sections in the manual for more details
  // org.checkerframework.framework.source.SourceChecker.useConservativeDefault
  "useConservativeDefaultsForUncheckedCode",

  // Whether to assume sound concurrent semantics or
  // simplified sequential semantics
  // org.checkerframework.framework.flow.CFAbstractTransfer.sequentialSemantics
  "concurrentSemantics",

  // Issues a "redundant.anno" warning if the annotation explicitly written on the type is
  // the same as the default annotation for this type and location.
  "warnRedundantAnnotations",

  // Whether to ignore all subtype tests for type arguments that
  // were inferred for a raw type. Defaults to true.
  // org.checkerframework.framework.type.TypeHierarchy.isSubtypeTypeArguments
  "ignoreRawTypeArguments",

  //
  // Type-checking modes:  enable/disable functionality
  //

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

  // Whether to use whole-program inference. Takes an argument to specify the output format:
  // "-Ainfer=stubs" or "-Ainfer=jaifs".
  "infer",

  // Whether to output a copy of each file for which annotations were inferred, formatted
  // as an ajava file. Can only be used with -Ainfer=ajava
  "inferOutputOriginal",

  // With each warning, in addition to the concrete error key,
  // output the SuppressWarnings strings that can be used to
  // suppress that warning.
  "showSuppressWarningsStrings",

  // Warn about @SuppressWarnings annotations that do not suppress any warnings.
  // org.checkerframework.common.basetype.BaseTypeChecker.warnUnneededSuppressions
  // org.checkerframework.framework.source.SourceChecker.warnUnneededSuppressions
  // org.checkerframework.framework.source.SourceChecker.shouldSuppressWarnings(javax.lang.model.element.Element, java.lang.String)
  // org.checkerframework.framework.source.SourceVisitor.checkForSuppressWarningsAnno
  "warnUnneededSuppressions",

  // Exceptions to -AwarnUnneededSuppressions.
  "warnUnneededSuppressionsExceptions",

  // Require that warning suppression annotations contain a checker key as a prefix in order for
  // the warning to be suppressed.
  // org.checkerframework.framework.source.SourceChecker.checkSuppressWarnings(java.lang.String[],
  // java.lang.String)
  "requirePrefixInWarningSuppressions",

  // Print a checker key as a prefix to each typechecking diagnostic.
  // org.checkerframework.framework.source.SourceChecker.suppressWarningsString(java.lang.String)
  "showPrefixInWarningMessages",

  // Ignore annotations in bytecode that have invalid annotation locations.
  // See https://github.com/typetools/checker-framework/issues/2173
  // org.checkerframework.framework.type.ElementAnnotationApplier.apply
  "ignoreInvalidAnnotationLocations",

  //
  // Partially-annotated libraries
  //

  // Additional stub files to use
  // org.checkerframework.framework.type.AnnotatedTypeFactory.parseStubFiles()
  "stubs",
  // Additional ajava files to use
  // org.checkerframework.framework.type.AnnotatedTypeFactory.parserAjavaFiles()
  "ajava",
  // Whether to print warnings about types/members in a stub file
  // that were not found on the class path
  // org.checkerframework.framework.stub.AnnotationFileParser.warnIfNotFound
  "stubWarnIfNotFound",
  "stubNoWarnIfNotFound",
  // Whether to ignore missing classes even when warnIfNotFound is set to true and other classes
  // from the same package are present (useful if a package spans more than one jar).
  // org.checkerframework.framework.stub.AnnotationFileParser.warnIfNotFoundIgnoresClasses
  "stubWarnIfNotFoundIgnoresClasses",
  // Whether to print warnings about stub files that overwrite annotations from bytecode.
  "stubWarnIfOverwritesBytecode",
  // Whether to print warnings about stub files that are redundant with the annotations from
  // bytecode.
  "stubWarnIfRedundantWithBytecode",
  // Whether to issue a NOTE rather than a WARNING for -AstubWarn* command-line options
  "stubWarnNote",
  // With this option, annotations in stub files are used EVEN IF THE SOURCE FILE IS
  // PRESENT. Only use this option when you intend to store types in stub files rather than
  // directly in source code, such as during whole-program inference. The annotations in the
  // stub files will be glb'd with those in the source code before local inference begins.
  "mergeStubsWithSource",
  // Already listed above, but worth noting again in this section:
  // "useConservativeDefaultsForUncheckedCode"

  //
  // Debugging
  //

  // Amount of detail in messages

  // Print the version of the Checker Framework
  "version",
  // Print info about git repository from which the Checker Framework was compiled
  "printGitProperties",

  // Whether to print @InvisibleQualifier marked annotations
  // org.checkerframework.framework.type.AnnotatedTypeMirror.toString()
  "printAllQualifiers",

  // Whether to print [] around a set of type parameters in order to clearly see where they end
  // e.g.  <E extends F, F extends Object>
  // without this option E is printed: E extends F extends Object
  // with this option:                 E [ extends F [ extends Object super Void ] super Void ]
  // when multiple type variables are used this becomes useful very quickly
  "printVerboseGenerics",

  // Whether to NOT output a stack trace for each framework error.
  // org.checkerframework.framework.source.SourceChecker.logBugInCF
  "noPrintErrorStack",

  // If true, issue a NOTE rather than a WARNING when performance is impeded by memory
  // constraints.
  "noWarnMemoryConstraints",

  // Only output error code, useful for testing framework
  // org.checkerframework.framework.source.SourceChecker.message(Kind, Object, String, Object...)
  "nomsgtext",

  // Controls the line separator output in Checker Framework exceptions.
  // org.checkerframework.framework.source.SourceChecker.logBug
  "exceptionLineSeparator",

  // Format of messages

  // Output detailed message in simple-to-parse format, useful
  // for tools parsing Checker Framework output.
  // org.checkerframework.framework.source.SourceChecker.message(Kind, Object, String, Object...)
  "detailedmsgtext",

  // Stub and JDK libraries

  // Ignore the standard jdk.astub file; primarily for testing or debugging.
  // org.checkerframework.framework.type.AnnotatedTypeFactory.parseStubFiles()
  "ignorejdkastub",

  // Whether to check that the annotated JDK is correctly provided
  // org.checkerframework.common.basetype.BaseTypeVisitor.checkForAnnotatedJdk()
  "permitMissingJdk",
  "nocheckjdk", // temporary, for backward compatibility

  // Parse all JDK files at startup rather than as needed.
  // org.checkerframework.framework.stub.AnnotationFileElementTypes.AnnotationFileElementTypes
  "parseAllJdk",

  // Whether to print debugging messages while processing the stub files
  // org.checkerframework.framework.stub.AnnotationFileParser.debugAnnotationFileParser
  "stubDebug",

  // Progress tracing

  // Output file names before checking
  // org.checkerframework.framework.source.SourceChecker.typeProcess()
  "filenames",

  // Output all subtyping checks
  // org.checkerframework.common.basetype.BaseTypeVisitor
  "showchecks",

  // Output a stack trace when reporting errors or warnings
  // org.checkerframework.common.basetype.SourceChecker.printStackTrace()
  "dumpOnErrors",

  // Visualizing the CFG

  // Implemented in the wrapper rather than this file, but worth noting here.
  // -AoutputArgsToFile

  // Mechanism to visualize the control flow graph (CFG).
  // The argument is a sequence of values or key-value pairs.
  // The first argument has to be the fully-qualified name of the
  // org.checkerframework.dataflow.cfg.CFGVisualizer implementation that should be used. The
  // remaining values or key-value pairs are passed to CFGVisualizer.init.
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

  // Caches

  // Set the cache size for caches in AnnotatedTypeFactory
  "atfCacheSize",

  // Sets AnnotatedTypeFactory shouldCache to false
  "atfDoNotCache",

  // Language Server Protocol (LSP) Support

  // TODO: document `-AlspTypeInfo` in manual, as a debugging option.
  // Output detailed type information for nodes in AST
  // org.checkerframework.framework.type.AnnotatedTypeFactory
  "lspTypeInfo",

  // Miscellaneous debugging options

  // Whether to output resource statistics at JVM shutdown
  // org.checkerframework.framework.source.SourceChecker.shutdownHook()
  "resourceStats",

  // Run checks that test ajava files.
  //
  // Whenever processing a source file, parse it with JavaParser and check that the AST can be
  // matched with javac's tree. Crash if not. For testing the class JointJavacJavaParserVisitor.
  //
  // Also checks that annotations can be inserted. For each Java file, clears all annotations and
  // reinserts them, then checks if the original and modified ASTs are equivalent.
  "ajavaChecks",

  // Converts type argument inference crashes into errors. By default, this option is true.
  // Use "-AconvertTypeArgInferenceCrashToWarning=false" to turn this option off and allow type
  // argument inference crashes to crash the type checker.
  "convertTypeArgInferenceCrashToWarning"
})
public abstract class SourceChecker extends AbstractTypeProcessor implements OptionConfiguration {

  // TODO A checker should export itself through a separate interface, and maybe have an interface
  // for all the methods for which it's safe to override.

  /** The message key that will suppress all warnings (it matches any message key). */
  public static final String SUPPRESS_ALL_MESSAGE_KEY = "all";

  /** The SuppressWarnings prefix that will suppress warnings for all checkers. */
  public static final String SUPPRESS_ALL_PREFIX = "allcheckers";

  /** The message key emitted when an unused warning suppression is found. */
  public static final @CompilerMessageKey String UNNEEDED_SUPPRESSION_KEY = "unneeded.suppression";

  /** File name of the localized messages. */
  protected static final String MSGS_FILE = "messages.properties";

  /** True if the Checker Framework version number has already been printed. */
  private static boolean printedVersion = false;

  /**
   * Maps error keys to localized/custom error messages. Do not use directly; call {@link
   * #fullMessageOf} or {@link #processErrorMessageArg}. Is set in {@link #initChecker}.
   */
  protected Properties messagesProperties;

  /**
   * Used to report error messages and warnings via the compiler. Is set in {@link
   * #typeProcessingStart}.
   */
  protected Messager messager;

  /** Element utilities. */
  @SuppressWarnings("nullness:initialization.field.uninitialized") // initialized in init()
  protected Elements elements;

  /** Tree utilities; used as a helper for the {@link SourceVisitor}. */
  @SuppressWarnings("nullness:initialization.field.uninitialized") // initialized in init()
  protected Trees trees;

  /** Type utilities. */
  @SuppressWarnings("nullness:initialization.field.uninitialized") // initialized in init()
  protected Types types;

  /** The source tree that is being scanned. Is set in {@link #setRoot}. */
  protected @MonotonicNonNull @InternedDistinct CompilationUnitTree currentRoot;

  /** The visitor to use. */
  protected SourceVisitor<?, ?> visitor;

  /**
   * The list of suppress warnings prefixes supported by this checker or any of its subcheckers
   * (including indirect subcheckers). Do not access this field directly; instead, use {@link
   * #getSuppressWarningsPrefixesOfSubcheckers}.
   */
  protected @MonotonicNonNull Collection<String> suppressWarningsPrefixesOfSubcheckers = null;

  /**
   * Stores all messages issued by this checker and its subcheckers for the current compilation
   * unit. The messages are printed after all checkers have processed the current compilation unit.
   * The purpose is to sort messages, grouping together all messages about a particular line of
   * code.
   *
   * <p>If this checker has no subcheckers and is not a subchecker for any other checker, then
   * messageStore is null and messages will be printed as they are issued by this checker.
   */
  protected @MonotonicNonNull TreeSet<CheckerMessage> messageStore;

  /**
   * Exceptions to {@code -AwarnUnneededSuppressions} processing. No warning about unneeded
   * suppressions is issued if the SuppressWarnings string matches this pattern.
   */
  private @Nullable Pattern warnUnneededSuppressionsExceptions;

  /**
   * SuppressWarnings strings supplied via the {@code -AsuppressWarnings} option. Do not use
   * directly, call {@link #getSuppressWarningsStringsFromOption()}.
   */
  private String @MonotonicNonNull [] suppressWarningsStringsFromOption;

  /** True if {@link #suppressWarningsStringsFromOption} has been computed. */
  private boolean computedSuppressWarningsStringsFromOption = false;

  /**
   * If true, use the "allcheckers:" warning string prefix.
   *
   * <p>Checkers that never issue any error messages should set this to false. That prevents {@code
   * -AwarnUnneededSuppressions} from issuing warnings about
   * {@code @SuppressWarnings("allcheckers:...")}.
   */
  protected boolean useAllcheckersPrefix = true;

  /**
   * Regular expression pattern to specify Java classes that are not annotated, so warnings about
   * uses of them should be suppressed.
   *
   * <p>It contains the pattern specified by the user, through the option {@code checkers.skipUses};
   * otherwise it contains a pattern that can match no class.
   */
  private @MonotonicNonNull Pattern skipUsesPattern;

  /**
   * Regular expression pattern to specify Java classes that are annotated, so warnings about them
   * should be issued but warnings about all other classes should be suppressed.
   *
   * <p>It contains the pattern specified by the user, through the option {@code checkers.onlyUses};
   * otherwise it contains a pattern that matches every class.
   */
  private @MonotonicNonNull Pattern onlyUsesPattern;

  /**
   * Regular expression pattern to specify Java classes whose definition should not be checked.
   *
   * <p>It contains the pattern specified by the user, through the option {@code checkers.skipDefs};
   * otherwise it contains a pattern that can match no class.
   */
  private @MonotonicNonNull Pattern skipDefsPattern;

  /**
   * Regular expression pattern to specify Java classes whose definition should be checked.
   *
   * <p>It contains the pattern specified by the user, through the option {@code checkers.onlyDefs};
   * otherwise it contains a pattern that matches every class.
   */
  private @MonotonicNonNull Pattern onlyDefsPattern;

  /**
   * Regular expression pattern to specify files or directories that should not be checked.
   *
   * <p>It contains the pattern specified by the user, through the option {@code
   * checkers.skipFiles}; otherwise it contains a pattern that can match no directory.
   */
  private @MonotonicNonNull Pattern skipFilesPattern;

  /**
   * Regular expression pattern to specify files or directories that should be checked.
   *
   * <p>It contains the pattern specified by the user, through the option {@code
   * checkers.onlyFiles}; otherwise it contains a pattern that can match no directory.
   */
  private @MonotonicNonNull Pattern onlyFilesPattern;

  /** The supported lint options. */
  private @MonotonicNonNull Set<String> supportedLints;

  /** The enabled lint options. Is set in {@link #initChecker}. */
  private Set<String> activeLints;

  /**
   * The active options for this checker. This is a processed version of {@link
   * ProcessingEnvironment#getOptions()}: If the option is of the form "-ACheckerName_key=value" and
   * the current checker class, or one of its superclasses, is named "CheckerName", then add key
   * &rarr; value. If the option is of the form "-ACheckerName_key=value" and the current checker
   * class, and none of its superclasses, is named "CheckerName", then do not add key &rarr; value.
   * If the option is of the form "-Akey=value", then add key &rarr; value.
   *
   * <p>Both the simple and the canonical name of the checker can be used. Superclasses of the
   * current checker are also considered.
   */
  protected @MonotonicNonNull Map<String, String> activeOptions;

  /**
   * Supported options for this checker. This is the set of all possible options that could be
   * passed to this checker. By contrast, {@link #activeOptions} is a map for options that were
   * passed for this run of the checker.
   */
  protected @MonotonicNonNull Set<String> supportedOptions = null;

  /**
   * The string that separates the checker name from the option name in a "-A" command-line
   * argument. This string may only consist of valid Java identifier part characters, because it
   * will be used within the key of an option.
   */
  protected static final String OPTION_SEPARATOR = "_";

  /**
   * The checker that called this one, whether that be a BaseTypeChecker (used as a compound
   * checker) or an AggregateChecker. Null if this is the checker that calls all others. Note that
   * in the case of a compound checker, the compound checker is the parent, not the checker that was
   * run prior to this one by the compound checker.
   */
  protected @Nullable SourceChecker parentChecker;

  /** List of upstream checker names. Includes the current checker. */
  protected @MonotonicNonNull List<@FullyQualifiedName String> upstreamCheckerNames;

  /** True if the -Afilenames command-line argument was passed. */
  private boolean printFilenames;

  /** True if the -Awarns command-line argument was passed. */
  private boolean warns;

  /** True if the -AshowSuppressWarningsStrings command-line argument was passed. */
  private boolean showSuppressWarningsStrings;

  /** True if the -ArequirePrefixInWarningSuppressions command-line argument was passed. */
  private boolean requirePrefixInWarningSuppressions;

  /** True if the -AshowPrefixInWarningMessages command-line argument was passed. */
  private boolean showPrefixInWarningMessages;

  /** True if the -AwarnUnneededSuppressions command-line argument was passed. */
  boolean warnUnneededSuppressions;

  /**
   * The full list of subcheckers that need to be run prior to this one, in the order they need to
   * be run. This list will only be non-empty for the one checker that runs all other subcheckers.
   * Do not read this field directly. Instead, retrieve it via {@link #getSubcheckers}.
   *
   * <p>This field will be {@code null} until {@code getSubcheckers} is called. {@code
   * getSubcheckers} sets this field to an immutable list which is empty for all but the ultimate
   * parent checker.
   */
  protected @MonotonicNonNull List<SourceChecker> subcheckers = null;

  /**
   * The list of subcheckers that are direct dependencies of this checker. This list will be
   * non-empty for any checker that has at least one subchecker.
   */
  // This field is set to non-null when `subcheckers` is.
  protected @MonotonicNonNull List<SourceChecker> immediateSubcheckers = null;

  /**
   * TreePathCacher to share between subcheckers. Initialized either in {@link #getTreePathCacher()}
   * or {@link #instantiateSubcheckers(Map)}.
   */
  protected TreePathCacher treePathCacher = null;

  /** Creates a source checker. */
  protected SourceChecker() {}

  // Also see initChecker().
  @Override
  public final synchronized void init(ProcessingEnvironment env) {
    ProcessingEnvironment unwrappedEnv = unwrapProcessingEnvironment(env);
    super.init(unwrappedEnv);
    // Sets processing enviroment and other related fields.
    setProcessingEnvironment(unwrappedEnv);

    if (!hasOption("warnUnneededSuppressionsExceptions")) {
      warnUnneededSuppressionsExceptions = null;
    } else {
      String warnUnneededSuppressionsExceptionsString =
          getOption("warnUnneededSuppressionsExceptions");
      if (warnUnneededSuppressionsExceptionsString == null) {
        throw new UserError("Must supply an argument to -AwarnUnneededSuppressionsExceptions");
      }
      try {
        warnUnneededSuppressionsExceptions =
            Pattern.compile(warnUnneededSuppressionsExceptionsString);
      } catch (PatternSyntaxException e) {
        throw new UserError(
            "Argument to -AwarnUnneededSuppressionsExceptions is not a regular expression: "
                + e.getMessage());
      }
    }

    if (hasOption("printGitProperties")) {
      printGitProperties();
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Getters and setters
  //

  /**
   * Returns the {@link ProcessingEnvironment} that was supplied to this checker.
   *
   * @return the {@link ProcessingEnvironment} that was supplied to this checker
   */
  public ProcessingEnvironment getProcessingEnvironment() {
    return this.processingEnv;
  }

  /**
   * Set the processing environment and other related fields of the current checker.
   *
   * @param env the new processing environment
   */
  private void setProcessingEnvironment(ProcessingEnvironment env) {
    this.processingEnv = env;
    this.elements = processingEnv.getElementUtils();
    this.trees = Trees.instance(processingEnv);
    this.types = processingEnv.getTypeUtils();
  }

  /** Set the parent checker of the current checker. */
  protected void setParentChecker(SourceChecker parentChecker) {
    this.parentChecker = parentChecker;
  }

  /**
   * Returns the immediate parent checker of the current checker.
   *
   * @return the immediate parent checker of the current checker, or null if there is none
   */
  public @Nullable SourceChecker getParentChecker() {
    return this.parentChecker;
  }

  /**
   * Invoked when the current compilation unit root changes.
   *
   * @param newRoot the new compilation unit root
   */
  @SuppressWarnings("interning:assignment") // used in == tests
  public void setRoot(CompilationUnitTree newRoot) {
    this.currentRoot = newRoot;
    visitor.setRoot(currentRoot);
    if (parentChecker == null) {
      // Only clear the path cache if this is the main checker.
      treePathCacher.clear();
    }
  }

  /**
   * Returns a list containing this checker name and all checkers it is a part of (that is, checkers
   * that called it).
   *
   * @return a list containing this checker name and all checkers it is a part of (that is, checkers
   *     that called it)
   */
  public List<@FullyQualifiedName String> getUpstreamCheckerNames() {
    if (upstreamCheckerNames == null) {
      upstreamCheckerNames = new ArrayList<>();

      SourceChecker checker = this;

      while (checker != null) {
        @NonNull String className = checker.getClass().getCanonicalName();
        assert className != null : "@AssumeAssertion(nullness): checker classes have names";
        upstreamCheckerNames.add(className);
        checker = checker.parentChecker;
      }
    }

    return upstreamCheckerNames;
  }

  /**
   * Returns the OptionConfiguration associated with this.
   *
   * @return the OptionConfiguration associated with this
   */
  public OptionConfiguration getOptionConfiguration() {
    return this;
  }

  /**
   * Returns the element utilities associated with this.
   *
   * @return the element utilities associated with this
   */
  public Elements getElementUtils() {
    return elements;
  }

  /**
   * Returns the type utilities associated with this.
   *
   * @return the type utilities associated with this
   */
  public Types getTypeUtils() {
    return types;
  }

  /**
   * Returns the tree utilities associated with this.
   *
   * @return the tree utilities associated with this
   */
  public Trees getTreeUtils() {
    return trees;
  }

  /**
   * Returns the SourceVisitor associated with this.
   *
   * @return the SourceVisitor associated with this
   */
  public SourceVisitor<?, ?> getVisitor() {
    return this.visitor;
  }

  /**
   * Like {@link #getOptions}, but only includes options passed to this checker. Does not include
   * those passed only to subcheckers.
   *
   * @return the active options for this checker, not including those passed only to subcheckers
   */
  public Map<String, String> getOptionsNoSubcheckers() {
    return createActiveOptions(processingEnv.getOptions());
  }

  /**
   * Like {@link #hasOption}, but checks whether the given option is passed to this checker. Does
   * not consider options only passed to subcheckers.
   *
   * @param name the name of the option to check
   * @return true if the option name was passed to this checker, false otherwise
   */
  public final boolean hasOptionNoSubcheckers(String name) {
    return getOptionsNoSubcheckers().containsKey(name);
  }

  /**
   * Return a list of stub files to be treated as if they had been written in a {@code @StubFiles}
   * annotation.
   *
   * @return stub files to be treated as if they had been written in a {@code @StubFiles} annotation
   */
  public List<String> getExtraStubFiles() {
    return Collections.emptyList();
  }

  /**
   * Provides the {@link SourceVisitor} that the checker should use to scan input source trees.
   *
   * @return a {@link SourceVisitor} to use to scan source trees
   */
  protected abstract SourceVisitor<?, ?> createSourceVisitor();

  /**
   * Returns the AnnotationProvider (the type factory) associated with this.
   *
   * @return the AnnotationProvider (the type factory) associated with this
   */
  public AnnotationProvider getAnnotationProvider() {
    throw new UnsupportedOperationException(
        "getAnnotationProvider is not implemented for " + this.getClass().getSimpleName() + ".");
  }

  /**
   * Provides a mapping of error keys to custom error messages.
   *
   * <p>As a default, this implementation builds a {@link Properties} out of file {@code
   * messages.properties}. It accumulates all the properties files in the Java class hierarchy from
   * the checker up to {@code SourceChecker}. This permits subclasses to inherit default messages
   * while being able to override them.
   *
   * @return a {@link Properties} that maps error keys to error message text
   */
  public Properties getMessagesProperties() {
    if (messagesProperties != null) {
      return messagesProperties;
    }

    messagesProperties = new Properties();

    ArrayDeque<Class<?>> checkers = new ArrayDeque<>();
    Class<?> currClass = this.getClass();
    while (currClass != AbstractTypeProcessor.class) {
      checkers.addFirst(currClass);
      currClass = currClass.getSuperclass();
      assert currClass != null : "@AssumeAssertion(nullness): won't encounter Object.class";
    }

    for (Class<?> checker : checkers) {
      messagesProperties.putAll(getProperties(checker, MSGS_FILE, true));
    }
    return messagesProperties;
  }

  /**
   * Return the given skip pattern if supplied by the user, or else a pattern that matches nothing.
   *
   * @param patternName "skipUses" or "skipDefs"
   * @param options the command-line options
   * @return the user-supplied regex for the given pattern, or a regex that matches nothing
   */
  private Pattern getSkipPattern(String patternName, Map<String, String> options) {
    // Default is an illegal Java identifier substring
    // so that it won't match anything.
    // Note that AnnotatedType's toString output format contains characters such as "():{}".
    return getPattern(patternName, options, "\\]'\"\\]");
  }

  /**
   * Return the given only pattern if supplied by the user, or else a pattern that matches
   * everything.
   *
   * @param patternName "onlyUses" or "onlyDefs"
   * @param options the command-line options
   * @return the user-supplied regex for the given pattern, or a regex that matches everything
   */
  private Pattern getOnlyPattern(String patternName, Map<String, String> options) {
    // default matches everything
    return getPattern(patternName, options, ".");
  }

  private Pattern getPattern(
      String patternName, Map<String, String> options, String defaultPattern) {
    String pattern;
    if (options.containsKey(patternName)) {
      pattern = options.get(patternName);
      if (pattern == null) {
        throw new UserError(
            "The " + patternName + " property is empty; please fix your command line");
      }
    } else {
      pattern = System.getProperty("checkers." + patternName);
      if (pattern == null) {
        pattern = System.getenv(patternName);
      }
      if (pattern == null) {
        pattern = "";
      }
    }

    if (pattern.equals("")) {
      pattern = defaultPattern;
    }

    try {
      return Pattern.compile(pattern);
    } catch (PatternSyntaxException e) {
      throw new UserError(
          "The " + patternName + " property is not a regular expression: " + pattern);
    }
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

  /**
   * Extract the value of the {@code skipFiles} option given the value of the options passed to the
   * checker.
   *
   * @param options the map of options and their values passed to the checker
   * @return the value of the {@code skipFiles} option
   */
  private Pattern getSkipFilesPattern(Map<String, String> options) {
    boolean hasSkipFiles = options.containsKey("skipFiles");
    boolean hasSkipDirs = options.containsKey("skipDirs");
    if (hasSkipFiles && hasSkipDirs) {
      throw new UserError("Do not supply both -AskipFiles and -AskipDirs command-line options.");
    }
    // This logic isn't quite right because the checker.skipDirs property might exist.
    if (hasSkipDirs) {
      return getSkipPattern("skipDirs", options);
    } else {
      return getSkipPattern("skipFiles", options);
    }
  }

  /**
   * Extract the value of the {@code onlyFiles} option given the value of the options passed to the
   * checker.
   *
   * @param options the map of options and their values passed to the checker
   * @return the value of the {@code onlyFiles} option
   */
  private Pattern getOnlyFilesPattern(Map<String, String> options) {
    return getOnlyPattern("onlyFiles", options);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Type-checking
  //

  /**
   * {@inheritDoc}
   *
   * <p>Type-checkers are not supposed to override this. Instead override initChecker. This allows
   * us to handle BugInCF only here and doesn't require all overriding implementations to be aware
   * of BugInCF.
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
            Diagnostic.Kind.WARNING,
            "You have forgotten to call super.initChecker in your "
                + "subclass of SourceChecker, "
                + this.getClass()
                + "! Please ensure your checker is properly initialized.");
      }
      if (shouldAddShutdownHook()) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
      }
      if (!printedVersion && hasOption("version")) {
        messager.printMessage(Diagnostic.Kind.NOTE, "Checker Framework " + getCheckerVersion());
        printedVersion = true;
      }
    } catch (UserError ce) {
      logUserError(ce);
    } catch (TypeSystemError ce) {
      logTypeSystemError(ce);
    } catch (BugInCF ce) {
      logBugInCF(ce);
    } catch (Throwable t) {
      logBugInCF(wrapThrowableAsBugInCF("SourceChecker.typeProcessingStart", t, null));
    }
  }

  @Override
  public void typeProcessingOver() {
    for (SourceChecker checker : getSubcheckers()) {
      checker.typeProcessingOver();
    }

    super.typeProcessingOver();
  }

  /**
   * Initialize the checker.
   *
   * @see AbstractProcessor#init(ProcessingEnvironment)
   */
  public void initChecker() {
    // Grab the Trees and Messager instances now; other utilities
    // (like Types and Elements) can be retrieved by subclasses.
    Trees trees = Trees.instance(processingEnv);
    assert trees != null;
    this.trees = trees;

    this.messager = processingEnv.getMessager();
    this.messagesProperties = getMessagesProperties();

    // Set the active options for this checker and all subcheckers.
    getOptions();

    // Initialize all checkers and share supported lint options.
    for (SourceChecker checker : getSubcheckers()) {
      // Each checker should "support" all possible lint options - otherwise
      // subchecker A would complain about a lint option for subchecker B.
      checker.setSupportedLintOptions(this.getSupportedLintOptions());

      // initChecker validates the passed options, so call it after setting supported options
      // and lints.
      checker.initChecker();
    }

    this.visitor = createSourceVisitor();

    if (!getSubcheckers().isEmpty() && parentChecker == null) {
      messageStore = new TreeSet<>();
    }

    // Validate the lint flags, if they haven't been used already.
    if (this.activeLints == null) {
      this.activeLints = createActiveLints(getOptions());
    }

    printFilenames = hasOption("filenames");
    warns = hasOption("warns");
    showSuppressWarningsStrings = hasOption("showSuppressWarningsStrings");
    requirePrefixInWarningSuppressions = hasOption("requirePrefixInWarningSuppressions");
    showPrefixInWarningMessages = hasOption("showPrefixInWarningMessages");
    warnUnneededSuppressions = hasOption("warnUnneededSuppressions");
  }

  /** Output the warning about source level at most once. */
  private boolean warnedAboutSourceLevel = false;

  /**
   * If true, javac failed to compile the code or a previously-run annotation processor issued an
   * error.
   */
  protected boolean javacErrored = false;

  /** Output the warning about memory at most once. */
  private boolean warnedAboutGarbageCollection = false;

  /**
   * The number of errors at the last exit of the type processor (that is, upon completion of
   * processing the previous compilation unit). At entry to the type processor, if the current error
   * count is higher, then javac must have issued an error. If javac issued an error, then don't
   * process the file, as it contains * some Java errors.
   */
  private int errsOnLastExit = 0;

  /**
   * Returns the requested subchecker. A checker of a given class can only be run once, so this
   * returns the only such checker, or null if none was found. The caller must know the exact
   * checker class to request.
   *
   * @param <T> the class of the subchecker to return
   * @param checkerClass the class of the subchecker to return
   * @return the requested subchecker or null if not found
   */
  @SuppressWarnings("unchecked")
  public <T extends SourceChecker> @Nullable T getSubchecker(Class<T> checkerClass) {
    for (SourceChecker checker : immediateSubcheckers) {
      if (checker.getClass() == checkerClass) {
        return (T) checker;
      }
    }
    return null;
  }

  /**
   * Computes the unmodifiable list of immediate subcheckers of this checker, in the order the
   * checkers need to be run.
   *
   * <p>Modifies the {@code alreadyInitializedSubcheckerMap} parameter by adding all recursively
   * newly instantiated subcheckers' class objects and instances. It is necessary to use a map that
   * preserves the order in which entries were inserted, such as LinkedHashMap or ArrayMap.
   *
   * @param alreadyInitializedSubcheckerMap subcheckers that have already been instantiated. Is
   *     modified by this method. Its point is to ensure that if two checkers A and B both depend on
   *     checker C, then checker C is instantiated and run only once, not twice.
   * @return the unmodifiable list of immediate subcheckers of this checker
   */
  protected List<SourceChecker> instantiateSubcheckers(
      Map<Class<? extends SourceChecker>, SourceChecker> alreadyInitializedSubcheckerMap) {
    Set<Class<? extends SourceChecker>> classesOfImmediateSubcheckers =
        getImmediateSubcheckerClasses();
    if (classesOfImmediateSubcheckers.isEmpty()) {
      return Collections.emptyList();
    }

    ArrayList<SourceChecker> immediateSubcheckers =
        new ArrayList<>(classesOfImmediateSubcheckers.size());

    // Performs a depth-first search for all checkers this checker depends on. The depth-first
    // search ensures that the collection has the correct order the checkers need to be run in.
    for (Class<? extends SourceChecker> subcheckerClass : classesOfImmediateSubcheckers) {
      SourceChecker subchecker = alreadyInitializedSubcheckerMap.get(subcheckerClass);
      if (subchecker != null) {
        // Add the already initialized subchecker to the list of immediate subcheckers so
        // that this checker can refer to it.
        immediateSubcheckers.add(subchecker);
        continue;
      }

      // The subchecker is not already initialized.  Do so.

      SourceChecker instance;
      try {
        instance = subcheckerClass.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        throw new TypeSystemError("Could not create an instance of " + subcheckerClass, e);
      }

      immediateSubcheckers.add(instance);
      instance.setProcessingEnvironment(this.processingEnv);
      instance.treePathCacher = this.getTreePathCacher();
      // Prevent the new checker from storing non-immediate subcheckers
      instance.subcheckers = Collections.emptyList();
      instance.immediateSubcheckers =
          instance.instantiateSubcheckers(alreadyInitializedSubcheckerMap);
      instance.setParentChecker(this);
      alreadyInitializedSubcheckerMap.put(subcheckerClass, instance);
    }

    return Collections.unmodifiableList(immediateSubcheckers);
  }

  /**
   * Get the list of all subcheckers (if any). This list is only non-empty for the one checker that
   * runs all other subcheckers. These are recursively instantiated via instantiateSubcheckers() the
   * first time this method is called if field {@code subcheckers} is null. Assumes all checkers run
   * on the same thread.
   *
   * @return the list of all subcheckers (if any)
   */
  public List<SourceChecker> getSubcheckers() {
    if (subcheckers == null) {
      // Instantiate the checkers this one depends on, if any.
      Map<Class<? extends SourceChecker>, SourceChecker> checkerMap = new ArrayMap<>(2);

      immediateSubcheckers = instantiateSubcheckers(checkerMap);

      subcheckers = Collections.unmodifiableList(new ArrayList<>(checkerMap.values()));
    }

    return subcheckers;
  }

  /**
   * Get the shared TreePathCacher instance.
   *
   * @return the shared TreePathCacher instance.
   */
  public TreePathCacher getTreePathCacher() {
    if (treePathCacher == null) {
      // In case it wasn't already set in instantiateSubcheckers.
      treePathCacher = new TreePathCacher();
    }
    return treePathCacher;
  }

  /**
   * Type-check the code using this checker's visitor.
   *
   * @see Processor#process(Set, RoundEnvironment)
   */
  @Override
  public void typeProcess(TypeElement e, TreePath p) {
    if (messageStore != null && parentChecker == null) {
      messageStore.clear();
    }

    // Errors (or other messages) issued via
    //   SourceChecker#message(Diagnostic.Kind, Object, String, Object...)
    // are stored in messageStore until all checkers have processed this compilation unit.
    // All other messages are printed immediately.  This includes errors issued because the
    // checker threw an exception.

    // Update errsOnLastExit for all checkers, so that no matter which one is run next, its test
    // of whether a Java error occurred is correct.

    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    Log log = Log.instance(context);

    int numErrorsOfAllPreviousCheckers = this.errsOnLastExit;
    for (SourceChecker subchecker : getSubcheckers()) {
      subchecker.errsOnLastExit = numErrorsOfAllPreviousCheckers;
      subchecker.messageStore = messageStore;
      int errorsBeforeTypeChecking = log.nerrors;

      subchecker.typeProcess(e, p);

      int errorsAfterTypeChecking = log.nerrors;
      numErrorsOfAllPreviousCheckers += errorsAfterTypeChecking - errorsBeforeTypeChecking;
    }

    this.errsOnLastExit = numErrorsOfAllPreviousCheckers;

    if (javacErrored) {
      return;
    }

    // Cannot use BugInCF here because it is outside of the try/catch for BugInCF.
    if (e == null) {
      messager.printMessage(Diagnostic.Kind.ERROR, "Refusing to process empty TypeElement");
      return;
    }
    if (p == null) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, "Refusing to process empty TreePath in TypeElement: " + e);
      return;
    }

    if (!warnedAboutGarbageCollection) {
      String gcUsageMessage = SystemPlume.gcUsageMessage(.25, 60);
      if (gcUsageMessage != null) {
        boolean noWarnMemoryConstraints =
            (processingEnv != null
                && processingEnv.getOptions() != null
                && processingEnv.getOptions().containsKey("noWarnMemoryConstraints"));
        Diagnostic.Kind kind =
            noWarnMemoryConstraints ? Diagnostic.Kind.NOTE : Diagnostic.Kind.WARNING;
        messager.printMessage(kind, gcUsageMessage);
        warnedAboutGarbageCollection = true;
      }
    }

    Source source = Source.instance(context);
    // Don't use source.allowTypeAnnotations() because that API changed after 9.
    // Also the enum constant Source.JDK1_8 was renamed at some point...
    if (!warnedAboutSourceLevel && source.compareTo(Source.lookup("8")) < 0) {
      messager.printMessage(
          Diagnostic.Kind.WARNING, "-source " + source.name + " does not support type annotations");
      warnedAboutSourceLevel = true;
    }

    if (log.nerrors > this.errsOnLastExit) {
      this.errsOnLastExit = log.nerrors;
      javacErrored = true;
      return;
    }

    if (visitor == null) {
      // typeProcessingStart invokes initChecker, which should have set the visitor. If the
      // field is still null, an exception occurred during initialization, which was already
      // logged there. Don't also cause a NPE here.
      return;
    }
    if (p.getCompilationUnit() != currentRoot) {
      setRoot(p.getCompilationUnit());
      if (printFilenames) {
        // TODO: Have a command-line option to turn the timestamps on/off too, because
        // they are nondeterministic across runs.

        // Add timestamp to indicate how long operations are taking.
        // Duplicate messages are suppressed, so this might not appear in front of every
        // " is type-checking " message (when a file takes less than a second to
        // type-check).
        message(Diagnostic.Kind.NOTE, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        message(
            Diagnostic.Kind.NOTE,
            "%s is type-checking %s",
            (Object) this.getClass().getSimpleName(),
            currentRoot.getSourceFile().getName());
      }
    }

    // Visit the attributed tree.
    try {
      visitor.visit(p);
      warnUnneededSuppressions();
    } catch (UserError ce) {
      logUserError(ce);
    } catch (TypeSystemError ce) {
      logTypeSystemError(ce);
    } catch (BugInCF ce) {
      logBugInCF(ce);
    } catch (Throwable t) {
      logBugInCF(wrapThrowableAsBugInCF("SourceChecker.typeProcess", t, p));
    } finally {
      // Also add possibly deferred diagnostics, which will get published back in
      // AbstractTypeProcessor.
      this.errsOnLastExit = log.nerrors;
      printStoredMessages(p.getCompilationUnit());
      if (!getSubcheckers().isEmpty()) {
        // Update errsOnLastExit to reflect the errors issued.
        this.errsOnLastExit = log.nerrors;
      }
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Reporting type-checking errors; most clients use reportError() or reportWarning()
  //

  /**
   * Reports an error. By default, prints it to the screen via the compiler's internal messager.
   *
   * @param source the source position information; may be an Element, a Tree, or null
   * @param messageKey the message key
   * @param args arguments for interpolation in the string corresponding to the given message key
   */
  public void reportError(
      @Nullable Object source, @CompilerMessageKey String messageKey, Object... args) {
    report(source, Diagnostic.Kind.ERROR, messageKey, args);
  }

  /**
   * Reports a warning. By default, prints it to the screen via the compiler's internal messager.
   *
   * @param source the source position information; may be an Element, a Tree, or null
   * @param messageKey the message key
   * @param args arguments for interpolation in the string corresponding to the given message key
   */
  public void reportWarning(
      @Nullable Object source, @CompilerMessageKey String messageKey, Object... args) {
    report(source, Diagnostic.Kind.MANDATORY_WARNING, messageKey, args);
  }

  /**
   * Reports a diagnostic message. By default, prints it to the screen via the compiler's internal
   * messager.
   *
   * <p>It is rare to use this method. Most clients should use {@link #reportError} or {@link
   * #reportWarning}.
   *
   * @param source the source position information; may be an Element, a Tree, or null
   * @param d the diagnostic message
   */
  public void report(@Nullable Object source, DiagMessage d) {
    report(source, d.getKind(), d.getMessageKey(), d.getArgs());
  }

  /**
   * Reports a diagnostic message. By default, it prints it to the screen via the compiler's
   * internal messager; however, it might also store it for later output.
   *
   * @param source the source position information; may be an Element or a Tree
   * @param kind the type of message
   * @param messageKey the message key
   * @param args arguments for interpolation in the string corresponding to the given message key
   */
  // Not a format method.  However, messageKey should be either a format string for `args`, or a
  // property key that maps to a format string for `args`.
  // @FormatMethod
  @SuppressWarnings("formatter:format.string") // arg is a format string or a property key
  private void report(
      Object source, Diagnostic.Kind kind, @CompilerMessageKey String messageKey, Object... args) {
    assert messagesProperties != null : "null messagesProperties";

    if (shouldSuppressWarnings(source, messageKey)) {
      return;
    }

    if (args != null) {
      for (int i = 0; i < args.length; ++i) {
        args[i] = processErrorMessageArg(args[i]);
      }
    }

    if (kind == Diagnostic.Kind.NOTE) {
      System.err.println("(NOTE) " + String.format(messageKey, args));
      return;
    }

    String defaultFormat = "(" + messageKey + ")";
    String prefix;
    String fmtString;
    if (this.processingEnv.getOptions() != null /*nnbug*/
        && this.processingEnv.getOptions().containsKey("nomsgtext")) {
      prefix = defaultFormat;
      fmtString = null;
    } else if (this.processingEnv.getOptions() != null /*nnbug*/
        && this.processingEnv.getOptions().containsKey("detailedmsgtext")) {
      // The -Adetailedmsgtext command-line option was given, so output
      // a stylized error message for easy parsing by a tool.
      prefix = detailedMsgTextPrefix(source, defaultFormat, args);
      fmtString = fullMessageOf(messageKey, defaultFormat);
    } else {
      prefix = "[" + suppressWarningsString(messageKey) + "] ";
      fmtString = fullMessageOf(messageKey, defaultFormat);
    }
    String messageText;
    try {
      messageText = prefix + (fmtString == null ? "" : String.format(fmtString, args));
    } catch (Exception e) {
      throw new BugInCF(
          "Invalid format string: \"" + fmtString + "\" args: " + Arrays.toString(args), e);
    }

    if (kind == Diagnostic.Kind.ERROR && warns) {
      kind = Diagnostic.Kind.MANDATORY_WARNING;
    }

    if (source instanceof Element) {
      messager.printMessage(kind, messageText, (Element) source);
    } else if (source instanceof Tree) {
      printOrStoreMessage(kind, messageText, (Tree) source, currentRoot);
    } else {
      throw new BugInCF("invalid position source of class " + source.getClass() + ": " + source);
    }
  }

  /**
   * Print a non-localized message using the javac messager. This is preferable to using System.out
   * or System.err, but should only be used for exceptional cases that don't happen in correct
   * usage. Localized messages should be raised using {@link #reportError}, {@link #reportWarning},
   * etc.
   *
   * @param kind the kind of message to print
   * @param msg the message text
   * @param args optional arguments to substitute in the message
   * @see SourceChecker#report(Object, DiagMessage)
   */
  @FormatMethod
  public void message(Diagnostic.Kind kind, String msg, Object... args) {
    message(kind, String.format(msg, args));
  }

  /**
   * Print a non-localized message using the javac messager. This is preferable to using System.out
   * or System.err, but should only be used for exceptional cases that don't happen in correct
   * usage. Localized messages should be raised using {@link #reportError}, {@link #reportWarning},
   * etc.
   *
   * @param kind the kind of message to print
   * @param msg the message text
   * @see SourceChecker#report(Object, DiagMessage)
   */
  public void message(javax.tools.Diagnostic.Kind kind, String msg) {
    if (messager == null) {
      // If this method is called before initChecker() sets the field
      messager = processingEnv.getMessager();
    }
    messager.printMessage(kind, msg);
  }

  /**
   * Print the given message.
   *
   * @param msg the message to print x
   */
  private void printMessage(String msg) {
    if (messager == null) {
      // If this method is called before initChecker() sets the field
      messager = processingEnv.getMessager();
    }
    messager.printMessage(Diagnostic.Kind.ERROR, msg);
  }

  /**
   * Like {@link SourceChecker#getSuppressWarningsPrefixes()}, but includes all prefixes supported
   * by this checker or any of its subcheckers. Does not guarantee that the result is in any
   * particular order. The result is immutable.
   *
   * @return the suppress warnings prefixes supported by this checker or any of its subcheckers
   */
  public Collection<String> getSuppressWarningsPrefixesOfSubcheckers() {
    if (this.suppressWarningsPrefixesOfSubcheckers == null) {
      Collection<String> prefixes = getSuppressWarningsPrefixes();
      for (SourceChecker subchecker : getSubcheckers()) {
        prefixes.addAll(subchecker.getSuppressWarningsPrefixes());
      }
      this.suppressWarningsPrefixesOfSubcheckers = ImmutableSet.copyOf(prefixes);
    }
    return this.suppressWarningsPrefixesOfSubcheckers;
  }

  /**
   * Do not call this method. Call {@link #reportError} or {@link #reportWarning} instead.
   *
   * <p>This method exists so that the BaseTypeChecker can override it. For compound checkers, it
   * stores all messages and sorts them by location before outputting them.
   *
   * @param kind the kind of message to print
   * @param message the message text
   * @param source the source code position of the diagnostic message
   * @param root the compilation unit
   */
  protected void printOrStoreMessage(
      javax.tools.Diagnostic.Kind kind, String message, Tree source, CompilationUnitTree root) {
    assert this.currentRoot == root;
    StackTraceElement[] trace = Thread.currentThread().getStackTrace();
    if (messageStore == null) {
      printOrStoreMessage(kind, message, source, root, trace);
    } else {
      CheckerMessage checkerMessage = new CheckerMessage(kind, message, source, this, trace);
      messageStore.add(checkerMessage);
    }
  }

  /**
   * Do not call this method. Call {@link #reportError} or {@link #reportWarning} instead.
   *
   * <p>This method exists so that the BaseTypeChecker can override it. For compound checkers, it
   * stores all messages and sorts them by location before outputting them.
   *
   * @param kind the kind of message to print
   * @param message the message text
   * @param source the source code position of the diagnostic message
   * @param root the compilation unit
   * @param trace the stack trace where the checker encountered an error. It is printed when the
   *     dumpOnErrors option is enabled.
   */
  protected void printOrStoreMessage(
      javax.tools.Diagnostic.Kind kind,
      String message,
      Tree source,
      CompilationUnitTree root,
      StackTraceElement[] trace) {
    Trees.instance(processingEnv).printMessage(kind, message, source, root);
    printStackTrace(trace);
  }

  /**
   * Output the given stack trace if the "dumpOnErrors" option is enabled.
   *
   * @param trace stack trace when the checker encountered a warning/error
   */
  private void printStackTrace(StackTraceElement[] trace) {
    if (hasOption("dumpOnErrors")) {
      StringJoiner msg = new StringJoiner(System.lineSeparator());
      for (StackTraceElement elem : trace) {
        msg.add("\tat " + elem);
      }
      message(Diagnostic.Kind.NOTE, msg.toString());
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Diagnostic message formatting
  //

  /**
   * Returns the localized long message corresponding to this key. If not found, tries suffixes of
   * this key, stripping off dot-separated prefixes. If still not found, returns {@code
   * defaultValue}.
   *
   * @param messageKey a message key
   * @param defaultValue a default value to use if {@code messageKey} is not a message key
   * @return the localized long message corresponding to this key or a suffix, or {@code
   *     defaultValue}
   */
  protected String fullMessageOf(String messageKey, String defaultValue) {
    String key = messageKey;

    do {
      String messageForKey = messagesProperties.getProperty(key);
      if (messageForKey != null) {
        return messageForKey;
      }

      int dot = key.indexOf('.');
      if (dot < 0) {
        return defaultValue;
      }
      key = key.substring(dot + 1);
    } while (true);
  }

  /**
   * Process an argument to an error message before it is passed to String.format.
   *
   * <p>This implementation expands the argument if it is exactly a message key.
   *
   * <p>By contrast, {@link #fullMessageOf} processes the message key itself but not the arguments,
   * and tries suffixes.
   *
   * @param arg the argument
   * @return the result after processing
   */
  protected Object processErrorMessageArg(Object arg) {
    // Check to see if the argument itself is a property to be expanded
    if (arg instanceof String) {
      return messagesProperties.getProperty((String) arg, (String) arg);
    } else {
      return arg;
    }
  }

  /** Separates parts of a "detailed message", to permit easier parsing. */
  public static final String DETAILS_SEPARATOR = " $$ ";

  /**
   * Returns all but the message key part of the message format output by {@code -Adetailedmsgtext}.
   *
   * @param source the object from which to obtain source position information; may be an Element, a
   *     Tree, or null
   * @param defaultFormat the message key, in parentheses
   * @param args arguments for interpolation in the string corresponding to the given message key
   * @return the first part of the message format output by {@code -Adetailedmsgtext}
   */
  private String detailedMsgTextPrefix(
      @Nullable Object source, String defaultFormat, Object[] args) {
    StringJoiner sj = new StringJoiner(DETAILS_SEPARATOR);

    // The parts, separated by " $$ " (DETAILS_SEPARATOR), are:

    // (1) error key
    sj.add(defaultFormat);

    // (2) number of additional tokens, and those tokens; this depends on the error message, and
    // an example is the found and expected types
    if (args != null) {
      sj.add(Integer.toString(args.length));
      for (Object arg : args) {
        sj.add(Objects.toString(arg));
      }
    } else {
      // Output 0 for null arguments.
      sj.add(Integer.toString(0));
    }

    // (3) The error position, as starting and ending characters in the source file.
    sj.add(detailedMsgTextPositionString(sourceToTree(source), currentRoot));

    // (4) The human-readable error message will be added by the caller.
    sj.add(""); // Add DETAILS_SEPARATOR at the end.
    return sj.toString();
  }

  /**
   * Returns the most specific warning suppression string for the warning/error being printed.
   *
   * <ul>
   *   <li>If {@code -AshowSuppressWarningsStrings} was supplied on the command line, this is {@code
   *       [checkername1, checkername2]:msg}, where each {@code checkername} is a checker name or
   *       "allcheckers".
   *   <li>If {@code -ArequirePrefixInWarningSuppressions} or {@code -AshowPrefixInWarningMessages}
   *       was supplied on the command line, this is {@code checkername:msg} (where {@code
   *       checkername} may be "allcheckers").
   *   <li>Otherwise, it is just {@code msg}.
   * </ul>
   *
   * @param messageKey the simple, checker-specific error message key
   * @return the most specific SuppressWarnings string for the warning/error being printed
   */
  private String suppressWarningsString(String messageKey) {
    Collection<String> prefixes = this.getSuppressWarningsPrefixes();
    prefixes.remove(SUPPRESS_ALL_PREFIX);
    if (showSuppressWarningsStrings) {
      List<String> list = new ArrayList<>(prefixes);
      // Make sure "allcheckers" is at the end of the list.
      if (useAllcheckersPrefix) {
        list.add(SUPPRESS_ALL_PREFIX);
      }
      return list + ":" + messageKey;
    } else if (requirePrefixInWarningSuppressions || showPrefixInWarningMessages) {
      // If the warning key must be prefixed with a prefix (a checker name), then add that to
      // the SuppressWarnings string that is printed.
      String defaultPrefix = getDefaultSuppressWarningsPrefix();
      if (prefixes.contains(defaultPrefix)) {
        return defaultPrefix + ":" + messageKey;
      } else {
        String firstKey = prefixes.iterator().next();
        return firstKey + ":" + messageKey;
      }
    } else {
      return messageKey;
    }
  }

  /**
   * Convert a Tree, Element, or null, into a Tree or null.
   *
   * @param source the object from which to obtain source position information; may be an Element, a
   *     Tree, or null
   * @return the tree associated with the given source object, or null if none
   */
  private @Nullable Tree sourceToTree(@Nullable Object source) {
    if (source instanceof Element) {
      return trees.getTree((Element) source);
    } else if (source instanceof Tree) {
      return (Tree) source;
    } else if (source == null) {
      return null;
    } else {
      throw new BugInCF("Unexpected source %s [%s]", source, source.getClass());
    }
  }

  /**
   * For the given tree, compute the source positions for that tree. Return a "tuple"-like string
   * (e.g. "( 1, 200 )" ) that contains the start and end position of the tree in the current
   * compilation unit. Used only by the -Adetailedmsgtext output format.
   *
   * @param tree tree to locate within the current compilation unit
   * @param currentRoot the current compilation unit
   * @return a tuple string representing the range of characters that tree occupies in the source
   *     file, or the empty string if {@code tree} is null
   */
  private String detailedMsgTextPositionString(
      @Nullable Tree tree, CompilationUnitTree currentRoot) {
    if (tree == null) {
      return "";
    }

    SourcePositions sourcePositions = trees.getSourcePositions();
    long start = sourcePositions.getStartPosition(currentRoot, tree);
    long end = sourcePositions.getEndPosition(currentRoot, tree);

    return "( " + start + ", " + end + " )";
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Lint options ("-Alint:xxxx" and "-Alint:-xxxx")
  //

  /**
   * Determine which lint options are artive.
   *
   * @param options the command-line options
   * @return the active lint options
   */
  private Set<String> createActiveLints(Map<String, String> options) {
    if (!options.containsKey("lint")) {
      return Collections.emptySet();
    }

    String lintString = options.get("lint");
    if (lintString == null) {
      return Collections.singleton("all");
    }

    List<String> lintStrings = SystemUtil.commaSplitter.splitToList(lintString);
    Set<String> activeLint = ArraySet.newArraySetOrHashSet(lintStrings.size());
    for (String s : lintStrings) {
      if (!this.getSupportedLintOptions().contains(s)
          && !(s.charAt(0) == '-' && this.getSupportedLintOptions().contains(s.substring(1)))
          && !s.equals("all")
          && !s.equals("none")) {
        this.messager.printMessage(
            Diagnostic.Kind.WARNING,
            "Unsupported lint option: " + s + "; All options: " + this.getSupportedLintOptions());
      }

      activeLint.add(s);
      if (s.equals("none")) {
        activeLint.add("-all");
      }
    }

    return Collections.unmodifiableSet(activeLint);
  }

  /**
   * Determines the value of the lint option with the given name. Just as <a
   * href="https://docs.oracle.com/javase/7/docs/technotes/guides/javac/index.html">javac</a> uses
   * "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx, annotation-related lint options
   * are enabled with "-Alint:xxx" and disabled with "-Alint:-xxx".
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
   * href="https://docs.oracle.com/javase/7/docs/technotes/tools/windows/javac.html">javac</a> uses
   * "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx, annotation-related lint options
   * are enabled with "-Alint=xxx" and disabled with "-Alint=-xxx".
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

    // This is only needed if initChecker() has not yet been called.
    if (activeLints == null) {
      activeLints = createActiveLints(getOptions());
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
   * href="https://docs.oracle.com/javase/7/docs/technotes/tools/windows/javac.html">javac</a> uses
   * "-Xlint:xxx" to enable and "-Xlint:-xxx" to disable option xxx, annotation-related lint options
   * are enabled with "-Alint=xxx" and disabled with "-Alint=-xxx". This method can be used by
   * subclasses to enforce having certain lint options enabled/disabled.
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

    Set<String> newlints = ArraySet.newArraySetOrHashSet(activeLints.size() + 1);
    newlints.addAll(activeLints);
    if (val) {
      newlints.add(name);
    } else {
      newlints.add(String.format("-%s", name));
    }
    activeLints = Collections.unmodifiableSet(newlints);
  }

  /**
   * Helper method to find the parent of a lint key. The lint hierarchy level is denoted by a colon
   * ':'. 'all' is the root for all hierarchy.
   *
   * <pre>
   * Example
   *    cast:unsafe &rarr; cast
   *    cast        &rarr; all
   *    all         &rarr; {@code null}
   * </pre>
   *
   * @param name the lint key whose parest to find
   * @return the parent of the lint key
   */
  private @Nullable String parentOfOption(String name) {
    if (name.equals("all")) {
      return null;
    }
    int colonIndex = name.lastIndexOf(':');
    if (colonIndex != -1) {
      return name.substring(0, colonIndex);
    } else {
      return "all";
    }
  }

  /**
   * Returns the set of subchecker classes on which this checker depends. ("Depends" means the
   * checkers that are subcheckers of the current checker rather than a subchecker of some other
   * checker.) Returns an empty set if this checker does not depend on any others.
   *
   * <p>If this checker should run multiple independent checkers and not contain a type system, then
   * subclass {@link AggregateChecker}.
   *
   * <p>Subclasses should override this method to specify subcheckers. If they do so, they should
   * call the super implementation of this method and add dependencies to the returned set so that
   * checkers required for reflection resolution are included if reflection resolution is requested.
   *
   * <p>If a checker should be added or not based on a command line option, use {@link
   * #getOptionsNoSubcheckers()} or {@link #hasOptionNoSubcheckers(String)} to avoid recursively
   * calling this method.
   *
   * <p>Each subchecker of this checker may also depend on other checkers. If this checker and one
   * of its subcheckers both depend on a third checker, that checker will only be instantiated once.
   *
   * <p>Though each checker is run on a whole compilation unit before the next checker is run, error
   * and warning messages are collected and sorted based on the location in the source file before
   * being printed. (See {@link #printOrStoreMessage(Diagnostic.Kind, String, Tree,
   * CompilationUnitTree)}.)
   *
   * <p>WARNING: Circular dependencies are not supported. (In other words, if checker A depends on
   * checker B, checker B cannot depend on checker A.) The Checker Framework does not check for
   * circularity. Make sure no circular dependencies are created when overriding this method.
   *
   * <p>This method is protected so it can be overridden, but it should only be called internally by
   * {@link SourceChecker}.
   *
   * @return the subchecker classes on which this checker depends; will be modified by callees
   */
  // This is never looked up in, but it is iterated over (and added to, which does a lookup).
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    // This must return a modifiable set because clients modify it.
    // Most checkers have 1 or fewer subcheckers.
    LinkedHashSet<Class<? extends SourceChecker>> result =
        new LinkedHashSet<>(CollectionsPlume.mapCapacity(2));
    if (shouldResolveReflection()) {
      result.add(MethodValChecker.class);
    }
    return result;
  }

  /**
   * Returns true if reflection should be resolved.
   *
   * @return true if reflection should be resolved
   */
  public boolean shouldResolveReflection() {
    return hasOptionNoSubcheckers("resolveReflection");
  }

  /**
   * Returns the name of a class related to a given one, by replacing "Checker" or "Subchecker" by
   * {@code replacement}.
   *
   * @param checkerClass the checker class
   * @param replacement the string that replaces "Checker" or "Subchecker"
   * @return the name of the related class
   */
  @SuppressWarnings("signature") // string manipulation of @ClassGetName string
  public static @ClassGetName String getRelatedClassName(
      Class<?> checkerClass, String replacement) {
    return checkerClass
        .getName()
        .replace("Checker", replacement)
        .replace("Subchecker", replacement);
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

  /**
   * Compute the set of supported lint options for this checker and its subcheckers.
   *
   * @return the set of supported lint options for this checker and its subcheckers
   */
  protected Set<String> createSupportedLintOptions() {
    Set<String> lintSet = getLintOptionsFromAnnotation();

    for (SourceChecker checker : getSubcheckers()) {
      lintSet.addAll(checker.createSupportedLintOptions());
    }
    return lintSet;
  }

  /**
   * Get the lint options from the {@link SupportedLintOptions} annotation on this class.
   *
   * @return the lint options from the {@link SupportedLintOptions} annotation
   */
  private Set<String> getLintOptionsFromAnnotation() {
    SupportedLintOptions sl = this.getClass().getAnnotation(SupportedLintOptions.class);

    if (sl == null) {
      return new HashSet<>();
    }

    @Nullable String @Nullable [] slValue = sl.value();
    assert slValue != null;

    @Nullable String[] lintArray = slValue;
    Set<String> lintSet = new HashSet<>(lintArray.length);
    Collections.addAll(lintSet, lintArray);
    return lintSet;
  }

  /**
   * Set the supported lint options.
   *
   * @param newLints the new supported lint options, which replace any existing ones
   */
  private void setSupportedLintOptions(Set<String> newLints) {
    supportedLints = newLints;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Regular (non-lint) options ("-Axxxx")
  //

  /**
   * Determine which options are active.
   *
   * @param options all provided options
   * @return a value for {@link #activeOptions}
   */
  @SuppressWarnings("LabelledBreakTarget")
  private Map<String, String> createActiveOptions(Map<String, String> options) {
    if (options.isEmpty()) {
      return new HashMap<>();
    }

    Map<String, String> activeOpts = new HashMap<>(CollectionsPlume.mapCapacity(options));

    for (Map.Entry<String, String> opt : options.entrySet()) {
      String key = opt.getKey();
      String value = opt.getValue();

      String[] split = key.split(OPTION_SEPARATOR);

      splitlengthswitch:
      switch (split.length) {
        case 1:
          // No separator, option always active.
          activeOpts.put(key, value);
          break;
        case 2:
          Class<?> clazz = this.getClass();

          do {
            if (clazz.getCanonicalName().equals(split[0])
                || clazz.getSimpleName().equals(split[0])) {
              // Valid class-option pair.
              activeOpts.put(split[1], value);
              break splitlengthswitch;
            }

            clazz = clazz.getSuperclass();
          } while (clazz != null
              && !clazz.getName().equals(AbstractTypeProcessor.class.getCanonicalName()));
          // Didn't find a matching class. Option might be for another processor. Add
          // option anyways. javac will warn if no processor supports the option.
          activeOpts.put(key, value);
          break;
        default:
          // Too many separators. Option might be for another processor. Add option
          // anyways. javac will warn if no processor supports the option.
          activeOpts.put(key, value);
      }
    }
    return activeOpts;
  }

  @Override
  public Map<String, String> getOptions() {
    if (activeOptions == null) {
      activeOptions = createActiveOptions(processingEnv.getOptions());

      for (SourceChecker subchecker : getSubcheckers()) {
        activeOptions.putAll(subchecker.createActiveOptions(processingEnv.getOptions()));
      }
    }
    return activeOptions;
  }

  @Override
  public final boolean hasOption(String name) {
    return getOptions().containsKey(name);
  }

  /**
   * {@inheritDoc}
   *
   * @see SourceChecker#getLintOption(String,boolean)
   */
  @Override
  public final String getOption(String name) {
    return getOption(name, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see SourceChecker#getLintOption(String,boolean)
   */
  @Override
  public final String getOption(String name, String defaultValue) {
    Set<String> supportedOptions = this.getSupportedOptions();
    if (!supportedOptions.contains(name)) {
      throw new UserError(
          "Illegal option: "
              + name
              + "; supported options = "
              + String.join(",", supportedOptions));
    }

    return getOptions().getOrDefault(name, defaultValue);
  }

  /**
   * {@inheritDoc}
   *
   * @see SourceChecker#getLintOption(String,boolean)
   */
  @Override
  public final boolean getBooleanOption(String name) {
    return getBooleanOption(name, false);
  }

  /**
   * {@inheritDoc}
   *
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
        String.format("Value of %s option should be a boolean, but is \"%s\".", name, value));
  }

  /**
   * {@inheritDoc}
   *
   * @see SourceChecker#getLintOption(String,boolean)
   */
  @Override
  public final List<String> getStringsOption(
      String name, char separator, List<String> defaultValue) {
    String value = getOption(name);
    if (value == null) {
      return defaultValue;
    }
    return Splitter.on(separator).omitEmptyStrings().splitToList(value);
  }

  /**
   * {@inheritDoc}
   *
   * @see SourceChecker#getLintOption(String,boolean)
   */
  @Override
  public final List<String> getStringsOption(
      String name, String separator, List<String> defaultValue) {
    String value = getOption(name);
    if (value == null) {
      return defaultValue;
    }
    return Splitter.on(separator).omitEmptyStrings().splitToList(value);
  }

  /**
   * Prints error messages for this checker and all subcheckers such that the errors are ordered by
   * line and column number and then by checker. (See {@link
   * CheckerMessage#compareTo(CheckerMessage)} for more precise order.)
   *
   * @param unit current compilation unit
   */
  protected void printStoredMessages(CompilationUnitTree unit) {
    if (messageStore == null || parentChecker != null) {
      return;
    }
    for (CheckerMessage msg : messageStore) {
      printOrStoreMessage(msg.kind, msg.message, msg.source, unit, msg.trace);
    }
  }

  @Override
  public Set<String> getSupportedOptions() {
    if (supportedOptions == null) {

      // Support all options provided with the standard {@link
      // javax.annotation.processing.SupportedOptions} annotation.
      Set<String> options = new HashSet<>(super.getSupportedOptions());

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

      for (SourceChecker checker : getSubcheckers()) {
        options.addAll(checker.getSupportedOptions());
      }

      supportedOptions = Collections.unmodifiableSet(options);
    }

    return supportedOptions;
  }

  /**
   * Generate the possible command-line option names by prefixing each class name from {@code
   * classPrefixes} to {@code options}, separated by {@link #OPTION_SEPARATOR}.
   *
   * @param clazzPrefixes the classes to prefix
   * @param options the option names
   * @return the possible combinations that should be supported
   */
  protected Collection<String> expandCFOptions(
      List<? extends Class<?>> clazzPrefixes, String[] options) {
    Set<String> res =
        new HashSet<>(CollectionsPlume.mapCapacity(options.length * (1 + clazzPrefixes.size())));
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
   * of returning "*" is as if the checker were annotated by {@code @SupportedAnnotationTypes("*")}:
   * javac runs the checker on every class mentioned on the javac command line. This method also
   * checks that subclasses do not contain a {@link SupportedAnnotationTypes} annotation.
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

  // ///////////////////////////////////////////////////////////////////////////
  // Warning suppression and unneeded warnings
  //

  /**
   * Returns the argument to {@code -AsuppressWarnings}, split on commas, or null if no such
   * argument. Only ever called once; the value is cached in field {@link
   * #suppressWarningsStringsFromOption}.
   *
   * @return the argument to {@code -AsuppressWarnings}, split on commas, or null if no such
   *     argument
   */
  private String @Nullable [] getSuppressWarningsStringsFromOption() {
    if (!computedSuppressWarningsStringsFromOption) {
      computedSuppressWarningsStringsFromOption = true;
      Map<String, String> options = getAllOptions();
      if (options.containsKey("suppressWarnings")) {
        String swStrings = options.get("suppressWarnings");
        if (swStrings != null) {
          this.suppressWarningsStringsFromOption = swStrings.split(",");
        }
      }
    }

    return this.suppressWarningsStringsFromOption;
  }

  /**
   * Returns the options passed to this checker and its immediate parent checker.
   *
   * @return the options passed to this checker and its immediate parent checker
   */
  private Map<String, String> getAllOptions() {
    if (parentChecker == null) {
      return getOptions();
    }
    Map<String, String> allOptions = new HashMap<>(this.getOptions());
    parentChecker
        .getOptions()
        .forEach(
            (parentOptKey, parentOptVal) -> {
              if (parentOptVal != null) {
                allOptions.merge(parentOptKey, parentOptVal, this::combineOptionValues);
              }
            });
    return Collections.unmodifiableMap(allOptions);
  }

  /**
   * Combines two comma-delimited strings into a single comma-delimited string that does not contain
   * duplicates.
   *
   * <p>Checker option values are comma-delimited. This method combines two option values while
   * discarding possible duplicates.
   *
   * @param optionValueA the first comma-delimited string
   * @param optionValueB the second comma-delimited string
   * @return a comma-delimited string containing values from the first and second string, with no
   *     duplicates
   */
  private String combineOptionValues(String optionValueA, String optionValueB) {
    Set<String> optionValueASet =
        Arrays.stream(optionValueA.split(",")).collect(Collectors.toSet());
    Set<String> optionValueBSet =
        Arrays.stream(optionValueB.split(",")).collect(Collectors.toSet());
    optionValueASet.addAll(optionValueBSet);
    return String.join(",", optionValueASet);
  }

  /**
   * Issues a warning about any {@code @SuppressWarnings} that didn't suppress a warning, but starts
   * with this checker name or "allcheckers".
   */
  protected void warnUnneededSuppressions() {
    if (parentChecker != null) {
      return;
    }

    if (!warnUnneededSuppressions) {
      return;
    }
    Set<Element> allElementsWithSuppressedWarnings =
        new HashSet<>(this.elementsWithSuppressedWarnings);
    this.elementsWithSuppressedWarnings.clear();

    Set<String> prefixes = new HashSet<>(getSuppressWarningsPrefixes());
    Set<String> errorKeys = new HashSet<>(messagesProperties.stringPropertyNames());
    for (SourceChecker subChecker : subcheckers) {
      allElementsWithSuppressedWarnings.addAll(subChecker.elementsWithSuppressedWarnings);
      subChecker.elementsWithSuppressedWarnings.clear();
      prefixes.addAll(subChecker.getSuppressWarningsPrefixes());
      errorKeys.addAll(subChecker.messagesProperties.stringPropertyNames());
      subChecker.getVisitor().treesWithSuppressWarnings.clear();
    }
    warnUnneededSuppressions(allElementsWithSuppressedWarnings, prefixes, errorKeys);

    getVisitor().treesWithSuppressWarnings.clear();
  }

  /**
   * Issues a warning about any {@code @SuppressWarnings} string that didn't suppress a warning, but
   * starts with one of the given prefixes (checker names). Does nothing if the string doesn't start
   * with a checker name.
   *
   * @param elementsSuppress elements with a {@code @SuppressWarnings} that actually suppressed a
   *     warning
   * @param prefixes the SuppressWarnings prefixes that suppress all warnings from this checker
   * @param allErrorKeys all error keys that can be issued by this checker
   */
  protected void warnUnneededSuppressions(
      Set<Element> elementsSuppress, Set<String> prefixes, Set<String> allErrorKeys) {
    for (Tree tree : getVisitor().treesWithSuppressWarnings) {
      Element elt = TreeUtils.elementFromTree(tree);
      // TODO: This test is too coarse.  The fact that this @SuppressWarnings suppressed
      // *some* warning doesn't mean that every value in it did so.
      if (elementsSuppress.contains(elt)) {
        continue;
      }
      // tree has a @SuppressWarnings annotation that didn't suppress any warnings.
      SuppressWarnings suppressAnno = elt.getAnnotation(SuppressWarnings.class);
      String[] suppressWarningsStrings = suppressAnno.value();
      for (String suppressWarningsString : suppressWarningsStrings) {
        if (warnUnneededSuppressionsExceptions != null
            && warnUnneededSuppressionsExceptions.matcher(suppressWarningsString).find(0)) {
          continue;
        }
        for (String prefix : prefixes) {
          if (suppressWarningsString.equals(prefix)
              || (suppressWarningsString.startsWith(prefix + ":")
                  && !suppressWarningsString.equals(prefix + ":unneeded.suppression"))) {
            reportUnneededSuppression(tree, suppressWarningsString);
            break; // Don't report the same warning string more than once.
          }
        }
      }
    }
  }

  /**
   * Issues a warning that the string in a {@code @SuppressWarnings} on {@code tree} isn't needed.
   *
   * @param tree has unneeded {@code @SuppressWarnings}
   * @param suppressWarningsString the SuppressWarnings string that isn't needed
   */
  private void reportUnneededSuppression(Tree tree, String suppressWarningsString) {
    Tree swTree = findSuppressWarningsAnnotationTree(tree);
    report(
        swTree,
        Diagnostic.Kind.MANDATORY_WARNING,
        SourceChecker.UNNEEDED_SUPPRESSION_KEY,
        "\"" + suppressWarningsString + "\"",
        getClass().getSimpleName());
  }

  /** The name of the @SuppressWarnings annotation. */
  private static final @CanonicalName String suppressWarningsClassName =
      SuppressWarnings.class.getCanonicalName();

  /**
   * Finds the tree that is a {@code @SuppressWarnings} annotation.
   *
   * @param tree a class, method, or variable tree annotated with {@code @SuppressWarnings}
   * @return tree for {@code @SuppressWarnings} or {@code default} if one isn't found
   */
  private Tree findSuppressWarningsAnnotationTree(Tree tree) {
    List<? extends AnnotationTree> annotations;
    if (TreeUtils.isClassTree(tree)) {
      annotations = ((ClassTree) tree).getModifiers().getAnnotations();
    } else if (tree.getKind() == Tree.Kind.METHOD) {
      annotations = ((MethodTree) tree).getModifiers().getAnnotations();
    } else {
      annotations = ((VariableTree) tree).getModifiers().getAnnotations();
    }

    for (AnnotationTree annotationTree : annotations) {
      if (AnnotationUtils.areSameByName(
          TreeUtils.annotationFromAnnotationTree(annotationTree), suppressWarningsClassName)) {
        return annotationTree;
      }
    }
    throw new BugInCF("Did not find @SuppressWarnings: " + tree);
  }

  /**
   * Returns true if all the warnings pertaining to the given source should be suppressed. This
   * implementation just delegates to an overloaded, more specific version of {@code
   * shouldSuppressWarnings()}.
   *
   * @param src the position object to test; may be an Element, a Tree, or null
   * @param errKey the error key the checker is emitting
   * @return true if all warnings pertaining to the given source should be suppressed
   * @see #shouldSuppressWarnings(Element, String)
   * @see #shouldSuppressWarnings(Tree, String)
   */
  private boolean shouldSuppressWarnings(@Nullable Object src, String errKey) {
    if (src instanceof Element) {
      return shouldSuppressWarnings((Element) src, errKey);
    } else if (src instanceof Tree) {
      return shouldSuppressWarnings((Tree) src, errKey);
    } else if (src == null) {
      return false;
    } else {
      throw new BugInCF("Unexpected source [" + src.getClass() + "] " + src);
    }
  }

  /**
   * Returns true if all the warnings pertaining to a given tree should be suppressed. Returns true
   * if the tree is within the scope of a @SuppressWarnings annotation, one of whose values
   * suppresses the checker's warning. Also, returns true if the {@code errKey} matches a string in
   * {@code -AsuppressWarnings}.
   *
   * @param tree the tree that might be a source of a warning
   * @param errKey the error key the checker is emitting
   * @return true if no warning should be emitted for the given tree because it is contained by a
   *     declaration with an appropriately-valued {@literal @}SuppressWarnings annotation; false
   *     otherwise
   */
  public boolean shouldSuppressWarnings(Tree tree, String errKey) {

    Collection<String> prefixes = getSuppressWarningsPrefixes();
    if (prefixes.isEmpty() || (prefixes.contains(SUPPRESS_ALL_PREFIX) && prefixes.size() == 1)) {
      throw new BugInCF(
          "Checker must provide a SuppressWarnings prefix."
              + " SourceChecker#getSuppressWarningsPrefixes was not overridden correctly.");
    }

    if (shouldSuppress(getSuppressWarningsStringsFromOption(), errKey)) {
      // If the error key matches a warning string in the -AsuppressWarnings, then suppress
      // the warning.
      return true;
    }

    assert this.currentRoot != null : "this.currentRoot == null";
    // trees.getPath might be slow, but this is only used in error reporting
    TreePath path = trees.getPath(this.currentRoot, tree);

    return shouldSuppressWarnings(path, errKey);
  }

  /**
   * Returns true if all the warnings pertaining to a given tree path should be suppressed. Returns
   * true if the path is within the scope of a @SuppressWarnings annotation, one of whose values
   * suppresses the checker's warning.
   *
   * @param path the TreePath that might be a source of, or related to, a warning
   * @param errKey the error key the checker is emitting
   * @return true if no warning should be emitted for the given path because it is contained by a
   *     declaration with an appropriately-valued {@code @SuppressWarnings} annotation; false
   *     otherwise
   */
  public boolean shouldSuppressWarnings(@Nullable TreePath path, String errKey) {
    if (path == null) {
      return false;
    }

    // iterate through the path; continue until path contains no declarations
    for (TreePath declPath = TreePathUtil.enclosingDeclarationPath(path);
        declPath != null;
        declPath = TreePathUtil.enclosingDeclarationPath(declPath.getParentPath())) {

      Tree decl = declPath.getLeaf();

      if (decl.getKind() == Tree.Kind.VARIABLE) {
        Element elt = TreeUtils.elementFromDeclaration((VariableTree) decl);
        if (shouldSuppressWarnings(elt, errKey)) {
          return true;
        }
      } else if (decl.getKind() == Tree.Kind.METHOD) {
        Element elt = TreeUtils.elementFromDeclaration((MethodTree) decl);
        if (shouldSuppressWarnings(elt, errKey)) {
          return true;
        }

        if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
          // Return false immediately. Do NOT check for AnnotatedFor in the enclosing
          // elements, because they may not have an @AnnotatedFor.
          return false;
        }
      } else if (TreeUtils.classTreeKinds().contains(decl.getKind())) {
        // A class tree
        Element elt = TreeUtils.elementFromDeclaration((ClassTree) decl);
        if (shouldSuppressWarnings(elt, errKey)) {
          return true;
        }

        if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
          // Return false immediately. Do NOT check for AnnotatedFor in the enclosing
          // elements, because they may not have an @AnnotatedFor.
          return false;
        }
      } else {
        throw new BugInCF("Unexpected declaration kind: " + decl.getKind() + " " + decl);
      }
    }

    if (useConservativeDefault("source")) {
      // If we got this far without hitting an @AnnotatedFor and returning
      // false, we DO suppress the warning.
      return true;
    }

    return false;
  }

  /**
   * Should conservative defaults be used for the kind of unchecked code indicated by the parameter?
   *
   * @param kindOfCode source or bytecode
   * @return whether conservative defaults should be used
   */
  public boolean useConservativeDefault(String kindOfCode) {
    boolean useUncheckedDefaultsForSource = false;
    boolean useUncheckedDefaultsForByteCode = false;
    for (String arg : this.getStringsOption("useConservativeDefaultsForUncheckedCode", ',')) {
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
          "SourceChecker: unexpected argument to useConservativeDefault: " + kindOfCode);
    }
  }

  /**
   * Elements with a {@code @SuppressWarnings} that actually suppressed a warning for this checker.
   */
  protected final Set<Element> elementsWithSuppressedWarnings = new HashSet<>();

  /**
   * Returns true if all the warnings pertaining to a given element should be suppressed. Returns
   * true if the element is within the scope of a @SuppressWarnings annotation, one of whose values
   * suppresses all the checker's warnings.
   *
   * @param elt the Element that might be a source of, or related to, a warning
   * @param errKey the error key the checker is emitting
   * @return true if no warning should be emitted for the given Element because it is contained by a
   *     declaration with an appropriately-valued {@code @SuppressWarnings} annotation; false
   *     otherwise
   */
  public boolean shouldSuppressWarnings(@Nullable Element elt, String errKey) {

    if (shouldSuppress(getSuppressWarningsStringsFromOption(), errKey)) {
      return true;
    }

    for (Element currElt = elt; currElt != null; currElt = currElt.getEnclosingElement()) {
      SuppressWarnings suppressWarningsAnno = currElt.getAnnotation(SuppressWarnings.class);
      if (suppressWarningsAnno != null) {
        String[] suppressWarningsStrings = suppressWarningsAnno.value();
        if (shouldSuppress(suppressWarningsStrings, errKey)) {
          if (warnUnneededSuppressions) {
            elementsWithSuppressedWarnings.add(currElt);
          }
          return true;
        }
      }
      if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
        // Return false immediately. Do NOT check for AnnotatedFor in the
        // enclosing elements, because they may not have an @AnnotatedFor.
        return false;
      }
    }
    return false;
  }

  /**
   * Returns true if an error (whose message key is {@code messageKey}) should be suppressed. It is
   * suppressed if any of the given SuppressWarnings strings suppresses it.
   *
   * <p>A SuppressWarnings string may be of the following pattern:
   *
   * <ol>
   *   <li>{@code "prefix"}, where prefix is a SuppressWarnings prefix, as specified by {@link
   *       #getSuppressWarningsPrefixes()}. For example, {@code "nullness"} and {@code
   *       "initialization"} for the Nullness Checker, {@code "regex"} for the Regex Checker.
   *   <li>{@code "partial-message-key"}, where partial-message-key is a prefix or suffix of the
   *       message key that it may suppress. So "generic.argument" would suppress any errors whose
   *       message key contains "generic.argument".
   *   <li>{@code "prefix:partial-message-key}, where the prefix and partial-message-key is as
   *       above. So "nullness:generic.argument", would suppress any errors in the Nullness Checker
   *       with a message key that contains "generic.argument".
   * </ol>
   *
   * {@code "allcheckers"} is a prefix that suppresses a warning from any checker. {@code "all"} is
   * a partial-message-key that suppresses a warning with any message key.
   *
   * <p>If the {@code -ArequirePrefixInWarningSuppressions} command-line argument was supplied, then
   * {@code "partial-message-key"} has no effect; {@code "prefix"} and {@code
   * "prefix:partial-message-key"} are the only SuppressWarnings strings that have an effect.
   *
   * @param suppressWarningsInEffect the SuppressWarnings strings that are in effect. May be null,
   *     in which case this method returns false.
   * @param messageKey the message key of the error the checker is emitting; a lowercase string,
   *     without any "checkername:" prefix
   * @return true if an element of {@code suppressWarningsInEffect} suppresses the error
   */
  private boolean shouldSuppress(String @Nullable [] suppressWarningsInEffect, String messageKey) {
    Set<String> prefixes = this.getSuppressWarningsPrefixes();
    return shouldSuppress(prefixes, suppressWarningsInEffect, messageKey);
  }

  /**
   * Helper method for {@link #shouldSuppress(String[], String)}.
   *
   * @param prefixes the SuppressWarnings prefixes used by this checker
   * @param suppressWarningsInEffect the SuppressWarnings strings that are in effect. May be null,
   *     in which case this method returns false.
   * @param messageKey the message key of the error the checker is emitting; a lowercase string,
   *     without any "checkername:" prefix
   * @return true if one of the {@code suppressWarningsInEffect} suppresses the error
   */
  private boolean shouldSuppress(
      Set<String> prefixes, String @Nullable [] suppressWarningsInEffect, String messageKey) {
    if (suppressWarningsInEffect == null) {
      return false;
    }

    for (String currentSuppressWarningsInEffect : suppressWarningsInEffect) {
      int colonPos = currentSuppressWarningsInEffect.indexOf(":");
      String messageKeyInSuppressWarningsString;
      if (colonPos == -1) {
        // The SuppressWarnings string has no colon, so it is not of the form
        // prefix:partial-message-key.
        if (prefixes.contains(currentSuppressWarningsInEffect)) {
          // The value in the @SuppressWarnings is exactly a prefix.
          // Suppress the warning unless its message key is "unneeded.suppression".
          boolean result = !currentSuppressWarningsInEffect.equals(UNNEEDED_SUPPRESSION_KEY);
          return result;
        } else if (requirePrefixInWarningSuppressions) {
          // A prefix is required, but this SuppressWarnings string does not have a
          // prefix; check the next SuppressWarnings string.
          continue;
        } else if (currentSuppressWarningsInEffect.equals(SUPPRESS_ALL_MESSAGE_KEY)) {
          // Prefixes aren't required and the SuppressWarnings string is "all".
          // Suppress the warning unless its message key is "unneeded.suppression".
          boolean result = !currentSuppressWarningsInEffect.equals(UNNEEDED_SUPPRESSION_KEY);
          return result;
        }
        // The currentSuppressWarningsInEffect is not a prefix or a prefix:message-key, so
        // it might be a message key.
        messageKeyInSuppressWarningsString = currentSuppressWarningsInEffect;
      } else {
        // The SuppressWarnings string has a colon; that is, it has a prefix.
        String currentSuppressWarningsPrefix =
            currentSuppressWarningsInEffect.substring(0, colonPos);
        if (!prefixes.contains(currentSuppressWarningsPrefix)) {
          // The prefix of this SuppressWarnings string is a not a prefix supported by
          // this checker. Proceed to the next SuppressWarnings string.
          continue;
        }
        messageKeyInSuppressWarningsString =
            currentSuppressWarningsInEffect.substring(colonPos + 1);
      }
      // Check if the message key in the warning suppression is part of the message key that
      // the checker is emiting.
      if (messageKeyMatches(messageKey, messageKeyInSuppressWarningsString)) {
        return true;
      }
    }

    // None of the SuppressWarnings strings suppresses this error.
    return false;
  }

  /**
   * Does the given messageKey match a messageKey that appears in a SuppressWarnings? Subclasses
   * should override this method if they need additional logic to compare message keys.
   *
   * @param messageKey the message key of the error that is being emitted, without any "checker:"
   *     prefix
   * @param messageKeyInSuppressWarningsString the message key in a {@code @SuppressWarnings}
   *     annotation
   * @return true if the arguments match
   */
  protected boolean messageKeyMatches(
      String messageKey, String messageKeyInSuppressWarningsString) {
    return messageKey.equals(messageKeyInSuppressWarningsString)
        || messageKey.startsWith(messageKeyInSuppressWarningsString + ".")
        || messageKey.endsWith("." + messageKeyInSuppressWarningsString)
        || messageKey.contains("." + messageKeyInSuppressWarningsString + ".");
  }

  /**
   * Return true if the element has an {@code @AnnotatedFor} annotation, for this checker or an
   * upstream checker that called this one.
   *
   * @param elt the source code element to check, or null
   * @return true if the element is annotated for this checker or an upstream checker
   */
  private boolean isAnnotatedForThisCheckerOrUpstreamChecker(@Nullable Element elt) {

    if (elt == null || !useConservativeDefault("source")) {
      return false;
    }

    AnnotatedFor anno = elt.getAnnotation(AnnotatedFor.class);

    String[] userAnnotatedFors = (anno == null ? null : anno.value());

    if (userAnnotatedFors != null) {
      List<@FullyQualifiedName String> upstreamCheckerNames = getUpstreamCheckerNames();

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
   * Returns a modifiable set of lower-case strings that are prefixes for SuppressWarnings strings.
   *
   * <p>The collection must not be empty and must not contain only {@link #SUPPRESS_ALL_PREFIX}.
   *
   * @return non-empty modifiable set of lower-case prefixes for SuppressWarnings strings
   */
  public NavigableSet<String> getSuppressWarningsPrefixes() {
    return getStandardSuppressWarningsPrefixes();
  }

  /**
   * Returns a sorted set of SuppressWarnings prefixes read from the {@link SuppressWarningsPrefix}
   * meta-annotation on the checker class. Or if no {@link SuppressWarningsPrefix} is used, the
   * checker name is used. {@link #SUPPRESS_ALL_PREFIX} is also added, at the end, unless {@link
   * #useAllcheckersPrefix} is false.
   *
   * @return a sorted set of SuppressWarnings prefixes
   */
  protected final NavigableSet<String> getStandardSuppressWarningsPrefixes() {
    NavigableSet<String> prefixes = new TreeSet<>();
    if (useAllcheckersPrefix) {
      prefixes.add(SUPPRESS_ALL_PREFIX);
    }
    SuppressWarningsPrefix prefixMetaAnno =
        this.getClass().getAnnotation(SuppressWarningsPrefix.class);
    if (prefixMetaAnno != null) {
      for (String prefix : prefixMetaAnno.value()) {
        prefixes.add(prefix);
      }
      return prefixes;
    }

    // No @SuppressWarningsPrefixes annotation, by default infer key from class name.
    String defaultPrefix = getDefaultSuppressWarningsPrefix();
    prefixes.add(defaultPrefix);
    return prefixes;
  }

  /**
   * Returns the default SuppressWarnings prefix for this checker based on the checker name.
   *
   * @return the default SuppressWarnings prefix for this checker based on the checker name
   */
  private String getDefaultSuppressWarningsPrefix() {
    String className = this.getClass().getSimpleName();
    int indexOfChecker = className.lastIndexOf("Checker");
    if (indexOfChecker == -1) {
      indexOfChecker = className.lastIndexOf("Subchecker");
    }
    String result = (indexOfChecker == -1) ? className : className.substring(0, indexOfChecker);
    return result.toLowerCase(Locale.getDefault());
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Skipping uses and defs
  //

  /**
   * Tests whether the class owner of the passed element is an unannotated class and matches the
   * pattern specified in the {@code checker.skipUses} property.
   *
   * @param element an element
   * @return true iff the enclosing class of element should be skipped
   */
  public final boolean shouldSkipUses(@Nullable Element element) {
    if (element == null) {
      return false;
    }
    TypeElement typeElement = ElementUtils.enclosingTypeElement(element);
    if (typeElement == null) {
      throw new BugInCF("enclosingTypeElement(%s [%s]) => null%n", element, element.getClass());
    }
    @SuppressWarnings("signature:assignment") // TypeElement.toString(): @FullyQualifiedName
    @FullyQualifiedName String name = typeElement.toString();
    return shouldSkipUses(name);
  }

  /**
   * Tests whether the class owner of the passed type matches the pattern specified in the {@code
   * checker.skipUses} property. In contrast to {@link #shouldSkipUses(Element)} this version can
   * also be used from primitive types, which don't have an element.
   *
   * <p>Checkers that require their annotations not to be checked on certain JDK classes may
   * override this method to skip them. They shall call {@code super.shouldSkipUses(typeName)} to
   * also skip the classes matching the pattern.
   *
   * @param typeName the fully-qualified name of a type
   * @return true iff the enclosing class of element should be skipped
   */
  public boolean shouldSkipUses(@FullyQualifiedName String typeName) {
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
    return skipUsesPattern.matcher(typeName).find() || !onlyUsesPattern.matcher(typeName).find();
  }

  /**
   * Tests whether the class definition should not be checked because it matches the {@code
   * checker.skipDefs} property.
   *
   * @param tree class to potentially skip
   * @return true if checker should not type-check {@code tree}
   */
  public final boolean shouldSkipDefs(ClassTree tree) {
    String qualifiedName = TreeUtils.typeOf(tree).toString();
    // System.out.printf("shouldSkipDefs(%s) %s%nskipDefs %s%nonlyDefs %s%nresult %s%n%n",
    //                   tree,
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
   * @param tree method to potentially skip
   * @return true if checker should not type-check {@code tree}
   */
  public boolean shouldSkipDefs(MethodTree tree) {
    return false; // subclasses may override this implementation
  }

  /**
   * Tests whether the method definition should not be checked because it matches the {@code
   * checker.skipDefs} property.
   *
   * <p>TODO: currently only uses the class definition. Refine pattern. Same for skipUses.
   *
   * @param cls class to potentially skip
   * @param meth method to potentially skip
   * @return true if checker should not type-check {@code meth}
   */
  public final boolean shouldSkipDefs(ClassTree cls, MethodTree meth) {
    return shouldSkipDefs(cls) || shouldSkipDefs(meth);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Skipping files
  //

  /**
   * Tests whether the enclosing file path of the passed tree matches the pattern specified in the
   * {@code checker.skipFiles} property.
   *
   * @param tree a tree
   * @return true iff the enclosing directory of the tree should be skipped
   */
  public final boolean shouldSkipFiles(ClassTree tree) {
    if (tree == null) {
      return false;
    }
    TypeElement typeElement = TreeUtils.elementFromDeclaration(tree);
    if (typeElement == null) {
      throw new BugInCF("elementFromDeclaration(%s [%s]) => null%n", tree, tree.getClass());
    }
    String sourceFilePathForElement = ElementUtils.getSourceFilePath(typeElement);
    return shouldSkipFiles(sourceFilePathForElement);
  }

  /**
   * Tests whether the file at the file path should be not be checked because it matches the {@code
   * checker.skipFiles} property.
   *
   * @param path the path to the file to potentially skip
   * @return true iff the checker should not check the file at {@code path}
   */
  private boolean shouldSkipFiles(String path) {
    if (skipFilesPattern == null) {
      skipFilesPattern = getSkipFilesPattern(getOptions());
    }
    if (onlyFilesPattern == null) {
      onlyFilesPattern = getOnlyFilesPattern(getOptions());
    }

    return skipFilesPattern.matcher(path).find() || !onlyFilesPattern.matcher(path).find();
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Errors other than type-checking errors
  //

  /**
   * Log (that is, print) a user error.
   *
   * @param ce the user error to output
   */
  private void logUserError(UserError ce) {
    String msg = ce.getMessage();
    printMessage(msg);
  }

  /**
   * Log (that is, print) a type system error.
   *
   * @param ce the type system error to output
   */
  private void logTypeSystemError(TypeSystemError ce) {
    logBug(
        ce, "A type system implementation is buggy.  Please report the crash to the maintainer.");
  }

  /**
   * Log (that is, print) an internal error in the framework or a checker.
   *
   * @param ce the internal error to output
   */
  private void logBugInCF(BugInCF ce) {
    String checkerVersion;
    try {
      checkerVersion = getCheckerVersion();
    } catch (Exception ex) {
      // getCheckerVersion() throws an exception when invoked during Junit tests.
      checkerVersion = null;
    }
    String msg = "The Checker Framework crashed.  Please report the crash.  ";
    if (checkerVersion != null) {
      msg += String.format("Version: Checker Framework %s. ", checkerVersion);
    }
    logBug(ce, msg);
  }

  /**
   * Log (that is, print) an internal error in the framework or a checker.
   *
   * @param ce the internal error to output
   * @param culprit a message to print about the cause
   */
  private void logBug(Throwable ce, String culprit) {
    String lineSeparator =
        getOptions().getOrDefault("exceptionLineSeparator", System.lineSeparator());
    StringJoiner msg = new StringJoiner(lineSeparator);
    if (ce.getCause() != null && ce.getCause() instanceof OutOfMemoryError) {
      msg.add(
          String.format(
              "OutOfMemoryError (max memory = %d, total memory = %d, free memory = %d)",
              Runtime.getRuntime().maxMemory(),
              Runtime.getRuntime().totalMemory(),
              Runtime.getRuntime().freeMemory()));
    } else {
      String message;
      if (getOptions().containsKey("exceptionLineSeparator")) {
        message = ce.getMessage().replaceAll(System.lineSeparator(), lineSeparator);
      } else {
        message = ce.getMessage();
      }
      msg.add(message);
      boolean noPrintErrorStack =
          (processingEnv != null
              && processingEnv.getOptions() != null
              && processingEnv.getOptions().containsKey("noPrintErrorStack"));

      msg.add("; " + culprit);
      if (noPrintErrorStack) {
        msg.add(" To see the full stack trace, don't invoke the compiler with -AnoPrintErrorStack");
      } else {
        if (this.currentRoot != null && this.currentRoot.getSourceFile() != null) {
          msg.add("Compilation unit: " + this.currentRoot.getSourceFile().getName());
        }

        DiagnosticPosition pos = null;
        if ((ce instanceof BugInCF) && ((BugInCF) ce).getLocation() != null) {
          pos = (DiagnosticPosition) ((BugInCF) ce).getLocation();
        } else if (this.visitor != null) {
          pos = (DiagnosticPosition) this.visitor.lastVisited;
        }
        if (pos != null) {
          DiagnosticSource source = new DiagnosticSource(this.currentRoot.getSourceFile(), null);
          int linenr = source.getLineNumber(pos.getStartPosition());
          int col = source.getColumnNumber(pos.getStartPosition(), true);
          String line = source.getLine(pos.getStartPosition());

          msg.add("Last visited tree at line " + linenr + " column " + col + ":");
          msg.add(line);
        }

        Throwable forStackTrace = ce.getCause() != null ? ce.getCause() : ce;
        if (forStackTrace != null) {
          msg.add(
              "Exception: " + forStackTrace + "; " + UtilPlume.stackTraceToString(forStackTrace));
          boolean printClasspath = forStackTrace instanceof NoClassDefFoundError;
          Throwable cause = forStackTrace.getCause();
          while (cause != null) {
            msg.add("Underlying Exception: " + cause + "; " + UtilPlume.stackTraceToString(cause));
            printClasspath |= cause instanceof NoClassDefFoundError;
            cause = cause.getCause();
          }

          if (printClasspath) {
            msg.add("Classpath:");
            for (URI uri : new ClassGraph().getClasspathURIs()) {
              msg.add(uri.toString());
            }
          }
        }
      }
    }

    printMessage(msg.toString());
  }

  /**
   * Converts a throwable to a BugInCF.
   *
   * @param methodName the method that caught the exception (redundant with stack trace)
   * @param t the throwable to be converted to a BugInCF
   * @param p what source code was being processed
   * @return a BugInCF that wraps the given throwable
   */
  private BugInCF wrapThrowableAsBugInCF(String methodName, Throwable t, @Nullable TreePath p) {
    return new BugInCF(
        methodName
            + ": unexpected Throwable ("
            + t.getClass().getSimpleName()
            + ")"
            + ((p == null)
                ? ""
                : " while processing " + p.getCompilationUnit().getSourceFile().getName())
            + (t.getMessage() == null ? "" : "; message: " + t.getMessage()),
        t);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Shutdown
  //

  /**
   * Return true to indicate that method {@link #shutdownHook} should be added as a shutdownHook of
   * the JVM.
   *
   * @return true to add {@link #shutdownHook} as a shutdown hook of the JVM
   */
  protected boolean shouldAddShutdownHook() {
    return hasOption("resourceStats");
  }

  /**
   * Method that gets called exactly once at shutdown time of the JVM. Checkers can override this
   * method to customize the behavior.
   *
   * <p>If you override this, you must also override {@link #shouldAddShutdownHook} to return true.
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

  // ///////////////////////////////////////////////////////////////////////////
  // Miscellaneous
  //

  /**
   * A helper function to parse a Properties file.
   *
   * @param cls the class whose location is the base of the file path
   * @param filePath the name/path of the file to be read
   * @param permitNonExisting if true, return an empty Properties if the file does not exist or
   *     cannot be parsed; if false, issue an error
   * @return the properties
   */
  protected Properties getProperties(Class<?> cls, String filePath, boolean permitNonExisting) {
    Properties prop = new Properties();
    try (InputStream base = cls.getResourceAsStream(filePath)) {

      if (base == null) {
        // The property file was not found.
        if (permitNonExisting) {
          return prop;
        } else {
          throw new BugInCF("Couldn't locate properties file " + filePath);
        }
      }

      prop.load(base);
    } catch (IOException e) {
      throw new BugInCF("Couldn't parse properties file: " + filePath, e);
    }
    return prop;
  }

  @Override
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  /** True if the git.properties file has been printed. */
  private static boolean gitPropertiesPrinted = false;

  /** Print information about the git repository from which the Checker Framework was compiled. */
  private void printGitProperties() {
    if (gitPropertiesPrinted) {
      return;
    }
    gitPropertiesPrinted = true;

    try (InputStream in = getClass().getResourceAsStream("/git.properties");
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)); ) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }
    } catch (IOException e) {
      System.out.println("IOException while reading git.properties: " + e.getMessage());
    }
  }

  /**
   * Returns the version of the Checker Framework.
   *
   * @return the Checker Framework version
   */
  private String getCheckerVersion() {
    Properties gitProperties = getProperties(getClass(), "/git.properties", false);
    String version = gitProperties.getProperty("git.build.version");
    if (version == null) {
      throw new BugInCF("Could not find the version in git.properties");
    }
    String branch = gitProperties.getProperty("git.branch");
    // git.dirty indicates modified tracked files and staged changes.  Untracked content doesn't
    // count, so not being dirty doesn't mean that exactly the printed commit is being run.
    String dirty = gitProperties.getProperty("git.dirty");
    if (version.endsWith("-SNAPSHOT") || !branch.equals("master")) {
      // Sometimes the branch is HEAD, which is not informative.
      // How does that happen, and how can I fix it?
      version += ", branch " + branch;
      // For brevity, only date but not time of day.
      version += ", " + gitProperties.getProperty("git.commit.time").substring(0, 10);
      version += ", commit " + gitProperties.getProperty("git.commit.id.abbrev");
      if (dirty.equals("true")) {
        version += ", dirty=true";
      }
    }
    return version;
  }

  /**
   * Gradle and IntelliJ wrap the processing environment to gather information about modifications
   * done by annotation processor during incremental compilation. But the Checker Framework calls
   * methods from javac that require the processing environment to be {@code
   * com.sun.tools.javac.processing.JavacProcessingEnvironment}. They fail if given a proxy. This
   * method unwraps a proxy if one is used.
   *
   * @param env a processing environment
   * @return unwrapped environment if the argument is a proxy created by IntelliJ or Gradle;
   *     original value (the argument) if the argument is a javac processing environment
   * @throws BugInCF if method fails to retrieve {@code
   *     com.sun.tools.javac.processing.JavacProcessingEnvironment}
   */
  private static ProcessingEnvironment unwrapProcessingEnvironment(ProcessingEnvironment env) {
    if (env.getClass().getName()
        == "com.sun.tools.javac.processing.JavacProcessingEnvironment") { // interned
      return env;
    }
    // IntelliJ >2020.3 wraps the processing environment in a dynamic proxy.
    ProcessingEnvironment unwrappedIntelliJ = unwrapIntelliJ(env);
    if (unwrappedIntelliJ != null) {
      return unwrapProcessingEnvironment(unwrappedIntelliJ);
    }
    // Gradle incremental build also wraps the processing environment.
    for (Class<?> envClass = env.getClass();
        envClass != null;
        envClass = envClass.getSuperclass()) {
      ProcessingEnvironment unwrappedGradle = unwrapGradle(envClass, env);
      if (unwrappedGradle != null) {
        return unwrapProcessingEnvironment(unwrappedGradle);
      }
    }
    throw new BugInCF("Unexpected processing environment: %s %s", env, env.getClass());
  }

  /**
   * Tries to unwrap ProcessingEnvironment from proxy in IntelliJ 2020.3 or later.
   *
   * @param env possibly a dynamic proxy wrapping processing environment
   * @return unwrapped processing environment, null if not successful
   */
  private static @Nullable ProcessingEnvironment unwrapIntelliJ(ProcessingEnvironment env) {
    if (!Proxy.isProxyClass(env.getClass())) {
      return null;
    }
    InvocationHandler handler = Proxy.getInvocationHandler(env);
    try {
      Field field = handler.getClass().getDeclaredField("val$delegateTo");
      field.setAccessible(true);
      Object o = field.get(handler);
      if (o instanceof ProcessingEnvironment) {
        return (ProcessingEnvironment) o;
      }
      return null;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }

  /**
   * Tries to unwrap processing environment in Gradle incremental processing. Inspired by project
   * Lombok.
   *
   * @param delegateClass a class in which to find a {@code delegate} field
   * @param env a processing environment wrapper
   * @return unwrapped processing environment, null if not successful
   */
  private static @Nullable ProcessingEnvironment unwrapGradle(
      Class<?> delegateClass, ProcessingEnvironment env) {
    try {
      Field field = delegateClass.getDeclaredField("delegate");
      field.setAccessible(true);
      Object o = field.get(env);
      if (o instanceof ProcessingEnvironment) {
        return (ProcessingEnvironment) o;
      }
      return null;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }

  /**
   * Return the path to the current compilation unit.
   *
   * @return path to the current compilation unit
   */
  public TreePath getPathToCompilationUnit() {
    return TreePath.getPath(currentRoot, currentRoot);
  }

  /**
   * Index of this checker {@link #getSubcheckers()} or the size of {@link #getSubcheckers()} if
   * this is the ultimate ancestor checker. Do not use this field directly. Call {@link
   * #getSubCheckerIndex()} instead.
   */
  private int subcheckerIndex = -1;

  /**
   * Index of this checker in {@link #getSubcheckers()} (when {@link #getSubcheckers()} is called on
   * the ultimate ancestor), or the size of {@link #getSubcheckers()} if this is the ancestor
   * checker.
   *
   * @return index of this checker in the ultimate ancestor's {@link #getSubcheckers()}, or the size
   *     of {@link #getSubcheckers()} if this is the ancestor checker
   */
  @SuppressWarnings("interning:not.interned") // Checking if ancestor is exactly this.
  protected int getSubCheckerIndex() {
    if (subcheckerIndex == -1) {
      SourceChecker ancestor = this;
      while (ancestor.parentChecker != null) {
        ancestor = ancestor.parentChecker;
      }
      if (ancestor == this) {
        subcheckerIndex = ancestor.getSubcheckers().size();
      } else {
        subcheckerIndex = ancestor.getSubcheckers().indexOf(this);
      }
      if (subcheckerIndex == -1) {
        throw new BugInCF("Checker not found in getSubcheckers.");
      }
    }
    return subcheckerIndex;
  }

  /** Represents a message (e.g., an error message) issued by a checker. */
  protected static class CheckerMessage implements Comparable<CheckerMessage> {
    /** The severity of the message. */
    final Diagnostic.Kind kind;

    /** The message itself. */
    final String message;

    /** The source code that the message is about. */
    final @InternedDistinct Tree source;

    /**
     * The checker that issued this message. The compound checker that depends on this checker uses
     * this to sort the messages.
     */
    final @InternedDistinct SourceChecker checker;

    /** The stack trace when the message was created. */
    final StackTraceElement[] trace;

    /**
     * Create a new CheckerMessage.
     *
     * @param kind kind of diagnostic, for example, error or warning
     * @param message error message that needs to be printed
     * @param source tree node causing the error
     * @param checker the type-checker in use
     * @param trace the stack trace when the message is created
     */
    protected CheckerMessage(
        Diagnostic.Kind kind,
        String message,
        @FindDistinct Tree source,
        @FindDistinct SourceChecker checker,
        StackTraceElement[] trace) {
      this.kind = kind;
      this.message = message;
      this.source = source;
      this.checker = checker;
      this.trace = trace;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      CheckerMessage that = (CheckerMessage) o;
      return this.kind == that.kind
          && this.message.equals(that.message)
          && this.source == that.source
          && this.checker == that.checker;
    }

    @Override
    public int hashCode() {
      return Objects.hash(kind, message, source, checker);
    }

    @Override
    public String toString() {
      return "CheckerMessage{"
          + "kind="
          + kind
          + ", checker="
          + checker.getClass().getSimpleName()
          + ", message='"
          + message
          + '\''
          + ", source="
          + source
          + '}';
    }

    /**
     * Compares {@code other} with {@code this} {@link CheckerMessage}. Compares first by position
     * at which the error will be printed, then by kind of message, then the order in which the
     * checkers run, and finally by the message string.
     *
     * @param other the other CheckerMessage
     * @return a negative integer, zero, or a positive integer if this CheckerMessage is less than,
     *     equal to, or greater than {@code other}
     */
    @Override
    public int compareTo(CheckerMessage other) {
      int byPos = InternalUtils.compareDiagnosticPosition(this.source, other.source);
      if (byPos != 0) {
        return byPos;
      }

      int kind = this.kind.compareTo(other.kind);
      if (kind != 0) {
        return kind;
      }

      // Sort by order in which the checkers are run. (All the subcheckers,
      // followed by the checker.)
      int thisIndex = this.checker.getSubCheckerIndex();
      int otherIndex = other.checker.getSubCheckerIndex();
      if (thisIndex != otherIndex) {
        return Integer.compare(thisIndex, otherIndex);
      }

      return this.message.compareTo(other.message);
    }
  }
}
