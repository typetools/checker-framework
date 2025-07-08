package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

public class FieldCriterion implements Criterion {

  public final String varName;
  public final boolean isDeclaration;
  public final Criterion varCriterion;
  public final Criterion notInMethodCriterion;

  public FieldCriterion(String varName) {
    this(varName, false);
  }

  public FieldCriterion(String varName, boolean isDeclaration) {
    this.varName = varName;
    this.isDeclaration = isDeclaration;
    this.varCriterion = Criteria.is(Tree.Kind.VARIABLE, varName);
    this.notInMethodCriterion = Criteria.notInMethod();
  }

  @Override
  public boolean isSatisfiedBy(TreePath path, Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(TreePath path) {
    if (path == null || (isDeclaration && !(path.getLeaf() instanceof VariableTree))) {
      return false;
    }

    if (varCriterion.isSatisfiedBy(path) && notInMethodCriterion.isSatisfiedBy(path)) {
      return true;
    } else {
      return this.isSatisfiedBy(path.getParentPath());
    }
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.FIELD;
  }

  @Override
  public String toString() {
    return "FieldCriterion: " + varName;
  }
}
