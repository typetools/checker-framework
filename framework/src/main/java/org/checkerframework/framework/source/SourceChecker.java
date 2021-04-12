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
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.CheckerMain;
import org.checkerframework.framework.util.OptionConfiguration;
import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SystemUtil;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;
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

  // Unsoundly assume all methods have no side effects, are deterministic, or both.
  "assumeSideEffectFree",
  "assumeDeterministic",
  "assumePure",

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

  // The next ones *increase* rather than *decrease* soundness.  They will eventually be replaced by
  // their complements (except -AconcurrentSemantics) and moved into the above section.

  // TODO: Checking of bodies of @SideEffectFree, @Deterministic, and
  // @Pure methods is temporarily disabled unless -AcheckPurityAnnotations is
  // supplied on the command line.
  // Re-enable it after making the analysis more precise.
  // org.checkerframework.common.basetype.BaseTypeVisitor.visitMethod(MethodTree, Void)
  "checkPurityAnnotations",
  "checkSideEffectsOnlyAnnotation",

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
  // Temporary, for backward compatibility
  "useDefaultsForUncheckedCode",

  // Whether to assume sound concurrent semantics or
  // simplified sequential semantics
  // org.checkerframework.framework.flow.CFAbstractTransfer.sequentialSemantics
  "concurrentSemantics",

  // Whether to use a conservative value for type arguments that could not be inferred.
  // See Issue 979.
  "conservativeUninferredTypeArguments",

  // Whether to ignore all subtype tests for type arguments that
  // were inferred for a raw type. Defaults to true.
  // org.checkerframework.framework.type.TypeHierarchy.isSubtypeTypeArguments
  "ignoreRawTypeArguments",

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

  // Whether to use whole-program inference. Takes an argument to specify the output format:
  // "-Ainfer=stubs" or "-Ainfer=jaifs".
  "infer",

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

  // Ignore annotations in bytecode that have invalid annotation locations.
  // See https://github.com/typetools/checker-framework/issues/2173
  // org.checkerframework.framework.type.ElementAnnotationApplier.apply
  "ignoreInvalidAnnotationLocations",

  ///
  /// Partially-annotated libraries
  ///

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
  // Whether to ignore missing classes even when warnIfNotFound is set to true and other classes
  // from the same package are present (useful if a package spans more than one jar).
  // org.checkerframework.framework.stub.AnnotationFileParser.warnIfNotFoundIgnoresClasses
  "stubWarnIfNotFoundIgnoresClasses",
  // Whether to print warnings about stub files that overwrite annotations from bytecode.
  "stubWarnIfOverwritesBytecode",
  // Whether to print warnings about stub files that are redundant with the annotations from
  // bytecode.
  "stubWarnIfRedundantWithBytecode",
  // With this option, annotations in stub files are used EVEN IF THE SOURCE FILE IS
  // PRESENT. Only use this option when you intend to store types in stub files rather than
  // directly in source code, such as during whole-program inference. The annotations in the
  // stub files will be glb'd with those in the source code before local inference begins.
  "mergeStubsWithSource",
  // Already listed above, but worth noting again in this section:
  // "useConservativeDefaultsForUncheckedCode"

  ///
  /// Debugging
  ///

  /// Amount of detail in messages

  // Print the version of the Checker Framework
  "version",
  // Print info about git repository from which the Checker Framework was compiled
  "printGitProperties",

  // Whether to print @InvisibleQualifier marked annotations
  // org.checkerframework.framework.type.AnnotatedTypeMirror.toString()
  "printAllQualifiers",

  // Whether to print [] around a set of type parameters in order to clearly see where they end
  // e.g.  <E extends F, F extends Object>
  // without this option the E is printed: E extends F extends Object
  // with this option:                     E [ extends F [ extends Object super Void ] super Void ]
  // when multiple type variables are used this becomes useful very quickly
  "printVerboseGenerics",

  // Whether to NOT output a stack trace for each framework error.
  // org.checkerframework.framework.source.SourceChecker.logBugInCF
  "noPrintErrorStack",

  // Only output error code, useful for testing framework
  // org.checkerframework.framework.source.SourceChecker.message(Kind, Object, String, Object...)
  "nomsgtext",

  /// Format of messages

  // Output detailed message in simple-to-parse format, useful
  // for tools parsing Checker Framework output.
  // org.checkerframework.framework.source.SourceChecker.message(Kind, Object, String, Object...)
  "detailedmsgtext",

  /// Stub and JDK libraries

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

  // Output a stack trace when reporting errors or warnings
  // org.checkerframework.common.basetype.SourceChecker.printStackTrace()
  "dumpOnErrors",

  /// Visualizing the CFG

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

  /// Caches

  // Set the cache size for caches in AnnotatedTypeFactory
  "atfCacheSize",

  // Sets AnnotatedTypeFactory shouldCache to false
  "atfDoNotCache",

  /// Miscellaneous debugging options

  // Whether to output resource statistics at JVM shutdown
  // org.checkerframework.framework.source.SourceChecker.shutdownHook()
  "resourceStats",

  // Parse all JDK files at startup rather than as needed.
  "parseAllJdk",

  // Run checks that test ajava files.
  //
  // Whenever processing a source file, parse it with JavaParser and check that the AST can be
  // matched with javac's tree. Crash if not. For testing the class JointJavacJavaParserVisitor.
  //
  // Also checks that annotations can be inserted. For each Java file, clears all annotations and
  // reinserts them, then checks if the original and modified ASTs are equivalent.
  "ajavaChecks",
})
public abstract class SourceChecker extends AbstractTypeProcessor implements OptionConfiguration {

