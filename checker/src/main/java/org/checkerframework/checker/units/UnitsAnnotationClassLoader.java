package org.checkerframework.checker.units;

import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.units.qual.UnitsMultiple;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotationClassLoader;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class UnitsAnnotationClassLoader extends AnnotationClassLoader {

  public UnitsAnnotationClassLoader(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Custom filter for units annotations:
   *
   * <p>This filter will ignore (by returning false) any units annotation which is an alias of
   * another base unit annotation (identified via {@link UnitsMultiple} meta-annotation). Alias
   * annotations can still be used in source code; they are converted into a base annotation by
   * {@link UnitsAnnotatedTypeFactory#canonicalAnnotation(AnnotationMirror)}. This filter simply
   * makes sure that the alias annotations themselves don't become part of the type hierarchy as
   * their base annotations already are in the hierarchy.
   */
  @Override
  protected boolean isSupportedAnnotationClass(Class<? extends Annotation> annoClass) {
    // build the initial annotation mirror (missing prefix)
    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, annoClass);
    AnnotationMirror initialResult = builder.build();

    // further refine to see if the annotation is an alias of some other SI Unit annotation
    for (AnnotationMirror metaAnno :
        initialResult.getAnnotationType().asElement().getAnnotationMirrors()) {
      // TODO : special treatment of invisible qualifiers?

      // if the annotation is a SI prefix multiple of some base unit, then return false
      // classic Units checker does not need to load the annotations of SI prefix multiples of
      // base units
      if (AnnotationUtils.areSameByName(
          metaAnno, "org.checkerframework.checker.units.qual.UnitsMultiple")) {
        return false;
      }
    }

    // Not an alias unit
    return true;
  }
}
