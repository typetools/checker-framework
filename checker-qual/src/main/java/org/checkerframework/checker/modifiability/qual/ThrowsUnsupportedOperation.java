package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the method's implementation always throws {@link UnsupportedOperationException},
 * as {@code AbstractList.set()} does.
 *
 * <p>Write this annotation on a skeletal implementation, such as one in {@code
 * java.util.AbstractList}, that a subclass is expected to override. A subclass that inherits the
 * implementation without overriding it does not support the operation, even if the subclass
 * declares that it does. The Modifiability Checker issues an error for such a subclass; without
 * this annotation, it could not, because the body of an inherited method is compiled separately and
 * the checker sees only its signature.
 *
 * <p>The Modifiability Checker verifies the annotation on any method it compiles: the body must be
 * exactly {@code throw new UnsupportedOperationException(...)}.
 *
 * <p>Do not write this annotation on a method that throws {@link UnsupportedOperationException}
 * only because some other method does, such as {@code AbstractList.add(E)}, whose body is {@code
 * add(size(), e)}. A subclass that overrides the other method makes such a method work.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ThrowsUnsupportedOperation {}
