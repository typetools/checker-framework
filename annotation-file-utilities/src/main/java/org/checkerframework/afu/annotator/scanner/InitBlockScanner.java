package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import java.lang.reflect.Field;

/**
 * InitScanner scans the source tree and determines the index of a given initializer block, where
 * index {@code i} corresponds to the (0-based) i^th initializer of the indicated kind (static or
 * instance),
 */
public class InitBlockScanner extends TreePathScanner<Void, Boolean> {
  public static int indexOfInitTree(TreePath path, boolean isStatic) {
    // we allow to start with any path/tree within an initializer.
    // first go to the enclosing initializer
    Tree tree = TreePathUtil.findEnclosingInitBlock(path, isStatic).getLeaf();
    // find the enclosing class
    path = TreePathUtil.findEnclosingClass(path);
    if (tree == null || path == null) {
      return -1;
    }
    // find the index of the current initializer within the
    // enclosing class
    InitBlockScanner bts = new InitBlockScanner(tree);
    bts.scan(path, isStatic);
    return bts.index;
  }

  private int index = -1;
  private boolean done = false;
  private final Tree tree;

  private InitBlockScanner(Tree tree) {
    this.index = -1;
    this.done = false;
    this.tree = tree;
  }

  @Override
  public Void visitBlock(BlockTree node, Boolean isStatic) {
    // TODO: is isStatic only used for static initializer blocks?
    if (!done && isStatic == node.isStatic() && getBlockEndPos((JCBlock) node) >= 0) {
      index++;
    }
    if (tree == node) {
      done = true;
    }
    return super.visitBlock(node, isStatic);
  }

  /**
   * Returns the end pos for {@code block}.
   *
   * @param block a block
   * @return the end pos for {@code block}
   */
  private static int getBlockEndPos(JCBlock block) {
    try {
      return BLOCK_END_POS_FIELD.getInt(block);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  /** Field for JCBlock#endpos or JCBlock#bracePos depending on the version of Java. */
  private static final Field BLOCK_END_POS_FIELD = getBlockEndPosField();

  /**
   * Returns field for JCBlock#endpos or JCBlock#bracePos depending on the version of Java.
   *
   * @return field for JCBlock#endpos or JCBlock#bracePos depending on the version of Java
   */
  private static Field getBlockEndPosField() {
    try {
      return JCBlock.class.getDeclaredField("endpos");
    } catch (NoSuchFieldException e1) {
      try {
        return JCBlock.class.getDeclaredField("bracePos");
      } catch (NoSuchFieldException e2) {
        e2.addSuppressed(e1);
        throw new AssertionError(e2);
      }
    }
  }
}