  // TODO A checker should export itself through a separate interface, and maybe have an interface
  // for all the methods for which it's safe to override.

  /** The line separator. */
  private static final String LINE_SEPARATOR = System.lineSeparator().intern();

  /** The message key that will suppress all warnings (it matches any message key). */
  public static final String SUPPRESS_ALL_MESSAGE_KEY = "all";

  /** The SuppressWarnings prefix that will suppress warnings for all checkers. */
  public static final String SUPPRESS_ALL_PREFIX = "allcheckers";

  /** The message key emitted when an unused warning suppression is found. */
  public static final @CompilerMessageKey String UNNEEDED_SUPPRESSION_KEY = "unneeded.suppression";

  /** File name of the localized messages. */
  protected static final String MSGS_FILE = "messages.properties";

  /**
   * Maps error keys to localized/custom error messages. Do not use directly; call {@link
   * #fullMessageOf} or {@link #processArg}.
   */
  protected Properties messagesProperties;

  /** Used to report error messages and warnings via the compiler. */
  protected Messager messager;

  /** Used as a helper for the {@link SourceVisitor}. */
  protected Trees trees;

  /** The source tree that is being scanned. */
  protected @InternedDistinct CompilationUnitTree currentRoot;

  /** The visitor to use. */
  protected SourceVisitor<?, ?> visitor;

  /**
   * Exceptions to -AwarnUnneededSuppressions processing. No warning about unneeded suppressions is
   * issued if the SuppressWarnings string matches this pattern.
   */
  private @Nullable Pattern warnUnneededSuppressionsExceptions;

  /**
   * SuppressWarnings strings supplied via the -AsuppressWarnings option. Do not use directly, call
   * {@link #getSuppressWarningsStringsFromOption()}.
   */
  private String @Nullable [] suppressWarningsStringsFromOption;

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
  private Pattern skipUsesPattern;

  /**
   * Regular expression pattern to specify Java classes that are annotated, so warnings about them
   * should be issued but warnings about all other classes should be suppressed.
   *
   * <p>It contains the pattern specified by the user, through the option {@code checkers.onlyUses};
   * otherwise it contains a pattern that matches every class.
   */
  private Pattern onlyUsesPattern;

  /**
   * Regular expression pattern to specify Java classes whose definition should not be checked.
   *
   * <p>It contains the pattern specified by the user, through the option {@code checkers.skipDefs};
   * otherwise it contains a pattern that can match no class.
   */
  private Pattern skipDefsPattern;

  /**
   * Regular expression pattern to specify Java classes whose definition should be checked.
   *
   * <p>It contains the pattern specified by the user, through the option {@code checkers.onlyDefs};
   * otherwise it contains a pattern that matches every class.
   */
  private Pattern onlyDefsPattern;

  /** The supported lint options. */
  private Set<String> supportedLints;

  /** The enabled lint options. */
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
  private Map<String, String> activeOptions;

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
  protected List<@FullyQualifiedName String> upstreamCheckerNames;

