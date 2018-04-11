package org.checkerframework.framework.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to use on an element of a dependent type qualifier to specify which elements of the
 * annotation should be interpreted as Java expressions. The type of the element must be an array of
 * Strings.
 *
 * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JavaExpression {}
