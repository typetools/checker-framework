package org.checkerframework.framework.stub;

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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser for IntelliJ IDEA external annotations format ({@code annotations.xml}).
 *
 * <p>IntelliJ stores external annotations in XML files named {@code annotations.xml} located in
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
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      // Disable external DTD and entity resolution for security and speed
      try {
        dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      } catch (Exception ignored) {
        // Fall back if feature is unsupported by specific parser
      }
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(inputStream);
      doc.getDocumentElement().normalize();

      NodeList itemNodes = doc.getElementsByTagName("item");
      for (int i = 0; i < itemNodes.getLength(); i++) {
        Node node = itemNodes.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          org.w3c.dom.Element itemElement = (org.w3c.dom.Element) node;
          String itemName = itemElement.getAttribute("name");
          if (itemName == null || itemName.trim().isEmpty()) {
            continue;
          }

          List<AnnotationMirror> annotations =
              parseItemAnnotations(itemElement, atypeFactory, processingEnv);
          if (!annotations.isEmpty()) {
            applyAnnotationsToElement(
                itemName.trim(), annotations, atypeFactory, processingEnv, annotationFileAnnos);
          }
        }
      }
    } catch (Exception e) {
      checker.message(
          Diagnostic.Kind.NOTE,
          String.format("Could not parse annotations XML %s: %s", filename, e.getMessage()));
    }
  }

  /**
   * Parses the {@code <annotation>} children of an {@code <item>} element.
   *
   * @param itemElement the XML item element containing annotation child tags
   * @param atypeFactory the type factory
   * @param processingEnv the processing environment
   * @return a list of parsed and canonicalized {@link AnnotationMirror}s
   */
  @SuppressWarnings("signature")
  private static List<AnnotationMirror> parseItemAnnotations(
      org.w3c.dom.Element itemElement,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv) {
    List<AnnotationMirror> result = new ArrayList<>();
    Elements elements = processingEnv.getElementUtils();

    NodeList children = itemElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE && "annotation".equals(child.getNodeName())) {
        org.w3c.dom.Element annoElement = (org.w3c.dom.Element) child;
        String annoName = annoElement.getAttribute("name");
        if (annoName == null || annoName.trim().isEmpty()) {
          continue;
        }
        annoName = annoName.trim();

        TypeElement annoTypeElt = elements.getTypeElement(annoName);
        if (annoTypeElt == null) {
          // Annotation not found on classpath; skip
          continue;
        }

        try {
          AnnotationMirror annoMirror =
              buildAnnotationMirror(annoElement, annoTypeElt, processingEnv);
          if (annoMirror != null) {
            AnnotationMirror canonical = atypeFactory.canonicalAnnotation(annoMirror);
            result.add(canonical != null ? canonical : annoMirror);
          }
        } catch (Exception ignored) {
          // If annotation building fails for this specific item, continue
        }
      }
    }
    return result;
  }

  /**
   * Constructs an {@link AnnotationMirror} from an XML {@code <annotation>} element.
   *
   * @param annoElement the XML element for the annotation
   * @param annoTypeElt the TypeElement corresponding to the annotation
   * @param processingEnv the processing environment
   * @return the constructed {@link AnnotationMirror}, or null if construction fails
   */
  private static @Nullable AnnotationMirror buildAnnotationMirror(
      org.w3c.dom.Element annoElement,
      TypeElement annoTypeElt,
      ProcessingEnvironment processingEnv) {
    @SuppressWarnings("signature")
    @CanonicalName String canonicalName = annoTypeElt.getQualifiedName().toString();
    Elements elements = processingEnv.getElementUtils();

    NodeList valNodes = annoElement.getElementsByTagName("val");
    if (valNodes.getLength() == 0) {
      return AnnotationBuilder.fromName(elements, canonicalName);
    }

    AnnotationBuilder builder = new AnnotationBuilder(processingEnv, canonicalName);
    for (int i = 0; i < valNodes.getLength(); i++) {
      org.w3c.dom.Element valElem = (org.w3c.dom.Element) valNodes.item(i);
      if (!valElem.hasAttribute("val")) {
        continue;
      }
      String memberName = valElem.hasAttribute("name") ? valElem.getAttribute("name") : "value";
      String valStr = valElem.getAttribute("val").trim();
      setBuilderValue(builder, memberName, valStr, annoTypeElt, processingEnv);
    }

    return builder.build();
  }

  /**
   * Sets a value on the {@link AnnotationBuilder} based on the expected element type.
   *
   * @param builder the annotation builder
   * @param memberName the name of the annotation element
   * @param valStr the raw string value from the XML
   * @param annoTypeElt the TypeElement of the annotation
   * @param processingEnv the processing environment
   */
  @SuppressWarnings("signature")
  private static void setBuilderValue(
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
      return;
    }

    TypeMirror returnType = memberMethod.getReturnType();
    String unquotedVal = stripQuotes(valStr);

    if (returnType.getKind() == TypeKind.BOOLEAN) {
      builder.setValue(memberName, Boolean.parseBoolean(unquotedVal));
    } else if (returnType.getKind() == TypeKind.INT) {
      builder.setValue(memberName, Integer.parseInt(unquotedVal));
    } else if (returnType.getKind() == TypeKind.LONG) {
      builder.setValue(memberName, Long.parseLong(unquotedVal.replaceAll("[lL]$", "")));
    } else if (returnType.getKind() == TypeKind.FLOAT) {
      builder.setValue(memberName, Float.parseFloat(unquotedVal.replaceAll("[fF]$", "")));
    } else if (returnType.getKind() == TypeKind.DOUBLE) {
      builder.setValue(memberName, Double.parseDouble(unquotedVal.replaceAll("[dD]$", "")));
    } else if (returnType.getKind() == TypeKind.SHORT) {
      builder.setValue(memberName, Short.parseShort(unquotedVal));
    } else if (returnType.getKind() == TypeKind.BYTE) {
      builder.setValue(memberName, Byte.parseByte(unquotedVal));
    } else if (returnType.getKind() == TypeKind.CHAR) {
      builder.setValue(memberName, unquotedVal.length() > 0 ? unquotedVal.charAt(0) : '\0');
    } else if (returnType.getKind() == TypeKind.DECLARED) {
      DeclaredType dt = (DeclaredType) returnType;
      TypeElement dtElt = (TypeElement) dt.asElement();
      if (dtElt.getQualifiedName().contentEquals("java.lang.String")) {
        builder.setValue(memberName, unquotedVal);
      } else if (dtElt.getKind() == ElementKind.ENUM) {
        String enumConstName =
            unquotedVal.substring(
                Math.max(unquotedVal.lastIndexOf('.'), unquotedVal.lastIndexOf('$')) + 1);
        for (VariableElement enumField : ElementFilter.fieldsIn(dtElt.getEnclosedElements())) {
          if (enumField.getSimpleName().contentEquals(enumConstName)) {
            builder.setValue(memberName, enumField);
            break;
          }
        }
      } else if (dtElt.getQualifiedName().contentEquals("java.lang.Class")) {
        String className = unquotedVal.replaceAll("\\.class$", "");
        TypeElement classTypeElt = processingEnv.getElementUtils().getTypeElement(className);
        if (classTypeElt != null) {
          builder.setValue(memberName, classTypeElt.asType());
        }
      }
    } else if (returnType.getKind() == TypeKind.ARRAY) {
      ArrayType at = (ArrayType) returnType;
      TypeMirror compType = at.getComponentType();
      List<String> items = parseArrayLiteral(valStr);
      if (compType.getKind() == TypeKind.DECLARED
          && ((DeclaredType) compType).asElement().getSimpleName().contentEquals("String")) {
        List<String> stringItems = new ArrayList<>();
        for (String item : items) {
          stringItems.add(stripQuotes(item));
        }
        builder.setValue(memberName, stringItems.toArray(new String[0]));
      }
    }
  }

  /**
   * Strips surrounding quotation marks from a string literal value.
   *
   * @param s the string to strip quotes from
   * @return the string without surrounding quotes
   */
  private static String stripQuotes(String s) {
    if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  /**
   * Parses an array literal in IntelliJ external annotations format (e.g. {@code {val1, val2}}).
   *
   * @param s the array literal string
   * @return a list of parsed item strings
   */
  private static List<String> parseArrayLiteral(String s) {
    s = s.trim();
    if (s.startsWith("{") && s.endsWith("}")) {
      s = s.substring(1, s.length() - 1).trim();
    }
    if (s.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> items = new ArrayList<>();
    boolean inQuote = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"') {
        inQuote = !inQuote;
        sb.append(c);
      } else if (c == ',' && !inQuote) {
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
        String name = parts[2];
        if (name.equals(className) || name.equals(simpleClassName) || name.equals("<init>")) {
          isConstructor = true;
          memberName = simpleClassName;
        } else {
          memberName = name;
        }
      }

      return new ParsedItemSignature(
          className, memberName, paramTypes, paramIndex, true, isConstructor, false, false);
    } else {
      // Class or Field
      String[] parts = sig.split("\\s+");
      if (parts.length == 1) {
        return new ParsedItemSignature(
            parts[0], null, Collections.emptyList(), -1, false, false, false, true);
      } else if (parts.length == 2) {
        return new ParsedItemSignature(
            parts[0], parts[1], Collections.emptyList(), -1, false, false, true, false);
      } else {
        return new ParsedItemSignature(
            parts[0], parts[2], Collections.emptyList(), -1, false, false, true, false);
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
   */
  @SuppressWarnings("signature")
  private static void applyAnnotationsToElement(
      String itemSignature,
      List<AnnotationMirror> annotations,
      AnnotatedTypeFactory atypeFactory,
      ProcessingEnvironment processingEnv,
      AnnotationFileAnnotations annos) {
    ParsedItemSignature parsed = parseSignature(itemSignature);
    Elements elements = processingEnv.getElementUtils();

    TypeElement classElem = elements.getTypeElement(parsed.className.replace('$', '.'));
    if (classElem == null) {
      classElem = elements.getTypeElement(parsed.className);
    }
    if (classElem == null) {
      return;
    }

    if (parsed.isMethodOrConstructor) {
      ExecutableElement execElem =
          findMatchingExecutable(
              classElem, parsed.memberName, parsed.paramTypes, parsed.isConstructor);
      if (execElem == null) {
        return;
      }

      AnnotatedExecutableType methodType =
          (AnnotatedExecutableType)
              annos.atypes.computeIfAbsent(execElem, e -> atypeFactory.fromElement(execElem));

      if (parsed.paramIndex >= 0 && parsed.paramIndex < methodType.getParameterTypes().size()) {
        AnnotatedTypeMirror paramType = methodType.getParameterTypes().get(parsed.paramIndex);
        VariableElement paramElem = execElem.getParameters().get(parsed.paramIndex);
        for (AnnotationMirror am : annotations) {
          if (atypeFactory.isSupportedQualifier(am)) {
            paramType.replaceAnnotation(am);
          }
          recordDeclAnnotationIfApplicable(paramElem, am, annos);
        }
        annos.atypes.put(paramElem, paramType);
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
        return;
      }

      final VariableElement finalFieldElem = fieldElem;
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
      for (AnnotationMirror am : annotations) {
        recordDeclAnnotationIfApplicable(classElem, am, annos);
      }
    }
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

    for (ExecutableElement candidate : candidates) {
      if (!isConstructor
          && methodName != null
          && !candidate.getSimpleName().contentEquals(methodName)) {
        continue;
      }
      List<? extends VariableElement> params = candidate.getParameters();
      if (params.size() != expectedParamTypes.size()) {
        continue;
      }
      boolean matches = true;
      for (int i = 0; i < params.size(); i++) {
        if (!typeMatches(params.get(i).asType(), expectedParamTypes.get(i))) {
          matches = false;
          break;
        }
      }
      if (matches) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Compares a {@link TypeMirror} with an expected type signature string from IntelliJ.
   *
   * @param typeMirror the type mirror of the element parameter
   * @param expectedTypeStr the expected type name string
   * @return true if the type matches
   */
  private static boolean typeMatches(TypeMirror typeMirror, String expectedTypeStr) {
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
          expectedTypeStr.substring(0, expectedTypeStr.length() - 2));
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
      return tvName.equals(rawExpected) || "java.lang.Object".equals(rawExpected);
    }

    return false;
  }
}
