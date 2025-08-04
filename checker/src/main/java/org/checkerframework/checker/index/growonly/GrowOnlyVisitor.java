package org.checkerframework.checker.index.growonly;

import com.sun.source.tree.NewClassTree;
import org.checkerframework.checker.index.qual.BottomGrowShrink;
import org.checkerframework.checker.index.qual.UnshrinkableRef;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/** The visitor for the Grow-only Checker. */
public class GrowOnlyVisitor extends BaseTypeVisitor<GrowOnlyAnnotatedTypeFactory> {

  /**
   * Creates a new GrowOnlyVisitor.
   *
   * @param checker the checker that created this visitor
   */
  public GrowOnlyVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Overridden to prevent the base checker from issuing a "cast.unsafe.constructor.invocation"
   * warning. This checker allows any annotation from the hierarchy on a constructor invocation,
   * except for the unsound @BottomGrowShrink.
   */
  @Override
  protected void checkConstructorInvocation(
      AnnotatedDeclaredType enclosingType,
      AnnotatedExecutableType constructorType,
      NewClassTree newClassTree) {
    AnnotatedTypeMirror newClassType = atypeFactory.getAnnotatedType(newClassTree);

    if (constructorType.getReturnType().hasPrimaryAnnotation(UnshrinkableRef.class)) {
      // The constructor's return type is the default (@UnshrinkableRef).
      // This is the normal case for unannotated JDK constructors.
      // *Do not* call super. This suppresses the default warning.
      return;
    }

    super.checkConstructorInvocation(enclosingType, constructorType, newClassTree);

    // Warn about `new @BottomGrowShrink ArrayList<>()`.
    if (newClassType.hasPrimaryAnnotation(BottomGrowShrink.class)) {
      checker.reportError(newClassTree, "growonly.new.bottom");
    }
  }
}
