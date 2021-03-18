package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods like {@code Class.getConstructor}, whose signature is: <br>
 * {@code @}{@link MethodVal}{@code (classname=c, methodname="<init>", params=p) Constructor<T>
 * method(Class<c> this, Object... params)}
 *
 * @checker_framework.manual #reflection-resolution Reflection resolution
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GetConstructor {}
