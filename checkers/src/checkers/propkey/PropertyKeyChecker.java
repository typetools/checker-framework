package checkers.propkey;

import checkers.basetype.BaseTypeChecker;
import checkers.propkey.quals.PropertyKey;
import checkers.quals.Bottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.source.SupportedOptions;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A type-checker that checks that only valid keys are used to access property files
 * and resource bundles.
 * Subclasses can specialize this class for the different uses of property files,
 * for examples see the I18n Checker and Compilermsgs Checker.
 *
 * Currently, the checker supports two methods to check:
 *
 * <ol>
 * <li value="1">Property files:
 * A common method for localization using a property file, mapping the
 * keys to values.
 * Programmers pass the property file locations via
 * {@code propfiles} option (e.g. {@code -Apropfiles=/path/to/messages.properties}),
 * separating multiple files by a colon ":".
 * </li>
 *
 * <li value="2">{@link ResourceBundle}:
 * Programmers pass the {@code baseName} name of the bundle via
 * {@code bundlename} (e.g. {@code -Abundlename=MyResource}.  The checker uses
 * the resource associated with the default {@link Locale} in the compilation
 * system.
 * </li>
 *
 * </ol>
 *
 * @checker.framework.manual #propkey-checker Property File Checker
 */
// Subclasses need something similar to this:
@TypeQualifiers( {PropertyKey.class, Unqualified.class, Bottom.class} )
@SupportedOptions( {"propfiles", "bundlenames"} )
public class PropertyKeyChecker extends BaseTypeChecker {
}
