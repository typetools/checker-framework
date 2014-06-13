package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * {@link Analyzable} is a method annotation that indicates the method can
 * attempt to be resolved. Classes covered by this checker (wrappers and
 * Strings) are automatically considered Analyzable, and no other classes are
 * currently intended to be able to be constructed at compile-time. This is why @Analyzable
 * is currently restricted to Methods and not Constructors
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Analyzable {
}