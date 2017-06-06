package org.checkerframework.checker.propkey;

import java.util.Locale;
import java.util.ResourceBundle;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A type-checker that checks that only valid keys are used to access property files and resource
 * bundles. Subclasses can specialize this class for the different uses of property files, for
 * examples see the I18n Checker and Compiler Messages Checker.
 *
 * <p>Currently, the checker supports two methods to check:
 *
 * <ol>
 *   <li value="1">Property files: A common method for localization using a property file, mapping
 *       the keys to values. Programmers pass the property file locations via {@code propfiles}
 *       option (e.g. {@code -Apropfiles=/path/to/messages.properties}), separating multiple files
 *       by a colon ":".
 *   <li value="2">{@link ResourceBundle}: Programmers pass the {@code baseName} name of the bundle
 *       via {@code bundlename} (e.g. {@code -Abundlename=MyResource}. The checker uses the resource
 *       associated with the default {@link Locale} in the compilation system.
 * </ol>
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
// Subclasses need something similar to this:
@SupportedOptions({"propfiles", "bundlenames"})
@RelevantJavaTypes(CharSequence.class)
public class PropertyKeyChecker extends BaseTypeChecker {}
