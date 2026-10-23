package org.checkerframework.checker.propkey;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.tools.Diagnostic;
import org.checkerframework.framework.source.SourceChecker;
import org.plumelib.reflection.Signatures;

/** Utilities for loading property files and resource bundles. */
public final class PropertyFileUtils {

  /** This class is not instantiable. */
  private PropertyFileUtils() {
    throw new Error("Do not instantiate");
  }

  /**
   * Loads the entries in the property files named by the {@code -Apropfiles} command-line option
   * and in the resource bundles named by the {@code -Abundlenames} command-line option. Reports a
   * warning for each file or bundle that cannot be loaded.
   *
   * @param checker the checker whose options name the files and bundles, and which reports warnings
   * @param contextClass the class whose class loader loads property-file resources
   * @return a map from each loaded key to its value
   */
  public static Map<String, Object> load(SourceChecker checker, Class<?> contextClass) {
    Map<String, Object> result = new HashMap<>();

    ClassLoader classLoader = contextClass.getClassLoader();
    if (classLoader == null) {
      // The class loader is null if the system class loader was used.
      classLoader = ClassLoader.getSystemClassLoader();
    }

    // `getStringsOption` throws an exception if the checker does not support the option.  Some
    // checkers that reuse a factory that calls this method, such as I18nFormatterLubGlbChecker,
    // do not support these options.
    List<String> propertyFiles =
        checker.hasOption("propfiles")
            ? checker.getStringsOption("propfiles", File.pathSeparator)
            : Collections.emptyList();
    List<String> bundleNames =
        checker.hasOption("bundlenames")
            ? checker.getStringsOption("bundlenames", ':')
            : Collections.emptyList();

    // These problems are not associated with an AST node, so they are reported via
    // `checker.message` rather than `checker.reportWarning`.
    for (String propertyFile : propertyFiles) {
      try {
        Properties properties = new Properties();
        try (InputStream in = classLoader.getResourceAsStream(propertyFile)) {
          if (in != null) {
            properties.load(in);
          } else {
            // Absolute paths, in particular, might not be class-loader resources.
            try (InputStream fileInput = Files.newInputStream(Paths.get(propertyFile))) {
              properties.load(fileInput);
            } catch (NoSuchFileException e) {
              checker.message(
                  Diagnostic.Kind.WARNING, "Couldn't find the properties file: " + propertyFile);
              continue;
            }
          }
        }
        for (String key : properties.stringPropertyNames()) {
          result.put(key, properties.getProperty(key));
        }
      } catch (Exception e) {
        checker.message(
            Diagnostic.Kind.WARNING,
            "Exception while loading properties file " + propertyFile + ": " + e);
      }
    }

    for (String bundleName : bundleNames) {
      if (!Signatures.isBinaryName(bundleName)) {
        checker.message(
            Diagnostic.Kind.WARNING,
            "Malformed resource bundle: <" + bundleName + "> should be a binary name.");
        continue;
      }
      ResourceBundle bundle;
      try {
        bundle = ResourceBundle.getBundle(bundleName);
      } catch (MissingResourceException e) {
        checker.message(
            Diagnostic.Kind.WARNING,
            "Couldn't find the resource bundle: <"
                + bundleName
                + "> for locale <"
                + Locale.getDefault()
                + ">.");
        continue;
      }
      for (String key : bundle.keySet()) {
        result.put(key, bundle.getObject(key));
      }
    }

    return result;
  }
}
