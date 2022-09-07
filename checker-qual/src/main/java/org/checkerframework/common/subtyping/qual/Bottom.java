package org.checkerframework.common.subtyping.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * A special annotation intended solely for representing the bottom type in the qualifier hierarchy.
 * It should not be used! Instead, each type system should define its own dedicated bottom type.
 *
 * <p>This qualifier is used automatically if the existing qualifiers do not have a bottom type.
 * This only works the user never runs two type systems together. Furthermore, because it has no
 * {@code @RetentionPolicy} meta-annotation, this qualifier will not be stored in bytecode. So, only
 * use this qualifier during prototyping of very simple type systems. For realistic systems,
 * introduce a top and bottom qualifier that gets stored in bytecode.
 *
 * <p>To use this qualifier, the type system designer needs to use methods like {@code
 * ImplicitsTreeAnnotator.addTreeKind()} to add default annotations and needs to manually add the
 * bottom qualifier to the qualifier hierarchy.
 *
 * <p>See {@code QualifierHierarchy.getBottomAnnotations()}
 *
 * @checker_framework.manual #subtyping-checker Subtyping Checker
 */
@Documented
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface Bottom {}
