package org.checkerframework.checker.index.growonly;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.qual.BottomGrowShrink;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.Shrinkable;
import org.checkerframework.checker.index.qual.UncheckedShrinkable;
import org.checkerframework.checker.index.qual.UnshrinkableRef;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.javacutil.AnnotationBuilder;

/** The type factory for the GrowOnly Checker. */
public class GrowOnlyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The canonical @{@link GrowOnly} annotation. */
  public final AnnotationMirror GROW_ONLY;

  /** The canonical @{@link UnshrinkableRef} (top) annotation. */
  public final AnnotationMirror UNSHRINKABLE_REF;

  /**
   * Creates a new GrowOnlyAnnotatedTypeFactory.
   *
   * @param checker the type-checker associated with this factory
   */
  @SuppressWarnings("this-escape")
  public GrowOnlyAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);

    this.GROW_ONLY = AnnotationBuilder.fromClass(elements, GrowOnly.class);
    this.UNSHRINKABLE_REF = AnnotationBuilder.fromClass(elements, UnshrinkableRef.class);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            UnshrinkableRef.class,
            GrowOnly.class,
            Shrinkable.class,
            UncheckedShrinkable.class,
            BottomGrowShrink.class));
  }
}
