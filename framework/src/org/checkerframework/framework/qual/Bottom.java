package org.checkerframework.framework.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * A special annotation intended solely for representing the bottom type in the qualifier hierarchy.
 * This qualifier is only used automatically if the existing qualifiers do not have a bottom type.
 *
 * <p>Other type systems could reuse this qualifier instead of introducing their own dedicated
 * bottom qualifier. However, this is usually a bad idea, because it will work only if the user
 * never runs two type systems together. Furthermore, because of the missing RetentionPolicy, this
 * qualifier will not be stored in bytecode. So, only use this qualifier during prototyping of very
 * simple type systems. For realistic systems, introduce a top and bottom qualifier that gets stored
 * in bytecode.
 *
 * <p>To use this qualifier, the type system designer needs to use methods like {@link
 * org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator#addTreeKind(com.sun.source.tree.Tree.Kind,
 * javax.lang.model.element.AnnotationMirror)} to add implicit annotations and needs to manually add
 * the bottom qualifier to the qualifier hierarchy.
 *
 * @see org.checkerframework.framework.type.QualifierHierarchy#getBottomAnnotations()
 */
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface Bottom {}
