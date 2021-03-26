package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Given two ASTs representing the same Java file that may differ in annotations, tests if they have
 * the same annotations.
 *
 * <p>To use this class, have the first AST node accept the visitor and pass the second AST node as
 * the second argument. Then, check {@link #getAnnotationsMatch}.
 */
public class AnnotationEqualityVisitor extends DoubleJavaParserVisitor {
  /** Whether or not a node with mismatched annotations has been seen. */
  private boolean annotationsMatch;
  /** If a node with mismatched annotations has been seen, stores the node from the first AST. */
  private @MonotonicNonNull NodeWithAnnotations<?> mismatchedNode1;
  /** If a node with mismatched annotations has been seen, stores the node from the second AST. */
  private @MonotonicNonNull NodeWithAnnotations<?> mismatchedNode2;

  /** Constructs an {@code AnnotationEqualityVisitor}. */
  public AnnotationEqualityVisitor() {
    annotationsMatch = true;
    mismatchedNode1 = null;
    mismatchedNode2 = null;
  }

  /**
   * Returns whether a visited pair of nodes differed in annotations.
   *
   * @return true if some visited pair of nodes differed in annotations
   */
  public boolean getAnnotationsMatch() {
    return annotationsMatch;
  }

  /**
   * If a visited pair of nodes has had mismatched annotations, returns the node from the first AST
   * where annotations differed, or null otherwise.
   *
   * @return the node from the first AST with differing annotations or null
   */
  public @Nullable NodeWithAnnotations<?> getMismatchedNode1() {
    return mismatchedNode1;
  }

  /**
   * If a visited pair of nodes has had mismatched annotations, returns the node from the second AST
   * where annotations differed, or null otherwise.
   *
   * @return the node from the second AST with differing annotations or null
   */
  public @Nullable NodeWithAnnotations<?> getMismatchedNode2() {
    return mismatchedNode2;
  }

  @Override
  public void defaultAction(Node node1, Node node2) {
    if (!(node1 instanceof NodeWithAnnotations<?>) || !(node2 instanceof NodeWithAnnotations<?>)) {
      return;
    }

    Node node1Copy = node1.clone();
    Node node2Copy = node2.clone();
    for (Comment comment : node1Copy.getAllContainedComments()) {
      comment.remove();
    }

    for (Comment comment : node2Copy.getAllContainedComments()) {
      comment.remove();
    }

    if (!((NodeWithAnnotations<?>) node1Copy)
        .getAnnotations()
        .equals(((NodeWithAnnotations<?>) node2Copy).getAnnotations())) {
      annotationsMatch = false;
      mismatchedNode1 = (NodeWithAnnotations<?>) node1;
      mismatchedNode2 = (NodeWithAnnotations<?>) node2;
    }
  }
}
