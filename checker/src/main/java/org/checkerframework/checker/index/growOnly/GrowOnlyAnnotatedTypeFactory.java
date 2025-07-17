package org.checkerframework.checker.index.growonly;

import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.index.qual.BottomGrowShrink;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.Shrinkable;
import org.checkerframework.checker.index.qual.UncheckedShrinkable;
import org.checkerframework.checker.index.qual.UnshrinkableRef;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * Implements the type introduction rules for the GrowOnly Checker. The GrowOnly Checker is a
 * sub-checker of the Index Checker designed to verify that certain collections never shrink in
 * size. This factory provides the default types for expressions, enabling the checker to work
 * correctly with unannotated code.
 *
 * <pre>
 * The type hierarchy is:
 *
 *         {@literal @}UnshrinkableRef (top)
 *              /             \
 *   {@literal @}GrowOnly    {@literal @}Shrinkable
 *                /               |
 *               /          {@literal @}UncheckedShrinkable
 *              /                  \
 *       {@literal @}BottomGrowShrink (bottom)
 * </pre>
 *
 * This class implements the following critical type introduction rule:
 *
 * <ul>
 *   <li>1. An object creation expression (e.g., {@code new ArrayList<>()}) that instantiates a
 *       subtype of {@code java.util.List} is given the {@code @BottomGrowShrink} annotation by
 *       default. This is the bottom type in the hierarchy, making it the most specific. This allows
 *       the new collection to be safely assigned to a variable of any type in the hierarchy (such
 *       as {@code @GrowOnly} or {@code @Shrinkable}), which provides maximum flexibility for the
 *       user. This default is only applied if the programmer has not written another annotation
 *       from this hierarchy.
 * </ul>
 */
public class GrowOnlyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The canonical @{@link GrowOnly} annotation. */
  public final AnnotationMirror GROW_ONLY;

  /** The canonical @{@link UnshrinkableRef} (top) annotation. */
  public final AnnotationMirror UNSHRINKABLE_REF;

  /** The canonical @{@link GrowOnly} (bottom) annotation. */
  public final AnnotationMirror BOTTOM_GROW_SHRINK;

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
    this.BOTTOM_GROW_SHRINK = AnnotationBuilder.fromClass(elements, BottomGrowShrink.class);
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

  /**
   * This method provides a default annotation for object creations. It is the correct hook for
   * implementing "type introduction" rule.
   */
  @Override
  public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
    super.addComputedTypeAnnotations(tree, type, useFlow);

    if (tree instanceof NewClassTree) {
      // Check if the type being created is a subtype of java.util.List.
      TypeElement listElement = elements.getTypeElement(List.class.getCanonicalName());
      if (listElement != null && types.isSubtype(type.getUnderlyingType(), listElement.asType())) {
        // If no other annotation from new type hierarchy is present, add @GrowOnly.
        if (!type.hasPrimaryAnnotationInHierarchy(this.UNSHRINKABLE_REF)) {
          type.addAnnotation(this.BOTTOM_GROW_SHRINK);
        }
      }
    }
  }
}
