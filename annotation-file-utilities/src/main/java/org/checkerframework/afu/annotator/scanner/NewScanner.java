package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.plumelib.util.IPair;

/**
 * NewScanner scans the source tree and determines the index of a given new, where the i^th index
 * corresponds to the i^th new, using 0-based indexing.
 */
public class NewScanner extends CommonScanner {
  private static boolean debug = false;

  static Map<IPair<TreePath, Tree>, Integer> cache = new HashMap<>();

  /**
   * Computes the index of the given new tree amongst all new trees inside its method, using 0-based
   * indexing. The tree has to be either a NewClassTree or a NewArrayTree. If the tree is not in a
   * method, then the index is computed
   *
   * @param origpath the path ending in the given cast tree
   * @param tree the cast tree to search for
   * @return the index of the given cast tree
   */
  public static int indexOfNewTree(TreePath origpath, Tree tree) {
    debug("indexOfNewTree: " + origpath.getLeaf());

    IPair<TreePath, Tree> args = IPair.of(origpath, tree);
    if (cache.containsKey(args)) {
      return cache.get(args);
    }

    TreePath path = TreePathUtil.findCountingContext(origpath);
    if (path == null) {
      return -1;
    }

    NewScanner lvts = new NewScanner(tree);
    lvts.scan(path, null);
    cache.put(args, lvts.index);

    return lvts.index;
  }

  private int index = -1;
  private boolean done = false;
  private final Tree tree;

  private NewScanner(Tree tree) {
    this.index = -1;
    this.done = false;
    this.tree = tree;
  }

  @Override
  @SuppressWarnings("interning:not.interned") // reference equality check
  public Void visitNewClass(NewClassTree node, Void p) {
    if (!done) {
      index++;
    }
    if (tree == node) {
      done = true;
    }
    return super.visitNewClass(node, null);
  }

  @Override
  @SuppressWarnings("interning:not.interned") // reference equality check
  public Void visitNewArray(NewArrayTree node, Void p) {
    if (!done) {
      index++;
    }
    if (tree == node) {
      done = true;
    }
    return super.visitNewArray(node, null);
  }

  public static void debug(String s) {
    if (debug) {
      System.out.println(s);
    }
  }

  private static Map<String, List<Integer>> methodNameToNewOffsets = new HashMap<>();

  public static void addNewToMethod(String methodName, Integer offset) {
    debug("adding new to method: " + methodName + " offset: " + offset);
    List<Integer> offsetList = methodNameToNewOffsets.get(methodName);
    if (offsetList == null) {
      offsetList = new ArrayList<Integer>();
      methodNameToNewOffsets.put(methodName, offsetList);
    }
    offsetList.add(offset);
  }

  public static Integer getMethodNewIndex(String methodName, Integer offset) {
    List<Integer> offsetList = methodNameToNewOffsets.get(methodName);
    if (offsetList == null) {
      throw new RuntimeException(
          "NewScanner.getMethodNewIndex() : " + "did not find offsets for method: " + methodName);
    }

    Integer offsetIndex = offsetList.indexOf(offset);
    if (offsetIndex < 0) {
      throw new RuntimeException(
          "NewScanner.getMethodNewIndex() : "
              + "in method: "
              + methodName
              + " did not find offset: "
              + offset);
    }

    return offsetIndex;
  }
}
