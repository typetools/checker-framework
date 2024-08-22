package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.javacutil.TypesUtils;

/** The result of type argument inferrece. */
public class InferenceResult {

  /** An empty inference result. */
  @SuppressWarnings("interning:assignment")
  private static final @InternedDistinct InferenceResult emptyResult =
      new InferenceResult(Collections.emptyList(), false, false, "");

  /**
   * Returns an empty inference result.
   *
   * @return an empty inference result
   */
  public static InferenceResult emptyResult() {
    return emptyResult;
  }

  /**
   * A mapping from a tree that needs type argument inference to a map from type parameter to its
   * inferred annotated type argument. If inference failed, this map will be empty.
   */
  private final Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> results;

  /**
   * If true, then type argument inference failed because an annotated type could not be inferred.
   */
  private final boolean annoInferenceFailed;

  /** Whether unchecked conversion was necessary to infer the type arguments. */
  private final boolean uncheckedConversion;

  /** Whether inference crashed. */
  private final boolean inferenceCrashed;

  /** If {@code annoInferenceFailed}, then this is the error message to report to the user. */
  private final String errorMsg;

  /**
   * Creates an inference result.
   *
   * @param variables instantiated variables
   * @param uncheckedConversion where unchecked conversion was required to infer the type arguments
   * @param annoInferenceFailed whether inference failed because of annotations
   * @param errorMsg message to report to users if inference failed
   */
  public InferenceResult(
      Collection<Variable> variables,
      boolean uncheckedConversion,
      boolean annoInferenceFailed,
      String errorMsg) {
    this(variables, uncheckedConversion, annoInferenceFailed, false, errorMsg);
  }

  /**
   * Creates an inference result.
   *
   * @param variables instantiated variables
   * @param uncheckedConversion where unchecked conversion was required to infer the type arguments
   * @param annoInferenceFailed whether inference failed because of annotations
   * @param inferenceCrashed the type argument inference code crashed
   * @param errorMsg message to report to users if inference failed
   */
  public InferenceResult(
      Collection<Variable> variables,
      boolean uncheckedConversion,
      boolean annoInferenceFailed,
      boolean inferenceCrashed,
      String errorMsg) {
    this.results = convert(variables);
    this.uncheckedConversion = uncheckedConversion;
    this.annoInferenceFailed = annoInferenceFailed;
    this.errorMsg = errorMsg;
    this.inferenceCrashed = inferenceCrashed;
  }

  /**
   * A mapping from a tree that needs type argument inference to a map from type parameter to its
   * inferred annotated type argument. If inference failed, this map will be empty.
   *
   * @return mapping from a tree that needs type argument inference to a map from type parameter to
   *     its inferred annotated type argument
   */
  public Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> getResults() {
    return results;
  }

  /**
   * Whether unchecked conversion was necessary to infer the type arguments.
   *
   * @return whether unchecked conversion was necessary to infer the type arguments
   */
  public boolean isUncheckedConversion() {
    return uncheckedConversion;
  }

  /**
   * Whether type argument inference failed because an annotated type could not be inferred.
   *
   * @return Whether type argument inference failed because an annotated type could not be inferred
   */
  public boolean inferenceFailed() {
    return annoInferenceFailed;
  }

  /**
   * Whether inference crashed.
   *
   * @return whether inference crashed
   */
  public boolean inferenceCrashed() {
    return inferenceCrashed;
  }

  /**
   * Convert the instantiated variables to a map from expression tree to a map from type variable to
   * its type argument.
   *
   * @param variables instantiated variables
   * @return a map from expression tree to a map from type variable to its type argument
   */
  private static Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> convert(
      Collection<Variable> variables) {
    Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> map = new HashMap<>();
    for (Variable variable : variables) {
      Map<TypeVariable, AnnotatedTypeMirror> typeMap =
          map.computeIfAbsent(variable.getInvocation(), k -> new HashMap<>());
      typeMap.put(variable.getJavaType(), variable.getInstantiation().getAnnotatedType());
    }
    return map;
  }

  /**
   * Returns a mapping from type variable to its type argument for the {@code expressionTree}.
   *
   * @param expressionTree a tree for which type arguments were inferred
   * @return a mapping from type variable to its type argument for the {@code expressionTree}
   */
  public Map<TypeVariable, AnnotatedTypeMirror> getTypeArgumentsForExpression(
      ExpressionTree expressionTree) {
    if (this == emptyResult || results.isEmpty()) {
      return Collections.emptyMap();
    }
    return results.get(expressionTree);
  }

  /**
   * An error message to report to the user.
   *
   * @return an error message to report to the user
   */
  public String getErrorMsg() {
    return errorMsg;
  }

  /**
   * Switch the {@link TypeVariable}s in {@code results} with the {@code TypeVariable}s in {@code
   * methodType} so that the {@code TypeVariable}s in the result are {@code .equals}. {@link
   * TypesUtils#areSame(TypeVariable, TypeVariable)} is used to decide which type variables to swap.
   *
   * @param methodType annotated method type
   * @param tree method invocation tree
   * @return this
   */
  /* package-private */ InferenceResult swapTypeVariables(
      AnnotatedExecutableType methodType, ExpressionTree tree) {
    Map<TypeVariable, AnnotatedTypeMirror> map = results.get(tree);
    for (AnnotatedTypeVariable tv : methodType.getTypeVariables()) {
      TypeVariable typeVariable = tv.getUnderlyingType();
      for (TypeVariable t : new HashSet<>(map.keySet())) {
        if (TypesUtils.areSame(t, typeVariable)) {
          map.put(typeVariable, map.remove(t));
        }
      }
    }
    return this;
  }
}
