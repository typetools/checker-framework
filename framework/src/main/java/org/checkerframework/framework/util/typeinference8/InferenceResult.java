package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference8.types.Variable;

public class InferenceResult {
  public static InferenceResult emptyResult = new InferenceResult(Collections.emptyList(), false);

  public static InferenceResult emptyResult() {
    return emptyResult;
  }

  private final Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> results;
  private final boolean annoInferenceFailed;

  public InferenceResult(List<Variable> thetaPrime, boolean annoInferenceFailed) {
    this.results = convert(thetaPrime);
    this.annoInferenceFailed = annoInferenceFailed;
  }

  public Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> getResults() {
    return results;
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
    return results.get(expressionTree);
  }
}
