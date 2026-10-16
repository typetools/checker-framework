package org.checkerframework.checker.lock;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.NewArrayTree;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A TreeAnnotator implementation to apply special type introduction rules to string concatenations,
 * binary comparisons, and new array instantiations.
 */
public class LockTreeAnnotator extends TreeAnnotator {

  public LockTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
    super(atypeFactory);
  }

  @Override
  public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
    // For any binary operation whose LHS or RHS can be a non-boolean type, and whose resulting
    // type is necessarily boolean, the resulting annotation on the boolean type must be
    // @GuardedBy({}).

    // There is no need to enforce that the annotation on the result of &&, ||, etc.  is
    // @GuardedBy({}) since for such operators, both operands are of type @GuardedBy({}) boolean
    // to begin with.

    if (TreeUtils.isBinaryComparison(tree) || TypesUtils.isString(type.getUnderlyingType())) {
      // A boolean or String is always @GuardedBy({}). LockVisitor determines whether
      // the LHS and RHS of this operation can be legally dereferenced.
      type.replaceAnnotation(((LockAnnotatedTypeFactory) atypeFactory).GUARDEDBY);

      return null;
    }

    return super.visitBinary(tree, type);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
    if (TypesUtils.isString(type.getUnderlyingType())) {
      type.replaceAnnotation(((LockAnnotatedTypeFactory) atypeFactory).GUARDEDBY);
    }

    return super.visitCompoundAssignment(tree, type);
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
    if (!type.hasPrimaryAnnotationInHierarchy(
        ((LockAnnotatedTypeFactory) atypeFactory).NEWOBJECT)) {
      type.replaceAnnotation(((LockAnnotatedTypeFactory) atypeFactory).NEWOBJECT);
    }
    return super.visitNewArray(tree, type);
  }
}
