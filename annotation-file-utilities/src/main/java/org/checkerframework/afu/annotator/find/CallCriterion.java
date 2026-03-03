package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.annotator.scanner.MethodCallScanner;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CallCriterion implements Criterion {
  private final String methodName;
  private final RelativeLocation loc;

  public CallCriterion(String methodName, RelativeLocation loc) {
    this.methodName = methodName;
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

    if (leaf instanceof MethodInvocationTree) {
      int indexInSource = MethodCallScanner.indexOfMethodCallTree(path, leaf);
      boolean b;
      if (loc.isBytecodeOffset()) {
        int indexInClass = MethodCallScanner.getMethodCallIndex(methodName, loc.offset);
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
    // This can probably return true?  No annotations go on it directly,
    // so rely on some other Criterion in the Criteria to return true.
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.METHOD_CALL;
  }

  @Override
  public String toString() {
    return "CallCriterion: in method: " + methodName + " location: " + loc;
  }
}
