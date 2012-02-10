package checkers.i18n;

import checkers.basetype.BaseTypeChecker;
import checkers.i18n.quals.Localized;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A type-checker that checks that only localized {@code String} are visible
 * to the user.
 */
@TypeQualifiers( {Localized.class, Unqualified.class} )
public class SubI18Checker extends BaseTypeChecker { }
