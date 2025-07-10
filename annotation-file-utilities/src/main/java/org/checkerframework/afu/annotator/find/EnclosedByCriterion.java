package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/**
 * Represents the criterion that a program element is enclosed (directly or indirect) by a program
 * element of a certain type.
 */
final class EnclosedByCriterion implements Criterion {

  private final Tree.Kind kind;

  EnclosedByCriterion(Tree.Kind kind) {
    this.kind = kind;
  }

  @Override
  public Kind getKind() {
    return Kind.ENCLOSED_BY;
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

    if (path == null) {
      return false;
    }

    for (Tree tree : path) {
      if (tree.getKind() == kind) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public String toString() {
    return "enclosed by '" + kind + "'";
  }
}
