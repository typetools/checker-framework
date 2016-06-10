package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The value's signedness is not known to the Unsignedness Checker.
 * This is also used for non-numeric values, which cannot have a signedness.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf( { } )
@DefaultQualifierInHierarchy
public @interface UnknownSignedness { }
