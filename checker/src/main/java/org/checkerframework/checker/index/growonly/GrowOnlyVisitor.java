package org.checkerframework.checker.index.growonly;

import com.sun.source.tree.NewClassTree;
import org.checkerframework.checker.index.qual.BottomGrowShrink;
import org.checkerframework.checker.index.qual.UnshrinkableRef;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * The visitor for the GrowOnly Checker.
 *
 * <p>This visitor's primary role is to handle annotations on constructor invocations, ensuring they
 * are used soundly.
 */
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
   * warning when a programmer correctly annotates a `new` expression. This checker allows any
   * annotation from the hierarchy on a constructor, except for the unsound @BottomGrowShrink.
   */
  @Override
  protected void checkConstructorInvocation(
      AnnotatedDeclaredType enclosingType,
      AnnotatedExecutableType constructorType,
      NewClassTree newClassTree) {
    AnnotatedTypeMirror newClassType = atypeFactory.getAnnotatedType(newClassTree);

    // 1. Check for the unsound case: `new @BottomGrowShrink ArrayList<>()`
    if (newClassType.hasPrimaryAnnotation(BottomGrowShrink.class)) {
      checker.reportError(newClassTree, "growonly.new.bottom");
      // call super to get the default assignment checks.
      super.checkConstructorInvocation(enclosingType, constructorType, newClassTree);
      return;
    }

    // 2. Check if the constructor's return type is the default (@UnshrinkableRef).
    // This is the normal case for unannotated JDK constructors.
    if (constructorType.getReturnType().hasPrimaryAnnotation(UnshrinkableRef.class)) {
      // If so, allow the user-written annotation on the `new` expression
      // and *do not* call super. This suppresses the default warning.
      return;
    }

    // 3. For any other case, fall back to the default Checker Framework behavior.
    super.checkConstructorInvocation(enclosingType, constructorType, newClassTree);
  }
}
