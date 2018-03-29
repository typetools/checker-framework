package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to the declaration of a type qualifier specifies that the given annotation should be the
 * default for a particular location.
 *
 * <p>TODO: Document use relative to the other annotations. This qualifier is for type system
 * developers, not end-users.
 *
 * @see TypeUseLocation
 * @see DefaultQualifier
 * @see DefaultQualifierInHierarchy
 * @see ImplicitFor
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultFor {
    /** @return the locations to which the annotation should be applied */
    TypeUseLocation[] value();
}
