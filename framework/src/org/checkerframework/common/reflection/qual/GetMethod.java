package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods like {@code Class.getMethod} and {@code Class.getDeclaredMethod}, whose
 * signature is: <br>
 * {@code {@link MethodVal}(classname=c, methodname=m, params=p) Method getMyMethod(Class<c> this,
 * String m, Object... params)}
 *
 * @checker_framework.manual #reflection-resolution Reflection resolution
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GetMethod {}