  @Override
  public final synchronized void init(ProcessingEnvironment env) {
    ProcessingEnvironment unwrappedEnv = unwrapProcessingEnvironment(env);
    super.init(unwrappedEnv);
    // The processingEnvironment field will be set by the superclass's init method.
    // This is used to trigger AggregateChecker's setProcessingEnvironment.
    setProcessingEnvironment(unwrappedEnv);

    // Keep in sync with check in checker-framework/build.gradle and text in installation
    // section of manual.
    int jreVersion = SystemUtil.getJreVersion();
    if (jreVersion < 8) {
      throw new UserError(
          "Use JDK 8 or JDK 11 to run the Checker Framework.  You are using version %d.",
          jreVersion);
    } else if (jreVersion > 12) {
      throw new UserError(
          String.format(
              "Use JDK 8 or JDK 11 to run the Checker Framework.  You are using version %d.",
              jreVersion));
    } else if (jreVersion != 8 && jreVersion != 11) {
      message(
          Kind.WARNING,
          "Use JDK 8 or JDK 11 to run the Checker Framework.  You are using version %d.",
          jreVersion);
    }

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

  ///////////////////////////////////////////////////////////////////////////
  /// Getters and setters
  ///

  /**
   * Returns the {@link ProcessingEnvironment} that was supplied to this checker.
   *
   * @return the {@link ProcessingEnvironment} that was supplied to this checker
   */
  public ProcessingEnvironment getProcessingEnvironment() {
    return this.processingEnv;
  }

  /** Set the processing environment of the current checker. */
  /* This method is protected only to allow the AggregateChecker and BaseTypeChecker to call it. */
  protected void setProcessingEnvironment(ProcessingEnvironment env) {
    this.processingEnv = env;
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
  @SuppressWarnings("interning:assignment.type.incompatible") // used in == tests
  protected void setRoot(CompilationUnitTree newRoot) {
    this.currentRoot = newRoot;
    visitor.setRoot(currentRoot);
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
        upstreamCheckerNames.add(checker.getClass().getCanonicalName());
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
    return getProcessingEnvironment().getElementUtils();
  }

  /**
   * Returns the type utilities associated with this.
   *
   * @return the type utilities associated with this
   */
  public Types getTypeUtils() {
    return getProcessingEnvironment().getTypeUtils();
  }

  /**
   * Returns the tree utilities associated with this.
   *
   * @return the tree utilities associated with this
   */
  public Trees getTreeUtils() {
    return Trees.instance(getProcessingEnvironment());
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
        "getAnnotationProvider is not implemented for this class.");
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
    }

    for (Class<?> checker : checkers) {
      messagesProperties.putAll(getProperties(checker, MSGS_FILE));
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

  ///////////////////////////////////////////////////////////////////////////
  /// Type-checking
  ///

  /**
   * {@inheritDoc}
   *
   * <p>Type-checkers are not supposed to override this. Instead use initChecker. This allows us to
   * handle BugInCF only here and doesn't require all overriding implementations to be aware of
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
            Kind.WARNING,
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
      if (hasOption("version")) {
        messager.printMessage(Kind.NOTE, "Checker Framework " + getCheckerVersion());
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

  /**
   * Initialize the checker.
   *
   * @see AbstractProcessor#init(ProcessingEnvironment)
   */
  public void initChecker() {
    // Grab the Trees and Messager instances now; other utilities
    // (like Types and Elements) can be retrieved by subclasses.
    @Nullable Trees trees = Trees.instance(processingEnv);
    assert trees != null;
    this.trees = trees;

    this.messager = processingEnv.getMessager();
    this.messagesProperties = getMessagesProperties();

    this.visitor = createSourceVisitor();

    // Validate the lint flags, if they haven't been used already.
    if (this.activeLints == null) {
      this.activeLints = createActiveLints(getOptions());
    }
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
   * The number of errors at the last exit of the type processor. At entry to the type processor we
   * check whether the current error count is higher and then don't process the file, as it contains
   * some Java errors. Needs to be protected to allow access from AggregateChecker and
   * BaseTypeChecker.
   */
  protected int errsOnLastExit = 0;

  /**
   * Report "type.checking.not.run" error.
   *
   * @param p error is reported at the leaf of the path
   */
  @SuppressWarnings("interning:assignment.type.incompatible") // used in == tests
  protected void reportJavacError(TreePath p) {
    // If javac issued any errors, do not type check any file, so that the Checker Framework
    // does not have to deal with error types.
    currentRoot = p.getCompilationUnit();
    reportError(p.getLeaf(), "type.checking.not.run", getClass().getSimpleName());
  }

  /**
   * Type-check the code using this checker's visitor.
   *
   * @see Processor#process(Set, RoundEnvironment)
   */
  @Override
  public void typeProcess(TypeElement e, TreePath p) {
    if (javacErrored) {
      reportJavacError(p);
      return;
    }

    // Cannot use BugInCF here because it is outside of the try/catch for BugInCF.
    if (e == null) {
      messager.printMessage(Kind.ERROR, "Refusing to process empty TypeElement");
      return;
    }
    if (p == null) {
      messager.printMessage(Kind.ERROR, "Refusing to process empty TreePath in TypeElement: " + e);
      return;
    }
    if (!warnedAboutGarbageCollection && SystemPlume.gcPercentage() > .25) {
      messager.printMessage(
          Kind.WARNING, "Garbage collection consumed over 25% of CPU during the past minute.");
      messager.printMessage(
          Kind.WARNING,
          String.format(
              "Perhaps increase max heap size"
                  + " (max memory = %d, total memory = %d, free memory = %d).",
              Runtime.getRuntime().maxMemory(),
              Runtime.getRuntime().totalMemory(),
              Runtime.getRuntime().freeMemory()));
      warnedAboutGarbageCollection = true;
    }

    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    Source source = Source.instance(context);
    // Don't use source.allowTypeAnnotations() because that API changed after 9.
    // Also the enum constant Source.JDK1_8 was renamed at some point...
    if (!warnedAboutSourceLevel && source.compareTo(Source.lookup("8")) < 0) {
      messager.printMessage(
          Kind.WARNING, "-source " + source.name + " does not support type annotations");
      warnedAboutSourceLevel = true;
    }

    Log log = Log.instance(context);
    if (log.nerrors > this.errsOnLastExit) {
      this.errsOnLastExit = log.nerrors;
      javacErrored = true;
      reportJavacError(p);
      return;
    }

    if (visitor == null) {
      // typeProcessingStart invokes initChecker, which should have set the visitor. If the field is
      // still null, an exception occurred during initialization, which was already logged
      // there. Don't also cause a NPE here.
      return;
    }
    if (p.getCompilationUnit() != currentRoot) {
      setRoot(p.getCompilationUnit());
      if (hasOption("filenames")) {
        // TODO: Have a command-line option to turn the timestamps on/off too, because
        // they are nondeterministic across runs.

        // Add timestamp to indicate how long operations are taking.
        // Duplicate messages are suppressed, so this might not appear in front of every "
        // is type-checking " message (when a file takes less than a second to type-check).
        message(Kind.NOTE, Instant.now().toString());
        message(
            Kind.NOTE,
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
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Reporting type-checking errors; most clients use reportError() or reportWarning()
  ///

  /**
   * Reports an error. By default, prints it to the screen via the compiler's internal messager.
   *
   * @param source the source position information; may be an Element, a Tree, or null
   * @param messageKey the message key
   * @param args arguments for interpolation in the string corresponding to the given message key
   */
  public void reportError(Object source, @CompilerMessageKey String messageKey, Object... args) {
    report(source, Kind.ERROR, messageKey, args);
  }

  /**
   * Reports a warning. By default, prints it to the screen via the compiler's internal messager.
   *
   * @param source the source position information; may be an Element, a Tree, or null
   * @param messageKey the message key
   * @param args arguments for interpolation in the string corresponding to the given message key
   */
  public void reportWarning(Object source, @CompilerMessageKey String messageKey, Object... args) {
    report(source, Kind.MANDATORY_WARNING, messageKey, args);
  }

  /**
   * Reports a diagnostic message. By default, prints it to the screen via the compiler's internal
   * messager.
   *
   * <p>Most clients should use {@link #reportError} or {@link #reportWarning}.
   *
   * @param source the source position information; may be an Element, a Tree, or null
   * @param d the diagnostic message
   */
  public void report(Object source, DiagMessage d) {
    report(source, d.getKind(), d.getMessageKey(), d.getArgs());
  }

  /**
   * Reports a diagnostic message. By default, it prints it to the screen via the compiler's
   * internal messager; however, it might also store it for later output.
   *
   * @param source the source position information; may be an Element, a Tree, or null
   * @param kind the type of message
   * @param messageKey the message key
   * @param args arguments for interpolation in the string corresponding to the given message key
   */
  // Not a format method.  However, messageKey should be either a format string for `args`, or  a
  // property key that maps to a format string for `args`.
  // @FormatMethod
  @SuppressWarnings("formatter:format.string.invalid") // arg is a format string or a property key
  private void report(
      Object source,
      javax.tools.Diagnostic.Kind kind,
      @CompilerMessageKey String messageKey,
      Object... args) {
    assert messagesProperties != null : "null messagesProperties";

    if (shouldSuppressWarnings(source, messageKey)) {
      return;
    }

    if (args != null) {
      for (int i = 0; i < args.length; ++i) {
        args[i] = processArg(args[i]);
      }
    }

    if (kind == Kind.NOTE) {
      System.err.println("(NOTE) " + String.format(messageKey, args));
      return;
    }

    final String defaultFormat = "(" + messageKey + ")";
    String fmtString;
    if (this.processingEnv.getOptions() != null /*nnbug*/
        && this.processingEnv.getOptions().containsKey("nomsgtext")) {
      fmtString = defaultFormat;
    } else if (this.processingEnv.getOptions() != null /*nnbug*/
        && this.processingEnv.getOptions().containsKey("detailedmsgtext")) {
      // The -Adetailedmsgtext command-line option was given, so output
      // a stylized error message for easy parsing by a tool.
      fmtString =
          detailedMsgTextPrefix(source, defaultFormat, args)
              + fullMessageOf(messageKey, defaultFormat);
    } else {
      fmtString =
          "["
              + suppressWarningsString(messageKey)
              + "] "
              + fullMessageOf(messageKey, defaultFormat);
    }
    String messageText;
    try {
      messageText = String.format(fmtString, args);
    } catch (Exception e) {
      throw new BugInCF(
          "Invalid format string: \"" + fmtString + "\" args: " + Arrays.toString(args), e);
    }

    if (kind == Kind.ERROR && hasOption("warns")) {
      kind = Kind.MANDATORY_WARNING;
    }

    if (source instanceof Element) {
      messager.printMessage(kind, messageText, (Element) source);
    } else if (source instanceof Tree) {
      printOrStoreMessage(kind, messageText, (Tree) source, currentRoot);
    } else {
      throw new BugInCF("invalid position source, class=" + source.getClass());
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
  public void message(javax.tools.Diagnostic.Kind kind, String msg, Object... args) {
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
    messager.printMessage(Kind.ERROR, msg);
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
    StackTraceElement[] trace = Thread.currentThread().getStackTrace();
    printOrStoreMessage(kind, message, source, root, trace);
  }

  /**
   * Stores all messages and sorts them by location before outputting them for compound checkers.
   * This method is overloaded with an additional stack trace argument. The stack trace is printed
   * when the dumpOnErrors option is enabled.
   *
   * @param kind the kind of message to print
   * @param message the message text
   * @param source the source code position of the diagnostic message
   * @param root the compilation unit
   * @param trace the stack trace where the checker encountered an error
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
      StringBuilder msg = new StringBuilder();
      for (StackTraceElement elem : trace) {
        msg.append("\tat " + elem + "\n");
      }
      message(Diagnostic.Kind.NOTE, msg.toString());
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Diagnostic message formatting
  ///

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
      if (messagesProperties.containsKey(key)) {
        return messagesProperties.getProperty(key);
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
  protected Object processArg(Object arg) {
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
   * Returns all but the message key part of the message format output by -Adetailedmsgtext.
   *
   * @param source the object from which to obtain source position information; may be an Element, a
   *     Tree, or null
   * @param defaultFormat the message key, in parentheses
   * @param args arguments for interpolation in the string corresponding to the given message key
   * @return the first part of the message format output by -Adetailedmsgtext
   */
  private String detailedMsgTextPrefix(Object source, String defaultFormat, Object[] args) {
    StringJoiner sj = new StringJoiner(DETAILS_SEPARATOR);

    // The parts, separated by " $$ " (DETAILS_SEPARATOR), are:

    // (1) error key
    sj.add(defaultFormat);

    // (2) number of additional tokens, and those tokens; this depends on the error message, and an
    // example is the found and expected types
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
   * Returns the most specific warning suppression string for the warning/error being printed. This
   * is {@code msg} prefixed by a checker name (or "allcheckers") and a colon.
   *
   * @param messageKey the simple, checker-specific error message key
   * @return the most specific SuppressWarnings string for the warning/error being printed
   */
  private String suppressWarningsString(String messageKey) {
    Collection<String> prefixes = this.getSuppressWarningsPrefixes();
    prefixes.remove(SUPPRESS_ALL_PREFIX);
    if (hasOption("showSuppressWarningsStrings")) {
      List<String> list = new ArrayList<>(prefixes);
      // Make sure "allcheckers" is at the end of the list.
      if (useAllcheckersPrefix) {
        list.add(SUPPRESS_ALL_PREFIX);
      }
      return list + ":" + messageKey;
    } else if (hasOption("requirePrefixInWarningSuppressions")) {
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
  private String detailedMsgTextPositionString(Tree tree, CompilationUnitTree currentRoot) {
    if (tree == null) {
      return "";
    }

    SourcePositions sourcePositions = trees.getSourcePositions();
    long start = sourcePositions.getStartPosition(currentRoot, tree);
    long end = sourcePositions.getEndPosition(currentRoot, tree);

    return "( " + start + ", " + end + " )";
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Lint options ("-Alint:xxxx" and "-Alint:-xxxx")
  ///

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

    Set<String> activeLint = new HashSet<>();
    for (String s : lintString.split(",")) {
      if (!this.getSupportedLintOptions().contains(s)
          && !(s.charAt(0) == '-' && this.getSupportedLintOptions().contains(s.substring(1)))
          && !s.equals("all")
          && !s.equals("none")) {
        this.messager.printMessage(
            Kind.WARNING,
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
   * Helper method to find the parent of a lint key. The lint hierarchy level is donated by a colon
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
    assert slValue != null;

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
   * NullnessSubchecker need to use this method, as one is created by the other.
   *
   * @param newLints the new supported lint options, which replace any existing ones
   */
  protected void setSupportedLintOptions(Set<String> newLints) {
    supportedLints = newLints;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Regular (non-lint) options ("-Axxxx")
  ///

  /**
   * Determine which options are active.
   *
   * @param options all provided options
   * @return a value for {@link #activeOptions}
   */
  private Map<String, String> createActiveOptions(Map<String, String> options) {
    if (options.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> activeOpts = new HashMap<>();

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
    return Collections.unmodifiableMap(activeOpts);
  }

  /**
   * Add additional active options. Use of this method should be limited to the AggregateChecker,
   * who needs to set the active options to the union of all subcheckers.
   *
   * @param moreOpts the active options to add
   */
  protected void addOptions(Map<String, String> moreOpts) {
    Map<String, String> activeOpts = new HashMap<>(getOptions());
    activeOpts.putAll(moreOpts);
    activeOptions = Collections.unmodifiableMap(activeOpts);
  }

  /**
   * Check whether the given option is provided.
   *
   * <p>Note that {@link #getOption(String)} can still return null even if {@code hasOption} returns
   * true: this happens e.g. for {@code -Amyopt}
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
   * Determines the boolean value of the option with the given name. Returns false if the option is
   * not set.
   *
   * @param name the name of the option to check
   * @see SourceChecker#getLintOption(String,boolean)
   */
  @Override
  public final boolean getBooleanOption(String name) {
    return getBooleanOption(name, false);
  }

  /**
   * Determines the boolean value of the option with the given name. Returns the given default value
   * if the option is not set.
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
        String.format("Value of %s option should be a boolean, but is \"%s\".", name, value));
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

    // Support all options provided with the standard {@link
    // javax.annotation.processing.SupportedOptions} annotation.
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
   * classPrefixes} to {@code options}, separated by {@link #OPTION_SEPARATOR}.
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

  ///////////////////////////////////////////////////////////////////////////
  /// Warning suppression and unneeded warnings
  ///

  /**
   * Returns the argument to -AsuppressWarnings, split on commas, or null if no such argument. Only
   * ever called once; the value is cached in field {@link #suppressWarningsStringsFromOption}.
   *
   * @return the argument to -AsuppressWarnings, split on commas, or null if no such argument
   */
  private String @Nullable [] getSuppressWarningsStringsFromOption() {
    Map<String, String> options = getOptions();
    if (this.suppressWarningsStringsFromOption == null) {
      if (!options.containsKey("suppressWarnings")) {
        return null;
      }

      String swStrings = options.get("suppressWarnings");
      if (swStrings == null) {
        return null;
      }
      this.suppressWarningsStringsFromOption = swStrings.split(",");
    }

    return this.suppressWarningsStringsFromOption;
  }

  /**
   * Issues a warning about any {@code @SuppressWarnings} that didn't suppress a warning, but starts
   * with this checker name or "allcheckers".
   */
  protected void warnUnneededSuppressions() {
    if (!hasOption("warnUnneededSuppressions")) {
      return;
    }

    Set<Element> elementsSuppress = new HashSet<>(this.elementsWithSuppressedWarnings);
    this.elementsWithSuppressedWarnings.clear();
    Set<String> prefixes = new HashSet<>(getSuppressWarningsPrefixes());
    Set<String> errorKeys = new HashSet<>(messagesProperties.stringPropertyNames());
    warnUnneededSuppressions(elementsSuppress, prefixes, errorKeys);
    getVisitor().treesWithSuppressWarnings.clear();
  }

  /**
   * Issues a warning about any {@code @SuppressWarnings} string that didn't suppress a warning, but
   * starts with one of the given prefixes (checker names).
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
              || suppressWarningsString.startsWith(prefix + ":")) {
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
    Tree swTree = findSuppressWarningsTree(tree);
    report(
        swTree,
        Kind.MANDATORY_WARNING,
        SourceChecker.UNNEEDED_SUPPRESSION_KEY,
        "\"" + suppressWarningsString + "\"",
        getClass().getSimpleName());
  }

  /** The name of the @SuppressWarnings annotation. */
  private final @CanonicalName String suppressWarningsClassName =
      SuppressWarnings.class.getCanonicalName();
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
      if (AnnotationUtils.areSameByName(
          TreeUtils.annotationFromAnnotationTree(annotationTree), suppressWarningsClassName)) {
        return annotationTree;
      }
    }
    throw new BugInCF("Did not find @SuppressWarnings: " + tree);
  }

  /**
   * Returns true if all the warnings pertaining to the given source should be suppressed. This
   * implementation just that delegates to an overloaded, more specific version of {@code
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
      throw new BugInCF("Unexpected source " + src);
    }
  }

  /**
   * Determines whether all the warnings pertaining to a given tree should be suppressed. Returns
   * true if the tree is within the scope of a @SuppressWarnings annotation, one of whose values
   * suppresses the checker's warnings. Also, returns true if the {@code errKey} matches a string in
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
      return true;
    }

    if (shouldSuppress(getSuppressWarningsStringsFromOption(), errKey)) {
      // If the error key matches a warning string in the -AsuppressWarnings, then suppress
      // the warning.
      return true;
    }

    // trees.getPath might be slow, but this is only used in error reporting
    @Nullable TreePath path = trees.getPath(this.currentRoot, tree);

    @Nullable VariableTree var = TreePathUtil.enclosingVariable(path);
    if (var != null && shouldSuppressWarnings(TreeUtils.elementFromTree(var), errKey)) {
      return true;
    }

    @Nullable MethodTree method = TreePathUtil.enclosingMethod(path);
    if (method != null) {
      @Nullable Element elt = TreeUtils.elementFromTree(method);

      if (shouldSuppressWarnings(elt, errKey)) {
        return true;
      }

      if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
        // Return false immediately. Do NOT check for AnnotatedFor in the enclosing elements,
        // because they may not have an @AnnotatedFor.
        return false;
      }
    }

    @Nullable ClassTree cls = TreePathUtil.enclosingClass(path);
    if (cls != null) {
      @Nullable Element elt = TreeUtils.elementFromTree(cls);

      if (shouldSuppressWarnings(elt, errKey)) {
        return true;
      }

      if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
        // Return false immediately. Do NOT check for AnnotatedFor in the enclosing elements,
        // because they may not have an @AnnotatedFor.
        return false;
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
    final boolean useUncheckedDefaultsForSource = false;
    final boolean useUncheckedDefaultsForByteCode = false;
    String option = this.getOption("useConservativeDefaultsForUncheckedCode");
    // Temporary, for backward compatibility.
    if (option == null) {
      this.getOption("useDefaultsForUncheckedCode");
    }

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
          "SourceChecker: unexpected argument to useConservativeDefault: " + kindOfCode);
    }
  }

  /**
   * Elements with a {@code @SuppressWarnings} that actually suppressed a warning for this checker.
   */
  protected final Set<Element> elementsWithSuppressedWarnings = new HashSet<>();

  /**
   * Determines whether all the warnings pertaining to a given element should be suppressed. Returns
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
    if (UNNEEDED_SUPPRESSION_KEY.equals(errKey)) {
      // Never suppress an "unneeded.suppression" warning.
      // TODO: This choice is questionable, because these warnings should be suppressable just
      // like any others.  The reason for the choice is that if a user writes
      // `@SuppressWarnings("nullness")` that isn't needed, then that annotation would
      // suppress the unneeded suppression warning.  It would take extra work to permit more
      // desirable behavior in that case.
      return false;
    }

    if (shouldSuppress(getSuppressWarningsStringsFromOption(), errKey)) {
      return true;
    }

    while (elt != null) {
      SuppressWarnings suppressWarningsAnno = elt.getAnnotation(SuppressWarnings.class);
      if (suppressWarningsAnno != null) {
        String[] suppressWarningsStrings = suppressWarningsAnno.value();
        if (shouldSuppress(suppressWarningsStrings, errKey)) {
          if (hasOption("warnUnneededSuppressions")) {
            elementsWithSuppressedWarnings.add(elt);
          }
          return true;
        }
      }
      if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
        // Return false immediately. Do NOT check for AnnotatedFor in the
        // enclosing elements, because they may not have an @AnnotatedFor.
        return false;
      }
      elt = elt.getEnclosingElement();
    }
    return false;
  }

  /**
   * Determines whether an error (whose message key is {@code messageKey}) should be suppressed. It
   * is suppressed if any of the given SuppressWarnings strings suppresses it.
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
   * @param suppressWarningsStrings the SuppressWarnings strings that are in effect. May be null, in
   *     which case this method returns false.
   * @param messageKey the message key of the error the checker is emitting; a lowercase string,
   *     without any "checkername:" prefix
   * @return true if an element of {@code suppressWarningsStrings} suppresses the error
   */
  private boolean shouldSuppress(String[] suppressWarningsStrings, String messageKey) {
    Set<String> prefixes = this.getSuppressWarningsPrefixes();
    return shouldSuppress(prefixes, suppressWarningsStrings, messageKey);
  }

  /**
   * Helper method for {@link #shouldSuppress(String[], String)}.
   *
   * @param prefixes the SuppressWarnings prefixes used by this checker
   * @param suppressWarningsStrings the SuppressWarnings strings that are in effect. May be null, in
   *     which case this method returns false.
   * @param messageKey the message key of the error the checker is emitting; a lowercase string,
   *     without any "checkername:" prefix
   * @return true if one of the {@code suppressWarningsStrings} suppresses the error
   */
  private boolean shouldSuppress(
      Set<String> prefixes, String[] suppressWarningsStrings, String messageKey) {
    if (suppressWarningsStrings == null) {
      return false;
    }
    // Is the name of the checker required to suppress a warning?
    boolean requirePrefix = hasOption("requirePrefixInWarningSuppressions");

    for (String suppressWarningsString : suppressWarningsStrings) {
      int colonPos = suppressWarningsString.indexOf(":");
      String messageKeyInSuppressWarningsString;
      if (colonPos == -1) {
        // The SuppressWarnings string is not of the form prefix:partial-message-key
        if (prefixes.contains(suppressWarningsString)) {
          // The value in the @SuppressWarnings is exactly a prefix. Suppress the warning
          // no matter its message key.
          return true;
        } else if (requirePrefix) {
          // A prefix is required, but this SuppressWarnings string does not have a
          // prefix; check the next SuppressWarnings string.
          continue;
        } else if (suppressWarningsString.equals(SUPPRESS_ALL_MESSAGE_KEY)) {
          // Prefixes aren't required and the SuppressWarnings string is "all".  Suppress
          // the warning no matter its message key.
          return true;
        }
        // The suppressWarningsString is not a prefix or a prefix:message-key, so it might
        // be a message key.
        messageKeyInSuppressWarningsString = suppressWarningsString;
      } else {
        // The SuppressWarnings string has a prefix.
        String suppressWarningsPrefix = suppressWarningsString.substring(0, colonPos);
        if (!prefixes.contains(suppressWarningsPrefix)) {
          // The prefix of this SuppressWarnings string is a not a prefix supported by
          // this checker. Proceed to the next SuppressWarnings string.
          continue;
        }
        messageKeyInSuppressWarningsString = suppressWarningsString.substring(colonPos + 1);
      }
      // Check if the message key in the warning suppression is part of the message key that
      // the checker is emiting.
      if (messageKey.equals(messageKeyInSuppressWarningsString)
          || messageKey.startsWith(messageKeyInSuppressWarningsString + ".")
          || messageKey.endsWith("." + messageKeyInSuppressWarningsString)
          || messageKey.contains("." + messageKeyInSuppressWarningsString + ".")) {
        return true;
      }
    }

    // None of the SuppressWarnings strings suppress this error.
    return false;
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

    @Nullable AnnotatedFor anno = elt.getAnnotation(AnnotatedFor.class);

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
  public SortedSet<String> getSuppressWarningsPrefixes() {
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

    @SuppressWarnings("deprecation") // SuppressWarningsKeys was renamed to SuppressWarningsPrefix
    SuppressWarningsKeys annotation = this.getClass().getAnnotation(SuppressWarningsKeys.class);
    if (annotation != null) {
      for (String prefix : annotation.value()) {
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
    return result.toLowerCase();
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Skipping uses and defs
  ///

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
    TypeElement typeElement = ElementUtils.enclosingTypeElement(element);
    if (typeElement == null) {
      throw new BugInCF("enclosingTypeElement(%s [%s]) => null%n", element, element.getClass());
    }
    @SuppressWarnings("signature:assignment.type.incompatible" // TypeElement.toString():
    // @FullyQualifiedName
    )
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

  ///////////////////////////////////////////////////////////////////////////
  /// Errors other than type-checking errors
  ///

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
    String msg = ce.getMessage();
    printMessage(msg);
  }

  /**
   * Log (that is, print) an internal error in the framework or a checker.
   *
   * @param ce the internal error to output
   */
  private void logBugInCF(BugInCF ce) {
    StringJoiner msg = new StringJoiner(LINE_SEPARATOR);
    if (ce.getCause() != null && ce.getCause() instanceof OutOfMemoryError) {
      msg.add(
          String.format(
              "The JVM ran out of memory.  Run with a larger max heap size"
                  + " (max memory = %d, total memory = %d, free memory = %d).",
              Runtime.getRuntime().maxMemory(),
              Runtime.getRuntime().totalMemory(),
              Runtime.getRuntime().freeMemory()));
    } else {

      msg.add(ce.getMessage());
      boolean noPrintErrorStack =
          (processingEnv != null
              && processingEnv.getOptions() != null
              && processingEnv.getOptions().containsKey("noPrintErrorStack"));

      msg.add("; The Checker Framework crashed.  Please report the crash.");
      if (noPrintErrorStack) {
        msg.add(" To see the full stack trace, don't invoke the compiler with -AnoPrintErrorStack");
      } else {
        if (this.currentRoot != null && this.currentRoot.getSourceFile() != null) {
          msg.add("Compilation unit: " + this.currentRoot.getSourceFile().getName());
        }

        if (this.visitor != null) {
          DiagnosticPosition pos = (DiagnosticPosition) this.visitor.lastVisited;
          if (pos != null) {
            DiagnosticSource source = new DiagnosticSource(this.currentRoot.getSourceFile(), null);
            int linenr = source.getLineNumber(pos.getStartPosition());
            int col = source.getColumnNumber(pos.getStartPosition(), true);
            String line = source.getLine(pos.getStartPosition());

            msg.add("Last visited tree at line " + linenr + " column " + col + ":");
            msg.add(line);
          }
        }

        msg.add("Exception: " + ce.getCause() + "; " + UtilPlume.stackTraceToString(ce.getCause()));
        boolean printClasspath = ce.getCause() instanceof NoClassDefFoundError;
        Throwable cause = ce.getCause().getCause();
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

  ///////////////////////////////////////////////////////////////////////////
  /// Shutdown
  ///

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

  ///////////////////////////////////////////////////////////////////////////
  /// Miscellaneous
  ///

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

  /** True if the git.properties file has been printed. */
  private static boolean gitPropertiesPrinted = false;

  /** Print information about the git repository from which the Checker Framework was compiled. */
  private void printGitProperties() {
    if (gitPropertiesPrinted) {
      return;
    }
    gitPropertiesPrinted = true;

    try (InputStream in = getClass().getResourceAsStream("/git.properties");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in)); ) {
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
    Properties gitProperties = getProperties(getClass(), "/git.properties");
    String version = gitProperties.getProperty("git.build.version");
    if (version != null) {
      return version;
    }
    throw new BugInCF("Could not find the version in git.properties");
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
}
