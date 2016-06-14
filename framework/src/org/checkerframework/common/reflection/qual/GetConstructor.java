package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods of the form:
 * <br>
 * {@code {@link MethodVal}(classname=c, methodname="<init>", params=p)
 * Constructor<T> method(Class<c> this, Object... params)}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface GetConstructor {
}
