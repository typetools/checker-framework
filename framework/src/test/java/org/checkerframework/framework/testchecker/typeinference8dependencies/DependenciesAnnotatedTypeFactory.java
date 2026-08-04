package org.checkerframework.framework.testchecker.typeinference8dependencies;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.subtyping.qual.Bottom;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.DefaultQualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * The type factory for {@link DependenciesChecker}. Its type hierarchy is Bottom &lt;: Unqualified.
 */
public class DependenciesAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a {@code DependenciesAnnotatedTypeFactory}.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public DependenciesAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected void addCheckedCodeDefaults(QualifierDefaults defs) {
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, Bottom.class), TypeUseLocation.LOWER_BOUND);
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, Unqualified.class), TypeUseLocation.OTHERWISE);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<>(Arrays.asList(Unqualified.class, Bottom.class));
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new NoElementQualifierHierarchy(getSupportedTypeQualifiers(), elements, this) {
      @Override
      protected QualifierKindHierarchy createQualifierKindHierarchy(
          Collection<Class<? extends Annotation>> qualifierClasses) {
        return new DefaultQualifierKindHierarchy(qualifierClasses, Bottom.class);
      }
    };
  }
}
