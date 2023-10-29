package org.checkerframework.checker.calledmethods;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.code.Type;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.accumulation.AccumulationAnalysis;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The analysis for the Called Methods Checker. The analysis is specialized to ignore certain
 * exception types; see {@link #isIgnoredExceptionType(TypeMirror)}.
 */
public class CalledMethodsAnalysis extends AccumulationAnalysis {

  /**
   * The fully-qualified names of the exception types that are ignored by this checker when
   * computing dataflow stores.
   */
  protected static final Set<@CanonicalName String> ignoredExceptionTypes =
      ImmutableSet.of(
          // Use the Nullness Checker instead.
          NullPointerException.class.getCanonicalName(),
          // Ignore run-time errors, which cannot be predicted statically. Doing
          // so is unsound in the sense that they could still occur - e.g., the
          // program could run out of memory - but if they did, the checker's
          // results would be useless anyway.
          Error.class.getCanonicalName(),
          RuntimeException.class.getCanonicalName());

  /**
   * Creates a new {@code CalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected CalledMethodsAnalysis(
      BaseTypeChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  /**
   * Ignore exceptional control flow due to ignored exception types.
   *
   * @param exceptionType exception type
   * @return {@code true} if {@code exceptionType} is a member of {@link #ignoredExceptionTypes},
   *     {@code false} otherwise
   */
  @Override
  protected boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ignoredExceptionTypes.contains(
        ((Type) exceptionType).tsym.getQualifiedName().toString());
  }
}
