package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.afu.annotator.scanner.InstanceOfScanner;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfCriterion implements Criterion {

  private final String methodName;
  private final RelativeLocation loc;

  public InstanceOfCriterion(String methodName, RelativeLocation loc) {
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
      Criteria.dbug.debug("return null");
      return false;
    }

    Tree leaf = path.getLeaf();

    if (Criteria.dbug.isEnabled()) {
      Criteria.dbug.debug("InstanceOfCriterion.isSatisfiedBy:%n");
      Criteria.dbug.debug("  this: %s%n", this);
      Criteria.dbug.debug("  leaf: %s%n", Main.treeToString(leaf));
      Criteria.dbug.debug("  kind: %s%n", leaf.getKind());
      Criteria.dbug.debug("  class: %s%n", leaf.getClass());
    }

    TreePath parentPath = path.getParentPath();
    if (parentPath == null) {
      Criteria.dbug.debug("return: parent path null%n");
      return false;
    }

    Tree parent = parentPath.getLeaf();
    if (parent == null) {
      Criteria.dbug.debug("return: parent null%n");
      return false;
    }

    if (parent instanceof InstanceOfTree) {
      InstanceOfTree instanceOfTree = (InstanceOfTree) parent;
      @SuppressWarnings("interning:not.interned") // reference equality check
      boolean foundLeaf = leaf == instanceOfTree.getType();
      if (!foundLeaf) {
        Criteria.dbug.debug("return: not type part of instanceof%n");
        return false;
      }

      int indexInSource = InstanceOfScanner.indexOfInstanceOfTree(path, parent);
      Criteria.dbug.debug("return source: %d%n", indexInSource);
      boolean b;
      if (loc.isBytecodeOffset()) {
        int indexInClass = InstanceOfScanner.getMethodInstanceOfIndex(methodName, loc.offset);
        Criteria.dbug.debug("return class: %d%n", indexInClass);
        b = (indexInSource == indexInClass);
      } else {
        b = (indexInSource == loc.index);
        Criteria.dbug.debug("return loc.index: %d%n", loc.index);
      }
      Criteria.dbug.debug("return new: %b", b);
      return b;
    } else {
      boolean b = this.isSatisfiedBy(path.getParentPath());
      Criteria.dbug.debug("return parent: %b%n", b);
      return b;
    }
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.INSTANCE_OF;
  }

  @Override
  public String toString() {
    return "InstanceOfCriterion: in method: " + methodName + " location: " + loc;
  }
}
