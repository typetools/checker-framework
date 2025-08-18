package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.List;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BoundLocationCriterion implements Criterion {

  private Criterion parentCriterion;
  private final int boundIndex;
  private final int paramIndex;

  public BoundLocationCriterion(BoundLocation boundLoc) {
    this(boundLoc.boundIndex, boundLoc.paramIndex);
  }

  private BoundLocationCriterion(int boundIndex, int paramIndex) {
    this.boundIndex = boundIndex;
    this.paramIndex = paramIndex;

    if (boundIndex != -1) {
      this.parentCriterion = new BoundLocationCriterion(-1, paramIndex);
    } else if (paramIndex != -1) {
      this.parentCriterion = null;
    }
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

    // System.out.printf("BoundLocationCriterion.isSatisfiedBy(%s):%n  leaf=%s (%s)%n", path, leaf,
    // leaf.getClass());

    TreePath parentPath = path.getParentPath();
    if (parentPath == null) {
      return false;
    }

    Tree parent = parentPath.getLeaf();
    if (parent == null) {
      return false;
    }

    boolean returnValue = false;

    // System.out.printf("BoundLocationCriterion.isSatisfiedBy(%s):%n  leaf=%s (%s)%n  parent=%s
    // (%s)%n", path, leaf, leaf.getClass(), parent, parent.getClass());

    // if boundIndex is not null, need to check that this is right bound
    // in parent
    if (boundIndex != -1) {
      if (parent instanceof TypeParameterTree) {
        List<? extends Tree> bounds = ((TypeParameterTree) parent).getBounds();
        int ix = boundIndex;
        if (!bounds.isEmpty() && isInterface((JCExpression) bounds.get(0))) {
          --ix;
        }
        @SuppressWarnings("interning:not.interned") // reference equality check
        boolean foundLeaf = ix < 0 || (ix < bounds.size() && bounds.get(ix) == leaf);
        if (foundLeaf) {
          returnValue = parentCriterion.isSatisfiedBy(parentPath);
        }
      } else if (boundIndex == 0 && leaf instanceof TypeParameterTree) {
        List<? extends Tree> bounds = ((TypeParameterTree) leaf).getBounds();
        if (bounds.isEmpty() || isInterface((JCExpression) bounds.get(0))) {
          // If the bound is implicit (i.e., a missing "extends Object"),
          // then permit the match here.
          returnValue = parentCriterion.isSatisfiedBy(path);
        } else {
          Type type = ((JCExpression) bounds.get(0)).type;
          if (type != null && type.tsym != null && type.tsym.isInterface()) {
            returnValue = parentCriterion.isSatisfiedBy(parentPath);
          }
        }
      }
    } else if (paramIndex != -1) {
      // if paramIndex is not null, need to ensure this present
      // typeparameter tree represents the correct parameter
      if (parent instanceof MethodTree || parent instanceof ClassTree) {
        List<? extends TypeParameterTree> params = null;

        if (parent instanceof MethodTree) {
          params = ((MethodTree) parent).getTypeParameters();
        } else if (parent instanceof ClassTree) {
          params = ((ClassTree) parent).getTypeParameters();
        }

        if (paramIndex < params.size()) {
          @SuppressWarnings("interning:not.interned") // reference equality check
          boolean foundLeaf = params.get(paramIndex) == leaf;
          if (foundLeaf) {
            returnValue = true;
          }
        }
      }
    }

    if (!returnValue) {
      return this.isSatisfiedBy(parentPath);
    } else {
      return true;
    }
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  /**
   * Returns true if the given bound is an interface.
   *
   * @param bound a type bound
   * @return true if the given bound is an interface
   */
  private boolean isInterface(JCExpression bound) {
    Type type = bound.type;
    return type != null && type.tsym != null && type.tsym.isInterface();
  }

  @Override
  public Kind getKind() {
    return Kind.BOUND_LOCATION;
  }

  @Override
  public String toString() {
    return "BoundCriterion: at param index: " + paramIndex + " at bound index: " + boundIndex;
  }
}
