package checkers.i18n.quals;

import java.lang.annotation.*;

import checkers.propkey.quals.PropertyKey;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that the {@code String} is a key into a property file
 * or resource bundle containing Localized Strings.
 */
@TypeQualifier
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LocalizableKey {}
