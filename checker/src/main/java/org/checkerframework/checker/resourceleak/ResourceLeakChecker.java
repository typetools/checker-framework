package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * The entry point for the Resource Leak Checker. This checker only counts the number of {@link
 * org.checkerframework.checker.mustcall.qual.MustCall} annotations and defines a set of ignored
 * exceptions. This checker calls the {@link RLCCalledMethodsChecker} as a direct subchecker, which
 * then in turn calls the {@link MustCallChecker} as a subchecker, and afterwards this checker
 * traverses the CFG to check whether all MustCall obligations are fulfilled.
 *
 * <p>The checker hierarchy is: this "empty" RLC &rarr; RLCCalledMethodsChecker &rarr;
 * MustCallChecker
 *
 * <p>The MustCallChecker is a subchecker of the RLCCm checker (instead of a sibling), since we want
 * them to operate on the same CFG (so we can get both a CM and MC store for a given CFG block),
 * which only works if one of them is a subchecker of the other.
 */
@SupportedOptions({
  "permitStaticOwning",
  "permitInitializationLeak",
  ResourceLeakChecker.COUNT_MUST_CALL,
  ResourceLeakChecker.IGNORED_EXCEPTIONS,
  MustCallChecker.NO_CREATES_MUSTCALLFOR,
  MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
  MustCallChecker.NO_RESOURCE_ALIASES,
  ResourceLeakChecker.ENABLE_WPI_FOR_RLC,
  ResourceLeakChecker.ENABLE_RETURNS_RECEIVER
})
public class ResourceLeakChecker extends AggregateChecker {

  /** Creates a ResourceLeakChecker. */
  public ResourceLeakChecker() {}

  /**
   * Command-line option for counting how many must-call obligations were checked by the Resource
   * Leak Checker, and emitting the number after processing all files. Used for generating tables
   * for a research paper. Not of interest to most users.
   */
  public static final String COUNT_MUST_CALL = "countMustCall";

  /**
   * The exception types in this set are ignored in the CFG when determining if a resource leaks
   * along an exceptional path. These kinds of errors fall into a few categories: runtime errors,
   * errors that the JVM can issue on any statement, and errors that can be prevented by running
   * some other CF checker.
   */
  private static final SetOfTypes DEFAULT_IGNORED_EXCEPTIONS =
      SetOfTypes.anyOfTheseNames(
          ImmutableSet.of(
              // Any method call has a CFG edge for Throwable/RuntimeException/Error
              // to represent run-time misbehavior. Ignore it.
              Throwable.class.getCanonicalName(),
              Error.class.getCanonicalName(),
              RuntimeException.class.getCanonicalName(),
              // Use the Nullness Checker to prove this won't happen.
              NullPointerException.class.getCanonicalName(),
              // These errors can't be predicted statically, so ignore them and assume
              // they won't happen.
              ClassCircularityError.class.getCanonicalName(),
              ClassFormatError.class.getCanonicalName(),
              NoClassDefFoundError.class.getCanonicalName(),
              OutOfMemoryError.class.getCanonicalName(),
              // It's not our problem if the Java type system is wrong.
              ClassCastException.class.getCanonicalName(),
              // It's not our problem if the code is going to divide by zero.
              ArithmeticException.class.getCanonicalName(),
              // Use the Index Checker to prevent these errors.
              ArrayIndexOutOfBoundsException.class.getCanonicalName(),
              NegativeArraySizeException.class.getCanonicalName(),
              // Most of the time, this exception is infeasible, as the charset used
              // is guaranteed to be present by the Java spec (e.g., "UTF-8").
              // Eventually, this exclusion could be refined by looking at the charset
              // being requested.
              UnsupportedEncodingException.class.getCanonicalName()));

  /**
   * Command-line option for controlling which exceptions are ignored.
   *
   * @see #DEFAULT_IGNORED_EXCEPTIONS
   * @see #getIgnoredExceptions()
   */
  public static final String IGNORED_EXCEPTIONS = "resourceLeakIgnoredExceptions";

