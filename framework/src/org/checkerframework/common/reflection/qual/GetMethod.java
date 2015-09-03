package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A @GetMethod annotation identifies the methods java.lang.Class.getMethod
 * and java.lang.Class.getDeclaredMethod.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface GetMethod {
}
