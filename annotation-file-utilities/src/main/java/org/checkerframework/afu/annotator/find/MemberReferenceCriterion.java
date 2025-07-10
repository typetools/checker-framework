package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.annotator.scanner.MemberReferenceScanner;
import org.checkerframework.afu.scenelib.el.RelativeLocation;

public class MemberReferenceCriterion implements Criterion {
  private final String methodName;
  private final RelativeLocation loc;

  public MemberReferenceCriterion(String methodName, RelativeLocation loc) {
    this.methodName = methodName;
    this.loc = loc;
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

    Tree leaf = path.getLeaf();

    if (leaf instanceof MemberReferenceTree) {
      int indexInSource = MemberReferenceScanner.indexOfMemberReferenceTree(path, leaf);
      boolean b;
      if (loc.isBytecodeOffset()) {
        int indexInClass = MemberReferenceScanner.getMemberReferenceIndex(methodName, loc.offset);
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
    return Kind.METHOD_REFERENCE;
  }

  @Override
  public String toString() {
    return "MemberReferenceCriterion: in method: " + methodName + " location: " + loc;
  }
}
