package checkers.i18n.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

/**
 * Indicates that the {@code String} type has been localized and
 * formatted for the target output locale
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LocalizableKey { }
