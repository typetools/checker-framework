package org.checkerframework.framework.type;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.Pair;

/**
 * Represents the state of a visitor. Stores the relevant information to find the type of 'this' in
 * the visitor.
 */
public class AssignmentContext {

  /** The assignment context is a tree as well as its type. */
  protected Pair<Tree, AnnotatedTypeMirror> assignmentContext;

  /** The visitor's current tree path. */
  protected TreePath path;

  /**
   * Updates the assignment context.
   *
   * @param assignmentContext the new assignment context to use
   */
  public void setAssignmentContext(Pair<Tree, AnnotatedTypeMirror> assignmentContext) {
    this.assignmentContext = assignmentContext;
  }

  /** Sets the current path for the visitor. */
  public void setPath(TreePath path) {
    this.path = path;
  }

  /**
   * Returns the assignment context.
   *
   * <p>NOTE: This method is known to be buggy.
   *
   * @return the assignment context
   */
  public Pair<Tree, AnnotatedTypeMirror> getAssignmentContext() {
    return assignmentContext;
  }

  /**
   * Returns the current path for the visitor.
   *
   * @return the current path for the visitor
   */
  public TreePath getPath() {
    return this.path;
  }

  @SideEffectFree
  @Override
  public String toString() {
    return String.format(
        "AssignmentContext: assignment context %s (%s)%n" + "    path is non-null: %s",
        (assignmentContext != null ? assignmentContext.first : "null"),
        (assignmentContext != null ? assignmentContext.second : "null"),
        path != null);
  }
}
