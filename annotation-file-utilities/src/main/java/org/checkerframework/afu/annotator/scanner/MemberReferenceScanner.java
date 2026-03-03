package org.checkerframework.afu.annotator.scanner;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberReferenceScanner extends CommonScanner {

  /**
   * Computes the index of the given method invocation amongst all method invocation trees inside
   * its method, using 0-based indexing.
   *
   * @param origpath the path ending in the given method invocation tree
   * @param tree the method invocation tree to search for
   * @return the index of the given method invocation tree
   */
  public static int indexOfMemberReferenceTree(TreePath origpath, Tree tree) {
    TreePath path = TreePathUtil.findCountingContext(origpath);
    if (path == null) {
      return -1;
    }

    MemberReferenceScanner mcs = new MemberReferenceScanner(tree);
    mcs.scan(path, null);
    return mcs.index;
  }

  private int index;
  private boolean done;
  private final Tree tree;

  /**
   * Creates an InstanceOfScanner that will scan the source tree for the given node representing the
   * method invocation to find.
   *
   * @param tree the given method invocation to search for
   */
  private MemberReferenceScanner(Tree tree) {
    this.index = -1;
    this.done = false;
    this.tree = tree;
  }

  @Override
  @SuppressWarnings("interning:not.interned") // reference equality check
  public Void visitMemberReference(MemberReferenceTree node, Void p) {
    if (!done) {
      index++;
    }
    if (tree == node) {
      done = true;
    }
    return super.visitMemberReference(node, null);
  }

  // Map from name of a method to a list of bytecode offsets of all
  // lambda expressions in that method.
  private static Map<String, List<Integer>> methodNameToMemberReferenceOffsets = new HashMap<>();

  /**
   * Adds a member reference bytecode offset to the current list of offsets for methodName. This
   * method must be called with monotonically increasing offsets for any one method.
   *
   * @param methodName the name of the method
   * @param offset the offset to add
   */
  public static void addMemberReferenceToMethod(String methodName, Integer offset) {
    List<Integer> offsetList = methodNameToMemberReferenceOffsets.get(methodName);
    if (offsetList == null) {
      offsetList = new ArrayList<Integer>();
      methodNameToMemberReferenceOffsets.put(methodName, offsetList);
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
  public static Integer getMemberReferenceIndex(String methodName, Integer offset) {
    List<Integer> offsetList = methodNameToMemberReferenceOffsets.get(methodName);
    if (offsetList == null) {
      return -1;
    }

    return offsetList.indexOf(offset);
  }
}
