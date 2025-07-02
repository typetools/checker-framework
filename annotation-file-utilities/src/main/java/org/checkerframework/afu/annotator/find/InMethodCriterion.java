package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import javax.lang.model.element.Modifier;
import org.checkerframework.afu.annotator.Main;

// TODO: the name is sometimes used as a signature and sometimes as a method!
/** Represents the criterion that a program element is in a method with a certain name. */
final class InMethodCriterion implements Criterion {

  /** The method name to search for. */
  public final String name;

  /** A criterion for an exact match. */
  private final IsSigMethodCriterion sigMethodCriterion;

  /**
   * Creates an InMethodCriterion.
   *
   * @param name the method name
   */
  @SuppressWarnings("signature:argument") // likely bug; value used as both a method & a signature
  InMethodCriterion(String name) {
    this.name = name;
    sigMethodCriterion = new IsSigMethodCriterion(name);
  }

  @Override
  public Kind getKind() {
    return Kind.IN_METHOD;
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
    Criteria.dbug.debug(
        "InMethodCriterion.isSatisfiedBy(%s); this=%s%n", Main.leafString(path), this.toString());

    // true if the argument is within a variable declaration's initializer expression.
    boolean inDecl = false;
    // Ignore the value if inDecl==false.  Otherwise:
    // true if in a static variable declaration, false if in a member variable declaration.
    boolean staticDecl = false;
    TreePath childPath = null;
    do {
      Tree leaf = path.getLeaf();
      if (leaf instanceof MethodTree) {
        boolean b = sigMethodCriterion.isSatisfiedBy(path);
        Criteria.dbug.debug("InMethodCriterion.isSatisfiedBy => %s%n", b);
        return b;
      }
      if (leaf instanceof VariableTree) { // variable declaration
        VariableTree varDecl = (VariableTree) leaf;
        if (childPath != null && childPath.getLeaf() == varDecl.getInitializer()) {
          inDecl = true;
          ModifiersTree mods = varDecl.getModifiers();
          staticDecl = mods.getFlags().contains(Modifier.STATIC);
        }
      }
      childPath = path;
      path = path.getParentPath();
    } while (path != null && path.getLeaf() != null);

    // We didn't find the method.  Return true if in a varable declarator,
    // which is initialization code that will go in <init> or <clinit>.
    boolean result = inDecl && (staticDecl ? "<clinit>()V" : "<init>()V").equals(name);
    Criteria.dbug.debug("InMethodCriterion.isSatisfiedBy => %s%n", result);
    return result;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    return false;
  }

  @Override
  public String toString() {
    return "in method '" + name + "'";
  }
}
