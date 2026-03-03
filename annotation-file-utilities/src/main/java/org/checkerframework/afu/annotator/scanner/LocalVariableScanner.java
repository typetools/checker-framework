package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LocalVariableScanner stores information about the names and offsets of local variables inside a
 * method, and can also be used to scan the source tree and determine the index of a local variable
 * with a given name, so that the i^th index corresponds to the i^th declaration of a local variable
 * with that name, using 0-based indexing.
 */
public class LocalVariableScanner extends CommonScanner {
  /**
   * Computes the index i of the given tree along the given tree path such that it is the i^th
   * declaration of the local variable with the given var name, using 0-based indexing.
   *
   * @param origpath the source path that ends in varTree
   * @param varTree the variable tree that declares the local variable
   * @param varName the name of the local variable
   * @return the index of the variable tree with respect to the given local variable name
   */
  public static int indexOfVarTree(TreePath origpath, Tree varTree, String varName) {
    TreePath path = TreePathUtil.findCountingContext(origpath);
    if (path == null) {
      return -1;
    }

    LocalVariableScanner lvts = new LocalVariableScanner(varTree, varName);

    try {
      lvts.scan(path, null);
    } catch (Throwable e) {
      System.out.println("LocalVariableScanner: can't locate: " + varTree);
      return -2; // Don't return -1, which is above return code
    }
    return lvts.index;
  }

  /*
   * For efficiency, we might want to have methods like the following, specialized for the
   * three different cases.
   */
  /*
  public static int indexOfVarTreeInStaticInit(TreePath path, Tree tree, String varName) {
    // only start searching from within this method
      path = findEnclosingStaticInit(path);
      if (path == null) {
        return -1;
      }

      LocalVariableScanner lvts = new LocalVariableScanner(tree, varName);
      lvts.scan(path, null);
      return lvts.index;
  }
  */

  private int index = -1;
  private boolean done = false;
  private final Tree varTree;
  private final String varName;

  private LocalVariableScanner(Tree varTree, String varName) {
    this.index = -1;
    this.done = false;
    this.varTree = varTree;
    this.varName = varName;
  }

  @Override
  @SuppressWarnings("interning:not.interned") // reference equality check
  public Void visitVariable(VariableTree node, Void p) {
    // increment index only if you have not already reached the right node, and
    // if this node declares the same local variable you are searching for
    if (varName.equals(node.getName().toString())) {
      if (!done) {
        index++;
      }
      if (varTree == node) {
        done = true;
      }
    }
    return null;
  }

  // TODO: refactor class keys to avoid so many uses of generics

  // mapping from (method-name, variable-index, start-offset)
  // to variable name
  private static Map<Pair<String, Pair<Integer, Integer>>, String> methodNameIndexMap =
      new HashMap<>();

  // map from method to map from variable name to
  // a list of start offsets
  private static Map<String, Map<String, List<Integer>>> methodNameCounter = new HashMap<>();

  /**
   * Adds the given variable specified as a pair of method name and (index, start-offset) under the
   * given name to the list of all local variables.
   *
   * @param varInfo a pair of the method and a pair describing the local variable index and start
   *     offset of the local variable
   * @param name the name of the local variable
   */
  public static void addToMethodNameIndexMap(
      Pair<String, Pair<Integer, Integer>> varInfo, String name) {
    methodNameIndexMap.put(varInfo, name);
  }

  /**
   * Gets the name of the local variable in the given method, and at the given index and offset.
   *
   * @param varInfo a pair of the method name and a pair of the local variable's index and start
   *     offset
   * @return the name of the local variable at the specified location
   */
  public static String getFromMethodNameIndexMap(Pair<String, Pair<Integer, Integer>> varInfo) {
    return methodNameIndexMap.get(varInfo);
  }

  /**
   * Adds to the given method the fact that the local variable with the given name is declared at
   * the given start offset.
   *
   * @param methodName the method containing the local variable
   * @param varName the name of the local variable
   * @param offset the start offset of the local variable
   */
  public static void addToMethodNameCounter(String methodName, String varName, Integer offset) {
    Map<String, List<Integer>> nameOffsetCounter = methodNameCounter.get(methodName);
    if (nameOffsetCounter == null) {
      nameOffsetCounter = new HashMap<>();
      methodNameCounter.put(methodName, nameOffsetCounter);
    }

    List<Integer> listOfOffsets = nameOffsetCounter.get(varName);
    if (listOfOffsets == null) {
      listOfOffsets = new ArrayList<Integer>();
      nameOffsetCounter.put(varName, listOfOffsets);
    }

    listOfOffsets.add(offset);
  }

  /**
   * Returns a list of all start bytecode offsets of variable declarations with the given variable
   * name in the given method.
   *
   * @param methodName the name of the method
   * @param varName the name of the local variable
   * @return a list of start offsets for live ranges of all local variables with the given name in
   *     the given method
   */
  public static List<Integer> getFromMethodNameCounter(String methodName, String varName) {
    return methodNameCounter.get(methodName).get(varName);
  }
}
