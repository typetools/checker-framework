package org.checkerframework.framework.ajava;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Given two javac ASTs representing the same Java file that may differ in annotations, tests if
 * they have the same annotations.
 *
 * <p>To use this class, call {@link #scan} with the roots of the two ASTs. Then, check {@link
 * #getAnnotationsMatch}.
 *
 * <p>This is the javac-based replacement for {@link AnnotationEqualityVisitor}.
 */
public class JavacAnnotationEqualityVisitor extends DoubleJavacVisitor {

  /** True if no node with mismatched annotations has been seen. */
  private boolean annotationsMatch;

  /** If a node with mismatched annotations has been seen, stores the node from the first AST. */
  private @MonotonicNonNull Tree mismatchedNode1;

  /** If a node with mismatched annotations has been seen, stores the node from the second AST. */
  private @MonotonicNonNull Tree mismatchedNode2;

  /** Constructs a {@code JavacAnnotationEqualityVisitor}. */
  public JavacAnnotationEqualityVisitor() {
    annotationsMatch = true;
    mismatchedNode1 = null;
    mismatchedNode2 = null;
  }

  /**
   * Returns true if all visited pairs of nodes had matching annotations.
   *
   * @return true if all visited pairs of nodes had matching annotations
   */
  public boolean getAnnotationsMatch() {
    return annotationsMatch;
  }

  /**
   * If a visited pair of nodes has had mismatched annotations, returns the node from the first AST
   * where annotations differed, or null otherwise.
   *
   * @return the node from the first AST with differing annotations, or null
   */
  public @Nullable Tree getMismatchedNode1() {
    return mismatchedNode1;
  }

  /**
   * If a visited pair of nodes has had mismatched annotations, returns the node from the second AST
   * where annotations differed, or null otherwise.
   *
   * @return the node from the second AST with differing annotations, or null
   */
  public @Nullable Tree getMismatchedNode2() {
    return mismatchedNode2;
  }

  /**
   * Returns the annotation trees on the given tree, or an empty list if the tree type does not
   * carry annotations.
   *
   * <p>In javac's AST, annotations appear on {@link ModifiersTree} (declaration annotations),
   * {@link AnnotatedTypeTree} (type-use annotations), {@link TypeParameterTree}, {@link
   * PackageTree}, {@link ModuleTree}, and {@link NewArrayTree}.
   *
   * @param tree a tree
   * @return the annotations on the tree
   */
  public static List<? extends AnnotationTree> getAnnotations(Tree tree) {
    if (tree instanceof ModifiersTree t) {
      return t.getAnnotations();
    }
    if (tree instanceof AnnotatedTypeTree t) {
      return t.getAnnotations();
    }
    if (tree instanceof TypeParameterTree t) {
      return t.getAnnotations();
    }
    if (tree instanceof PackageTree t) {
      return t.getAnnotations();
    }
    if (tree instanceof ModuleTree t) {
      return t.getAnnotations();
    }
    if (tree instanceof NewArrayTree t) {
      return t.getAnnotations();
    }
    return Collections.emptyList();
  }

  /**
   * Compares two lists of annotation trees by their string representations. Javac trees do not
   * implement structural {@code equals}, so string comparison is used instead. Javac tree {@code
   * toString} does not include comments, so no comment-stripping is needed.
   *
   * @param annos1 the first list of annotations
   * @param annos2 the second list of annotations
   * @return true if the two lists represent the same annotations
   */
  private static boolean annotationsEqual(
      List<? extends AnnotationTree> annos1, List<? extends AnnotationTree> annos2) {
    if (annos1.size() != annos2.size()) {
      return false;
    }
    for (int i = 0; i < annos1.size(); i++) {
      if (!annos1.get(i).toString().equals(annos2.get(i).toString())) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected Void defaultAction(Tree tree1, Tree tree2) {
    if (!annotationsMatch) {
      return null;
    }

    List<? extends AnnotationTree> annos1 = getAnnotations(tree1);
    List<? extends AnnotationTree> annos2 = getAnnotations(tree2);

    if (!annotationsEqual(annos1, annos2)) {
      annotationsMatch = false;
      mismatchedNode1 = tree1;
      mismatchedNode2 = tree2;
    }

    return null;
  }
}
