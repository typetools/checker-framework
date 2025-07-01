package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import org.checkerframework.afu.annotator.Main;

/** Represents the criterion that a program element is in a method with a certain name. */
final class PackageCriterion implements Criterion {

  private final String name;

  PackageCriterion(String name) {
    this.name = name;
  }

  @Override
  public Kind getKind() {
    return Kind.PACKAGE;
  }

  @Override
  public boolean isSatisfiedBy(TreePath path, Tree tree) {
    assert path == null || path.getLeaf() == tree;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(TreePath path) {
    Tree tree = path.getLeaf();
    Criteria.dbug.debug(
        "PackageCriterion.isSatisfiedBy(%s, %s); this=%s%n",
        Main.leafString(path), tree, this.toString());

    if (tree instanceof CompilationUnitTree) {
      CompilationUnitTree cu = (CompilationUnitTree) tree;
      if (cu.getSourceFile().getName().endsWith("package-info.java")) {
        ExpressionTree pn = cu.getPackageName();
        assert ((pn instanceof IdentifierTree) || (pn instanceof MemberSelectTree));
        if (this.name.equals(pn.toString())) {
          return true;
        }
      }
    }
    Criteria.dbug.debug("PackageCriterion.isSatisfiedBy => false%n");
    return false;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public String toString() {
    return "package '" + name + "'";
  }
}
