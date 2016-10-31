package org.checkerframework.framework.qual;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Applied to a declaration of a package, type, method, variable, etc.,
 * specifies that the given annotation should be the default.  The default is
 * applied to type uses within the declaration for which no other
 * annotation is explicitly written.
 * (The default is not applied to the "parametric locations":  class
 * declarations, type parameter declarations, and type parameter uses.)
 * If multiple {@code DefaultQualifier} annotations are in scope, the innermost one
 * takes precedence.
 * DefaultQualifier takes precedence over {@link DefaultQualifierInHierarchy}.
 * <p>
 *
 * If you wish to write multiple {@code @DefaultQualifier} annotations (for
 * unrelated type systems, or with different {@code locations} fields) at
 * the same location, use {@link DefaultQualifiers}.
 *
 * @see TypeUseLocation
 * @see DefaultQualifiers
 * @see DefaultQualifierInHierarchy
 * @see DefaultFor
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({PACKAGE, TYPE, CONSTRUCTOR, METHOD, FIELD, LOCAL_VARIABLE, PARAMETER})
public @interface DefaultQualifier {

    /**
     * The Class for the default annotation.
     *
     * <p>
     *
     * To prevent affecting other type systems, always specify an annotation
     * in your own type hierarchy.  (For example, do not set
     * "org.checkerframework.framework.qual.Unqualified" as the default.)
     */
    Class<? extends Annotation> value();

    /** @return the locations to which the annotation should be applied */
    TypeUseLocation[] locations() default {TypeUseLocation.ALL};
}
