package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;
import org.checkerframework.afu.annotator.scanner.TreePathUtil;
import org.checkerframework.afu.scenelib.el.TypeIndexLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A criterion to find a given extends or implements clause. */
public class ExtImplsLocationCriterion implements Criterion {

  private final String classname;
  private final Integer index;

  /**
   * @param classname the class name; for debugging purposes only, not used to constrain
   * @param tyLoc -1 for an extends clause, $ge; 0 for the zero-based implements clause
   */
  public ExtImplsLocationCriterion(String classname, TypeIndexLocation tyLoc) {
    this.classname = classname;
    this.index = tyLoc.typeIndex;
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

    // System.out.printf("ExtImplsLocationCriterion.isSatisfiedBy(%s):%n  leaf=%s (%s)%n", path,
    // leaf, leaf.getClass());

    TreePath parentPath = path.getParentPath();
    if (parentPath == null) {
      return false;
    }

    Tree parent = parentPath.getLeaf();
    if (parent == null) {
      return false;
    }

    // System.out.printf("ExtImplsLocationCriterion.isSatisfiedBy(%s):%n  leaf=%s (%s)%n  parent=%s
    // (%s)%n", path, leaf, leaf.getClass(), parent, parent.getClass());

    if (index == -1 && leaf.getKind() == Tree.Kind.CLASS) {
      return ((JCTree.JCClassDecl) leaf).getExtendsClause() == null;
    }
    if (TreePathUtil.hasClassKind(parent)) {
      ClassTree ct = (ClassTree) parent;

      if (index == -1) {
        Tree ext = ct.getExtendsClause();
        if (ext == leaf) {
          return true;
        }
      } else {
        List<? extends Tree> impls = ct.getImplementsClause();
        if (index < impls.size() && impls.get(index) == leaf) {
          return true;
        }
      }
    }

    return this.isSatisfiedBy(parentPath);
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  /**
   * Returns the index of this.
   *
   * @return the index of this
   */
  public Integer getIndex() {
    return index;
  }

  @Override
  public Kind getKind() {
    return Kind.EXTIMPLS_LOCATION;
  }

  @Override
  public String toString() {
    return "ExtImplsLocationCriterion: class " + classname + " at type index: " + index;
  }
}
