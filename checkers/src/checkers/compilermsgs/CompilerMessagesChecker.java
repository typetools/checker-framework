package checkers.compilermsgs;

import javax.annotation.processing.SupportedOptions;

import checkers.compilermsgs.quals.CompilerMessageKey;
import checkers.propkey.PropertyKeyChecker;
import checkers.quals.Bottom;
import checkers.propkey.quals.PropertyKey;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A PropertyKeyChecker for the compiler message keys that are used
 * in the Checker framework.
 *
 * @author wmdietl
 */
@TypeQualifiers( {CompilerMessageKey.class, PropertyKey.class, Unqualified.class, Bottom.class} )
@SupportedOptions( {"propfiles", "bundlenames"} )
public class CompilerMessagesChecker extends PropertyKeyChecker {}
