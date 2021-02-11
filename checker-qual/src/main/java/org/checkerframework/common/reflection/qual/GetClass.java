package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods like {@code Object.getClassName}. Their signature is:<br>
 * {@code @}{@link ClassBound}{@code ("ReceiverType") Class method(ReceiverType this) {...}}
 *
 * @checker_framework.manual #reflection-resolution Reflection resolution
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GetClass {}