  /**
   * A pattern that matches one or more consecutive commas, optionally preceded and followed by
   * whitespace.
   */
  private static final Pattern COMMAS = Pattern.compile("\\s*(?:" + Pattern.quote(",") + "\\s*)+");

  /**
   * A pattern that matches an exception specifier for the {@link #IGNORED_EXCEPTIONS} option: an
   * optional "=" followed by a qualified name. The whole thing can be padded with whitespace.
   */
  private static final Pattern EXCEPTION_SPECIFIER =
      Pattern.compile(
          "^\\s*" + "(" + Pattern.quote("=") + "\\s*" + ")?" + "(\\w+(?:\\.\\w+)*)" + "\\s*$");

  /**
   * Ordinarily, when the -Ainfer flag is used, whole-program inference is run for every checker and
   * sub-checker. However, the Resource Leak Checker is different. The -Ainfer flag enables the
   * RLC's own (non-WPI) inference mechanism ({@link MustCallInference}). To use WPI in addition to
   * this mechanism for its sub-checkers, use the -AenableWpiForRlc flag, which is intended only for
   * testing and experiments.
   */
  public static final String ENABLE_WPI_FOR_RLC = "enableWpiForRlc";

  /**
   * The Returns Receiver Checker is disabled by default for the Resource Leak Checker, as it adds
   * significant overhead and typically provides little benefit. To enable it, use the
   * -AenableReturnsReceiverForRlc flag.
   */
  public static final String ENABLE_RETURNS_RECEIVER = "enableReturnsReceiverForRlc";

  /**
   * The number of expressions with must-call obligations that were checked. Incremented only if the
   * {@link #COUNT_MUST_CALL} command-line option was supplied.
   */
  /*package-private*/ int numMustCall = 0;

  /**
   * The number of must-call-related errors issued. The count of verified must-call expressions is
   * the difference between this and {@link #numMustCall}.
   */
  private int numMustCallFailed = 0;

  /**
   * The cached set of ignored exceptions parsed from {@link #IGNORED_EXCEPTIONS}. Caching this
   * field prevents the checker from issuing duplicate warnings about missing exception types.
   *
   * @see #getIgnoredExceptions()
   */
  private @MonotonicNonNull SetOfTypes ignoredExceptions = null;

  @Override
  protected Set<Class<? extends SourceChecker>> getSupportedCheckers() {
    Set<Class<? extends SourceChecker>> checkers = new LinkedHashSet<>(1);
    checkers.add(RLCCalledMethodsChecker.class);

    return checkers;
  }

  @Override
  public void reportError(
      @Nullable Object source, @CompilerMessageKey String messageKey, Object... args) {
    if (messageKey.equals("required.method.not.called")) {
      // This is safe because of the message key.
      String qualifiedTypeName = (String) args[1];
      // Only count classes in the JDK, not user-defined classes.
      if (MustCallConsistencyAnalyzer.isJdkClass(qualifiedTypeName)) {
        numMustCallFailed++;
      }
    }
    super.reportError(source, messageKey, args);
  }

  @Override
  public void typeProcessingOver() {
    if (hasOption(COUNT_MUST_CALL)) {
      message(Diagnostic.Kind.WARNING, "Found %d must call obligation(s).%n", numMustCall);
      message(
          Diagnostic.Kind.WARNING,
          "Successfully verified %d must call obligation(s).%n",
          numMustCall - numMustCallFailed);
    }
    super.typeProcessingOver();
  }

  /**
   * Returns the set of exceptions that should be ignored. This set comes from the {@link
   * #IGNORED_EXCEPTIONS} option if it was provided, or {@link #DEFAULT_IGNORED_EXCEPTIONS} if not.
   *
   * @return the set of exceptions to ignore
   */
  public SetOfTypes getIgnoredExceptions() {
    SetOfTypes result = ignoredExceptions;
    if (result == null) {
      String ignoredExceptionsOptionValue = getOption(IGNORED_EXCEPTIONS);
      result =
          ignoredExceptionsOptionValue == null
              ? DEFAULT_IGNORED_EXCEPTIONS
              : parseIgnoredExceptions(ignoredExceptionsOptionValue);
      ignoredExceptions = result;
    }
    return result;
  }

