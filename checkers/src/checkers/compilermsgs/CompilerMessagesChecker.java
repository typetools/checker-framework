package checkers.compilermsgs;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.propkey.PropertyKeyChecker;
import checkers.propkey.quals.PropertyKey;
import checkers.propkey.quals.PropertyKeyBottom;
import checkers.propkey.quals.UnknownPropertyKey;
import checkers.quals.TypeQualifiers;

/**
 * A PropertyKeyChecker for the compiler message keys that are used
 * in the Checker framework.
 *
 * @author wmdietl
 */
@TypeQualifiers( {CompilerMessageKey.class, PropertyKey.class,
    UnknownPropertyKey.class, PropertyKeyBottom.class} )
public class CompilerMessagesChecker extends PropertyKeyChecker {}
