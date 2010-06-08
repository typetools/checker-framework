package checkers.propkey;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;

import checkers.basetype.BaseTypeChecker;
import checkers.propkey.quals.PropertyKey;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;


/**
 * A type-checker that checks that only valid keys are used to access property files
 * and resource bundles.
 * Subclasses can specialize this class for the different uses of property files,
 * for examples see the i18n and compilermsgs checkers.
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
 */
// Subclasses need something similar to this:
@TypeQualifiers( {PropertyKey.class, Unqualified.class} )
// Subclasses need exactly this:
@SupportedOptions( {"propfiles", "bundlename"} )
public class PropertyKeyChecker extends BaseTypeChecker {

    private Set<String> lookupKeys;

    @Override
    public void init(ProcessingEnvironment env) {
        super.init(env);
        this.lookupKeys =
            Collections.unmodifiableSet(buildLookupKeys(env.getOptions()));
    }

    /**
     * Returns a set of the valid keys that can be used.
     */
    public Set<String> getLookupKeys() {
        return this.lookupKeys;
    }

    private Set<String> buildLookupKeys(Map<String, String> options) {
        if (options.containsKey("propfiles"))
            return keysOfPropertyFiles(env.getOptions().get("propfiles"));
        if (options.containsKey("bundlename"))
            return keysOfResourceBundle(env.getOptions().get("bundlename"));

        return Collections.emptySet();
    }

	private Set<String> keysOfPropertyFiles(String names) {
		String[] namesArr = names.split(":");

		if (namesArr == null) {
			System.err.println("Couldn't find the files: <" + names + ">");
			return Collections.emptySet();
		}

		Set<String> result = new HashSet<String>();

		for (String name : namesArr) {
			try {
				Properties prop = new Properties();

				InputStream in = null;

				ClassLoader cl = this.getClass().getClassLoader();
				if (cl == null) {
					// the class loader is null if the system class loader was
					// used
					cl = ClassLoader.getSystemClassLoader();
				}
				in = cl.getResourceAsStream(name);

				if (in == null) {
					// if the classloader didn't manage to load the file, try
					// whether a FileInputStream works. For absolute paths this
					// might help.
					try {
						in = new FileInputStream(name);
					} catch (FileNotFoundException e) {
						// ignore
					}
				}

				if (in == null) {
					System.err.println("Couldn't find the properties file: " + name);
					// report(Result.failure("propertykeychecker.filenotfound",
					// name), null);
					return Collections.emptySet();
				}

				prop.load(in);
				result.addAll(prop.stringPropertyNames());
			} catch (Exception e) {
				// TODO: is there a nicer way to report messages, that are not
				// connected to an AST node?
				// One cannot use report, because it needs a node.
				System.err.println("Exception in PropertyKeyChecker.keysOfPropertyFile: " + e);
				e.printStackTrace();
			}
		}

		return result;
	}

	private Set<String> keysOfResourceBundle(String bundleName) {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		if (bundle == null) {
			System.err.println("Couldn't find the bundle: <" + bundleName
					+ "> for locale <" + Locale.getDefault() + ">");
			return Collections.emptySet();
		}

		return bundle.keySet();
	}

}
