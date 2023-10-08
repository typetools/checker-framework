package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * The entry point for the Resource Leak Checker. This checker is a modifed {@link
 * CalledMethodsChecker} that checks that the must-call obligations of each expression (as computed
 * via the {@link org.checkerframework.checker.mustcall.MustCallChecker} have been fulfilled.
 */
@SupportedOptions({
  "permitStaticOwning",
  "permitInitializationLeak",
  ResourceLeakChecker.COUNT_MUST_CALL,
  ResourceLeakChecker.IGNORED_EXCEPTIONS,
  MustCallChecker.NO_CREATES_MUSTCALLFOR,
  MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP,
  MustCallChecker.NO_RESOURCE_ALIASES
})
@StubFiles("IOUtils.astub")
public class ResourceLeakChecker extends CalledMethodsChecker {

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
  private static final Set<@CanonicalName String> DEFAULT_IGNORED_EXCEPTIONS =
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
          UnsupportedEncodingException.class.getCanonicalName());

  /**
   * Command-line option for controlling which exceptions are ignored.
   *
   * @see #DEFAULT_IGNORED_EXCEPTIONS
   * @see #getIgnoredExceptions()
   */
  public static final String IGNORED_EXCEPTIONS = "resourceLeakIgnoredExceptions";

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

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();

    if (this.processingEnv.getOptions().containsKey(MustCallChecker.NO_CREATES_MUSTCALLFOR)) {
      checkers.add(MustCallNoCreatesMustCallForChecker.class);
    } else {
      checkers.add(MustCallChecker.class);
    }

    return checkers;
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new ResourceLeakVisitor(this);
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

  public Set<@CanonicalName String> getIgnoredExceptions() {
    String ignoredExceptionsOption = getOption(IGNORED_EXCEPTIONS);
    if (ignoredExceptionsOption == null) {
      return DEFAULT_IGNORED_EXCEPTIONS;
    } else {
      String[] exceptions = ignoredExceptionsOption.split(Pattern.quote(","));
      return Arrays.stream(exceptions).map(this::checkCanonicalName).collect(Collectors.toSet());
    }
  }

  private @CanonicalName String checkCanonicalName(String s) {
    // TODO
    return s;
  }
}
