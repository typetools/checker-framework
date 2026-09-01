package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;

/**
 * Given two javac ASTs representing the same Java file that may differ in annotations, {@link
 * #findMismatch(Tree, Tree)} tests if they have the same annotations.
 *
 * <p>This is the javac-based replacement for {@link AnnotationEqualityVisitor}.
 */
public class JavacAnnotationEqualityVisitor extends DoubleJavacVisitor {

  /** The most recently visited tree from the first AST. Set by {@link #defaultAction}. */
  private @Nullable Tree currentTree1 = null;

  /** The most recently visited tree from the second AST. Set by {@link #defaultAction}. */
  private @Nullable Tree currentTree2 = null;

  /** If a node with mismatched annotations has been seen, stores the node from the first AST. */
  private @MonotonicNonNull Tree mismatchedNode1 = null;

  /** If a node with mismatched annotations has been seen, stores the node from the second AST. */
  private @MonotonicNonNull Tree mismatchedNode2 = null;

  /** Constructs a {@code JavacAnnotationEqualityVisitor}. */
  private JavacAnnotationEqualityVisitor() {}

  /**
   * Returns null if the two ASTs have matching annotations everywhere, or the first pair of
   * corresponding nodes where annotations differ. The comparison is order-sensitive: nodes with the
   * same annotations in a different order are considered to differ. Only the first mismatch is
   * returned even if multiple mismatches exist.
   *
   * @param tree1 root of the first AST
   * @param tree2 root of the second AST
   * @return null if annotations match everywhere, or a pair of corresponding nodes from {@code
   *     tree1} and {@code tree2} where annotations differ
   */
  public static @Nullable IPair<Tree, Tree> findMismatch(Tree tree1, Tree tree2) {
    JavacAnnotationEqualityVisitor visitor = new JavacAnnotationEqualityVisitor();
    visitor.scan(tree1, tree2);
    Tree node1 = visitor.mismatchedNode1;
    Tree node2 = visitor.mismatchedNode2;
    assert (node1 == null) == (node2 == null);
    if (node1 != null && node2 != null) {
      return IPair.of(node1, node2);
    }
    return null;
  }

  @Override
  protected Void defaultAction(Tree tree1, Tree tree2) {
    currentTree1 = tree1;
    currentTree2 = tree2;
    return null;
  }

  @Override
  protected void visitAnnotationList(
      List<? extends AnnotationTree> annotations1, List<? extends AnnotationTree> annotations2) {
    if (mismatchedNode1 != null) {
      return;
    }
    // currentTree{1,2} are always set by defaultAction before visitAnnotationList is called.
    assert (currentTree1 == null) == (currentTree2 == null);
    if (currentTree1 == null || currentTree2 == null) {
      return;
    }
    if (annotations1.size() != annotations2.size()) {
      mismatchedNode1 = currentTree1;
      mismatchedNode2 = currentTree2;
      return;
    }
    for (int i = 0; i < annotations1.size(); i++) {
      AnnotationMirror mirror1 = TreeUtils.annotationFromAnnotationTree(annotations1.get(i));
      AnnotationMirror mirror2 = TreeUtils.annotationFromAnnotationTree(annotations2.get(i));
      if (!AnnotationUtils.areSame(mirror1, mirror2)) {
        mismatchedNode1 = currentTree1;
        mismatchedNode2 = currentTree2;
        return;
      }
    }
  }
}
