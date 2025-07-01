package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.scenelib.io.ASTPath;

/**
 * Represents the criterion that a program element is not enclosed by any method (i.e. it's a field,
 * class type parameter, etc.).
 */
final class NotInMethodCriterion implements Criterion {

  @Override
  public Kind getKind() {
    return Kind.NOT_IN_METHOD;
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
    do {
      Tree.Kind kind = path.getLeaf().getKind();
      if (kind == Tree.Kind.METHOD) {
        return false;
      }
      if (ASTPath.isClassEquiv(kind)) {
        return true;
      }
      path = path.getParentPath();
    } while (path != null && path.getLeaf() != null);

    return true;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public String toString() {
    return "not in method";
  }
}
