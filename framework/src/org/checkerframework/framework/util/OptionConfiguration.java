package org.checkerframework.framework.util;

import java.util.Map;
import java.util.Set;

/**
 * Provides methods for querying the Checker's options.
 */
public interface OptionConfiguration {
    String getOption(String name);
    Map<String, String> getOptions();
    boolean hasOption(String name);
    String getOption(String name, String def);
    Set<String> getSupportedOptions();
}
