package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation wrapper to allow multiple receiver annotations.
 *
 * <p>Programmers generally do not need to use this; it is created by Java when a programmer writes
 * more than one {@code @ReceiverQualifier} annotation at the same location.
 *
 * @see ReceiverQualifier
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InheritedAnnotation
public @interface ReceiverQualifiers {
    ReceiverQualifier[] value();
}
