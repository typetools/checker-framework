package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods like {@code Class.forName}. Their signature is
 *
 * <pre><code>
 * &nbsp;@ClassVal("name") Class method(@ForName String name) {...}
 * </code></pre>
 *
 * @checker_framework.manual #reflection-resolution Reflection resolution
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ForName {}
