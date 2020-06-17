package org.checkerframework.framework.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the argument(s) that this checker recognizes for suppressing warnings via the {@link
 * SuppressWarnings} annotation. Any of the given arguments suppresses all warnings related to the
 * checker.
 *
 * <p>In order for this annotation to have an effect, it must be placed on the declaration of a
 * class that extends {@link SourceChecker}.
 *
 * <p>If this annotation is not present on a checker class, then the name of the checker is used by
 * default. (The name of the checker is the part of the checker classname that comes before Checker
 * or Subchecker. If the checker classname is not of this form, then the classname is used.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SuppressWarningsKeys {

    /**
     * Returns array of strings, any one of which causes this checker to suppress a warning when
     * passed as the argument of {@literal @}{@link SuppressWarnings}.
     *
     * @return array of strings, any one of which causes this checker to suppress a warning when
     *     passed as the argument of {@literal @}{@link SuppressWarnings}
     */
    String[] value();
}
