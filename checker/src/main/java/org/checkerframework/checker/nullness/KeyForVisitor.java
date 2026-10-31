package org.checkerframework.checker.nullness;

import com.sun.source.tree.Tree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/** The visitor for the Map Key Checker. */
public class KeyForVisitor extends BaseTypeVisitor<KeyForAnnotatedTypeFactory> {

  /**
   * Create a KeyForVisitor.
   *
   * @param checker the associated checker
   */
  public KeyForVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected OverrideChecker createOverrideChecker(
      Tree overriderTree,
      AnnotatedExecutableType overrider,
      AnnotatedTypeMirror overriderType,
      AnnotatedTypeMirror overriderReturnType,
      AnnotatedExecutableType overridden,
      AnnotatedDeclaredType overriddenType,
      AnnotatedTypeMirror overriddenReturnType) {
    return new KeyForOverrideChecker(
        overriderTree,
        overrider,
        overriderType,
        overriderReturnType,
        overridden,
        overriddenType,
        overriddenReturnType);
  }

  /**
   * An override checker that does not check the lower bounds of an overriding method's type
   * parameters.
   *
   * <p>The default for lower bounds is {@code @UnknownKeyFor}, except in {@code java.*} packages,
   * where it is {@code @KeyForBottom}. Checking lower bounds would therefore require an annotation
   * on the type parameter of every method that overrides a generic JDK method, such as {@code
   * Collection.toArray(T[])}.
   *
   * @checker_framework.manual #map-key-defaults-lowerbound Default for lower bounds
   */
  protected class KeyForOverrideChecker extends OverrideChecker {

    /**
     * Create a KeyForOverrideChecker.
     *
     * @param overriderTree the AST node of the overriding method or method reference
     * @param overrider the type of the overriding method
     * @param overriderType the type enclosing the overrider method, usually an
     *     AnnotatedDeclaredType; for Method References may be something else
     * @param overriderReturnType the return type of the overriding method
     * @param overridden the type of the overridden method
     * @param overriddenType the declared type enclosing the overridden method
     * @param overriddenReturnType the return type of the overridden method
     */
    public KeyForOverrideChecker(
        Tree overriderTree,
        AnnotatedExecutableType overrider,
        AnnotatedTypeMirror overriderType,
        AnnotatedTypeMirror overriderReturnType,
        AnnotatedExecutableType overridden,
        AnnotatedDeclaredType overriddenType,
        AnnotatedTypeMirror overriddenReturnType) {
      super(
          overriderTree,
          overrider,
          overriderType,
          overriderReturnType,
          overridden,
          overriddenType,
          overriddenReturnType);
    }

    @Override
    protected boolean checkTypeParameterLowerBound(int index) {
      return true;
    }
  }
}
