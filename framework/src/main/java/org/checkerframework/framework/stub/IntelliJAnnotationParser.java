package org.checkerframework.framework.stub;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
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
import org.plumelib.util.ArrayMap;
import org.plumelib.util.ArraySet;
import org.plumelib.util.StringsP;
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
    } catch (ParserConfigurationException | SAXException | IOException e) {
      checker.message(
          Diagnostic.Kind.WARNING,
          String.format("Could not parse annotations XML %s: %s", filename, e.getMessage()));
      return;
    }

    NodeList itemNodes = doc.getElementsByTagName("item");
    for (int i = 0; i < itemNodes.getLength(); i++) {
      Node node = itemNodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        org.w3c.dom.Element itemElement = (org.w3c.dom.Element) node;
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
        warnNotFound(atypeFactory.getChecker(), "Unknown annotation: " + annoName);
        continue;
      }
      if (annoTypeElt.getKind() != ElementKind.ANNOTATION_TYPE) {
        warnNotFound(atypeFactory.getChecker(), "Not an annotation type: " + annoName);
        continue;
      }

      try {
        AnnotationMirror annoMirror =
            buildAnnotationMirror(
                annoElement, annoTypeElt, processingEnv, atypeFactory.getChecker(), context);
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
      if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
        result.add((org.w3c.dom.Element) child);
      }
    }
    return result;
  }

  /**
   * Constructs an {@link AnnotationMirror} from an XML {@code <annotation>} element.
   *
   * <p>If any {@code <val>} child cannot be parsed, or if some element that has no default value is
   * not given a value, this issues a warning and returns null, rather than building an annotation
   * that is missing a mandatory element.
   *
   * @param annoElement the XML element for the annotation
   * @param annoTypeElt the TypeElement corresponding to the annotation
   * @param processingEnv the processing environment
   * @param checker the source checker, for issuing diagnostics
   * @param context a description of the enclosing item, for diagnostics
   * @return the constructed {@link AnnotationMirror}, or null if construction fails
   */
  private static @Nullable AnnotationMirror buildAnnotationMirror(
      org.w3c.dom.Element annoElement,
      TypeElement annoTypeElt,
      ProcessingEnvironment processingEnv,
      SourceChecker checker,
      String context) {
    @SuppressWarnings("signature") // the qualified name of a TypeElement is a canonical name
    @CanonicalName String canonicalName = annoTypeElt.getQualifiedName().toString();
    Elements elements = processingEnv.getElementUtils();

    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, canonicalName);
    Set<String> writtenElements = new ArraySet<>(2); // most annotations have few elements
    for (org.w3c.dom.Element valElem : childElements(annoElement, "val")) {
      if (!valElem.hasAttribute("val")) {
        continue;
      }
      String memberName = valElem.hasAttribute("name") ? valElem.getAttribute("name") : "value";
      String valStr = valElem.getAttribute("val").trim();
      String problem = setBuilderValue(builder, memberName, valStr, annoTypeElt, processingEnv);
      if (problem != null) {
        checker.message(
            Diagnostic.Kind.WARNING,
            String.format("Ignoring annotation @%s on %s: %s", canonicalName, context, problem));
        return null;
      }
      writtenElements.add(memberName);
    }

    for (ExecutableElement annoElt : ElementFilter.methodsIn(annoTypeElt.getEnclosedElements())) {
      String elementName = annoElt.getSimpleName().toString();
      if (annoElt.getDefaultValue() == null && !writtenElements.contains(elementName)) {
        checker.message(
            Diagnostic.Kind.WARNING,
            String.format(
                "Ignoring annotation @%s on %s: no value for element '%s', which has no default",
                canonicalName, context, elementName));
        return null;
      }
    }

    // Index the values by element name, so that fromName can supply the default value of every
    // element that the annotations.xml file does not mention.
    Map<? extends ExecutableElement, ? extends AnnotationValue> builtValues =
        builder.build().getElementValues();
    Map<String, AnnotationValue> elementValues = new ArrayMap<>(builtValues.size());
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        builtValues.entrySet()) {
      elementValues.put(entry.getKey().getSimpleName().toString(), entry.getValue());
    }
    return AnnotationBuilder.fromName(elements, canonicalName, elementValues);
  }

  /**
   * Sets a value on the {@link AnnotationBuilder} based on the expected element type.
   *
   * @param builder the annotation builder
   * @param memberName the name of the annotation element
   * @param valStr the raw string value from the XML
   * @param annoTypeElt the TypeElement of the annotation
   * @param processingEnv the processing environment
   * @return null if the value was set, or a description of the problem if it was not
   */
  private static @Nullable String setBuilderValue(
      AnnotationBuilder builder,
      String memberName,
      String valStr,
      TypeElement annoTypeElt,
      ProcessingEnvironment processingEnv) {
    ExecutableElement memberMethod = null;
    for (ExecutableElement m : ElementFilter.methodsIn(annoTypeElt.getEnclosedElements())) {
      if (m.getSimpleName().contentEquals(memberName)) {
        memberMethod = m;
        break;
      }
    }
    if (memberMethod == null) {
      return String.format("the annotation has no element named '%s'", memberName);
    }

    TypeMirror returnType = memberMethod.getReturnType();
    if (returnType.getKind() == TypeKind.ARRAY) {
      ArrayType at = (ArrayType) returnType;
      TypeMirror compType = at.getComponentType();
      List<String> items = parseArrayLiteral(valStr);
      List<Object> parsedItems = new ArrayList<>(items.size());
      for (String item : items) {
        Object val = parseElementValue(item, compType, processingEnv);
        if (val == null) {
          return String.format(
              "cannot parse '%s' as a value of type %s, for element '%s'",
              item, compType, memberName);
        }
        parsedItems.add(val);
      }
      builder.setValue(memberName, parsedItems);
      return null;
    }

    Object val = parseElementValue(valStr, returnType, processingEnv);
    if (val instanceof Boolean b) {
      builder.setValue(memberName, b);
    } else if (val instanceof Integer i) {
      builder.setValue(memberName, i);
    } else if (val instanceof Long l) {
      builder.setValue(memberName, l);
    } else if (val instanceof Float f) {
      builder.setValue(memberName, f);
    } else if (val instanceof Double d) {
      builder.setValue(memberName, d);
    } else if (val instanceof Short s) {
      builder.setValue(memberName, s);
    } else if (val instanceof Byte b) {
      builder.setValue(memberName, b);
    } else if (val instanceof Character c) {
      builder.setValue(memberName, c);
    } else if (val instanceof String s) {
      builder.setValue(memberName, s);
    } else if (val instanceof VariableElement ve) {
      builder.setValue(memberName, ve);
    } else if (val instanceof TypeMirror tm) {
      builder.setValue(memberName, tm);
    } else {
      return String.format(
          "cannot parse '%s' as a value of type %s, for element '%s'",
          valStr, returnType, memberName);
    }
    return null;
  }

  /**
   * Parses a single element value according to its expected type.
   *
   * @param rawVal the raw value string
   * @param type the expected TypeMirror of the value
   * @param processingEnv the processing environment
   * @return the parsed object or null if it cannot be parsed
   */
  private static @Nullable Object parseElementValue(
      String rawVal, TypeMirror type, ProcessingEnvironment processingEnv) {
    String unquotedVal = stripQuotes(rawVal);
    TypeKind kind = type.getKind();
    try {
      if (kind == TypeKind.BOOLEAN) {
        return parseBoolean(unquotedVal);
      } else if (kind == TypeKind.INT) {
        return Integer.parseInt(unquotedVal);
      } else if (kind == TypeKind.LONG) {
        return Long.parseLong(unquotedVal.replaceAll("[lL]$", ""));
      } else if (kind == TypeKind.FLOAT) {
        return Float.parseFloat(unquotedVal.replaceAll("[fF]$", ""));
      } else if (kind == TypeKind.DOUBLE) {
        return Double.parseDouble(unquotedVal.replaceAll("[dD]$", ""));
      } else if (kind == TypeKind.SHORT) {
        return Short.parseShort(unquotedVal);
      } else if (kind == TypeKind.BYTE) {
        return Byte.parseByte(unquotedVal);
      }
    } catch (NumberFormatException e) {
      return null;
    }
    if (kind == TypeKind.CHAR) {
      return parseChar(unquotedVal);
    } else if (kind == TypeKind.DECLARED) {
      DeclaredType dt = (DeclaredType) type;
      TypeElement dtElt = (TypeElement) dt.asElement();
      if (dtElt.getQualifiedName().contentEquals("java.lang.String")) {
        return unquotedVal;
      } else if (dtElt.getKind() == ElementKind.ENUM) {
        String enumConstName =
            unquotedVal.substring(
                Math.max(unquotedVal.lastIndexOf('.'), unquotedVal.lastIndexOf('$')) + 1);
        for (VariableElement enumField : ElementFilter.fieldsIn(dtElt.getEnclosedElements())) {
          if (enumField.getSimpleName().contentEquals(enumConstName)) {
            return enumField;
          }
        }
      } else if (dtElt.getQualifiedName().contentEquals("java.lang.Class")) {
        String className = unquotedVal.replaceAll("\\.class$", "");
        TypeElement classTypeElt = getTypeElement(className, processingEnv.getElementUtils());
        if (classTypeElt != null) {
          // Erase, because that is what javac stores for a class literal and what
          // AnnotationBuilder.setValue(CharSequence, TypeMirror) does.
          return processingEnv.getTypeUtils().erasure(classTypeElt.asType());
        }
      }
    }
    return null;
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

  /**
   * Parses a boolean literal. Unlike {@link Boolean#parseBoolean}, which treats every string other
   * than "true" as false, this returns null for a string that is not a boolean literal.
   *
   * @param s a string
   * @return the boolean that {@code s} represents, or null if {@code s} is not a boolean literal
   */
  static @Nullable Boolean parseBoolean(String s) {
    if (s.equalsIgnoreCase("true")) {
      return true;
    } else if (s.equalsIgnoreCase("false")) {
      return false;
    } else {
      return null;
    }
  }

  /**
   * Parses a char literal value string, interpreting escape sequences.
   *
   * @param s the unquoted string
   * @return the char value
   */
  private static char parseChar(String s) {
    if (s.isEmpty()) {
      return '\0';
    }
    if (s.startsWith("\\") && s.length() > 1) {
      char next = s.charAt(1);
      return switch (next) {
        case 'n' -> '\n';
        case 'r' -> '\r';
        case 't' -> '\t';
        case 'b' -> '\b';
        case 'f' -> '\f';
        case '\'' -> '\'';
        case '\"' -> '\"';
        case '\\' -> '\\';
        case '0' -> '\0';
        default -> next;
      };
    }
    return s.charAt(0);
  }

  /**
   * Strips surrounding single or double quotation marks from a string literal value, and interprets
   * the Java escape sequences within it.
   *
   * @param s the string to strip quotes from
   * @return the string without surrounding quotes and with escape sequences interpreted
   */
  /*package*/ static String stripQuotes(String s) {
    s = s.trim();
    if (s.length() >= 2
        && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))
        && !lastCharIsEscaped(s)) {
      return StringsP.unescapeJava(s.substring(1, s.length() - 1));
    }
    return s;
  }

  /**
   * Returns true if the last character of {@code s} is escaped by a preceding backslash; that is,
   * the last character is preceded by an odd number of backslashes.
   *
   * @param s a string of length at least 2
   * @return true if the last character of {@code s} is escaped
   */
  private static boolean lastCharIsEscaped(String s) {
    int backslashes = 0;
    for (int i = s.length() - 2; i >= 0 && s.charAt(i) == '\\'; i--) {
      backslashes++;
    }
    return backslashes % 2 == 1;
  }

  /**
   * Parses an array literal in IntelliJ IDEA external annotations file format (e.g., {@code {val1,
   * val2}}).
   *
   * @param s the array literal string
   * @return a list of parsed item strings
   */
  /*package*/ static List<String> parseArrayLiteral(String s) {
    s = s.trim();
    if (s.startsWith("{") && s.endsWith("}")) {
      s = s.substring(1, s.length() - 1).trim();
    }
    if (s.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> items = new ArrayList<>();
    char quoteChar = '\0';
    boolean escaped = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (escaped) {
        sb.append(c);
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
        sb.append(c);
      } else if (quoteChar == '\0' && (c == '"' || c == '\'')) {
        quoteChar = c;
        sb.append(c);
      } else if (quoteChar != '\0' && c == quoteChar) {
        quoteChar = '\0';
        sb.append(c);
      } else if (c == ',' && quoteChar == '\0') {
        items.add(sb.toString().trim());
        sb.setLength(0);
      } else {
        sb.append(c);
      }
    }
    if (sb.length() > 0) {
      items.add(sb.toString().trim());
    }
    return items;
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
      int paramIndex = -1;
      if (!trailing.isEmpty()) {
        try {
          paramIndex = Integer.parseInt(trailing);
        } catch (NumberFormatException ignored) {
          // not an index
        }
      }

      String paramListStr = sig.substring(firstParen + 1, lastParen).trim();
      List<String> paramTypes = parseParameterTypes(paramListStr);

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
   * @return a list of trimmed parameter type strings
   */
  private static List<String> parseParameterTypes(String paramContent) {
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
    if (current.length() > 0) {
      paramTypes.add(current.toString().trim());
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
      warnNotFound(checker, "Class not found: " + parsed.className);
      return;
    }

    if (parsed.isMethodOrConstructor) {
      ExecutableElement execElem =
          findMatchingExecutable(
              classElem, parsed.memberName, parsed.paramTypes, parsed.isConstructor);
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

      markAsFromStubFile(execElem, processingEnv, annos);
      AnnotatedExecutableType methodType =
          (AnnotatedExecutableType)
              annos.atypes.computeIfAbsent(execElem, e -> atypeFactory.fromElement(execElem));

      if (parsed.paramIndex >= 0 && parsed.paramIndex < methodType.getParameterTypes().size()) {
        AnnotatedTypeMirror paramType = methodType.getParameterTypes().get(parsed.paramIndex);
        VariableElement paramElem = execElem.getParameters().get(parsed.paramIndex);
        markAsFromStubFile(paramElem, processingEnv, annos);
        for (AnnotationMirror am : annotations) {
          if (atypeFactory.isSupportedQualifier(am)) {
            paramType.replaceAnnotation(am);
          }
          recordDeclAnnotationIfApplicable(paramElem, am, annos);
        }
        annos.atypes.put(paramElem, paramType);
      } else if (parsed.paramIndex >= methodType.getParameterTypes().size()) {
        warnNotFound(
            checker,
            "Parameter index "
                + parsed.paramIndex
                + " out of bounds for "
                + parsed.memberName
                + " in "
                + parsed.className);
      } else if (parsed.paramIndex < 0) {
        AnnotatedTypeMirror returnType = methodType.getReturnType();
        for (AnnotationMirror am : annotations) {
          if (atypeFactory.isSupportedQualifier(am)) {
            returnType.replaceAnnotation(am);
          }
          recordDeclAnnotationIfApplicable(execElem, am, annos);
        }
      }
      annos.atypes.put(execElem, methodType);
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

      final VariableElement finalFieldElem = fieldElem;
      markAsFromStubFile(finalFieldElem, processingEnv, annos);
      AnnotatedTypeMirror fieldType =
          annos.atypes.computeIfAbsent(
              finalFieldElem, e -> atypeFactory.fromElement(finalFieldElem));
      for (AnnotationMirror am : annotations) {
        if (atypeFactory.isSupportedQualifier(am)) {
          fieldType.replaceAnnotation(am);
        }
        recordDeclAnnotationIfApplicable(finalFieldElem, am, annos);
      }
      annos.atypes.put(finalFieldElem, fieldType);
    } else if (parsed.isClass) {
      TypeElement finalClassElem = classElem;
      markAsFromStubFile(finalClassElem, processingEnv, annos);
      AnnotatedTypeMirror classType =
          annos.atypes.computeIfAbsent(
              finalClassElem, e -> atypeFactory.fromElement(finalClassElem));
      for (AnnotationMirror am : annotations) {
        if (atypeFactory.isSupportedQualifier(am)) {
          classType.replaceAnnotation(am);
        }
        recordDeclAnnotationIfApplicable(finalClassElem, am, annos);
      }
      annos.atypes.put(finalClassElem, classType);
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
   * @return the matching {@link ExecutableElement}, or null if not found
   */
  private static @Nullable ExecutableElement findMatchingExecutable(
      TypeElement classElem,
      @Nullable String methodName,
      List<String> expectedParamTypes,
      boolean isConstructor) {
    List<ExecutableElement> candidates =
        isConstructor
            ? ElementFilter.constructorsIn(classElem.getEnclosedElements())
            : ElementFilter.methodsIn(classElem.getEnclosedElements());

    // Pass 1: exact match
    for (ExecutableElement candidate : candidates) {
      if (matchesExecutable(candidate, methodName, expectedParamTypes, isConstructor, false)) {
        return candidate;
      }
    }
    // Pass 2: fallback match allowing Object for type variables
    for (ExecutableElement candidate : candidates) {
      if (matchesExecutable(candidate, methodName, expectedParamTypes, isConstructor, true)) {
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
   * @param allowTypeVarAsObject true if type variables are allowed to match java.lang.Object
   * @return true if candidate matches
   */
  private static boolean matchesExecutable(
      ExecutableElement candidate,
      @Nullable String methodName,
      List<String> expectedParamTypes,
      boolean isConstructor,
      boolean allowTypeVarAsObject) {
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
      if (!typeMatches(params.get(i).asType(), expectedParamTypes.get(i), allowTypeVarAsObject)) {
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
   * @param allowTypeVarAsObject true if type variables should match java.lang.Object as fallback
   * @return true if the type matches
   */
  private static boolean typeMatches(
      TypeMirror typeMirror, String expectedTypeStr, boolean allowTypeVarAsObject) {
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
          allowTypeVarAsObject);
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
          || (allowTypeVarAsObject && "java.lang.Object".equals(rawExpected));
    }

    return false;
  }
}
