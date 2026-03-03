package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.annotator.scanner.NewScanner;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Criterion for being a specific object creation expression. */
public class NewCriterion implements Criterion {

  private final String methodName;
  private final Criterion inMethodCriterion;

  private final RelativeLocation loc;

  public NewCriterion(String methodName, RelativeLocation loc) {
    this.methodName = methodName.substring(0, methodName.lastIndexOf(')') + 1);

    if (!(methodName.startsWith("init for field")
        || methodName.startsWith("static init number")
        || methodName.startsWith("instance init number"))) {
      // keep strings consistent with text used in IndexFileSpecification
      this.inMethodCriterion = Criteria.inMethod(methodName);
    } else {
      this.inMethodCriterion = null;
    }

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

    if (inMethodCriterion != null && !inMethodCriterion.isSatisfiedBy(path)) {
      // If we're not in the method now, the parent path may still be in the method.
      // For example, the current leaf could be inside a method inside of an
      // anonymous inner class defined in another method.
      return this.isSatisfiedBy(path.getParentPath());
    }
    if (leaf instanceof NewClassTree || leaf instanceof NewArrayTree) {
      int indexInSource = NewScanner.indexOfNewTree(path, leaf);
      // System.out.printf("indexInSource=%d%n", indexInSource);
      boolean b;
      if (loc.isBytecodeOffset()) {
        int indexInClass = NewScanner.getMethodNewIndex(methodName, loc.offset);
        b = (indexInSource == indexInClass);
      } else {
        b = (indexInSource == loc.index);
      }
      return b;
    } else {
      return this.isSatisfiedBy(path.getParentPath());
    }
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.NEW;
  }

  @Override
  public String toString() {
    return "NewCriterion in method: " + methodName + " at location " + loc;
  }
}
