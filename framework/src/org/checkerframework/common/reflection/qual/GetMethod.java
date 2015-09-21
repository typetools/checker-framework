package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods of the form:
 * <br>
 * <code>{@link MethodVal}(classname=c, methodname=m, params=p) Method
 * method(Class&lt;c&gt; this, String m, Object... params)</code>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface GetMethod {
}
