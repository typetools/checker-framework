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
 * <p>For example, warnings issued by the Nullness Checker can be suppressed using
 * {@code @SuppressWarnings("nullness")} because {@link
 * org.checkerframework.checker.nullness.NullnessChecker} is annotated with
 * {@code @SuppressWarningsKey("nullness")}.
 *
 * <p>TODO: the previous paragraph about the Nullness Checker is out-of-date. We only consider this
 * meta-annotation on the most-concrete SourceChecker subclass and only use the suppression keys
 * listed there. For the Nullness Checker we add multiple suppression keys along the hierarchy.
 * Should we change the semantics of this annotation to look for it on all classes from the
 * most-concrete class up to SourceChecker? That would make the behavior consistent with e.g. our
 * SupportedOptions. Is there ever a reason where that would be unwanted?
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SuppressWarningsKeys {

    /**
     * @return array of strings, any one of which causes this checker to suppress a warning when
     *     passed as the argument of {@literal @}{@link SuppressWarnings}
     */
    String[] value();
}
