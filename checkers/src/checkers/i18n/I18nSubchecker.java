package checkers.i18n;

import checkers.basetype.BaseTypeChecker;
import checkers.i18n.quals.Localized;
import checkers.i18n.quals.UnknownLocalized;
import checkers.quals.TypeQualifiers;

/**
 * A type-checker that checks that only localized {@code String}s are visible
 * to the user.
 *
 * @checker_framework_manual #i18n-checker Internationalization Checker
 */
@TypeQualifiers( {Localized.class, UnknownLocalized.class} )
public class I18nSubchecker extends BaseTypeChecker {
}