  /**
   * Parse the argument given for the {@link #IGNORED_EXCEPTIONS} option. Warnings will be issued
   * for any problems in the argument, for instance if any of the named exceptions cannot be found.
   *
   * @param ignoredExceptionsOptionValue the value given for {@link #IGNORED_EXCEPTIONS}
   * @return the set of ignored exceptions
   */
  protected SetOfTypes parseIgnoredExceptions(String ignoredExceptionsOptionValue) {
    String[] exceptions = COMMAS.split(ignoredExceptionsOptionValue);
    List<SetOfTypes> sets = new ArrayList<>();
    for (String e : exceptions) {
      SetOfTypes set = parseExceptionSpecifier(e, ignoredExceptionsOptionValue);
      if (set != null) {
        sets.add(set);
      }
    }
    return SetOfTypes.union(sets.toArray(new SetOfTypes[0]));
  }

  /**
   * Parse a single exception specifier from the {@link #IGNORED_EXCEPTIONS} option and issue
   * warnings if it does not parse. See {@link #EXCEPTION_SPECIFIER} for a description of the
   * syntax.
   *
   * @param exceptionSpecifier the exception specifier to parse
   * @param ignoredExceptionsOptionValue the whole value of the {@link #IGNORED_EXCEPTIONS} option;
   *     only used for error reporting
   * @return the parsed set of types, or null if the value does not parse
   */
  @SuppressWarnings({
    // user input might not be a legal @CanonicalName, but it should be safe to pass to
    // `SetOfTypes.anyOfTheseNames`
    "signature:type.arguments.not.inferred",
  })
  protected @Nullable SetOfTypes parseExceptionSpecifier(
      String exceptionSpecifier, String ignoredExceptionsOptionValue) {
    Matcher m = EXCEPTION_SPECIFIER.matcher(exceptionSpecifier);
    if (m.matches()) {
      @Nullable String equalsSign = m.group(1);
      String qualifiedName = m.group(2);

      if (qualifiedName.equalsIgnoreCase("default")) {
        return DEFAULT_IGNORED_EXCEPTIONS;
      }
      TypeMirror type = checkCanonicalName(qualifiedName);
      if (type == null) {
        // There is a chance that the user named a real type, but the class is not
        // accessible for some reason. We'll issue a warning (in case this was a typo) but
        // add the type as ignored anyway (in case it's just an inaccessible type).
        //
        // Note that if the user asked to ignore subtypes of this exception, this code won't
        // do it because we can't know what those subtypes are. We have to treat this as if
        // it were "=qualifiedName" even if no equals sign was provided.
        message(
            Diagnostic.Kind.WARNING,
            "The exception '%s' appears in the -A%s=%s option, but it does not seem to exist",
            exceptionSpecifier,
            IGNORED_EXCEPTIONS,
            ignoredExceptionsOptionValue);
        return SetOfTypes.anyOfTheseNames(ImmutableSet.of(qualifiedName));
      } else {
        return equalsSign == null ? SetOfTypes.allSubtypes(type) : SetOfTypes.singleton(type);
      }
    } else if (!exceptionSpecifier.trim().isEmpty()) {
      message(
          Diagnostic.Kind.WARNING,
          "The string '%s' appears in the -A%s=%s option,"
              + " but it is not a legal exception specifier",
          exceptionSpecifier,
          IGNORED_EXCEPTIONS,
          ignoredExceptionsOptionValue);
    }
    return null;
  }

  /**
   * Check if the given String refers to an actual type.
   *
   * @param s any string
   * @return the referenced type, or null if it does not exist
   */
  @SuppressWarnings({
    "signature:argument", // `s` is not a qualified name, but we pass it to getTypeElement
  })
  protected @Nullable TypeMirror checkCanonicalName(String s) {
    TypeElement elem = getProcessingEnvironment().getElementUtils().getTypeElement(s);
    if (elem == null) {
      return null;
    }
    return types.getDeclaredType(elem);
  }

  @Override
  public NavigableSet<String> getSuppressWarningsPrefixes() {
    NavigableSet<String> result = super.getSuppressWarningsPrefixes();
    result.add("builder");
    return result;
  }
}
