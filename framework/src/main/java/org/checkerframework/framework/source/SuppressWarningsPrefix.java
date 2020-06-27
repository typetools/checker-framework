package org.checkerframework.framework.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the prefixes or checkernames that suppress warnings issued by this checker. When used
 * as the argument to {@link SuppressWarnings}, any of the given arguments suppresses all warnings
 * related to the checker. They can also be used as a prefix, followed by a colon and a message key.
 *
 * <p>In order for this annotation to have an effect, it must be placed on the declaration of a
 * class that extends {@link SourceChecker}.
 *
 * <p>If this annotation is not present on a checker class, then the lowercase name of the checker
 * is used by default. The name of the checker is the part of the checker classname that comes
 * before "Checker" or "Subchecker". If the checker classname is not of this form, then the
 * classname is the checker name.)
 *
 * @checker_framework.manual #suppresswarnings-annotation-syntax
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
// In the manual section #suppresswarnings-annotation-syntax, the term checkername is used instead
// of prefix.
public @interface SuppressWarningsPrefix {

    /**
     * Returns array of strings, any one of which causes this checker to suppress a warning when
     * passed as the argument of {@literal @}{@link SuppressWarnings}.
     *
     * @return array of strings, any one of which causes this checker to suppress a warning when
     *     passed as the argument of {@literal @}{@link SuppressWarnings}
     */
    String[] value();
}
