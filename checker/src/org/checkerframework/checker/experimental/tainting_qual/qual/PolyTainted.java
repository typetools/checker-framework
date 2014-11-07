package org.checkerframework.checker.experimental.tainting_qual.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A polymorphic qualifier for the Tainting type system.
 *
 * @checker_framework_manual #tainting-checker Tainting Checker
 */
@Documented
@TypeQualifier
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyTainted {}
