package org.checkerframework.checker.propkey;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import org.plumelib.reflection.Signatures;

/** Utilities for loading property files and resource bundles. */
public final class PropertyFileUtils {

  /** This class is not instantiable. */
  private PropertyFileUtils() {
    throw new Error("Do not instantiate");
  }

  /**
   * Loads the entries in property files and resource bundles.
   *
   * @param propertyFiles property-file names
   * @param bundleNames resource-bundle names
   * @param contextClass the class whose class loader loads property-file resources
   * @param warn reports a warning that is not associated with an AST node
   * @return a map from each loaded key to its value
   */
  public static Map<String, Object> load(
      List<String> propertyFiles,
      List<String> bundleNames,
      Class<?> contextClass,
      Consumer<String> warn) {
    Map<String, Object> result = new HashMap<>();

    ClassLoader classLoader = contextClass.getClassLoader();
    if (classLoader == null) {
      // The class loader is null if the system class loader was used.
      classLoader = ClassLoader.getSystemClassLoader();
    }

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
            } catch (FileNotFoundException e) {
              warn.accept("Couldn't find the properties file: " + propertyFile);
              continue;
            }
          }
        }
        for (String key : properties.stringPropertyNames()) {
          result.put(key, properties.getProperty(key));
        }
      } catch (Exception e) {
        warn.accept(
            "Exception while loading properties file " + propertyFile + ": " + e.getMessage());
        e.printStackTrace();
      }
    }

    for (String bundleName : bundleNames) {
      if (!Signatures.isBinaryName(bundleName)) {
        warn.accept("Malformed resource bundle: <" + bundleName + "> should be a binary name.");
        continue;
      }
      ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
      if (bundle == null) {
        warn.accept(
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
