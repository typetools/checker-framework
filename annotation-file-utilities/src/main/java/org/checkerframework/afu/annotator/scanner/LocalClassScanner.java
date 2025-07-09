package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import org.checkerframework.checker.interning.qual.FindDistinct;

/**
 * LocalClassScanner determines the index of a tree for a local class. If the index is i, it is the
 * ith local class with the class name in the file. Thus, if i = 2, it will have a name of the form
 * OuterClass$2InnerClass.
 */
public class LocalClassScanner extends TreePathScanner<Void, Integer> {

  /**
   * Given a local class, computes and returns its 1-based index in the given tree representing a
   * local class.
   *
   * @param path the source path ending in the local class
   * @param localClass the local class to search for
   * @return the index of the local class in the source code
   */
  public static int indexOfClassTree(TreePath path, @FindDistinct ClassTree localClass) {
    // Move up to the CLASS tree enclosing this CLASS tree and start the tree
    // traversal from there. This prevents us from counting local classes that
    // are in a different part of the tree and therefore aren't included in the
    // index number.
    int classesFound = 0;
    boolean localClassFound = false;
    while (path.getParentPath() != null && classesFound < 1) {
      if (path.getLeaf() == localClass) {
        localClassFound = true;
      }
      path = path.getParentPath();
      if (localClassFound && path.getLeaf().getKind() == Tree.Kind.CLASS) {
        classesFound++;
      }
    }
    LocalClassScanner lcs = new LocalClassScanner(localClass);
    lcs.scan(path, 0);
    if (lcs.found) {
      return lcs.index;
    } else {
      return -1;
    }
  }

  private int index;
  private boolean found;
  private ClassTree localClass;

  /**
   * Creates a new LocalClassScanner that searches for the index of the given tree, representing a
   * local class.
   *
   * @param localClass the local class to search for
   */
  private LocalClassScanner(ClassTree localClass) {
    this.index = 1;
    this.found = false;
    this.localClass = localClass;
  }

  // The level parameter keeps us from traversing too low in the tree and
  // counting classes that aren't included in the index number.

  @Override
  public Void visitBlock(BlockTree node, Integer level) {
    if (level < 1) {
      // Visit blocks since a local class can only be in a block. Then visit each
      // statement of the block to see if any are the correct local class.
      for (StatementTree statement : node.getStatements()) {
        if (!found && statement.getKind() == Tree.Kind.CLASS) {
          ClassTree c = (ClassTree) statement;
          if (localClass == statement) {
            found = true;
          } else if (c.getSimpleName() == localClass.getSimpleName()) {
            index++;
          }
        }
      }
      super.visitBlock(node, level + 1);
    }
    return null;
  }
}
