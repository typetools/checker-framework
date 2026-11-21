package org.checkerframework.framework.stub;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.FromStubFile;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parser for IntelliJ IDEA external annotations format ({@code annotations.xml}).
 *
 * <p>IntelliJ stores "external" annotations in XML files named {@code annotations.xml} located in
 * directory trees mirroring package names.
 */
public final class IntelliJAnnotationParser {

  /** Do not instantiate. */
  private IntelliJAnnotationParser() {
    throw new AssertionError("Do not instantiate");
  }

  /**
   * Parses an IntelliJ {@code annotations.xml} stream and populates {@code annotationFileAnnos}.
   *
   * @param filename the name or path of the file (for diagnostic reporting)
   * @param inputStream the input stream of the annotations.xml file
   * @param atypeFactory the type factory
   * @param processingEnv the processing environment
   * @param annotationFileAnnos the annotation storage to populate
   */
  public static void parseAnnotationsXml(
      String filename,
      InputStream inputStream,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations annotationFileAnnos) {
    SourceChecker checker = atypeFactory.getChecker();
    Document doc;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      try {
        dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      } catch (Exception ignored) {
        // Feature unsupported by specific parser
      }
      try {
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      } catch (Exception ignored) {
        // Feature unsupported by specific parser
      }
      try {
        dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      } catch (Exception ignored) {
        // Feature unsupported by specific parser
      }
      try {
        dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      } catch (Exception ignored) {
        // Feature unsupported by specific parser
      }
      try {
        dbFactory.setFeature(
            "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      } catch (Exception ignored) {
        // Feature unsupported by specific parser
      }
      dbFactory.setXIncludeAware(false);
      dbFactory.setExpandEntityReferences(false);
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(inputStream);
      doc.getDocumentElement().normalize();
    } catch (IOException e) {
      // The file exists but could not be read, so all of its annotations would be lost.
      checker.message(
          Diagnostic.Kind.ERROR,
          String.format("Could not read annotations XML %s: %s", filename, e.getMessage()));
      return;
    } catch (ParserConfigurationException | SAXException e) {
      checker.message(
          Diagnostic.Kind.WARNING,
          String.format("Could not parse annotations XML %s: %s", filename, e.getMessage()));
      return;
    }

    NodeList itemNodes = doc.getElementsByTagName("item");
    for (int i = 0; i < itemNodes.getLength(); i++) {
      Node node = itemNodes.item(i);
      if (node instanceof org.w3c.dom.Element itemElement) {
        String itemName = itemElement.getAttribute("name");
        if (itemName == null || itemName.trim().isEmpty()) {
          continue;
        }

        try {
          List<AnnotationMirror> annotations =
              parseItemAnnotations(itemElement, atypeFactory, processingEnv, filename);
          if (!annotations.isEmpty()) {
            applyAnnotationsToElement(
                itemName.trim(),
                annotations,
                atypeFactory,
                processingEnv,
                annotationFileAnnos,
                filename);
          }
        } catch (BugInCF e) {
          throw e;
        } catch (Exception e) {
          checker.message(
              Diagnostic.Kind.WARNING,
              String.format(
                  "Could not apply annotation item '%s' in %s: %s",
                  itemName.trim(), filename, e.getMessage()));
        }
      }
    }
  }

  /**
   * Issues a warning about a missing element, unless the -AstubNoWarnIfNotFound option is set.
   *
   * <p>An IntelliJ annotation file is always supplied on the command line, so this warns by
   * default, as {@link AnnotationFileParser} does for a command-line file.
   *
   * @param checker the source checker
   * @param message the warning message
   */
  private static void warnNotFound(SourceChecker checker, String message) {
    if (!checker.hasOption("stubNoWarnIfNotFound")) {
      Diagnostic.Kind kind =
          checker.hasOption("stubWarnNote") ? Diagnostic.Kind.NOTE : Diagnostic.Kind.WARNING;
      checker.message(kind, message);
    }
  }

