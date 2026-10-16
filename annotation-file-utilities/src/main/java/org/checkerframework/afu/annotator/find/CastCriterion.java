package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.annotator.scanner.CastScanner;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Criterion for being a specific type cast expression. */
public class CastCriterion implements Criterion {

  private final String methodName;
  private final RelativeLocation loc;

  public CastCriterion(String methodName, RelativeLocation loc) {
    this.methodName = methodName.substring(0, methodName.lastIndexOf(')') + 1);
    this.loc = loc;
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

    Tree leaf = path.getLeaf();

    if (leaf instanceof TypeCastTree) {
      int indexInSource = CastScanner.indexOfCastTree(path, leaf);
      boolean b;
      if (loc.isBytecodeOffset()) {
        int indexInClass = CastScanner.getMethodCastIndex(methodName, loc.offset);
        b = (indexInSource == indexInClass);
      } else {
        b = (indexInSource == loc.index);
      }
      return b;

    } else {
      boolean b = this.isSatisfiedBy(path.getParentPath());
      return b;
    }
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  /**
   * Returns the location of this.
   *
   * @return the location of this
   */
  public RelativeLocation getLocation() {
    return loc;
  }

  @Override
  public Kind getKind() {
    return Kind.CAST;
  }

  @Override
  public String toString() {
    return "CastCriterion: in method: " + methodName + " location: " + loc;
  }
}
