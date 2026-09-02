package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;
import org.plumelib.util.IPair;

/**
 * Given two javac ASTs representing the same Java file that may differ in annotations, {@link
 * #findMismatch(Tree, Tree)} tests if they have the same annotations.
 *
 * <p>Two annotations are compared as {@link AnnotationMirror}s when both ASTs have been attributed
 * by javac, and as source text otherwise; see {@link #sameAnnotation}.
 *
 * <p>Known gap: a receiver parameter is compared only if both ASTs have one, because a {@code
 * .ajava} file may add an explicit receiver parameter that the Java file omits. As a consequence,
 * if a receiver parameter appears in only one of the two ASTs, its annotations are ignored rather
 * than reported as a mismatch. See {@link DoubleJavacVisitor#visitMethod}.
 *
 * <p>This is the javac-based replacement for {@link AnnotationEqualityVisitor}.
 */
public class JavacAnnotationEqualityVisitor extends DoubleJavacVisitor {

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
   * @throws UserError if the two ASTs differ other than in annotations; for example, if one
   *     declares a member that the other does not
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
    return null;
  }

  @Override
  protected void visitAnnotationList(
      Tree owner1,
      Tree owner2,
      List<? extends AnnotationTree> annotations1,
      List<? extends AnnotationTree> annotations2) {
    if (mismatchedNode1 != null) {
      return;
    }
    if (annotations1.size() != annotations2.size()) {
      mismatchedNode1 = owner1;
      mismatchedNode2 = owner2;
      return;
    }
    for (int i = 0; i < annotations1.size(); i++) {
      if (!sameAnnotation(annotations1.get(i), annotations2.get(i))) {
        mismatchedNode1 = owner1;
        mismatchedNode2 = owner2;
        return;
      }
    }
  }

  /**
   * Returns true if the two trees represent the same annotation.
   *
   * <p>If both trees have been attributed by javac, this compares their {@link AnnotationMirror}s,
   * which disregards how the annotation is written in source code; for example, a simple name and a
   * fully-qualified name for the same annotation compare equal. {@link
   * TreeUtils#annotationFromAnnotationTree} returns null for an unattributed tree, in which case
   * this falls back to comparing source text, which does make those distinctions.
   *
   * @param annotationTree1 an annotation tree from the first AST
   * @param annotationTree2 the corresponding annotation tree from the second AST
   * @return true if the two trees represent the same annotation
   */
  private static boolean sameAnnotation(
      AnnotationTree annotationTree1, AnnotationTree annotationTree2) {
    AnnotationMirror mirror1 = TreeUtils.annotationFromAnnotationTree(annotationTree1);
    AnnotationMirror mirror2 = TreeUtils.annotationFromAnnotationTree(annotationTree2);
    if (mirror1 != null && mirror2 != null) {
      return AnnotationUtils.areSame(mirror1, mirror2);
    }
    return annotationTree1.toString().equals(annotationTree2.toString());
  }
}