  /**
   * Issues a warning about a class that the annotation file mentions but that does not exist,
   * unless the -AstubNoWarnIfNotFound or -AstubWarnIfNotFoundIgnoresClasses option is set.
   *
   * @param checker the source checker
   * @param message the warning message
   */
  private static void warnClassNotFound(SourceChecker checker, String message) {
    if (!checker.hasOption("stubWarnIfNotFoundIgnoresClasses")) {
      warnNotFound(checker, message);
    }
  }

  /**
   * Returns true if {@code atypeFactory}'s checker should warn that an annotation name that is not
   * on the classpath is unknown. That is the case if the checker is an ultimate parent checker (see
   * {@link BaseTypeChecker#getUltimateParentChecker}) and neither it nor any of its subcheckers
   * treats the name as an alias (see {@link AnnotatedTypeFactory#canonicalAnnotationForAliasName}).
   *
   * <p>Every checker of a compound checker reads the annotation file, but an alias such as {@code
   * androidx.annotation.NonNull} is typically known to only one of them, such as the Nullness
   * Checker's subchecker that does not check initialization. A subchecker cannot consult the
   * factories of the checkers that depend on it, because those factories are created after the
   * subchecker's factory has read the annotation file. So a subchecker never warns, and the
   * ultimate parent checker, whose factory is created last, warns once for the whole compound
   * checker.
   *
   * @param annoName the fully-qualified name of an annotation that is not on the classpath and that
   *     {@code atypeFactory} does not treat as an alias
   * @param atypeFactory a type factory that is reading an annotation file
   * @return true if {@code atypeFactory}'s checker should warn that {@code annoName} is unknown
   */
  @SuppressWarnings("interning:not.interned") // checkers are compared by identity
  private static boolean isUnknownToCompoundChecker(
      String annoName, AnnotatedTypeFactory atypeFactory) {
    BaseTypeChecker checker = atypeFactory.getChecker();
    if (checker.getUltimateParentChecker() != checker) {
      return false;
    }
    // The outermost checker's getSubcheckers() returns every checker that runs, even if the
    // outermost checker is an aggregate checker, of which `checker` is one part.
    SourceChecker outermost = checker;
    for (SourceChecker parent = checker.getParentChecker();
        parent != null;
        parent = parent.getParentChecker()) {
      outermost = parent;
    }
    for (SourceChecker subchecker : outermost.getSubcheckers()) {
      if (subchecker instanceof BaseTypeChecker baseTypeSubchecker
          && baseTypeSubchecker != checker
          && baseTypeSubchecker.getUltimateParentChecker() == checker
          && baseTypeSubchecker.getVisitor() != null
          && baseTypeSubchecker.getTypeFactory().canonicalAnnotationForAliasName(annoName)
              != null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Parses the {@code <annotation>} children of an {@code <item>} element.
   *
   * @param itemElement the XML item element containing annotation child tags
   * @param atypeFactory the type factory
   * @param processingEnv the processing environment
   * @param filename the name or path of the file (for diagnostic reporting)
   * @return a list of parsed and canonicalized {@link AnnotationMirror}s
   */
  private static List<AnnotationMirror> parseItemAnnotations(
      org.w3c.dom.Element itemElement,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      String filename) {
    List<AnnotationMirror> result = new ArrayList<>();
    Elements elements = processingEnv.getElementUtils();
    String context =
        String.format("item '%s' in %s", itemElement.getAttribute("name").trim(), filename);

    for (org.w3c.dom.Element annoElement : childElements(itemElement, "annotation")) {
      String annoName = annoElement.getAttribute("name");
      if (annoName == null || annoName.trim().isEmpty()) {
        continue;
      }
      annoName = annoName.trim();

      TypeElement annoTypeElt = getTypeElement(annoName, elements);
      if (annoTypeElt == null) {
        // The annotation may be an alias that is not on the classpath, such as
        // org.jetbrains.annotations.NotNull.  An alias that is identified by name does not depend
        // on the alias's element values, so it can be resolved without the alias's class.
        AnnotationMirror aliasCanonical = atypeFactory.canonicalAnnotationForAliasName(annoName);
        if (aliasCanonical != null) {
          result.add(aliasCanonical);
        } else if (isUnknownToCompoundChecker(annoName, atypeFactory)) {
          warnNotFound(atypeFactory.getChecker(), "Unknown annotation: " + annoName);
        }
        continue;
      }
      if (annoTypeElt.getKind() != ElementKind.ANNOTATION_TYPE) {
        warnNotFound(atypeFactory.getChecker(), "Not an annotation type: " + annoName);
        continue;
      }

      if (!childElements(annoElement, "val").isEmpty()) {
        // Annotation element values are not supported, so ignore the annotation.  Warn only if
        // the checker supports the annotation, because annotations.xml files commonly contain
        // annotations with element values, such as @Contract(pure = true), that are irrelevant
        // to the checker.
        if (atypeFactory.isSupportedQualifier(annoTypeElt.getQualifiedName().toString())) {
          atypeFactory
              .getChecker()
              .message(
                  Diagnostic.Kind.WARNING,
                  String.format(
                      "Ignoring annotation @%s on %s: annotation element values are not supported",
                      annoTypeElt.getQualifiedName(), context));
        }
        continue;
      }

      try {
        AnnotationMirror annoMirror =
            buildAnnotationMirror(annoTypeElt, processingEnv, atypeFactory.getChecker(), context);
        if (annoMirror != null) {
          AnnotationMirror canonical = atypeFactory.canonicalAnnotation(annoMirror);
          result.add(canonical != null ? canonical : annoMirror);
        }
      } catch (BugInCF e) {
        throw e;
      } catch (Exception e) {
        warnNotFound(
            atypeFactory.getChecker(),
            "Failed to build annotation @" + annoName + ": " + e.getMessage());
      }
    }
    return result;
  }

  /**
   * Returns the children of {@code parent} that are elements with the given tag name. Unlike {@link
   * org.w3c.dom.Element#getElementsByTagName}, this returns only direct children rather than all
   * descendants.
   *
   * @param parent an XML element
   * @param tagName a tag name
   * @return the direct children of {@code parent} whose tag name is {@code tagName}
   */
  private static List<org.w3c.dom.Element> childElements(
      org.w3c.dom.Element parent, String tagName) {
    List<org.w3c.dom.Element> result = new ArrayList<>();
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child instanceof org.w3c.dom.Element childElement
          && tagName.equals(childElement.getNodeName())) {
        result.add(childElement);
      }
    }
    return result;
  }

  /**
   * Constructs an {@link AnnotationMirror} for a marker annotation (one with no element values)
   * from an XML {@code <annotation>} element.
   *
   * <p>If some element of the annotation type has no default value, this issues a warning and
   * returns null, rather than building an annotation that is missing a mandatory element.
   *
   * @param annoTypeElt the TypeElement corresponding to the annotation
   * @param processingEnv the processing environment
   * @param checker the source checker, for issuing diagnostics
   * @param context a description of the enclosing item, for diagnostics
   * @return the constructed {@link AnnotationMirror}, or null if construction fails
   */
  private static @Nullable AnnotationMirror buildAnnotationMirror(
      TypeElement annoTypeElt,
      ProcessingEnvironment processingEnv,
      SourceChecker checker,
      String context) {
    @SuppressWarnings("signature") // the qualified name of a TypeElement is a canonical name
    @CanonicalName String canonicalName = annoTypeElt.getQualifiedName().toString();

    for (ExecutableElement annoElt : ElementFilter.methodsIn(annoTypeElt.getEnclosedElements())) {
      if (annoElt.getDefaultValue() == null) {
        checker.message(
            Diagnostic.Kind.WARNING,
            String.format(
                "Ignoring annotation @%s on %s: no value for element '%s', which has no default",
                canonicalName, context, annoElt.getSimpleName()));
        return null;
      }
    }

    // fromName supplies the default value of every element.
    return AnnotationBuilder.fromName(processingEnv.getElementUtils(), canonicalName);
  }

  /**
   * Returns the {@link TypeElement} for the given class name, which may use either '.' or '$' to
   * separate the name of a nested class from the name of its enclosing class.
   *
   * @param className a class name read from an {@code annotations.xml} file
   * @param elements the element utilities
   * @return the {@link TypeElement} for {@code className}, or null if there is none
   */
  @SuppressWarnings("signature:argument") // an annotations.xml file contains canonical names
  private static @Nullable TypeElement getTypeElement(String className, Elements elements) {
    TypeElement result = elements.getTypeElement(className.replace('$', '.'));
    if (result == null) {
      result = elements.getTypeElement(className);
    }
    return result;
  }

  /** Represents a parsed IntelliJ item signature. */
  /* package-private */ static final class ParsedItemSignature {
    /** Fully qualified class name. */
    final String className;

    /** Simple member name (method name, constructor name, or field name), or null for class. */
    final @Nullable String memberName;

    /** List of parameter type names for executable members. */
    final List<String> paramTypes;

    /** Zero-based parameter index, or -1 if the target is not a parameter. */
    final int paramIndex;

    /** True if the target is a method or constructor. */
    final boolean isMethodOrConstructor;

    /** True if the target is a constructor. */
    final boolean isConstructor;

    /** True if the target is a field. */
    final boolean isField;

    /** True if the target is a class. */
    final boolean isClass;

    /**
     * Creates a new {@link ParsedItemSignature}.
     *
     * @param className fully qualified class name
     * @param memberName member name or null
     * @param paramTypes list of parameter type names
     * @param paramIndex parameter index or -1
     * @param isMethodOrConstructor true if method or constructor
     * @param isConstructor true if constructor
     * @param isField true if field
     * @param isClass true if class
     */
    ParsedItemSignature(
        String className,
        @Nullable String memberName,
        List<String> paramTypes,
        int paramIndex,
        boolean isMethodOrConstructor,
        boolean isConstructor,
        boolean isField,
        boolean isClass) {
      this.className = className;
      this.memberName = memberName;
      this.paramTypes = paramTypes;
      this.paramIndex = paramIndex;
      this.isMethodOrConstructor = isMethodOrConstructor;
      this.isConstructor = isConstructor;
      this.isField = isField;
      this.isClass = isClass;
    }

    /**
     * Returns true if the item signature was malformed, so it names no program element.
     *
     * @return true if the item signature was malformed
     */
    boolean isMalformed() {
      return !isMethodOrConstructor && !isField && !isClass;
    }
  }

  /**
   * Parses an IntelliJ item signature string into a {@link ParsedItemSignature}.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>Class: {@code "java.lang.String"}
   *   <li>Field: {@code "java.lang.String CASE_INSENSITIVE_ORDER"}
   *   <li>Field with type: {@code "java.lang.String java.util.Comparator CASE_INSENSITIVE_ORDER"}
   *   <li>Method: {@code "java.lang.String java.lang.String substring(int, int)"}
   *   <li>Method param: {@code "java.lang.String java.lang.String concat(java.lang.String) 0"}
   *   <li>Constructor: {@code "java.lang.String java.lang.String(byte[], int)"}
   *   <li>Constructor param: {@code "java.lang.String java.lang.String(byte[], int) 1"}
   * </ul>
   *
   * <p>If {@code sig} is malformed, the result's {@link ParsedItemSignature#isMalformed} method
   * returns true.
   *
   * @param sig the raw signature string from the XML item name attribute
   * @return the parsed item signature
   */
  /* package-private */ static ParsedItemSignature parseSignature(String sig) {
    sig = sig.trim();
    int lastParen = sig.lastIndexOf(')');
    if (lastParen != -1) {
      // Method, Constructor, or Parameter
      int firstParen = sig.indexOf('(');
      if (firstParen == -1 || firstParen > lastParen) {
        return new ParsedItemSignature(
            sig, null, Collections.emptyList(), -1, false, false, false, false);
      }
      String trailing = sig.substring(lastParen + 1).trim();
      // -1 means the signature names the method or constructor itself, not one of its
      // parameters.
      int paramIndex = -1;
      if (!trailing.isEmpty()) {
        try {
          paramIndex = Integer.parseInt(trailing);
        } catch (NumberFormatException e) {
          // The signature is malformed: the only thing that may follow the parameter list is a
          // parameter index.
          return new ParsedItemSignature(
              sig, null, Collections.emptyList(), -1, false, false, false, false);
        }
        if (paramIndex < 0) {
          // The signature is malformed: a parameter index is non-negative.
          return new ParsedItemSignature(
              sig, null, Collections.emptyList(), -1, false, false, false, false);
        }
      }

      String paramListStr = sig.substring(firstParen + 1, lastParen).trim();
      List<String> paramTypes = parseParameterTypes(paramListStr);
      if (paramTypes == null) {
        // The signature is malformed: some parameter type is empty.
        return new ParsedItemSignature(
            sig, null, Collections.emptyList(), -1, false, false, false, false);
      }

      String beforeParen = sig.substring(0, firstParen).trim();
      String[] parts = beforeParen.split("\\s+");

      String className = parts[0];
      String memberName = null;
      boolean isConstructor = false;

      String simpleClassName = getSimpleName(className);

      if (parts.length == 1) {
        isConstructor = true;
        memberName = simpleClassName;
      } else if (parts.length == 2) {
        String name = parts[1];
        if (name.equals(className) || name.equals(simpleClassName) || name.equals("<init>")) {
          isConstructor = true;
          memberName = simpleClassName;
        } else {
          memberName = name;
        }
      } else {
        isConstructor = false;
        memberName = parts[parts.length - 1];
      }

      return new ParsedItemSignature(
          className, memberName, paramTypes, paramIndex, true, isConstructor, false, false);
    } else {
      // Class or Field
      String[] parts = sig.split("\\s+");
      if (parts.length == 1) {
        return new ParsedItemSignature(
            parts[0], null, Collections.emptyList(), -1, false, false, false, true);
      } else {
        return new ParsedItemSignature(
            parts[0],
            parts[parts.length - 1],
            Collections.emptyList(),
            -1,
            false,
            false,
            true,
            false);
      }
    }
  }

  /**
   * Parses the comma-separated parameter type list from inside parentheses.
   *
   * @param paramContent the raw string of parameter types
   * @return a list of trimmed parameter type strings, or null if some parameter type is empty, as
   *     in {@code "java.lang.String,"}
   */
  private static @Nullable List<String> parseParameterTypes(String paramContent) {
    if (paramContent.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> paramTypes = new ArrayList<>();
    int depth = 0;
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < paramContent.length(); i++) {
      char c = paramContent.charAt(i);
      if (c == '<') {
        depth++;
        current.append(c);
      } else if (c == '>') {
        depth--;
        current.append(c);
      } else if (c == ',' && depth == 0) {
        paramTypes.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    paramTypes.add(current.toString().trim());
    for (String paramType : paramTypes) {
      if (paramType.isEmpty()) {
        return null;
      }
    }
    return paramTypes;
  }

  /**
   * Returns the simple class name from a binary or qualified class name.
   *
   * @param className the fully qualified or binary class name
   * @return the simple class name
   */
  private static String getSimpleName(String className) {
    int lastDot = Math.max(className.lastIndexOf('.'), className.lastIndexOf('$'));
    return lastDot != -1 ? className.substring(lastDot + 1) : className;
  }

  /**
   * Applies parsed annotations to the target element in {@code AnnotationFileAnnotations}.
   *
   * @param itemSignature the raw signature string from the XML item
   * @param annotations the list of parsed AnnotationMirrors to apply
   * @param atypeFactory the type factory
   * @param processingEnv the processing environment
   * @param annos the annotation container to populate
   * @param filename the name or path of the file (for diagnostic reporting)
   */
  private static void applyAnnotationsToElement(
      String itemSignature,
      List<AnnotationMirror> annotations,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations annos,
      String filename) {
    ParsedItemSignature parsed = parseSignature(itemSignature);
    Elements elements = processingEnv.getElementUtils();
    SourceChecker checker = atypeFactory.getChecker();

    if (parsed.isMalformed()) {
      checker.message(
          Diagnostic.Kind.WARNING,
          String.format("Cannot parse item name '%s' in %s", itemSignature, filename));
      return;
    }

    TypeElement classElem = getTypeElement(parsed.className, elements);
    if (classElem == null) {
      if (parsed.isClass) {
        // IntelliJ IDEA writes an annotation on a package using the package name as the item
        // name, as in <item name="java.lang">.  A package name is not syntactically
        // distinguishable from a class name, so resolution determines which it is.
        PackageElement packageElem = elements.getPackageElement(parsed.className);
        if (packageElem != null) {
          for (AnnotationMirror am : annotations) {
            recordDeclAnnotationIfApplicable(packageElem, am, annos);
          }
          return;
        }
      }
      warnClassNotFound(checker, "Class not found: " + parsed.className);
      return;
    }

    // A type annotation that this checker does not support, or a declaration annotation, does not
    // change the element's type.  If the item has no supported type qualifier, then do not install
    // an annotated type for the element and do not mark the element as being from an annotation
    // file.  Either of those would make the element's type be the one derived from bytecode, which
    // takes precedence over the annotated JDK (parsed later) and over conservative defaults for
    // unchecked bytecode.
    List<AnnotationMirror> qualifiers = new ArrayList<>(annotations.size());
    for (AnnotationMirror am : annotations) {
      if (atypeFactory.isSupportedQualifier(am)) {
        qualifiers.add(am);
      }
    }

    if (parsed.isMethodOrConstructor) {
      ExecutableElement execElem =
          findMatchingExecutable(
              classElem,
              parsed.memberName,
              parsed.paramTypes,
              parsed.isConstructor,
              processingEnv.getTypeUtils());
      if (execElem == null) {
        warnNotFound(
            checker,
            (parsed.isConstructor ? "Constructor" : "Method")
                + " not found: "
                + parsed.memberName
                + " in "
                + parsed.className);
        return;
      }

      if (parsed.paramIndex >= execElem.getParameters().size()) {
        warnNotFound(
            checker,
            "Parameter index "
                + parsed.paramIndex
                + " out of bounds for "
                + parsed.memberName
                + " in "
                + parsed.className);
        return;
      }

      VariableElement paramElem =
          parsed.paramIndex >= 0 ? execElem.getParameters().get(parsed.paramIndex) : null;
      Element target = paramElem != null ? paramElem : execElem;
      for (AnnotationMirror am : annotations) {
        recordDeclAnnotationIfApplicable(target, am, annos);
      }
      if (qualifiers.isEmpty()) {
        return;
      }

      AnnotatedExecutableType methodType =
          (AnnotatedExecutableType)
              annos.atypes.computeIfAbsent(execElem, e -> atypeFactory.fromElement(execElem));
      markAsFromStubFile(execElem, processingEnv, annos);
      AnnotatedTypeMirror targetType;
      if (paramElem != null) {
        markAsFromStubFile(paramElem, processingEnv, annos);
        targetType = methodType.getParameterTypes().get(parsed.paramIndex);
        annos.atypes.put(paramElem, targetType);
      } else {
        targetType = methodType.getReturnType();
      }
      for (AnnotationMirror am : qualifiers) {
        targetType.replaceAnnotation(am);
      }
    } else if (parsed.isField && parsed.memberName != null) {
      VariableElement fieldElem = null;
      for (VariableElement f : ElementFilter.fieldsIn(classElem.getEnclosedElements())) {
        if (f.getSimpleName().contentEquals(parsed.memberName)) {
          fieldElem = f;
          break;
        }
      }
      if (fieldElem == null) {
        warnNotFound(checker, "Field not found: " + parsed.memberName + " in " + parsed.className);
        return;
      }
      applyAnnotationsToDeclaration(
          fieldElem, annotations, qualifiers, atypeFactory, processingEnv, annos);
    } else if (parsed.isClass) {
      applyAnnotationsToDeclaration(
          classElem, annotations, qualifiers, atypeFactory, processingEnv, annos);
    }
  }

  /**
   * Applies parsed annotations to a field or class.
   *
   * @param elt the field or class
   * @param annotations the parsed annotations
   * @param qualifiers the elements of {@code annotations} that are supported type qualifiers
   * @param atypeFactory the type factory
   * @param processingEnv the processing environment
   * @param annos the annotation container to populate
   */
  private static void applyAnnotationsToDeclaration(
      Element elt,
      List<AnnotationMirror> annotations,
      List<AnnotationMirror> qualifiers,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations annos) {
    for (AnnotationMirror am : annotations) {
      recordDeclAnnotationIfApplicable(elt, am, annos);
    }
    if (qualifiers.isEmpty()) {
      return;
    }
    markAsFromStubFile(elt, processingEnv, annos);
    AnnotatedTypeMirror type =
        annos.atypes.computeIfAbsent(elt, e -> atypeFactory.fromElement(elt));
    for (AnnotationMirror am : qualifiers) {
      type.replaceAnnotation(am);
    }
  }

  /**
   * Marks the element with {@code @FromStubFile}.
   *
   * @param elt the element to mark
   * @param processingEnv the processing environment
   * @param annos the annotation storage
   */
  private static void markAsFromStubFile(
      Element elt, ProcessingEnvironment processingEnv, AnnotationFileAnnotations annos) {
    AnnotationMirror fromStubFile =
        AnnotationBuilder.fromClass(processingEnv.getElementUtils(), FromStubFile.class);
    String eltName = ElementUtils.getQualifiedName(elt);
    annos.declAnnos.computeIfAbsent(eltName, k -> new AnnotationMirrorSet()).add(fromStubFile);
  }

  /**
   * Records a declaration annotation on the given element if applicable.
   *
   * @param elt the element to annotate
   * @param am the annotation mirror to record
   * @param annos the annotation storage
   */
  private static void recordDeclAnnotationIfApplicable(
      Element elt, AnnotationMirror am, AnnotationFileAnnotations annos) {
    Target target = am.getAnnotationType().asElement().getAnnotation(Target.class);
    if (AnnotationUtils.getElementKindsForTarget(target).contains(elt.getKind())) {
      String eltName = ElementUtils.getQualifiedName(elt);
      annos.declAnnos.computeIfAbsent(eltName, k -> new AnnotationMirrorSet()).add(am);
    }
  }

  /**
   * Finds the matching constructor or method in a given class.
   *
   * @param classElem the enclosing TypeElement
   * @param methodName the method name, or null for constructors
   * @param expectedParamTypes the list of expected parameter type strings
   * @param isConstructor true if searching for a constructor
   * @param types the type utilities
   * @return the matching {@link ExecutableElement}, or null if not found
   */
  private static @Nullable ExecutableElement findMatchingExecutable(
      TypeElement classElem,
      @Nullable String methodName,
      List<String> expectedParamTypes,
      boolean isConstructor,
      Types types) {
    List<ExecutableElement> candidates =
        isConstructor
            ? ElementFilter.constructorsIn(classElem.getEnclosedElements())
            : ElementFilter.methodsIn(classElem.getEnclosedElements());

    // Pass 1: exact match
    for (ExecutableElement candidate : candidates) {
      if (matchesExecutable(
          candidate, methodName, expectedParamTypes, isConstructor, false, types)) {
        return candidate;
      }
    }
    // Pass 2: fallback match allowing the erasure of type variables
    for (ExecutableElement candidate : candidates) {
      if (matchesExecutable(
          candidate, methodName, expectedParamTypes, isConstructor, true, types)) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Tests whether an executable candidate matches the given method name and expected parameter
   * types.
   *
   * @param candidate the executable element candidate
   * @param methodName the expected method name, or null for constructors
   * @param expectedParamTypes the expected parameter type names
   * @param isConstructor true if searching for a constructor
   * @param allowTypeVarErasure true if type variables are allowed to match their erasure
   * @param types the type utilities
   * @return true if candidate matches
   */
  private static boolean matchesExecutable(
      ExecutableElement candidate,
      @Nullable String methodName,
      List<String> expectedParamTypes,
      boolean isConstructor,
      boolean allowTypeVarErasure,
      Types types) {
    if (!isConstructor
        && methodName != null
        && !candidate.getSimpleName().contentEquals(methodName)) {
      return false;
    }
    List<? extends VariableElement> params = candidate.getParameters();
    if (params.size() != expectedParamTypes.size()) {
      return false;
    }
    for (int i = 0; i < params.size(); i++) {
      if (!typeMatches(
          params.get(i).asType(), expectedParamTypes.get(i), allowTypeVarErasure, types)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares a {@link TypeMirror} with an expected type signature string from IntelliJ.
   *
   * @param typeMirror the type mirror of the element parameter
   * @param expectedTypeStr the expected type name string
   * @param allowTypeVarErasure true if type variables should match their erasure as fallback
   * @param types the type utilities
   * @return true if the type matches
   */
  private static boolean typeMatches(
      TypeMirror typeMirror, String expectedTypeStr, boolean allowTypeVarErasure, Types types) {
    expectedTypeStr = expectedTypeStr.trim();
    if (expectedTypeStr.endsWith("...")) {
      expectedTypeStr = expectedTypeStr.substring(0, expectedTypeStr.length() - 3) + "[]";
    }

    if (typeMirror.getKind() == TypeKind.ARRAY) {
      if (!expectedTypeStr.endsWith("[]")) {
        return false;
      }
      return typeMatches(
          ((ArrayType) typeMirror).getComponentType(),
          expectedTypeStr.substring(0, expectedTypeStr.length() - 2),
          allowTypeVarErasure,
          types);
    }

    if (expectedTypeStr.endsWith("[]")) {
      return false;
    }

    // Strip generics from expectedTypeStr for comparison (e.g. List<String> -> List)
    String rawExpected = expectedTypeStr.replaceAll("<.*>", "").trim();
    rawExpected = rawExpected.replace('$', '.');

    if (typeMirror.getKind().isPrimitive()) {
      return typeMirror.getKind().name().toLowerCase(Locale.ROOT).equals(rawExpected);
    }

    if (typeMirror.getKind() == TypeKind.DECLARED) {
      DeclaredType dt = (DeclaredType) typeMirror;
      TypeElement te = (TypeElement) dt.asElement();
      String qualName = te.getQualifiedName().toString().replace('$', '.');
      return qualName.equals(rawExpected);
    }

    if (typeMirror.getKind() == TypeKind.TYPEVAR) {
      TypeVariable tv = (TypeVariable) typeMirror;
      String tvName = tv.asElement().getSimpleName().toString();
      return tvName.equals(rawExpected)
          || (allowTypeVarErasure && typeMatches(types.erasure(tv), rawExpected, false, types));
    }

    return false;
  }
}
