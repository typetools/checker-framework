package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * Instances of TypeArgumentInference are used to infer the types of method type arguments when no
 * explicit arguments are provided.
 *
 * <p>e.g. If we have a method declaration:
 *
 * <pre>{@code
 * <A,B> B method(A a, B b) {...}
 * }</pre>
 *
 * And an invocation of that method:
 *
 * <pre>{@code
 * method("some Str", 35);
 * }</pre>
 *
 * TypeArgumentInference will determine what the type arguments to type parameters A and B are. In
 * Java, if T(A) = the type argument for a, in the above example T(A) == String and T(B) == Integer
 *
 * <p>For the Checker Framework we also need to infer reasonable annotations for these type
 * arguments. For information on inferring type arguments see JLS chapter 18:
 * https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html
 */
public interface TypeArgumentInference {

  /**
   * Infer the type arguments for the method or constructor invocation given by invocation.
   *
   * @param typeFactory the type factory used to create executableType
   * @param invocation a tree representing the method or constructor invocation for which we are
   *     inferring type arguments
   * @param executableType the declaration type of the invoked method
   * @return the result which includes the inferred type arguments or an error message if they were
   *     not inferred
   */
  InferenceResult inferTypeArgs(
      AnnotatedTypeFactory typeFactory,
      ExpressionTree invocation,
      AnnotatedExecutableType executableType);

  /**
   * Returns true if type argument inference for some method or constructor invocation is currently
   * in progress anywhere on the call stack, that is, if this method is called while a previous,
   * not-yet-returned call to {@link #inferTypeArgs} is running.
   *
   * <p>Code that needs the type of an expression that is only meaningful once some invocation's
   * type arguments are known (for example, the type of an implicitly typed lambda parameter whose
   * lambda is an argument to a still-uninferred invocation) must check this method first.
   * Re-deriving that type via the normal target-type machinery -- which re-invokes method
   * applicability/inference for the invocation, and in turn needs the types of that invocation's
   * receiver and arguments -- can revisit an invocation whose own inference is mid-flight further
   * up the stack (not necessarily the immediately enclosing one) and compute a different,
   * incomplete answer than the one the in-progress inference will eventually settle on.
   *
   * @return true if type argument inference for some invocation is currently in progress
   */
  default boolean isAnyInferenceInProgress() {
    return false;
  }
}
