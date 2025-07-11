package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents the criterion that a program element is in a package with a certain name. */
final class InPackageCriterion implements Criterion {

  private final String name;

  InPackageCriterion(String name) {
    this.name = name;
  }

  @Override
  public Kind getKind() {
    return Kind.IN_PACKAGE;
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

    Criteria.dbug.debug(
        "InPackageCriterion.isSatisfiedBy(%s); this=%s", Main.leafString(path), this.toString());

    do {
      Tree tree = path.getLeaf();
      if (tree instanceof CompilationUnitTree) {
        CompilationUnitTree cu = (CompilationUnitTree) tree;
        ExpressionTree pn = cu.getPackageName();
        if (pn == null) {
          return name == null || name.equals("");
        } else {
          String packageName = pn.toString();
          return name != null && name.equals(packageName);
        }
      }
      path = path.getParentPath();
    } while (path != null && path.getLeaf() != null);

    Criteria.dbug.debug("InPackageCriterion.isSatisfiedBy => false");
    return false;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public String toString() {
    return "in package '" + name + "'";
  }
}
