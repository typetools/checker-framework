package org.checkerframework.framework.testchecker.customapplier;

import java.lang.annotation.Annotation;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Adds a checked code default of {@code @CustomApplierBottom} for {@link TypeUseLocation#RETURN},
 * and uses an applier that ignores {@code @SubQual}.
 *
 * <p>A test file that also writes {@code @DefaultQualifier(value = SubQual.class, locations =
 * RETURN)} therefore produces a precedence list in which a {@code @SubQual} default for RETURN
 * precedes the {@code @CustomApplierBottom} default for RETURN. The second of those is redundant
 * under the standard applier, whose {@code addAnnotation} adds a qualifier only if the hierarchy is
 * empty, but not under this applier, which never applies the first of them.
 */
public class CustomApplierAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a CustomApplierAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public CustomApplierAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return Set.of(SuperQual.class, SubQual.class, CustomApplierBottom.class);
  }

  @Override
  protected void addCheckedCodeDefaults(QualifierDefaults defs) {
    super.addCheckedCodeDefaults(defs);
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(getElementUtils(), CustomApplierBottom.class),
        TypeUseLocation.RETURN);
  }

  @Override
  protected QualifierDefaults createQualifierDefaults() {
    return new CustomApplierQualifierDefaults(elements, this);
  }

  /** QualifierDefaults whose applier ignores {@code @SubQual}. */
  private static class CustomApplierQualifierDefaults extends QualifierDefaults {

    /**
     * Creates a CustomApplierQualifierDefaults.
     *
     * @param elements the element utilities
     * @param atypeFactory the type factory
     */
    public CustomApplierQualifierDefaults(Elements elements, AnnotatedTypeFactory atypeFactory) {
      super(elements, atypeFactory);
    }

    @Override
    protected DefaultApplierElement createDefaultApplierElement(
        AnnotatedTypeFactory atypeFactory,
        Element annotationScope,
        AnnotatedTypeMirror type,
        boolean applyToTypeVar) {
      return new IgnoreSubQualApplier(atypeFactory, annotationScope, type, applyToTypeVar);
    }

    /** An applier that ignores {@code @SubQual} and applies every other qualifier as usual. */
    private class IgnoreSubQualApplier extends DefaultApplierElement {

      /**
       * Creates an IgnoreSubQualApplier.
       *
       * @param atypeFactory the type factory
       * @param scope the element whose defaults are being applied
       * @param type the type to which to apply defaults
       * @param applyToTypeVar true if the default should be applied to the primary annotation of a
       *     local variable whose type is a type variable
       */
      public IgnoreSubQualApplier(
          AnnotatedTypeFactory atypeFactory,
          Element scope,
          AnnotatedTypeMirror type,
          boolean applyToTypeVar) {
        super(atypeFactory, scope, type, applyToTypeVar);
      }

      @Override
      protected void addAnnotation(AnnotatedTypeMirror type, AnnotationMirror qual) {
        if (AnnotationUtils.areSameByName(
            qual, "org.checkerframework.framework.testchecker.util.SubQual")) {
          return;
        }
        super.addAnnotation(type, qual);
      }
    }
  }
}
