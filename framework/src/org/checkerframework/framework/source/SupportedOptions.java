package org.checkerframework.framework.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to indicate what Checker Framework options a checker supports.
 * The {@link SourceChecker#getSupportedOptions} method constructs its
 * result from the value of this annotation and additionally prefixing
 * the checker class name.
 *
 * In contrast to
 * {@link javax.annotation.processing.SupportedOptions},
 * note that this qualifier is {@link Inherited}.
 *
 * @see SupportedLintOptions
 * @see javax.annotation.processing.SupportedOptions
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedOptions {
    String[] value();
}
