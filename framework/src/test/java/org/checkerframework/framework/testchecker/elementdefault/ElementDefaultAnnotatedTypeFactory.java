package org.checkerframework.framework.testchecker.elementdefault;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.Critical;
import org.checkerframework.framework.testchecker.util.Odd;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Registers defaults through {@code QualifierDefaults.addElementDefault}. {@code @Critical} is an
 * alias for {@code @SubQual}. On every class with the given name, this registers:
 *
 * <ul>
 *   <li>{@code ElementDefault}: {@code @SubQual} for {@link TypeUseLocation#RETURN}, and {@code
 *       Critical} for the same location.
 *   <li>{@code ElementDefaultConflict}: {@code @SubQual} for {@link TypeUseLocation#RETURN}, and
 *       {@code @Odd} followed by {@code @SubQual} for {@link TypeUseLocation#PARAMETER}.
 *   <li>{@code ElementDefaultEnclosing}: on its member class {@code Inner}, {@code @Critical} for
 *       {@link TypeUseLocation#RETURN}.
 *   <li>{@code ElementDefaultLate}: {@code @SubQual} for {@link TypeUseLocation#RETURN}; then,
 *       after computing the type of its method {@code first}, {@code @SubQual} for {@link
 *       TypeUseLocation#PARAMETER}.
 * </ul>
 *
 * <p>The test files then check how such a default combines with the {@code @DefaultQualifier}
 * annotations that apply to the same element.
 */
public class ElementDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @SubQual} annotation. */
  private final AnnotationMirror SUB_QUAL;

  /** The {@code @Critical} annotation, which this type system treats as an alias for SubQual. */
  private final AnnotationMirror CRITICAL;

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
    CRITICAL = AnnotationBuilder.fromClass(getElementUtils(), Critical.class);
    ODD = AnnotationBuilder.fromClass(getElementUtils(), Odd.class);
    addAliasedTypeAnnotation(Critical.class, SUB_QUAL);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return Set.of(SuperQual.class, SubQual.class);
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
          // The alias is canonicalized to @SubQual, so it duplicates the previous default rather
          // than conflicting with it.
          defaults.addElementDefault(classElt, CRITICAL, TypeUseLocation.RETURN);
        } else if (className.contentEquals("ElementDefaultConflict")) {
          defaults.addElementDefault(classElt, SUB_QUAL, TypeUseLocation.RETURN);
          // This type system does not support @Odd, so the @Odd default is ignored.  In
          // particular, it does not conflict with the @SubQual default for the same location.
          defaults.addElementDefault(classElt, ODD, TypeUseLocation.PARAMETER);
          defaults.addElementDefault(classElt, SUB_QUAL, TypeUseLocation.PARAMETER);
        } else if (className.contentEquals("ElementDefaultEnclosing")) {
          for (TypeElement member : ElementFilter.typesIn(classElt.getEnclosedElements())) {
            if (member.getSimpleName().contentEquals("Inner")) {
              defaults.addElementDefault(member, CRITICAL, TypeUseLocation.RETURN);
            }
          }
        } else if (className.contentEquals("ElementDefaultLate")) {
          defaults.addElementDefault(classElt, SUB_QUAL, TypeUseLocation.RETURN);
          for (ExecutableElement method : ElementFilter.methodsIn(classElt.getEnclosedElements())) {
            if (method.getSimpleName().contentEquals("first")) {
              // Computing this type computes the defaults that apply within the class.
              getAnnotatedType(method);
            }
          }
          defaults.addElementDefault(classElt, SUB_QUAL, TypeUseLocation.PARAMETER);
        }
      }
    }
  }
}
