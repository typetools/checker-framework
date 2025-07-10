package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LambdaScanner stores information about the names and offsets of lambda expressions inside a
 * method, and can also be used to scan the source tree and determine the index of a given
 * instanceof check, where the i^th index corresponds to the i^th instanceof check, using 0-based
 * indexing.
 */
public class LambdaScanner extends CommonScanner {

  /**
   * Computes the index of the given lambda expression tree amongst all lambda expression trees
   * inside its method, using 0-based indexing.
   *
   * @param origpath the path ending in the given lambda expression tree
   * @param tree the lambda expression tree to search for
   * @return the index of the given lambda expression tree
   */
  public static int indexOfLambdaExpressionTree(TreePath origpath, Tree tree) {
    TreePath path = TreePathUtil.findCountingContext(origpath);
    if (path == null) {
      return -1;
    }

    LambdaScanner ls = new LambdaScanner(tree);
    ls.scan(path, null);
    return ls.index;
  }

  private int index;
  private boolean done;
  private final Tree tree;

  /**
   * Creates an InstanceOfScanner that will scan the source tree for the given node representing the
   * lambda expression to find.
   *
   * @param tree the given lambda expression to search for
   */
  private LambdaScanner(Tree tree) {
    this.index = -1;
    this.done = false;
    this.tree = tree;
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
    if (!done) {
      index++;
    }
    if (tree == node) {
      done = true;
    }
    return super.visitLambdaExpression(node, null);
  }

  // Map from name of a method to a list of bytecode offsets of all
  // lambda expressions in that method.
  private static Map<String, List<Integer>> methodNameToLambdaExpressionOffsets = new HashMap<>();

  /**
   * Adds a lambda expression bytecode offset to the current list of offsets for methodName. This
   * method must be called with monotonically increasing offsets for any one method.
   *
   * @param methodName the name of the method
   * @param offset the offset to add
   */
  public static void addLambdaExpressionToMethod(String methodName, Integer offset) {
    List<Integer> offsetList = methodNameToLambdaExpressionOffsets.get(methodName);
    if (offsetList == null) {
      offsetList = new ArrayList<Integer>();
      methodNameToLambdaExpressionOffsets.put(methodName, offsetList);
    }
    offsetList.add(offset);
  }

  /**
   * Returns the index of the given offset within the list of offsets for the given method, using
   * 0-based indexing, or returns a negative number if the offset is not one of the offsets in the
   * context.
   *
   * @param methodName the name of the method
   * @param offset the offset of the lambda expression
   * @return the index of the given offset, or a negative number if the offset does not exist inside
   *     the context
   */
  public static Integer getMethodLambdaExpressionIndex(String methodName, Integer offset) {
    List<Integer> offsetList = methodNameToLambdaExpressionOffsets.get(methodName);
    if (offsetList == null) {
      return -1;
    }

    return offsetList.indexOf(offset);
  }
}
