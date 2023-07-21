package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.javacutil.TypesUtils;

public class InferenceResult {
  @SuppressWarnings("interning:assignment")
  public static @InternedDistinct InferenceResult emptyResult =
      new InferenceResult(Collections.emptyList(), false, false, "");

  public static InferenceResult emptyResult() {
    return emptyResult;
  }

  private final Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> results;
  private final boolean annoInferenceFailed;
  private final boolean uncheckedConversion;
  private final String errorMsg;

  public InferenceResult(
      List<Variable> thetaPrime,
      boolean uncheckedConversion,
      boolean annoInferenceFailed,
      String errorMsg) {
    this.results = convert(thetaPrime);
    this.uncheckedConversion = uncheckedConversion;
    this.annoInferenceFailed = annoInferenceFailed;
    this.errorMsg = errorMsg;
  }

  public Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> getResults() {
    return results;
  }

  public boolean isUncheckedConversion() {
    return uncheckedConversion;
  }

  public boolean isAnnoInferenceFailed() {
    return annoInferenceFailed;
  }

  private static Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> convert(
      List<Variable> variables) {
    Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> map = new HashMap<>();
    for (Variable variable : variables) {
      Map<TypeVariable, AnnotatedTypeMirror> typeMap =
          map.computeIfAbsent(variable.getInvocation(), k -> new HashMap<>());
      typeMap.put(variable.getJavaType(), variable.getInstantiation().getAnnotatedType());
    }
    return map;
  }

  public Map<TypeVariable, AnnotatedTypeMirror> getTypeArgumentsForExpression(
      ExpressionTree expressionTree) {
    if (this == emptyResult || results.isEmpty()) {
      return Collections.emptyMap();
    }
    return results.get(expressionTree);
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public InferenceResult swap(AnnotatedExecutableType methodType, ExpressionTree tree) {
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
