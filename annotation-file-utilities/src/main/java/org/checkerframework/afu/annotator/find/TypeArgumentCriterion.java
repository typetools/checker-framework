package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeArgumentCriterion implements Criterion {
  private final String methodName;
  private final RelativeLocation loc;

  public TypeArgumentCriterion(String methodName, RelativeLocation loc) {
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
    if (path == null || path.getParentPath() == null) {
      return false;
    }

    TreePath parentPath = path.getParentPath();
    Tree parent = parentPath.getLeaf();
    List<? extends Tree> typeArgs;

    switch (parent.getKind()) {
      case MEMBER_REFERENCE:
        typeArgs = ((JCTree.JCMemberReference) parent).getTypeArguments();
        break;
      case METHOD_INVOCATION:
        typeArgs = ((JCTree.JCMethodInvocation) parent).getTypeArguments();
        break;
      default:
        return isSatisfiedBy(parentPath);
    }

    return typeArgs != null
        && loc.index >= 0
        && loc.index < typeArgs.size()
        && typeArgs.get(loc.index) == path.getLeaf();
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.TYPE_ARGUMENT;
  }

  @Override
  public String toString() {
    return "TypeArgumentCriterion: in method: " + methodName + " location: " + loc;
  }
}
