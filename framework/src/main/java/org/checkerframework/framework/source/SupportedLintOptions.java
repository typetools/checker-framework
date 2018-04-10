package org.checkerframework.framework.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: Are superclasses considered? Should we?
/**
 * An annotation used to indicate what lint options a checker supports. For example, if a checker
 * class (one that extends BaseTypeChecker) is annotated with
 * {@code @SupportedLintOptions({"dotequals"})}, then the checker accepts the command-line option
 * {@code -Alint=-dotequals}.
 *
 * <p>This annotation is optional and many checkers do not contain an {@code @SupportedLintOptions}
 * annotation.
 *
 * <p>The {@link SourceChecker#getSupportedLintOptions} method can construct its result from the
 * value of this annotation.
 *
 * @see org.checkerframework.framework.source.SupportedOptions
 * @checker_framework.manual #creating-compiler-interface The checker class: Compiler interface
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedLintOptions {
    String[] value();
}
