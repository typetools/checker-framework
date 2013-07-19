package checkers.i18n;

import checkers.basetype.BaseTypeChecker;
import checkers.i18n.quals.Localized;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A type-checker that checks that only localized {@code String}s are visible
 * to the user.
 *
 * @checker.framework.manual #i18n-checker Internationalization Checker
 */
@TypeQualifiers( {Localized.class, Unqualified.class} )
public class I18nSubchecker extends BaseTypeChecker<I18nAnnotatedTypeFactory> {
}
