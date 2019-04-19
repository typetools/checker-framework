package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotation indicating that the method's receiver has a qualified type.
 *
 * <p>The following two code snippets have identical effect:
 *
 * <pre><code>
 *  void myMethod(@Anno MyClass this, int x) { ... }
 *
 * {@literal @}ReceiverQualifier(Anno.class)
 *  void myMethod(int x) { ... }
 * </code></pre>
 *
 * The latter is necessary for methods of anonymous classes, because Java does not permit writing
 * the {@code this} formal parameter in such a method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@InheritedAnnotation
@Repeatable(ReceiverQualifiers.class)
public @interface ReceiverQualifier {
    /** The qualifier on the receiver argument type. */
    Class<? extends Annotation> qualifier();
}
