package org.checkerframework.framework.qual;

import java.lang.annotation.Target;

/**
 * A special annotation intended solely for representing the bottom type in
 * the qualifier hierarchy.
 * This qualifier is only used automatically if the existing qualifiers do not have a
 * bottom type.
 * Other type systems could reuse this qualifier instead of introducing their own
 * dedicated bottom qualifier. The programmer would then use methods like
 * {@link org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator#addTreeKind(com.sun.source.tree.Tree.Kind, javax.lang.model.element.AnnotationMirror)} to
 * add implicit annotations and needs to manually add the bottom qualifier to the
 * qualifier hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the Checker Framework.
 *
 * Note that because of the missing RetentionPolicy, the qualifier will
 * not be stored in bytecode.
 * <p>
 * 
 * Only use this qualifier for very simple type systems.
 * For realistic systems, introduce a top and bottom qualifier
 * that gets stored in bytecode.
 *
 * @see org.checkerframework.framework.type.QualifierHierarchy#getBottomAnnotations()
 */
@TypeQualifier
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface Bottom { }
