package org.checkerframework.framework.util;

import java.util.Map;
import java.util.Set;

/** Provides methods for querying the Checker's options. */
public interface OptionConfiguration {
    String getOption(String name);

    Map<String, String> getOptions();

    /**
     * Check whether the given option is provided.
     *
     * @param name the name of the option to check
     * @return true if the option name was provided, false otherwise
     */
    boolean hasOption(String name);

    /**
     * Determines the boolean value of the option with the given name. Returns {@code def} if the
     * option is not set.
     *
     * @param name the name of the option to check
     * @param def the default value to return if the option is not set
     */
    String getOption(String name, String def);

    /**
     * Determines the boolean value of the option with the given name. Returns false if the option
     * is not set.
     *
     * @param name the name of the option to check
     */
    boolean getBooleanOption(String name);

    Set<String> getSupportedOptions();
}
