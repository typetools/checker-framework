package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.List;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.io.ASTPath;

@SuppressWarnings("MissingSummary") // TODO
public class IntersectionTypeLocationCriterion implements Criterion {
  private final int typeIndex;

  public IntersectionTypeLocationCriterion(RelativeLocation loc) {
    typeIndex = loc.type_index;
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
    TreePath parentPath = path.getParentPath();
    if (parentPath != null) {
      Tree parent = parentPath.getLeaf();
      if (parent instanceof IntersectionTypeTree) {
        IntersectionTypeTree itt = (IntersectionTypeTree) parent;
        List<? extends Tree> bounds = itt.getBounds();
        Tree leaf = path.getLeaf();
        if (typeIndex < bounds.size() && leaf == bounds.get(typeIndex)) {
          return true;
        }
      }
    }
    Tree.Kind kind = path.getLeaf().getKind();
    if (ASTPath.isTypeKind(kind) || kind == Tree.Kind.MEMBER_SELECT) {
      return isSatisfiedBy(path.getParentPath());
    }
    return false;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.INTERSECT_LOCATION;
  }

  @Override
  public String toString() {
    return "IntersectionTypeLocation: at type index: " + typeIndex;
  }
}
