package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ReceiverCriterion implements Criterion {

  private final String methodName; // no return type
  private final Criterion isSigMethodCriterion;

  public ReceiverCriterion(String methodName) {
    this.methodName = methodName;
    isSigMethodCriterion = Criteria.isSigMethod(methodName);
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path, @FindDistinct Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(@Nullable TreePath path) {
    // want to annotate BlockTree returned by MethodTree.getBody();
    if (path == null) {
      return false;
    }

    if (path.getLeaf() instanceof MethodTree) {
      if (isSigMethodCriterion.isSatisfiedBy(path)) {
        MethodTree leaf = (MethodTree) path.getLeaf();
        // If the method already has a receiver, then insert directly on the
        // receiver, not on the method.
        return leaf.getReceiverParameter() == null;
      }
      return false;
    } else {
      // We may be attempting to insert an annotation on a type parameter of an
      // existing receiver, so make sure this is the right receiver parameter:
      // work up the tree to find the method declaration. Store the parameter we
      // pass through up to the method declaration so we can make sure we came up
      // through the receiver. Then check to make sure this is the correct method
      // declaration.
      Tree param = null;
      TreePath parent = path;
      while (parent != null && !(parent.getLeaf() instanceof MethodTree)) {
        if (parent.getLeaf() instanceof VariableTree) {
          if (param == null) {
            param = parent.getLeaf();
          } else {
            // The only variable we should pass through is the receiver parameter.
            // If we pass through more than one then this isn't the right place.
            return false;
          }
        }
        parent = parent.getParentPath();
      }
      if (parent != null && param != null) {
        MethodTree method = (MethodTree) parent.getLeaf();
        boolean foundParam = param == method.getReceiverParameter();
        if (foundParam) {
          return isSigMethodCriterion.isSatisfiedBy(parent);
        }
      }
      return false;
    }
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    // Declaration annotations are not allowed on the receiver (per JLS 8.4.1).
    return true;
  }

  @Override
  public Kind getKind() {
    return Kind.RECEIVER;
  }

  @Override
  public String toString() {
    return "ReceiverCriterion for method: " + methodName;
  }
}
