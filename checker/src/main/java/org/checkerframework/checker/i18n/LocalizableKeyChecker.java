package org.checkerframework.checker.i18n;

import org.checkerframework.checker.propkey.PropertyKeyChecker;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.processing.SupportedOptions;

/**
 * A type-checker that checks that only valid localizable keys are used when using localizing
 * methods (e.g. {@link ResourceBundle#getString(String)}).
 *
 * <p>Currently, the checker supports two methods for localization checks:
 *
 * <ol>
 *   <li value="1">Properties files: A common method for localization using a properties file,
 *       mapping the localization keys to localized messages. Programmers pass the property file
 *       location via {@code propfiles} option (e.g. {@code
 *       -Apropfiles=/path/to/messages.properties}), separating multiple files by a colon ":".
 *   <li value="2">{@link ResourceBundle}: The proper recommended mechanism for localization.
 *       Programmers pass the {@code baseName} name of the bundle via {@code bundlename} (e.g.
 *       {@code -Abundlename=MyResource}. The checker uses the resource associated with the default
 *       {@link Locale} in the compilation system.
 * </ol>
 */
@SupportedOptions({"propfiles", "bundlenames"})
public class LocalizableKeyChecker extends PropertyKeyChecker {}
