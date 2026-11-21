package org.checkerframework.framework.testchecker.customshouldbeannotated;

import java.lang.annotation.Annotation;
import java.util.Set;
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

/**
 * Uses an applier whose {@code shouldBeAnnotated} rejects every node while a default for {@link
 * TypeUseLocation#FIELD} is being applied, so that no FIELD default has any effect.
 *
 * <p>That override reads {@code DefaultApplierElement.location}, which is correct only if the
 * location is set before the traversal calls {@code shouldBeAnnotated} on the node. (A FIELD
 * default annotates the node being visited, so {@code shouldBeAnnotated} is not called again once
 * the location is known, as it is for a RETURN default.)
 */
public class CustomShouldBeAnnotatedAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a CustomShouldBeAnnotatedAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public CustomShouldBeAnnotatedAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return Set.of(SuperQual.class, SubQual.class, CustomShouldBeAnnotatedBottom.class);
  }

  @Override
  protected QualifierDefaults createQualifierDefaults() {
    return new CustomShouldBeAnnotatedQualifierDefaults(elements, this);
  }

  /** QualifierDefaults whose applier applies no FIELD default. */
  private static class CustomShouldBeAnnotatedQualifierDefaults extends QualifierDefaults {

    /**
     * Creates a CustomShouldBeAnnotatedQualifierDefaults.
     *
     * @param elements the element utilities
     * @param atypeFactory the type factory
     */
    public CustomShouldBeAnnotatedQualifierDefaults(
        Elements elements, AnnotatedTypeFactory atypeFactory) {
      super(elements, atypeFactory);
    }

    @Override
    protected DefaultApplierElement createDefaultApplierElement(
        AnnotatedTypeFactory atypeFactory,
        Element annotationScope,
        AnnotatedTypeMirror type,
        boolean applyToTypeVar) {
      return new SkipFieldApplier(atypeFactory, annotationScope, type, applyToTypeVar);
    }

    /** An applier that applies no FIELD default and applies every other default as usual. */
    private class SkipFieldApplier extends DefaultApplierElement {

      /**
       * Creates a SkipFieldApplier.
       *
       * @param atypeFactory the type factory
       * @param scope the element whose defaults are being applied
       * @param type the type to which to apply defaults
       * @param applyToTypeVar true if the default should be applied to the primary annotation of a
       *     local variable whose type is a type variable
       */
      public SkipFieldApplier(
          AnnotatedTypeFactory atypeFactory,
          Element scope,
          AnnotatedTypeMirror type,
          boolean applyToTypeVar) {
        super(atypeFactory, scope, type, applyToTypeVar);
      }

      @Override
      protected boolean shouldBeAnnotated(AnnotatedTypeMirror type, boolean applyToTypeVar) {
        if (location == TypeUseLocation.FIELD) {
          return false;
        }
        return super.shouldBeAnnotated(type, applyToTypeVar);
      }
    }
  }
}
