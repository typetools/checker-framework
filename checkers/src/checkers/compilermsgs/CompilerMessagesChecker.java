package checkers.compilermsgs;

import javax.annotation.processing.SupportedOptions;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.propkey.PropertyKeyChecker;
import checkers.propkey.quals.PropertyKey;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A PropertyKeyChecker for the compiler message keys that are used
 * in the Checker framework.
 * 
 * @author wmdietl
 */
@TypeQualifiers( {CompilerMessageKey.class, PropertyKey.class, Unqualified.class} )
@SupportedOptions( {"propfiles", "bundlename"} )
public class CompilerMessagesChecker extends PropertyKeyChecker {
}
