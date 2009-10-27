package checkers.localizing;

import checkers.basetype.BaseTypeChecker;
import checkers.localizing.quals.LocalizedString;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A type-checker that checks that only localized {@Code String} are visible
 * to the user
 */
@TypeQualifiers( {LocalizedString.class, Unqualified.class} )
public class LocalizingChecker extends BaseTypeChecker { }
