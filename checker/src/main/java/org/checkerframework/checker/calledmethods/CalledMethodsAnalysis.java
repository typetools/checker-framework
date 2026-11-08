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
   * The canonical name of {@code java.lang.MatchException}. {@code MatchException.class} is not
   * available before Java 21.
   *
   * <p>An exhaustive switch throws a {@code MatchException} for a value that none of its cases
   * matches. Examples are an instance of a class that was added to a sealed hierarchy after the
   * switch was compiled, and a record whose component is null where a nested pattern, such as
   * {@code Circle c} in {@code case R(Circle c)}, does not match null. Pattern matching also throws
   * a {@code MatchException} if a record component accessor throws an exception.
   */
  @SuppressWarnings("signature:assignment") // the string literal is a canonical name
  private static final @CanonicalName String MATCH_EXCEPTION_NAME = "java.lang.MatchException";

  /**
   * The fully-qualified names of the exception types that are ignored by this checker when
   * computing dataflow stores.
   */
  protected static final Set<@CanonicalName String> ignoredExceptionTypes =
      ImmutableSet.of(
          // Use the Nullness Checker instead.
          NullPointerException.class.getCanonicalName(),
          // Ignore run-time errors, which cannot be predicted statically. Doing
          // so is unsound in the sense that they could still occur -- e.g., the
          // program could run out of memory -- but if they did, the checker's
          // results would be useless anyway.
          Error.class.getCanonicalName(),
          RuntimeException.class.getCanonicalName(),
          // An exhaustive switch throws this for a value that no case matches.
          MATCH_EXCEPTION_NAME);

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
