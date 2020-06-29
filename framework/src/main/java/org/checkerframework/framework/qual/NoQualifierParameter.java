package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a declaration annotation that applies to type declarations. Some classes conceptually
 * take a type qualifier parameter. This annotations indicates that this class explicitly does not
 * do so. If {@code HasQualifierParameter} is enabled by default, for example, by writing {@code
 * HasQualifierParameter} on a package, then this annotation can disable it for a specific class.
 *
 * <p>One or more top qualifiers must be given for the hierarchies for which there are no qualifier
 * parameters. This annotation may not be written on the same class as {@code HasQualifierParameter}
 * for the same hierarchy.
 *
 * @see HasQualifierParameter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NoQualifierParameter {

    /**
     * Class of the top qualifier for the hierarchy for which this class has no qualifier parameter.
     *
     * @return the value
     */
    Class<? extends Annotation>[] value();
}
