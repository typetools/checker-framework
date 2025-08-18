package org.checkerframework.framework.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.source.SupportedOptions;

/** Provides methods for querying the Checker's options. */
public interface OptionConfiguration {

  /**
   * Returns all active options for this checker.
   *
   * @return all active options for this checker
   */
  Map<String, String> getOptions();

  /**
   * Returns true if the given option is provided.
   *
   * <p>Note that {@link #getOption} can still return null even if {@code hasOption} returns true:
   * this happens e.g. for {@code -Amyopt}
   *
   * @param name the name of the option to check
   * @return true if the option name was provided, false otherwise
   */
  boolean hasOption(String name);

  /**
   * Determines the value of the option with the given name.
   *
   * <p>Note that {@code getOption} can still return null even if {@link #hasOption} returns true:
   * this happens e.g. for {@code -Amyopt}
   *
   * @param name the name of the option to check
   * @return the value of the option with the given name
   * @see #getOption(String,String)
   */
  @Nullable String getOption(String name);

  /**
   * Determines the boolean value of the option with the given name. Returns {@code defaultValue} if
   * the option is not set.
   *
   * @param name the name of the option to check
   * @param defaultValue the default value to return if the option is not set
   * @return the value of the option with the given name, or {@code defaultValue}
   * @see #getOption(String)
   */
  String getOption(String name, String defaultValue);

  /**
   * Determines the boolean value of the option with the given name. Returns false if the option is
   * not set.
   *
   * @param name the name of the option to check
   * @return the boolean value of the option
   */
  boolean getBooleanOption(String name);

  /**
   * Determines the boolean value of the option with the given name. Returns the given default value
   * if the option is not set.
   *
   * @param name the name of the option to check
   * @param defaultValue the default value to use if the option is not set
   * @return the boolean value of the option
   */
  boolean getBooleanOption(String name, boolean defaultValue);

  /**
   * Determines the string list value of the option with the given name. The option's value is split
   * on the given separator. Returns an empty list if the option is not set.
   *
   * @param name the name of the option to check
   * @param separator the separator for list elements
   * @return the list of options
   */
  default List<String> getStringsOption(String name, char separator) {
    return getStringsOption(name, separator, Collections.emptyList());
  }

  /**
   * Determines the string list value of the option with the given name. The option's value is split
   * on the given separator. Returns the given default value if the option is not set.
   *
   * @param name the name of the option to check
   * @param separator the separator for list elements
   * @param defaultValue the default value to use if the option is not set
   * @return the list of options
   */
  public List<String> getStringsOption(String name, char separator, List<String> defaultValue);

  /**
   * Determines the string list value of the option with the given name. The option's value is split
   * on the given separator. Returns an empty list if the option is not set.
   *
   * @param name the name of the option to check
   * @param separator the separator for list elements
   * @return the list of options
   */
  default List<String> getStringsOption(String name, String separator) {
    return getStringsOption(name, separator, Collections.emptyList());
  }

  /**
   * Determines the string list value of the option with the given name. The option's value is split
   * on the given separator. Returns the given default value if the option is not set.
   *
   * @param name the name of the option to check
   * @param separator the separator for list elements
   * @param defaultValue the default value to use if the option is not set
   * @return the list of options
   */
  public List<String> getStringsOption(String name, String separator, List<String> defaultValue);

  /**
   * Map the Checker Framework version of {@link SupportedOptions} to the standard annotation
   * provided version {@link javax.annotation.processing.SupportedOptions}.
   *
   * @return the supported options
   */
  Set<String> getSupportedOptions();
}
