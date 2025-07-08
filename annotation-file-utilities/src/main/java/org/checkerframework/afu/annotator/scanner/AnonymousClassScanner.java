package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

/**
 * AnonymousClassScanner determine the index of a tree for an anonymous class. If the index is i, it
 * is the ith anonymous class in the file. Thus, if i = 2, it will have a name of the form
 * NamedClass$2.
 */
public class AnonymousClassScanner extends TreePathScanner<Void, Integer> {

  /**
   * Given an anonymous class, computes and returns its 1-based index in the given tree representing
   * an anonymous class.
   *
   * @param path the source path ending in the anonymous class
   * @param anonclass the anonymous class to search for
   * @return the index of the anonymous class in the source code
   */
  public static int indexOfClassTree(TreePath path, Tree anonclass) {
    // Move up to the CLASS tree enclosing this CLASS tree and start the tree
    // traversal from there. This prevents us from counting anonymous classes
    // that are in a different part of the tree and therefore aren't included
    // in the index number.
    int classesFound = 0;
    boolean anonclassFound = false;
    while (path.getParentPath() != null && classesFound < 1) {
      if (path.getLeaf() == anonclass) {
        anonclassFound = true;
      }
      path = path.getParentPath();
      if (anonclassFound && TreePathUtil.hasClassKind(path.getLeaf())) {
        classesFound++;
      }
    }
    AnonymousClassScanner lvts = new AnonymousClassScanner(anonclass);
    lvts.scan(path, 0);
    if (lvts.found) {
      return lvts.index;
    } else {
      return -1;
    }
  }

  private int index;
  // top-level class doesn't count, so first index will be -1
  private boolean found;
  private Tree anonclass;

  /**
   * Creates a new AnonymousClassScanner that searches for the index of the given tree, representing
   * an anonymous class.
   *
   * @param anonclass the anonymous class to search for
   */
  private AnonymousClassScanner(Tree anonclass) {
    this.index = 1; // start counting at 1
    this.found = false;
    this.anonclass = anonclass;
  }

  // Slightly tricky counting:  if the target item is a CLASS, only count
  // CLASSes.  If it is a NEW_CLASS, only count NEW_CLASSes
  // The level parameter keeps us from traversing too low in the tree and
  // counting classes that aren't included in the index number.

  @Override
  public Void visitClass(ClassTree node, Integer level) {
    if (level < 2) {
      if (!found && TreePathUtil.hasClassKind(anonclass)) {
        if (anonclass == node) {
          found = true;
        } else if (node.getSimpleName().toString().trim().isEmpty()) {
          // don't count classes with given names in source
          index++;
        }
      }
      super.visitClass(node, level + 1);
    }
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree node, Integer level) {
    // if (level < 2) {
    if (!found && anonclass instanceof NewClassTree) {
      if (anonclass == node) {
        found = true;
      } else if (node.getClassBody() != null) {
        // Need to make sure you actually are creating anonymous inner class,
        // not just object creation.
        index++;
      } else {
        return null;
      }
    }
    super.visitNewClass(node, level + 1);
    // }
    return null;
  }
}
