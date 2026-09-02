package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
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
   * If {@code param} is an implicitly typed lambda parameter whose type an in-progress inference
   * has already determined, returns that type. Otherwise, returns null.
   *
   * <p>If an already completed inference, determined the type of the lambda parameter, null is
   * returned.
   *
   * @param param an element that might be an implicitly typed lambda parameter
   * @return the type of {@code param} as computed by an in-progress inference, or null
   */
  default @Nullable AnnotatedTypeMirror getLambdaParameterType(VariableElement param) {
    return null;
  }
}
