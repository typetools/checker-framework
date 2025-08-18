package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MethodBoundCriterion implements Criterion {

  private final String methodName;
  public final BoundLocation boundLoc;
  private final Criterion sigMethodCriterion;
  private final Criterion boundLocationCriterion;

  public MethodBoundCriterion(String methodName, BoundLocation boundLoc) {
    this.methodName = methodName;
    this.boundLoc = boundLoc;
    this.sigMethodCriterion = Criteria.inMethod(methodName);
    this.boundLocationCriterion = Criteria.atBoundLocation(boundLoc);
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
    return sigMethodCriterion.isSatisfiedBy(path) && boundLocationCriterion.isSatisfiedBy(path);
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.METHOD_BOUND;
  }

  @Override
  public String toString() {
    return "MethodBoundCriterion: method: " + methodName + " bound boundLoc: " + boundLoc;
  }
}
