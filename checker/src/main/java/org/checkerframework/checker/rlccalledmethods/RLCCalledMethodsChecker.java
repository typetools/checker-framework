package org.checkerframework.checker.rlccalledmethods;

import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * The CalledMethodsChecker used as a subchecker in the ResourceLeakChecker and never independently.
 */
public class RLCCalledMethodsChecker extends CalledMethodsChecker {

  /** Creates a RLCCalledMethodsChecker. */
  public RLCCalledMethodsChecker() {
    // super();
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new RLCCalledMethodsVisitor(this);
  }

  // /**
  //  * Get the set of exceptions that should be ignored. This set comes from the {@link
  //  * #IGNORED_EXCEPTIONS} option if it was provided, or {@link #DEFAULT_IGNORED_EXCEPTIONS} if
  // not.
  //  *
  //  * @return the set of exceptions to ignore
  //  */
  // public SetOfTypes getIgnoredExceptions() {
  //   SetOfTypes result = ignoredExceptions;
  //   if (result == null) {
  //     String ignoredExceptionsOptionValue = getOption(IGNORED_EXCEPTIONS);
  //     result =
  //         ignoredExceptionsOptionValue == null
  //             ? DEFAULT_IGNORED_EXCEPTIONS
  //             : parseIgnoredExceptions(ignoredExceptionsOptionValue);
  //     ignoredExceptions = result;
  //   }
  //   return result;
  // }

  // /**
  //  * The cached set of ignored exceptions parsed from {@link #IGNORED_EXCEPTIONS}. Caching this
  //  * field prevents the checker from issuing duplicate warnings about missing exception types.
  //  *
  //  * @see #getIgnoredExceptions()
  //  */
  // private @MonotonicNonNull SetOfTypes ignoredExceptions = null;

  // /**
  //  * The exception types in this set are ignored in the CFG when determining if a resource leaks
  //  * along an exceptional path. These kinds of errors fall into a few categories: runtime errors,
  //  * errors that the JVM can issue on any statement, and errors that can be prevented by running
  //  * some other CF checker.
  //  */
  // private static final SetOfTypes DEFAULT_IGNORED_EXCEPTIONS =
  //     SetOfTypes.anyOfTheseNames(
  //         ImmutableSet.of(
  //             // Any method call has a CFG edge for Throwable/RuntimeException/Error
  //             // to represent run-time misbehavior. Ignore it.
  //             Throwable.class.getCanonicalName(),
  //             Error.class.getCanonicalName(),
  //             RuntimeException.class.getCanonicalName(),
  //             // Use the Nullness Checker to prove this won't happen.
  //             NullPointerException.class.getCanonicalName(),
  //             // These errors can't be predicted statically, so ignore them and assume
  //             // they won't happen.
  //             ClassCircularityError.class.getCanonicalName(),
  //             ClassFormatError.class.getCanonicalName(),
  //             NoClassDefFoundError.class.getCanonicalName(),
  //             OutOfMemoryError.class.getCanonicalName(),
  //             // It's not our problem if the Java type system is wrong.
  //             ClassCastException.class.getCanonicalName(),
  //             // It's not our problem if the code is going to divide by zero.
  //             ArithmeticException.class.getCanonicalName(),
  //             // Use the Index Checker to prevent these errors.
  //             ArrayIndexOutOfBoundsException.class.getCanonicalName(),
  //             NegativeArraySizeException.class.getCanonicalName(),
  //             // Most of the time, this exception is infeasible, as the charset used
  //             // is guaranteed to be present by the Java spec (e.g., "UTF-8").
  //             // Eventually, this exclusion could be refined by looking at the charset
  //             // being requested.
  //             UnsupportedEncodingException.class.getCanonicalName()));

  // /**
  //  * Command-line option for controlling which exceptions are ignored.
  //  *
  //  * @see #DEFAULT_IGNORED_EXCEPTIONS
  //  * @see #getIgnoredExceptions()
  //  */
  // public static final String IGNORED_EXCEPTIONS = "resourceLeakIgnoredExceptions";
}
