package org.checkerframework.framework.test;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SimpleOptionMap is a very basic Option container. The keys of the Option container are the set of
 * Options and the values are the arguments to those options if they exists: e.g.,
 *
 * <pre>{@code
 * Map(
 *    "-AprintAllQualifiers" => null
 *    "-classpath" => "myDir1:myDir2"
 * )
 * }</pre>
 *
 * This class is mainly used by TestConfigurationBuilder to make working with existing options
 * simpler and less error prone. It is not intended for a general Option container because users
 * creating tests via source code can more easily manipulate the map whereas a lot of sugar would be
 * needed to make this class usable from the command line.
 */
public class SimpleOptionMap {
    /** A Map from optionName to arg, where arg is null if the option doesn't require any args. */
    private final Map<String, @Nullable String> options = new LinkedHashMap<>();

    /**
     * Clears the current set of options and copies the input options to this map.
     *
     * @param options the new options to use for this object
     */
    public void setOptions(Map<String, @Nullable String> options) {
        this.options.clear();
        this.options.putAll(options);
    }

    /**
     * A method to easily add Strings to an option that takes a filepath as an argument.
     *
     * @param key an option with an argument of the form "arg1[path-separator]arg2..." e.g., "-cp
     *     myDir:myDir2:myDir3"
     * @param toAppend a string to append onto the path or, if the path is null/empty, the argument
     *     to the option indicated by key
     */
    public void addToPathOption(String key, String toAppend) {
        if (toAppend == null) {
            throw new IllegalArgumentException("Null string appended to sourcePath.");
        }

        String path = options.get(key);

        if (toAppend.startsWith(File.pathSeparator)) {
            if (path == null || path.isEmpty()) {
                path = toAppend.substring(1, toAppend.length());
            } else {
                path += toAppend;
            }
        } else {
            if (path == null || path.isEmpty()) {
                path = toAppend;
            } else {
                path += File.pathSeparator + toAppend;
            }
        }

        addOption(key, path);
    }

    /**
     * Adds an option that takes no argument.
     *
     * @param option the no-argument option to add to this object
     */
    public void addOption(String option) {
        this.options.put(option, null);
    }

    /**
     * Adds an option that takes an argument.
     *
     * @param option the option to add to this object
     * @param value the argument to the option
     */
    public void addOption(String option, String value) {
        this.options.put(option, value);
    }

    /**
     * Adds the option only if value is a non-null, non-empty String.
     *
     * @param option the option to add to this object
     * @param value the argument to the option (or null)
     */
    public void addOptionIfValueNonEmpty(String option, String value) {
        if (value != null && !value.isEmpty()) {
            addOption(option, value);
        }
    }

    /**
     * Adds all of the options in the given map to this one.
     *
     * @param options the options to add to this object
     */
    public void addOptions(Map<String, @Nullable String> options) {
        this.options.putAll(options);
    }

    public void addOptions(Iterable<String> newOptions) {
        Iterator<String> optIter = newOptions.iterator();
        while (optIter.hasNext()) {
            String opt = optIter.next();
            if (this.options.get(opt) != null) {
                if (!optIter.hasNext()) {
                    throw new RuntimeException(
                            "Expected a value for option: "
                                    + opt
                                    + " in option list: "
                                    + String.join(", ", newOptions));
                }
                this.options.put(opt, optIter.next());

            } else {
                this.options.put(opt, null);
            }
        }
    }

    /**
     * Returns the map that backs this SimpleOptionMap.
     *
     * @return the options in this object
     */
    public Map<String, @Nullable String> getOptions() {
        return options;
    }

    /**
     * Creates a "flat" list representation of these options.
     *
     * @return a list of the string representations of the options in this object
     */
    public List<String> getOptionsAsList() {
        return TestUtilities.optionMapToList(options);
    }
}
