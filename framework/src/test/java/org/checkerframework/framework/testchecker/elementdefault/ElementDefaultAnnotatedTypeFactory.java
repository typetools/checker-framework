package org.checkerframework.framework.testchecker.elementdefault;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.Odd;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Registers defaults through {@code QualifierDefaults.addElementDefault}: a {@code @SubQual}
 * default for {@link TypeUseLocation#RETURN} on every class named {@code ElementDefault}, and a
 * {@code @SubQual} default for {@link TypeUseLocation#FIELD} on every class named {@code
 * ElementDefaultPrecedence}.
 *
 * <p>The test files then check that such a default composes with the {@code @DefaultQualifier}
 * annotations on the same element and on the element's enclosing scopes, rather than suppressing
 * them, and that it takes precedence over them.
 */
public class ElementDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @SubQual} annotation. */
  private final AnnotationMirror SUB_QUAL;

  /** The {@code @Odd} annotation, which this type system does not support. */
  private final AnnotationMirror ODD;

  /**
   * Creates an ElementDefaultAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public ElementDefaultAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    SUB_QUAL = AnnotationBuilder.fromClass(getElementUtils(), SubQual.class);
    ODD = AnnotationBuilder.fromClass(getElementUtils(), Odd.class);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return Set.of(
        SuperQual.class, SubQual.class, ElementDefaultQual.class, ElementDefaultBottom.class);
  }

  @Override
  public void setRoot(@Nullable CompilationUnitTree root) {
    super.setRoot(root);
    if (root == null) {
      return;
    }
    for (Tree typeDecl : root.getTypeDecls()) {
      if (typeDecl.getKind() == Tree.Kind.CLASS) {
        TypeElement classElt = TreeUtils.elementFromDeclaration((ClassTree) typeDecl);
        if (classElt == null) {
          continue;
        }
        Name className = classElt.getSimpleName();
        if (className.contentEquals("ElementDefault")) {
          defaults.addElementDefault(classElt, SUB_QUAL, TypeUseLocation.RETURN);
          // A qualifier of some other type system is tolerated; it has no effect, and it does
          // not prevent another default from applying at the same location.
          defaults.addElementDefault(classElt, ODD, TypeUseLocation.PARAMETER);
        } else if (className.contentEquals("ElementDefaultPrecedence")) {
          defaults.addElementDefault(classElt, SUB_QUAL, TypeUseLocation.FIELD);
        }
      }
    }
  }
}
