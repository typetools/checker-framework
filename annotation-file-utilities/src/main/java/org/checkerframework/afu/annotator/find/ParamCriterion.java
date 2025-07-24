package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.List;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ParamCriterion implements Criterion {

  private final String methodName;
  private final Integer paramPos;

  public ParamCriterion(String methodName, Integer pos) {
    this.methodName = methodName.substring(0, methodName.indexOf(")") + 1);
    this.paramPos = pos;
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path, @FindDistinct Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path) {

    if (path == null) {
      return false;
    }

    // no inner type location, want to annotate outermost type
    // i.e.   @Nullable List list;
    //        @Nullable List<String> list;
    Tree leaf = path.getLeaf();
    if (leaf instanceof VariableTree) {
      Tree parent = path.getParentPath().getLeaf();
      List<? extends VariableTree> params;
      switch (parent.getKind()) {
        case METHOD:
          params = ((MethodTree) parent).getParameters();
          break;
        case LAMBDA_EXPRESSION:
          params = ((LambdaExpressionTree) parent).getParameters();
          break;
        default:
          params = null;
          break;
      }
      return params != null && params.size() > paramPos && params.get(paramPos).equals(leaf);
    }

    return this.isSatisfiedBy(path.getParentPath());
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    // This can probably return true?  No annotations go on it directly,
    // so rely on some other Criterion in the Criteria to return true.
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.PARAM;
  }

  @Override
  public String toString() {
    return "ParamCriterion for method: " + methodName + " at position: " + paramPos;
  }
}
