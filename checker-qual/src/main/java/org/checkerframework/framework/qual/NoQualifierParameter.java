package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a declaration annotation that applies to type declarations. Some classes conceptually
 * take a type qualifier parameter. This annotation indicates that this class and its subclasses
 * explicitly do not do so. The only reason to write this annotation is when {@code
 * HasQualifierParameter} is enabled by default, by writing {@code HasQualifierParameter} on a
 * package.
 *
 * <p>When a class is {@code @NoQualifierParameter}, all its subclasses are as well.
 *
 * <p>One or more top qualifiers must be given for the hierarchies for which there are no qualifier
 * parameters. This annotation may not be written on the same class as {@code HasQualifierParameter}
 * for the same hierarchy.
 *
 * <p>It is an error for a superclass to be {@code @HasQualifierParameter} but a subclass to be
 * {@code @NoQualifierParameter} for the same hierarchy.
 *
 * @see HasQualifierParameter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface NoQualifierParameter {

    /**
     * Class of the top qualifier for the hierarchy for which this class has no qualifier parameter.
     *
     * @return the value
     */
    Class<? extends Annotation>[] value();
}
