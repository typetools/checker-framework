package org.checkerframework.framework.util;

import java.util.Map;
import java.util.Set;

/** Provides methods for querying the Checker's options. */
public interface OptionConfiguration {

    Map<String, String> getOptions();

    /**
     * Check whether the given option is provided.
     *
     * @param name the name of the option to check
     * @return true if the option name was provided, false otherwise
     */
    boolean hasOption(String name);

    /**
     * Determines the value of the option with the given name.
     *
     * @param name the name of the option to check
     */
    String getOption(String name);

    /**
     * Determines the boolean value of the option with the given name. Returns {@code defaultValue}
     * if the option is not set.
     *
     * @param name the name of the option to check
     * @param defaultValue the default value to return if the option is not set
     */
    String getOption(String name, String defaultValue);

    /**
     * Determines the boolean value of the option with the given name. Returns false if the option
     * is not set.
     *
     * @param name the name of the option to check
     */
    boolean getBooleanOption(String name);

    /**
     * Determines the boolean value of the option with the given name. Returns the given default
     * value if the option is not set.
     *
     * @param name the name of the option to check
     * @param defaultValue the default value to use if the option is not set
     */
    boolean getBooleanOption(String name, boolean defaultValue);

    Set<String> getSupportedOptions();
}
