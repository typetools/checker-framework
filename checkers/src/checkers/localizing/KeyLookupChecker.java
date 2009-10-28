package checkers.localizing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;

import checkers.basetype.BaseTypeChecker;
import checkers.localizing.quals.LocalizableKey;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

/**
 * A type-checker that checks that only valid localizable keys are used
 * when using localizing methods
 * (e.g. {@link ResourceBundle#getString(String)}).
 *
 * Currently, the checker supports two methods for localization checks:
 *
 * <ol>
 * <li value="1">Properties files:
 * A common method for localization using a properties file, mapping the
 * localization keys to localized messages.
 * Programmers pass the property file location via
 * {@code propfile} option (e.g. {@code -Apropfile=/path/to/messages.properties})
 * </li>
 *
 * <li value="2">{@link ResourceBundle}:
 * The proper recommended mechanism for localization.
 * Prograppers pass the {@code baseName} name of the bundle via
 * {@code bundlename} (e.g. {@code -Abundlename=MyResource}.  The checker uses
 * the resource associdated with the default {@link Locale} in the compilation
 * system.
 * </li>
 *
 * </ol>
 */
@TypeQualifiers( {LocalizableKey.class, Unqualified.class} )
@SupportedOptions( {"propfile", "bundlename"} )
public class KeyLookupChecker extends BaseTypeChecker {

    private Set<String> localizableKeys;

    @Override
    public void init(ProcessingEnvironment env) {
        super.init(env);
        this.localizableKeys =
            Collections.unmodifiableSet(buildLocalizableKeys(env.getOptions()));
    }

    /**
     * Returns a set of the valid keys that can be localized.
     */
    public Set<String> getLocalizableKeys() {
        return this.localizableKeys;
    }

    private Set<String> buildLocalizableKeys(Map<String, String> options) {
        if (options.containsKey("propfile"))
            return keysOfPropertyFile(env.getOptions().get("propfile"));
        if (options.containsKey("bundlename"))
            return keysOfResourceBundle(env.getOptions().get("bundlename"));

        return null;
    }

    private Set<String> keysOfPropertyFile(String name) {
        try {
            Properties prop = new Properties();

            InputStream in = null;

            try {
                in = new FileInputStream(name);
            } catch (FileNotFoundException e) { }

            in = this.getClass().getClassLoader().getResourceAsStream(name);

            if (in == null) {
                System.err.println("Couldn't find the properties file: " + name);
                return Collections.emptySet();
            }

            prop.load(in);
            return prop.stringPropertyNames();
        } catch (Exception e) { }
        return null;
    }

    private Set<String> keysOfResourceBundle(String bundleName) {
        ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
        return bundle.keySet();
    }

}
