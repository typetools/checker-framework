package org.checkerframework.checker.propkey;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic;
import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.plumelib.reflection.Signatures;
import org.plumelib.util.MapsP;

/**
 * This AnnotatedTypeFactory adds PropertyKey annotations to String literals that contain values
 * from lookupKeys.
 */
public class PropertyKeyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  private final Set<String> lookupKeys;

  @SuppressWarnings("this-escape")
  public PropertyKeyAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.lookupKeys = Collections.unmodifiableSet(buildLookupKeys());

    this.postInit();
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(), new KeyLookupTreeAnnotator(this, PropertyKey.class));
  }

  // To allow subclasses access to createTreeAnnotator from the BATF.
  protected TreeAnnotator createBasicTreeAnnotator() {
    return super.createTreeAnnotator();
  }

  /**
   * This TreeAnnotator checks for every String literal whether it is included in the lookup keys.
   * If it is, the given annotation is added to the literal; otherwise, nothing happens. Subclasses
   * of this AnnotatedTypeFactory can directly reuse this class and use a different annotation as
   * parameter.
   */
  protected class KeyLookupTreeAnnotator extends TreeAnnotator {
    AnnotationMirror theAnnot;

    public KeyLookupTreeAnnotator(BaseAnnotatedTypeFactory atf, Class<? extends Annotation> annot) {
      super(atf);
      theAnnot = AnnotationBuilder.fromClass(elements, annot);
    }

    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
      if (!type.hasPrimaryAnnotationInHierarchy(theAnnot)
          && tree.getKind() == Tree.Kind.STRING_LITERAL
          && strContains(lookupKeys, tree.getValue().toString())) {
        type.addAnnotation(theAnnot);
      }
      // A possible extension is to record all the keys that have been used and
      // in the end output a list of keys that were not used in the program,
      // possibly pointing to the opposite problem, keys that were supposed to
      // be used somewhere, but have not been, maybe because of copy-and-paste errors.
      return super.visitLiteral(tree, type);
    }

    // Result of binary op might not be a property key.
    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      type.removePrimaryAnnotation(theAnnot);
      return null; // super.visitBinary(tree, type);
    }

    // Result of unary op might not be a property key.
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
      type.removePrimaryAnnotation(theAnnot);
      return null; // super.visitCompoundAssignment(tree, type);
    }
  }

  /**
   * Instead of a precise comparison, we incrementally remove leading dot-separated strings until we
   * find a match. For example if messages contains "y.z" and we look for "x.y.z" we find a match
   * after removing the first "x.".
   *
   * <p>Compare to SourceChecker.fullMessageOf.
   */
  private static boolean strContains(Set<String> messages, String messageKey) {
    String key = messageKey;

    do {
      if (messages.contains(key)) {
        return true;
      }

      int dot = key.indexOf('.');
      if (dot < 0) {
        return false;
      }
      key = key.substring(dot + 1);
    } while (true);
  }

  /**
   * Returns a set of the valid keys that can be used.
   *
   * @return the valid keys that can be used
   */
  public Set<String> getLookupKeys() {
    return this.lookupKeys;
  }

  private Set<String> buildLookupKeys() {
    Set<String> result = new HashSet<>();

    if (checker.hasOption("propfiles")) {
      result.addAll(keysOfPropertyFiles(checker.getStringsOption("propfiles", File.pathSeparator)));
    }
    if (checker.hasOption("bundlenames")) {
      result.addAll(keysOfResourceBundle(checker.getStringsOption("bundlenames", ':')));
    }

    return result;
  }

  /**
   * Obtains the keys from all the property files.
   *
   * @param propfiles an array of property files, separated by {@link File#pathSeparator}
   * @return a set of all the keys found in all the property files
   */
  private Set<String> keysOfPropertyFiles(List<String> propfiles) {

    if (propfiles.isEmpty()) {
      return Collections.emptySet();
    }

    Set<String> result = new HashSet<>(MapsP.mapCapacity(propfiles));

    for (String propfile : propfiles) {
      try {
        Properties prop = new Properties();

        ClassLoader cl = this.getClass().getClassLoader();
        if (cl == null) {
          // The class loader is null if the system class loader was used.
          cl = ClassLoader.getSystemClassLoader();
        }

        try (InputStream in = cl.getResourceAsStream(propfile)) {
          if (in != null) {
            prop.load(in);
          } else {
            // If the classloader didn't manage to load the file, try whether a
            // FileInputStream works. For absolute paths this might help.
            try (InputStream fis = new FileInputStream(propfile)) {
              prop.load(fis);
            } catch (FileNotFoundException e) {
              checker.message(
                  Diagnostic.Kind.WARNING, "Couldn't find the properties file: " + propfile);
              // report(null, "propertykeychecker.filenotfound", propfile);
              // return Collections.emptySet();
              continue;
            }
          }
        }

        result.addAll(prop.stringPropertyNames());

      } catch (Exception e) {
        // TODO: is there a nicer way to report messages, that are not connected to an AST
        // node?
        // One cannot use `report`, because it needs a node.
        checker.message(
            Diagnostic.Kind.WARNING, "Exception in PropertyKeyChecker.keysOfPropertyFile: " + e);
        e.printStackTrace();
      }
    }

    return result;
  }

  /**
   * Returns the keys for the given resource bundles.
   *
   * @param bundleNames names of resource bundles
   * @return the keys for the given resource bundles
   */
  private Set<String> keysOfResourceBundle(List<String> bundleNames) {

    if (bundleNames.isEmpty()) {
      return Collections.emptySet();
    }

    Set<String> result = new HashSet<>(MapsP.mapCapacity(bundleNames));

    for (String bundleName : bundleNames) {
      if (!Signatures.isBinaryName(bundleName)) {
        System.err.println(
            "Malformed resource bundle: <" + bundleName + "> should be a binary name.");
        continue;
      }
      ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
      if (bundle == null) {
        checker.message(
            Diagnostic.Kind.WARNING,
            "Couldn't find the resource bundle: <"
                + bundleName
                + "> for locale <"
                + Locale.getDefault()
                + ">");
        continue;
      }

      result.addAll(bundle.keySet());
    }
    return result;
  }
}
