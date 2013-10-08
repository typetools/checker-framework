package checkers.compilermsgs;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.propkey.PropertyKeyChecker;
import checkers.propkey.quals.PropertyKey;
import checkers.quals.Bottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A PropertyKeyChecker for the compiler message keys that are used
 * in the Checker framework.
 *
 * @author wmdietl
 */
@TypeQualifiers( {CompilerMessageKey.class, PropertyKey.class, Unqualified.class, Bottom.class} )
public class CompilerMessagesChecker extends PropertyKeyChecker {}
