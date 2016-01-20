package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.*;

import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import com.sun.source.tree.Tree;

/**
 * Indicates that the annotated reference is an immutable reference to an
 * immutable object.
 * <p>
 *
 * An Immutable object cannot be modified. Its fields may be reassigned or
 * mutated only if they are explicitly marked as Mutable or Assignable.
 *
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(AssignsFields.class)
@ImplicitFor(
        trees = { Tree.Kind.NEW_CLASS },
        types = { TypeKind.ARRAY }
)
@DefaultQualifierInHierarchy
@DefaultFor({ DefaultLocation.IMPLICIT_UPPER_BOUNDS,
              DefaultLocation.IMPLICIT_LOWER_BOUNDS,
              DefaultLocation.EXCEPTION_PARAMETER})
public @interface Mutable {}
