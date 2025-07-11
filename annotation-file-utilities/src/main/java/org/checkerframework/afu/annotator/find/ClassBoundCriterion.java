package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.scenelib.el.BoundLocation;

public class ClassBoundCriterion implements Criterion {

  private final String className;
  public final BoundLocation boundLoc;
  private final Criterion notInMethodCriterion;
  private final Criterion boundLocCriterion;

  public ClassBoundCriterion(String className, BoundLocation boundLoc) {
    this.className = className;
    this.boundLoc = boundLoc;
    this.notInMethodCriterion = Criteria.notInMethod();
    this.boundLocCriterion = Criteria.atBoundLocation(boundLoc);
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

    return boundLocCriterion.isSatisfiedBy(path) && notInMethodCriterion.isSatisfiedBy(path);
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.CLASS_BOUND;
  }

  @Override
  public String toString() {
    return "ClassBoundCriterion: for " + className + " at " + boundLoc;
  }
}
