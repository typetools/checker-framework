package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.code.Type;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * This variant of CFAnalysis extends the set of ignored exception types to include all those
 * ignored by the {@link MustCallConsistencyAnalyzer}. See {@link
 * MustCallConsistencyAnalyzer#ignoredExceptionTypes}.
 */
public class ResourceLeakAnalysis extends CalledMethodsAnalysis {

  /**
   * The exception types in this set are ignored in the CFG when determining if a resource leaks
   * along an exceptional path. These kinds of errors fall into a few categories: runtime errors,
   * errors that the JVM can issue on any statement, and errors that can be prevented by running
   * some other CF checker.
   *
   * <p>Package-private to permit access from {@link ResourceLeakAnalysis}.
   */
  /*package-private*/ static final Set<@CanonicalName String> ignoredExceptionTypes =
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
   * Creates a new {@code CalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected ResourceLeakAnalysis(
      BaseTypeChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return super.isIgnoredExceptionType(exceptionType)
        || ignoredExceptionTypes.contains(
            ((Type) exceptionType).tsym.getQualifiedName().toString());
  }
}
