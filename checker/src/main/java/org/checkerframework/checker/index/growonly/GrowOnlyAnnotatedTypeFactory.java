package org.checkerframework.checker.index.growonly;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.checker.index.qual.BottomGrowShrink;
import org.checkerframework.checker.index.qual.CanShrink;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.UncheckedCanShrink;
import org.checkerframework.checker.index.qual.UnshrinkableRef;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

/** The type factory for the Grow-only Checker. */
public class GrowOnlyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a new GrowOnlyAnnotatedTypeFactory.
   *
   * @param checker the type-checker associated with this factory
   */
  @SuppressWarnings("this-escape")
  public GrowOnlyAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);

    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new LinkedHashSet<>(
        Arrays.asList(
            UnshrinkableRef.class,
            GrowOnly.class,
            CanShrink.class,
            UncheckedCanShrink.class,
            BottomGrowShrink.class));
  }
}
