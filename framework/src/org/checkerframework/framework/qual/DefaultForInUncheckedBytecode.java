package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to the declaration of a type qualifier specifies that
 * the given annotation should be the default for a particular location,
 * only when the symbol is from untyped code, this qualifier will not
 * apply if unsafeDefaultsForUncheckedBytecode command line option is passed.
 *
 * TODO: Document use relative to the other annotations.
 * This qualifier is for type system developers, not end-users.
 *
 * @see DefaultLocation
 * @see DefaultQualifier
 * @see DefaultQualifierInUncheckedBytecode
 * @see ImplicitFor
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultForInUncheckedBytecode {
    /**
     * @return the locations to which the annotation should be applied
     */
    DefaultLocation[] value();
}
