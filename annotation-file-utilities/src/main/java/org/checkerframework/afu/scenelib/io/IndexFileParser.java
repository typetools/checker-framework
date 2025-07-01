package org.checkerframework.afu.scenelib.io;

import static java.io.StreamTokenizer.TT_EOF;
import static java.io.StreamTokenizer.TT_NUMBER;
import static java.io.StreamTokenizer.TT_WORD;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.source.tree.Tree.Kind;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.checkerframework.afu.scenelib.Annotation;
import org.checkerframework.afu.scenelib.AnnotationBuilder;
import org.checkerframework.afu.scenelib.AnnotationFactory;
import org.checkerframework.afu.scenelib.Annotations;
import org.checkerframework.afu.scenelib.ArrayBuilder;
import org.checkerframework.afu.scenelib.el.ABlock;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.ADeclaration;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AExpression;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.ATypeElementWithType;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.el.BoundLocation;
import org.checkerframework.afu.scenelib.el.LocalLocation;
import org.checkerframework.afu.scenelib.el.RelativeLocation;
import org.checkerframework.afu.scenelib.el.TypeIndexLocation;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.afu.scenelib.field.AnnotationAFT;
import org.checkerframework.afu.scenelib.field.AnnotationFieldType;
import org.checkerframework.afu.scenelib.field.ArrayAFT;
import org.checkerframework.afu.scenelib.field.BasicAFT;
import org.checkerframework.afu.scenelib.field.ClassTokenAFT;
import org.checkerframework.afu.scenelib.field.EnumAFT;
import org.checkerframework.afu.scenelib.field.ScalarAFT;
import org.checkerframework.afu.scenelib.type.ArrayType;
import org.checkerframework.afu.scenelib.type.BoundedType;
import org.checkerframework.afu.scenelib.type.BoundedType.BoundKind;
import org.checkerframework.afu.scenelib.type.DeclaredType;
import org.checkerframework.afu.scenelib.type.Type;
import org.checkerframework.afu.scenelib.util.coll.VivifyingMap;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.checker.signature.qual.Identifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.objectweb.asm.TypePath;
import org.plumelib.util.ArraysPlume;
import org.plumelib.util.FileIOException;
import org.plumelib.util.IPair;

/**
 * IndexFileParser provides static methods {@link #parse(LineNumberReader, String, AScene)}, {@link
 * #parseFile(String, AScene)}, and {@link #parseString(String, String, AScene)}. Each of these
 * parses an index file into a {@link AScene}.
 *
 * <p>If there are any problems, it throws a ParseException internally, or a FileIOException
 * externally.
 */
public final class IndexFileParser {

  private static final String[] typeSelectors = {
    "bound",
    "identifier",
    "type",
    "typeAlternative",
    "typeArgument",
    "typeParameter",
    "underlyingType"
  };

  private static boolean abbreviate = true;

  // The input
  private final StreamTokenizer st;
  // filename or other source
  private final String source;

  // The output
  private final AScene scene;

  private String curPkgPrefix;

  /**
   * Holds definitions we've seen so far. Maps from annotation name to the definition itself. Maps
   * from both the qualified name and the unqualified name. If the unqualified name is not unique,
   * it maps to null and the qualified name should be used instead.
   */
  private final HashMap<String, AnnotationDef> defs;

  public static void setAbbreviate(boolean b) {
    abbreviate = b;
  }

  private int expectNonNegative(int i) throws ParseException {
    if (i >= 0) {
      return i;
    } else {
      throw new ParseException("Expected a nonnegative integer, got " + st);
    }
  }

  /** True if the next thing from st is the given character. */
  private boolean checkChar(char c) {
    return st.ttype == c;
  }

  /** True if the next thing from st is the given string token. */
  private boolean checkKeyword(String s) {
    return st.ttype == TT_WORD && st.sval.equals(s);
  }

  /**
   * Return true if the next thing to be read from st is the given string. In that case, also read
   * past the given string. If the result is false, reads nothing from st.
   */
  private boolean matchChar(char c) throws IOException {
    if (checkChar(c)) {
      st.nextToken();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Return true if the next thing to be read from st is the given string. In that case, also read
   * past the given string. If the result is false, reads nothing from st.
   */
  private boolean matchKeyword(String s) throws IOException {
    if (checkKeyword(s)) {
      st.nextToken();
      return true;
    } else {
      return false;
    }
  }

  /** Reads from st. If the result is not c, throws an exception. */
  private void expectChar(char c) throws IOException, ParseException {
    if (!matchChar(c)) {
      // Alternately, could use st.toString().
      String found;
      switch (st.ttype) {
        case StreamTokenizer.TT_WORD:
          found = st.sval;
          break;
        case StreamTokenizer.TT_NUMBER:
          found = "" + st.nval;
          break;
        case StreamTokenizer.TT_EOL:
          found = "end of line";
          break;
        case StreamTokenizer.TT_EOF:
          found = "end of file";
          break;
        default:
          found = "'" + ((char) st.ttype) + "'";
          break;
      }
      throw new ParseException("Expected '" + c + "', found " + found);
    }
  }

  /** Reads from st. If the result is not s, throws an exception. */
  private void expectKeyword(String s) throws IOException, ParseException {
    if (!matchKeyword(s)) {
      throw new ParseException("Expected `" + s + "'");
    }
  }

  private static final Set<String> knownKeywords;

  static {
    String[] knownKeywords_array = {
      "abstract",
      "assert",
      "boolean",
      "break",
      "byte",
      "case",
      "catch",
      "char",
      "class",
      "const",
      "continue",
      "default",
      "do",
      "double",
      "else",
      "enum",
      "extends",
      "false",
      "final",
      "finally",
      "float",
      "for",
      "if",
      "goto",
      "implements",
      "import",
      "instanceof",
      "int",
      "interface",
      "long",
      "native",
      "new",
      "null",
      "package",
      "private",
      "protected",
      "public",
      "return",
      "short",
      "static",
      "strictfp",
      "super",
      "switch",
      "synchronized",
      "this",
      "throw",
      "throws",
      "transient",
      "true",
      "try",
      "void",
      "volatile",
      "while",
    };
    knownKeywords = new LinkedHashSet<String>();
    Collections.addAll(knownKeywords, knownKeywords_array);
  }

  /**
   * Returns true if the given string is an identifier.
   *
   * @param x a string
   * @return true if the given string is an identifier
   */
  @SuppressWarnings("signature:contracts.conditional.postcondition") // string parsing
  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Identifier.class)
  private boolean isValidIdentifier(String x) {
    if (x.length() == 0
        || !Character.isJavaIdentifierStart(x.charAt(0))
        || knownKeywords.contains(x)) return false;
    for (int i = 1; i < x.length(); i++) {
      if (!Character.isJavaIdentifierPart(x.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the next token, if it is an identifier. Does not advance parsing past the identifier.
   *
   * @return the next token, so long as it is an identifier; otherwise, returns null
   */
  private @Identifier String checkIdentifier() {
    if (st.sval == null) {
      return null;
    } else {
      String val = st.sval;
      if (st.ttype == TT_WORD && isValidIdentifier(val)) {
        return val;
      } else {
        return null;
      }
    }
  }

  /**
   * Returns the next token, if it is an identifier. Advances parsing past the identifier.
   *
   * @return the next token, so long as it is an identifier; otherwise, returns null
   * @throws IOException if there is trouble reading the index file
   */
  private @Identifier String matchIdentifier() throws IOException {
    String x = checkIdentifier();
    if (x != null) {
      st.nextToken();
      return x;
    } else {
      return null;
    }
  }

  /**
   * Returns the next token, if it is an identifier. Advances parsing past the identifier.
   *
   * @return the next token, so long as it is an identifier; otherwise, throws ParseException
   * @throws IOException if there is trouble reading the index file
   * @throws ParseException if the file contents are not valid
   */
  private @Identifier String expectIdentifier() throws IOException, ParseException {
    String id = matchIdentifier();
    if (id == null) {
      throw new ParseException("Expected an identifier");
    }
    return id;
  }

  private String checkPrimitiveType() {
    if (st.sval == null) {
      return null;
    } else {
      String val = st.sval;
      if (st.ttype == TT_WORD && primitiveTypes.containsKey(val)) {
        return st.sval;
      } else {
        return null;
      }
    }
  }

  private String matchPrimitiveType() throws IOException {
    String x = checkPrimitiveType();
    if (x != null) {
      st.nextToken();
      return x;
    } else {
      return null;
    }
  }

  // an identifier, or a sequence of dot-separated identifiers
  private String expectQualifiedName() throws IOException, ParseException {
    String name = expectIdentifier();
    while (matchChar('.')) {
      name += '.' + expectIdentifier();
    }
    return name;
  }

  private int checkNNInteger() {
    if (st.ttype == TT_NUMBER) {
      int x = (int) st.nval;
      if (x == st.nval && x >= -1) // shouldn't give us a huge number
      return x;
    }
    return -1;
  }

  private int matchNNInteger() throws IOException {
    int x = checkNNInteger();
    if (x >= -1) {
      st.nextToken();
      return x;
    } else {
      return -1;
    }
  }

  // Mapping from primitive types and void to their corresponding
  // class objects. Class.forName doesn't directly support these.
  // Using this map we can go from "void.class" to the correct
  // Class object.
  private static final Map<String, Class<?>> primitiveTypes;

  static {
    Map<String, Class<?>> pt = new LinkedHashMap<>();
    pt.put("byte", byte.class);
    pt.put("short", short.class);
    pt.put("int", int.class);
    pt.put("long", long.class);
    pt.put("float", float.class);
    pt.put("double", double.class);
    pt.put("char", char.class);
    pt.put("boolean", boolean.class);
    pt.put("void", void.class);
    primitiveTypes = pt;
  }

  /**
   * Expect the class name in the format that Class.forName accepts. Examples:
   *
   * <pre>{@code
   * "[[I"            for int[][].class
   * "[java.util.Map" for Map[].class
   * "java.util.Map"  for Map.class
   * }</pre>
   *
   * Thes use fully-qualified names, i.e. "Object" alone won't work.
   */
  private @ClassGetName String expectClassGetName() throws IOException, ParseException {
    int arrays = 0;
    StringBuilder type = new StringBuilder();
    while (matchChar('[')) {
      // Array dimensions as prefix
      ++arrays;
    }
    while (!matchKeyword("class")) {
      if (st.ttype >= 0) {
        type.append((char) st.ttype);
      } else if (st.ttype == TT_WORD) {
        type.append(st.sval);
      } else {
        throw new ParseException("Found something that doesn't belong in a signature");
      }
      st.nextToken();
    }

    // Drop the '.' before the "class"
    type.deleteCharAt(type.length() - 1);
    // expectKeyword("class");

    // Add arrays as prefix in the type.
    while (arrays-- > 0) {
      type.insert(0, '[');
    }

    @SuppressWarnings("signature") // string manipulation while parsing file
    @ClassGetName String result = type.toString();
    return result;
  }

  /** Parse scalar annotation value. */
  // HMMM can a (readonly) Integer be casted to a writable Object?
  private Object parseScalarAFV(ScalarAFT aft) throws IOException, ParseException {
    if (aft instanceof BasicAFT) {
      Object val;
      BasicAFT baft = (BasicAFT) aft;
      Class<?> type = baft.type;
      if (type == boolean.class) {
        if (matchKeyword("true")) {
          val = true;
        } else if (matchKeyword("false")) {
          val = false;
        } else {
          throw new ParseException("Expected `true' or `false'");
        }
      } else if (type == char.class) {
        if (st.ttype == '\'' && st.sval.length() == 1) {
          val = st.sval.charAt(0);
        } else {
          throw new ParseException("Expected a character literal");
        }
        st.nextToken();
      } else if (type == String.class) {
        if (st.ttype == '"') {
          val = st.sval;
        } else {
          throw new ParseException("Expected a string literal");
        }
        st.nextToken();
      } else {
        if (st.ttype == TT_NUMBER) {
          double n = st.nval;
          st.nextToken();
          if (type == byte.class) {
            val = (byte) n;
          } else if (type == short.class) {
            val = (short) n;
          } else if (type == int.class) {
            val = (int) n;
          } else if (type == long.class) {
            val = (long) n;
            // permit optional 'L' character after long literals
            matchKeyword("L");
          } else if (type == float.class) {
            val = (float) n;
            // StreamTokenizer can't handle all floating point numbers, so parse them here.
            if (st.sval.matches("E[0-9]+")) {
              val = Float.parseFloat(val + st.sval);
              st.nextToken();
            }
          } else if (type == double.class) {
            val = n;
            // StreamTokenizer can't handle all floating point numbers, so parse them here.
            if (st.sval.matches("E[0-9]+")) {
              val = Double.parseDouble(val + st.sval);
              st.nextToken();
            }
          } else {
            throw new AssertionError();
          }
        } else {
          throw new ParseException("Expected a number literal");
        }
      }
      assert aft.isValidValue(val);
      return val;
    } else if (aft instanceof ClassTokenAFT) {
      String cgname = expectClassGetName();
      try {
        Class<?> tktype;
        if (primitiveTypes.containsKey(cgname)) {
          tktype = primitiveTypes.get(cgname);
        } else {
          tktype = Class.forName(cgname);
        }
        assert aft.isValidValue(tktype);
        return tktype;
      } catch (ClassNotFoundException e) {
        throw new ParseException("Could not load class: " + cgname, e);
      }
    } else if (aft instanceof EnumAFT) {
      String name = expectQualifiedName();
      assert aft.isValidValue(name);
      return name;
    } else if (aft instanceof AnnotationAFT) {
      AnnotationAFT aaft = (AnnotationAFT) aft;
      AnnotationDef d = parseAnnotationHead();
      if (!d.name.equals(aaft.annotationDef.name)) {
        throw new ParseException(
            "Got an "
                + d.name
                + " subannotation where an "
                + aaft.annotationDef.name
                + " was expected");
      }
      AnnotationBuilder ab = AnnotationFactory.saf.beginAnnotation(d, source);
      // interested in this annotation,
      // so should be interested in subannotations
      assert ab != null;
      AnnotationBuilder ab2 = ab;
      Annotation suba = parseAnnotationBody(d, ab2);
      assert aft.isValidValue(suba);
      return suba;
    } else {
      throw new AssertionError("IndexFileParser.parseScalarAFV: unreachable code.");
    }
  }

  private void parseAndAddArrayAFV(ArrayAFT aaft, ArrayBuilder arrb)
      throws IOException, ParseException {
    ScalarAFT comp;
    if (aaft.elementType != null) {
      comp = aaft.elementType;
    } else {
      throw new IllegalArgumentException("array AFT has null elementType");
    }
    if (matchChar('{')) {
      // read an array
      while (!matchChar('}')) {
        arrb.appendElement(parseScalarAFV(comp));
        if (!checkChar('}')) {
          expectChar(',');
        }
      }
    } else {
      // not an array, so try reading just one value as an array
      arrb.appendElement(parseScalarAFV(comp));
    }
    arrb.finish();
  }

  // parses a field such as "f1=5" in "@A(f1=5, f2=10)".
  private void parseAnnotationField(AnnotationDef d, AnnotationBuilder ab)
      throws IOException, ParseException {
    String fieldName;
    if (d.fieldTypes.size() == 1 && d.fieldTypes.containsKey("value")) {
      fieldName = "value";
      if (matchKeyword("value")) {
        expectChar('=');
      }
    } else {
      fieldName = expectIdentifier();
      expectChar('=');
    }
    // HMMM let's hope the builder checks for duplicate fields
    // because we can't do it any more
    AnnotationFieldType aft1 = d.fieldTypes.get(fieldName);
    if (aft1 == null) {
      throw new ParseException(
          "The annotation type " + d.name + " has no field called " + fieldName);
    }
    AnnotationFieldType aft = aft1;
    if (aft instanceof ArrayAFT) {
      ArrayAFT aaft = (ArrayAFT) aft;
      if (aaft.elementType == null) {
        // Array of unknown element type--must be zero-length
        expectChar('{');
        expectChar('}');
        ab.addEmptyArrayField(fieldName);
      } else {
        parseAndAddArrayAFV(aaft, ab.beginArrayField(fieldName, aaft));
      }
    } else if (aft instanceof ScalarAFT) {
      ScalarAFT saft = (ScalarAFT) aft;
      Object value = parseScalarAFV(saft);
      ab.addScalarField(fieldName, saft, value);
    } else {
      throw new AssertionError();
    }
  }

  // reads the "@A" part of an annotation such as "@A(f1=5, f2=10)".
  private AnnotationDef parseAnnotationHead() throws IOException, ParseException {
    expectChar('@');
    String name = expectQualifiedName();
    AnnotationDef d = defs.get(name);
    if (d == null) {
      ParseException e = new ParseException("No definition for annotation type " + name);
      if (false) { // for debugging
        System.err.println("No definition for annotation type " + name);
        System.err.printf("  defs contains %d entries%n", defs.size());
        for (Map.Entry<String, AnnotationDef> entry : defs.entrySet()) {
          System.err.printf("    defs entry: %s => %s%n", entry.getKey(), entry.getValue());
        }
        e.printStackTrace(System.err);
      }
      throw e;
    }
    return d;
  }

  private Annotation parseAnnotationBody(AnnotationDef d, AnnotationBuilder ab)
      throws IOException, ParseException {
    if (matchChar('(')) {
      parseAnnotationField(d, ab);
      while (matchChar(',')) {
        parseAnnotationField(d, ab);
      }
      expectChar(')');
    }
    Annotation ann = ab.finish();
    if (!ann.def.equals(d)) {
      throw new ParseException(
          "parseAnnotationBody: Annotation def isn't as it should be.\n" + d + "\n" + ann.def);
    }
    if (ann.def().fieldTypes.size() != d.fieldTypes.size()) {
      throw new ParseException("At least one annotation field is missing");
    }
    return ann;
  }

  private void parseAnnotations(AElement e) throws IOException, ParseException {
    while (checkChar('@')) {
      AnnotationDef d = parseAnnotationHead();
      AnnotationBuilder ab = AnnotationFactory.saf.beginAnnotation(d, source);
      if (ab == null) {
        // don't care about the result
        // but need to skip over it anyway
        @SuppressWarnings("unused")
        Object trash = parseAnnotationBody(d, AnnotationFactory.saf.beginAnnotation(d, source));
      } else {
        Annotation a = parseAnnotationBody(d, ab);
        for (Annotation other : e.tlAnnotationsHere) {
          if (a.def.name.equals(other.def.name)) {
            // Don't output this warning, because the annotation might be repeatable.
            // TODO: IndexFileWriter should output the @Repeatable(EnsuresQualifier.List.class)
            // meta-annotation if present (and maybe other meta-annotations too), and
            // then this code can output the warning (or even crash) if it is not present.
            if (false) {
              System.err.printf(
                  "WARNING: duplicate annotation of type %s on %s%n", a.def().name, e.description);
            }
          }
        }
        Annotation tla = a;
        if (!tla.def.equals(d)) {
          throw new ParseException("Bad def");
        }
        e.tlAnnotationsHere.add(tla);
      }
    }
  }

  /**
   * Get the {@link ScalarAFT} for the annotation currenttly being parsed.
   *
   * @param annotationFullyQualifiedName the fully-qualified name of current parsing annotation
   * @return the {@link ScalarAFT} of current parsing annotation
   */
  private ScalarAFT parseScalarAFT(String annotationFullyQualifiedName)
      throws IOException, ParseException {
    for (BasicAFT baft : BasicAFT.bafts.values()) {
      if (matchKeyword(baft.toString())) {
        return baft;
      }
    }
    // wasn't a BasicAFT
    if (matchKeyword("Class")) {
      return ClassTokenAFT.ctaft /* dumpParameterization() */;
    } else if (matchKeyword("enum")) {
      String name = expectQualifiedName();
      if (abbreviate) {
        int i = name.lastIndexOf('.');
        if (i >= 0) {
          Set<String> importSet = scene.imports.get(annotationFullyQualifiedName);
          if (importSet == null) {
            importSet = new TreeSet<String>();
            scene.imports.put(annotationFullyQualifiedName, importSet);
          }
          importSet.add(name);
          String baseName = name.substring(i + 1);
          name = baseName;
        }
      }
      return new EnumAFT(name);
    } else if (matchKeyword("annotation-field")) {
      String name = expectQualifiedName();
      AnnotationDef ad = defs.get(name);
      if (ad == null) {
        throw new ParseException(
            "Annotation type " + name + " used as a field before it is defined");
      }
      return new AnnotationAFT(ad);
    } else {
      throw new ParseException(
          "Expected the beginning of an annotation field type: "
              + "a primitive type, `String', `Class', `enum', or `annotation-field'. Got '"
              + st.sval
              + "'.");
    }
  }

  /**
   * Parse {@link AnnotationFieldType} of current parsing annotation.
   *
   * @param annotationFullyQualifiedName the fully-qualified name of the annotation
   * @return {@link AnnotationFieldType} of current parsing annotation
   * @throws IOException if any IOException happened
   * @throws ParseException if any ParseException happened
   */
  private AnnotationFieldType parseAFT(String annotationFullyQualifiedName)
      throws IOException, ParseException {
    if (matchKeyword("unknown")) {
      // Handle unknown[]; see AnnotationBuilder#addEmptyArrayField
      expectChar('[');
      expectChar(']');
      return new ArrayAFT(null);
    }
    ScalarAFT baseAFT = parseScalarAFT(annotationFullyQualifiedName);
    // only one level of array is permitted
    if (matchChar('[')) {
      expectChar(']');
      return new ArrayAFT(baseAFT);
    } else {
      return baseAFT;
    }
  }

  private void parseAnnotationDef() throws IOException, ParseException {
    expectKeyword("annotation");

    expectChar('@');
    String basename = expectIdentifier();
    @SuppressWarnings("signature") // string concatenation
    @BinaryName String fullName = curPkgPrefix + basename;

    AnnotationDef ad = new AnnotationDef(fullName, source);
    expectChar(':');
    parseAnnotations(ad);

    Map<String, AnnotationFieldType> fields = new LinkedHashMap<>();

    // yuck; it would be nicer to do a positive match
    while (st.ttype != TT_EOF
        && !checkKeyword("annotation")
        && !checkKeyword("class")
        && !checkKeyword("package")) {
      AnnotationFieldType type = parseAFT(fullName);
      String name = expectIdentifier();
      if (fields.containsKey(name)) {
        throw new ParseException("Duplicate definition of field " + name);
      }
      fields.put(name, type);
    }

    ad.setFieldTypes(fields);

    // Now add the definition to the map of all definitions.
    addDef(ad, basename);
  }

  // Add the definition to the map of all definitions.
  // also see addDef(AnnotationDef, String).
  public void addDef(AnnotationDef ad) throws ParseException {
    String basename = ad.name;
    int dotPos = basename.lastIndexOf('.');
    if (dotPos != -1) {
      basename = basename.substring(dotPos + 1);
    }
    addDef(ad, basename);
  }

  // Add the definition to the map of all definitions.
  public void addDef(AnnotationDef ad, String basename) throws ParseException {
    // System.out.println("addDef:" + ad);

    if (defs.containsKey(ad.name)) {
      // TODO:  permit identical re-definition
      System.err.println("Duplicate definition of annotation type " + ad.name);
    }
    defs.put(ad.name, ad);
    // Add short name; but if it's already there, remove it to avoid ambiguity.
    if (!basename.equals(ad.name)) {
      if (defs.containsKey(basename)) {
        // not "defs.remove(basename)" because then a subsequent
        // one could get added, which would be wrong.
        defs.put(basename, null);
      } else {
        defs.put(basename, ad);
      }
    }
  }

  private void parseInnerTypes(ATypeElement e) throws IOException, ParseException {
    parseInnerTypes(e, 0);
  }

  private void parseInnerTypes(ATypeElement e, int offset) throws IOException, ParseException {
    while (matchKeyword("inner-type")) {
      ArrayList<Integer> locNumbers = new ArrayList<>();
      locNumbers.add(offset + expectNonNegative(matchNNInteger()));
      // TODO: currently, we simply read the binary representation.
      // Should we read a higher-level format?
      while (matchChar(',')) {
        locNumbers.add(expectNonNegative(matchNNInteger()));
      }
      TypePath typePath;
      try {
        typePath = TypePathEntry.getTypePathFromBinary(locNumbers);
      } catch (AssertionError ex) {
        throw new ParseException(ex.getMessage(), ex);
      }
      AElement it = e.innerTypes.getVivify(TypePathEntry.typePathToList(typePath));
      expectChar(':');
      parseAnnotations(it);
    }
  }

  private void parseBounds(VivifyingMap<BoundLocation, ATypeElement> bounds)
      throws IOException, ParseException {
    while (checkKeyword("typeparam") || checkKeyword("bound")) {
      if (matchKeyword("typeparam")) {
        int paramIndex = expectNonNegative(matchNNInteger());
        BoundLocation bl = new BoundLocation(paramIndex, -1);
        ATypeElement b = bounds.getVivify(bl);
        expectChar(':');
        parseAnnotations(b);
        // does this make sense?
        parseInnerTypes(b);
      } else if (matchKeyword("bound")) {
        // expectChar(',');
        int paramIndex = expectNonNegative(matchNNInteger());
        expectChar('&');
        int boundIndex = expectNonNegative(matchNNInteger());
        BoundLocation bl = new BoundLocation(paramIndex, boundIndex);
        ATypeElement b = bounds.getVivify(bl);
        expectChar(':');
        parseAnnotations(b);
        // does this make sense?
        parseInnerTypes(b);
      } else {
        throw new Error("impossible");
      }
    }
  }

  private void parseExtends(AClass cls) throws IOException, ParseException {
    expectKeyword("extends");
    TypeIndexLocation idx = new TypeIndexLocation(-1);
    ATypeElement ext = cls.extendsImplements.getVivify(idx);
    expectChar(':');
    parseAnnotations(ext);
    parseInnerTypes(ext);
  }

  private void parseImplements(AClass cls) throws IOException, ParseException {
    expectKeyword("implements");
    int implIndex = expectNonNegative(matchNNInteger());
    TypeIndexLocation idx = new TypeIndexLocation(implIndex);
    ATypeElement impl = cls.extendsImplements.getVivify(idx);
    expectChar(':');
    parseAnnotations(impl);
    parseInnerTypes(impl);
  }

  private void parseField(AClass c) throws IOException, ParseException {
    expectKeyword("field");
    String name = expectIdentifier();
    AField f = c.fields.getVivify(name);

    expectChar(':');
    parseAnnotations(f);
    if (checkKeyword("type") && matchKeyword("type")) {
      expectChar(':');
      parseAnnotations(f.type);
      parseInnerTypes(f.type);
    }

    f.init = c.fieldInits.getVivify(name);
    parseExpression(f.init);
    parseASTInsertions(f);
  }

  private void parseStaticInit(AClass c) throws IOException, ParseException {
    expectKeyword("staticinit");
    expectChar('*');
    int blockIndex = expectNonNegative(matchNNInteger());
    expectChar(':');

    ABlock staticinit = c.staticInits.getVivify(blockIndex);
    parseBlock(staticinit);
  }

  private void parseInstanceInit(AClass c) throws IOException, ParseException {
    expectKeyword("instanceinit");
    expectChar('*');
    int blockIndex = expectNonNegative(matchNNInteger());
    expectChar(':');

    ABlock instanceinit = c.instanceInits.getVivify(blockIndex);
    parseBlock(instanceinit);
  }

  private void parseMethod(AClass c) throws IOException, ParseException {
    expectKeyword("method");
    // special case: method could be <init> or <clinit>
    String key;
    if (matchChar('<')) {
      String basename = expectIdentifier();
      if (!(basename.equals("init") || basename.equals("clinit"))) {
        throw new ParseException("The only special methods allowed are <init> and <clinit>");
      }
      expectChar('>');
      key = "<" + basename + ">";
    } else {
      key = expectIdentifier();
      // too bad className is private in AClass and thus must be
      // extracted from what toString() returns
      if (Pattern.matches("AClass: (?:[^. ]+\\.)*" + key, c.toString())) { // ugh
        key = "<init>";
      }
    }

    expectChar('(');
    key += '(';
    while (!matchChar(':')) {
      if (st.ttype >= 0) {
        key += st.ttype == 46 ? '/' : (char) st.ttype;
      } else if (st.ttype == TT_WORD) {
        key += st.sval;
      } else {
        throw new ParseException("Found something that doesn't belong in a signature");
      }
      st.nextToken();
    }

    AMethod m = c.methods.getVivify(key);
    parseAnnotations(m);
    parseMethod(m);
  }

  private void parseMethod(AMethod m) throws IOException, ParseException {
    parseBounds(m.bounds);

    // Permit return value, receiver, and parameters in any order.
    while (checkKeyword("return") || checkKeyword("receiver") || checkKeyword("parameter")) {
      if (matchKeyword("return")) {
        expectChar(':');
        parseAnnotations(m.returnType);
        parseInnerTypes(m.returnType);
      } else if (matchKeyword("parameter")) {
        // make "#" optional
        if (checkChar('#')) {
          matchChar('#');
        }
        int idx = expectNonNegative(matchNNInteger());
        AField p = m.parameters.getVivify(idx);
        expectChar(':');
        parseAnnotations(p);
        if (checkKeyword("type") && matchKeyword("type")) {
          expectChar(':');
          parseAnnotations(p.type);
          parseInnerTypes(p.type);
        }
      } else if (matchKeyword("receiver")) {
        expectChar(':');
        parseAnnotations(m.receiver.type);
        parseInnerTypes(m.receiver.type);
      } else {
        throw new Error("This can't happen");
      }
    }

    parseBlock(m.body);
    parseASTInsertions(m);
  }

  private void parseLambda(AMethod m) throws IOException, ParseException {
    while (checkKeyword("parameter")) {
      matchKeyword("parameter");
      // make "#" optional
      if (checkChar('#')) {
        matchChar('#');
      }
      int idx = expectNonNegative(matchNNInteger());
      AField p = m.parameters.getVivify(idx);
      expectChar(':');
      parseAnnotations(p);
      if (checkKeyword("type") && matchKeyword("type")) {
        expectChar(':');
        parseAnnotations(p.type);
        parseInnerTypes(p.type);
      }
    }

    // parseBlock(m.body, true);
    parseASTInsertions(m);
  }

  private void parseBlock(ABlock bl) throws IOException, ParseException {
    boolean matched = true;

    while (matched) {
      matched = false;

      while (checkKeyword("local")) {
        matchKeyword("local");
        matched = true;
        LocalLocation loc;
        if (checkNNInteger() != -1) {
          // the local variable is specified by bytecode index/range
          int index = expectNonNegative(matchNNInteger());
          expectChar('#');
          int scopeStart = expectNonNegative(matchNNInteger());
          expectChar('+');
          int scopeLength = expectNonNegative(matchNNInteger());
          loc = new LocalLocation(scopeStart, scopeLength, index);
        }
        // TODO: Need some way to get the actual variable info from string, or deprecate this
        // feature.
        else {
          // look for a valid identifier for the local variable
          String lvar = expectIdentifier();
          int varIndex;
          if (checkChar('*')) {
            expectChar('*');
            varIndex = expectNonNegative(matchNNInteger());
          } else {
            // default the variable index to 0, the most common case
            varIndex = 0;
          }
          loc = new LocalLocation(varIndex, lvar);
        }
        AField l = bl.locals.getVivify(loc);
        expectChar(':');
        parseAnnotations(l);
        if (checkKeyword("type") && matchKeyword("type")) {
          expectChar(':');
          parseAnnotations(l.type);
          parseInnerTypes(l.type);
        }
      }
      matched = parseExpression(bl) || matched;
    }
  }

  private boolean parseExpression(AExpression exp) throws IOException, ParseException {
    boolean matched = true;
    boolean evermatched = false;

    while (matched) {
      matched = false;

      while (checkKeyword("typecast")) {
        matchKeyword("typecast");
        matched = true;
        evermatched = true;
        RelativeLocation loc;
        if (checkChar('#')) {
          expectChar('#');
          int offset = expectNonNegative(matchNNInteger());
          int type_index = 0;
          if (checkChar(',')) {
            expectChar(',');
            type_index = expectNonNegative(matchNNInteger());
          }
          loc = RelativeLocation.createOffset(offset, type_index);
        } else {
          expectChar('*');
          int index = expectNonNegative(matchNNInteger());
          int type_index = 0;
          if (checkChar(',')) {
            expectChar(',');
            type_index = expectNonNegative(matchNNInteger());
          }
          loc = RelativeLocation.createIndex(index, type_index);
        }
        ATypeElement t = exp.typecasts.getVivify(loc);
        expectChar(':');
        parseAnnotations(t);
        parseInnerTypes(t);
      }
      while (checkKeyword("instanceof")) {
        matchKeyword("instanceof");
        matched = true;
        evermatched = true;
        RelativeLocation loc;
        if (checkChar('#')) {
          expectChar('#');
          int offset = expectNonNegative(matchNNInteger());
          loc = RelativeLocation.createOffset(offset, 0);
        } else {
          expectChar('*');
          int index = expectNonNegative(matchNNInteger());
          loc = RelativeLocation.createIndex(index, 0);
        }
        ATypeElement i = exp.instanceofs.getVivify(loc);
        expectChar(':');
        parseAnnotations(i);
        parseInnerTypes(i);
      }
      while (checkKeyword("new")) {
        matchKeyword("new");
        matched = true;
        evermatched = true;
        RelativeLocation loc;
        if (checkChar('#')) {
          expectChar('#');
          int offset = expectNonNegative(matchNNInteger());
          loc = RelativeLocation.createOffset(offset, 0);
        } else {
          expectChar('*');
          int index = expectNonNegative(matchNNInteger());
          loc = RelativeLocation.createIndex(index, 0);
        }
        ATypeElement n = exp.news.getVivify(loc);
        expectChar(':');
        parseAnnotations(n);
        parseInnerTypes(n);
      }
      while (checkKeyword("call")) {
        matchKeyword("call");
        matched = true;
        evermatched = true;
        int i;
        boolean isOffset = checkChar('#');
        expectChar(isOffset ? '#' : '*');
        i = expectNonNegative(matchNNInteger());
        expectChar(':');
        while (checkKeyword("typearg")) {
          matchKeyword("typearg");
          if (checkChar('#')) {
            matchChar('#');
          }
          int type_index = expectNonNegative(matchNNInteger());
          RelativeLocation loc =
              isOffset
                  ? RelativeLocation.createOffset(i, type_index)
                  : RelativeLocation.createIndex(i, type_index);
          ATypeElement t = exp.calls.getVivify(loc);
          expectChar(':');
          parseAnnotations(t);
          parseInnerTypes(t);
        }
      }
      while (checkKeyword("reference")) {
        matchKeyword("reference");
        matched = true;
        evermatched = true;
        ATypeElement t;
        RelativeLocation loc;
        int i;
        boolean isOffset = checkChar('#');
        if (isOffset) {
          expectChar('#');
          i = expectNonNegative(matchNNInteger());
          loc = RelativeLocation.createOffset(i, 0);
        } else {
          expectChar('*');
          i = expectNonNegative(matchNNInteger());
          loc = RelativeLocation.createIndex(i, 0);
        }
        expectChar(':');
        t = exp.refs.getVivify(loc);
        parseAnnotations(t);
        parseInnerTypes(t);
        while (checkKeyword("typearg")) {
          matchKeyword("typearg");
          if (checkChar('#')) {
            matchChar('#');
          }
          int type_index = expectNonNegative(matchNNInteger());
          loc =
              isOffset
                  ? RelativeLocation.createOffset(i, type_index)
                  : RelativeLocation.createIndex(i, type_index);
          t = exp.refs.getVivify(loc);
          expectChar(':');
          parseAnnotations(t);
          parseInnerTypes(t);
        }
      }
      while (checkKeyword("lambda")) {
        matchKeyword("lambda");
        matched = true;
        evermatched = true;
        RelativeLocation loc;
        if (checkChar('#')) {
          expectChar('#');
          int offset = expectNonNegative(matchNNInteger());
          int type_index = 0;
          if (checkChar(',')) {
            expectChar(',');
            type_index = expectNonNegative(matchNNInteger());
          }
          loc = RelativeLocation.createOffset(offset, type_index);
        } else {
          expectChar('*');
          int index = expectNonNegative(matchNNInteger());
          int type_index = 0;
          if (checkChar(',')) {
            expectChar(',');
            type_index = expectNonNegative(matchNNInteger());
          }
          loc = RelativeLocation.createIndex(index, type_index);
        }
        AMethod m = exp.funs.getVivify(loc);
        expectChar(':');
        // parseAnnotations(m);
        parseLambda(m);
        // parseMethod(m);
      }
    }
    return evermatched;
  }

  private static boolean isTypeSelector(String selector) {
    return Arrays.<String>binarySearch(typeSelectors, selector, Collator.getInstance()) >= 0;
  }

  private static boolean selectsExpression(ASTPath astPath) {
    int n = astPath.size();
    if (--n >= 0) {
      ASTPath.ASTEntry entry = astPath.get(n);
      while (--n >= 0
          && entry.getTreeKind() == Kind.MEMBER_SELECT
          && entry.childSelectorIs(ASTPath.EXPRESSION)) {
        entry = astPath.get(n);
      }
      return !isTypeSelector(entry.getChildSelector());
    }
    return false;
  }

  private boolean parseASTInsertions(ADeclaration decl) throws IOException, ParseException {
    boolean matched = false;
    while (checkKeyword("insert-annotation")) {
      matched = true;
      matchKeyword("insert-annotation");
      ASTPath astPath = parseASTPath();
      expectChar(':');
      // if path doesn't indicate a type, a cast must be generated
      if (selectsExpression(astPath)) {
        ATypeElementWithType i = decl.insertTypecasts.getVivify(astPath);
        parseAnnotations(i);
        i.setType(new DeclaredType());
        parseInnerTypes(i);
      } else {
        // astPath = fixNewArrayType(astPath);  // handle special case
        // ATypeElement i = decl.insertAnnotations.getVivify(astPath);
        // parseAnnotations(i);
        // parseInnerTypes(i);
        int offset = 0;
        IPair<ASTPath, TypePath> pair = splitNewArrayType(astPath); // handle special case
        ATypeElement i;
        if (pair == null) {
          i = decl.insertAnnotations.getVivify(astPath);
        } else {
          i = decl.insertAnnotations.getVivify(pair.first);
          if (pair.second != null) {
            i = i.innerTypes.getVivify(TypePathEntry.typePathToList(pair.second));
            offset = pair.second.getLength();
          }
        }
        parseAnnotations(i);
        parseInnerTypes(i, offset);
      }
    }
    while (checkKeyword("insert-typecast")) {
      matched = true;
      matchKeyword("insert-typecast");
      ASTPath astPath = parseASTPath();
      expectChar(':');
      ATypeElementWithType i = decl.insertTypecasts.getVivify(astPath);
      parseAnnotations(i);
      Type type = parseType();
      i.setType(type);
      parseInnerTypes(i);
    }
    return matched;
  }

  /**
   * Due to the unfortunate representation of new array expressions, ASTPaths to their inner array
   * types break the usual rule that an ASTPath corresponds to an AST node. This method restores the
   * invariant by separating out the inner type information.
   *
   * @param astPath the ASTPath to process
   * @return IPair of modified ASTPath and extracted TypePath
   */
  private IPair<ASTPath, TypePath> splitNewArrayType(ASTPath astPath) {
    ASTPath outerPath = astPath;
    TypePath loc = null;
    int last = astPath.size() - 1;

    if (last > 0) {
      ASTPath.ASTEntry entry = astPath.get(last);
      if (entry.getTreeKind() == Kind.NEW_ARRAY && entry.childSelectorIs(ASTPath.TYPE)) {
        int a = entry.getArgument();
        if (a > 0) {
          outerPath =
              astPath.getParentPath().extend(new ASTPath.ASTEntry(Kind.NEW_ARRAY, ASTPath.TYPE, 0));
          loc = TypePathEntry.getTypePathFromBinary(Collections.nCopies(2 * a, 0));
        }
      }
    }
    return IPair.of(outerPath, loc);
  }

  /**
   * Parses an AST path.
   *
   * @return the AST path
   */
  private ASTPath parseASTPath() throws IOException, ParseException {
    ASTPath astPath = ASTPath.empty().extend(parseASTEntry());
    while (matchChar(',')) {
      astPath = astPath.extend(parseASTEntry());
    }
    return astPath;
  }

  /**
   * Parses and returns the next AST entry.
   *
   * @return a new AST entry
   * @throws ParseException if the next entry type is invalid
   */
  private ASTPath.ASTEntry parseASTEntry() throws IOException, ParseException {
    ASTPath.ASTEntry entry;
    if (matchKeyword("AnnotatedType")) {
      entry =
          newASTEntry(
              Kind.ANNOTATED_TYPE,
              new String[] {ASTPath.ANNOTATION, ASTPath.UNDERLYING_TYPE},
              new String[] {ASTPath.ANNOTATION});
    } else if (matchKeyword("ArrayAccess")) {
      entry = newASTEntry(Kind.ARRAY_ACCESS, new String[] {ASTPath.EXPRESSION, ASTPath.INDEX});
    } else if (matchKeyword("ArrayType")) {
      entry = newASTEntry(Kind.ARRAY_TYPE, new String[] {ASTPath.TYPE});
    } else if (matchKeyword("Assert")) {
      entry = newASTEntry(Kind.ASSERT, new String[] {ASTPath.CONDITION, ASTPath.DETAIL});
    } else if (matchKeyword("Assignment")) {
      entry = newASTEntry(Kind.ASSIGNMENT, new String[] {ASTPath.VARIABLE, ASTPath.EXPRESSION});
    } else if (matchKeyword("Binary")) {
      // Always use Kind.PLUS for Binary
      entry = newASTEntry(Kind.PLUS, new String[] {ASTPath.LEFT_OPERAND, ASTPath.RIGHT_OPERAND});
    } else if (matchKeyword("Block")) {
      entry =
          newASTEntry(
              Kind.BLOCK, new String[] {ASTPath.STATEMENT}, new String[] {ASTPath.STATEMENT});
    } else if (matchKeyword("Case")) {
      entry =
          newASTEntry(
              Kind.CASE,
              new String[] {ASTPath.EXPRESSION, ASTPath.STATEMENT},
              new String[] {ASTPath.STATEMENT});
    } else if (matchKeyword("Catch")) {
      entry = newASTEntry(Kind.CATCH, new String[] {ASTPath.PARAMETER, ASTPath.BLOCK});
    } else if (matchKeyword("Class")) {
      entry =
          newASTEntry(
              Kind.CLASS,
              new String[] {ASTPath.BOUND, ASTPath.INITIALIZER, ASTPath.TYPE_PARAMETER},
              new String[] {ASTPath.BOUND, ASTPath.INITIALIZER, ASTPath.TYPE_PARAMETER});
    } else if (matchKeyword("CompoundAssignment")) {
      // Always use Kind.PLUS_ASSIGNMENT for CompoundAssignment
      entry =
          newASTEntry(Kind.PLUS_ASSIGNMENT, new String[] {ASTPath.VARIABLE, ASTPath.EXPRESSION});
    } else if (matchKeyword("ConditionalExpression")) {
      entry =
          newASTEntry(
              Kind.CONDITIONAL_EXPRESSION,
              new String[] {ASTPath.CONDITION, ASTPath.TRUE_EXPRESSION, ASTPath.FALSE_EXPRESSION});
    } else if (matchKeyword("DoWhileLoop")) {
      entry = newASTEntry(Kind.DO_WHILE_LOOP, new String[] {ASTPath.CONDITION, ASTPath.STATEMENT});
    } else if (matchKeyword("EnhancedForLoop")) {
      entry =
          newASTEntry(
              Kind.ENHANCED_FOR_LOOP,
              new String[] {ASTPath.VARIABLE, ASTPath.EXPRESSION, ASTPath.STATEMENT});
    } else if (matchKeyword("ExpressionStatement")) {
      entry = newASTEntry(Kind.EXPRESSION_STATEMENT, new String[] {ASTPath.EXPRESSION});
    } else if (matchKeyword("ForLoop")) {
      entry =
          newASTEntry(
              Kind.FOR_LOOP,
              new String[] {
                ASTPath.INITIALIZER, ASTPath.CONDITION, ASTPath.UPDATE, ASTPath.STATEMENT
              },
              new String[] {ASTPath.INITIALIZER, ASTPath.UPDATE});
    } else if (matchKeyword("If")) {
      entry =
          newASTEntry(
              Kind.IF,
              new String[] {ASTPath.CONDITION, ASTPath.THEN_STATEMENT, ASTPath.ELSE_STATEMENT});
    } else if (matchKeyword("InstanceOf")) {
      entry = newASTEntry(Kind.INSTANCE_OF, new String[] {ASTPath.EXPRESSION, ASTPath.TYPE});
    } else if (matchKeyword("LabeledStatement")) {
      entry = newASTEntry(Kind.LABELED_STATEMENT, new String[] {ASTPath.STATEMENT});
    } else if (matchKeyword("LambdaExpression")) {
      entry =
          newASTEntry(
              Kind.LAMBDA_EXPRESSION,
              new String[] {ASTPath.PARAMETER, ASTPath.BODY},
              new String[] {ASTPath.PARAMETER});
    } else if (matchKeyword("MemberReference")) {
      entry =
          newASTEntry(
              Kind.MEMBER_REFERENCE,
              new String[] {ASTPath.QUALIFIER_EXPRESSION, ASTPath.TYPE_ARGUMENT},
              new String[] {ASTPath.TYPE_ARGUMENT});
    } else if (matchKeyword("MemberSelect")) {
      entry = newASTEntry(Kind.MEMBER_SELECT, new String[] {ASTPath.EXPRESSION});
    } else if (matchKeyword("Method")) {
      entry =
          newASTEntry(
              Kind.METHOD,
              new String[] {ASTPath.BODY, ASTPath.TYPE, ASTPath.PARAMETER, ASTPath.TYPE_PARAMETER},
              new String[] {ASTPath.PARAMETER, ASTPath.TYPE_PARAMETER});
    } else if (matchKeyword("MethodInvocation")) {
      entry =
          newASTEntry(
              Kind.METHOD_INVOCATION,
              new String[] {ASTPath.TYPE_ARGUMENT, ASTPath.METHOD_SELECT, ASTPath.ARGUMENT},
              new String[] {ASTPath.TYPE_ARGUMENT, ASTPath.ARGUMENT});
    } else if (matchKeyword("NewArray")) {
      entry =
          newASTEntry(
              Kind.NEW_ARRAY,
              new String[] {ASTPath.TYPE, ASTPath.DIMENSION, ASTPath.INITIALIZER},
              new String[] {ASTPath.TYPE, ASTPath.DIMENSION, ASTPath.INITIALIZER});
    } else if (matchKeyword("NewClass")) {
      entry =
          newASTEntry(
              Kind.NEW_CLASS,
              new String[] {
                ASTPath.ENCLOSING_EXPRESSION,
                ASTPath.TYPE_ARGUMENT,
                ASTPath.IDENTIFIER,
                ASTPath.ARGUMENT,
                ASTPath.CLASS_BODY
              },
              new String[] {ASTPath.TYPE_ARGUMENT, ASTPath.ARGUMENT});
    } else if (matchKeyword("ParameterizedType")) {
      entry =
          newASTEntry(
              Kind.PARAMETERIZED_TYPE,
              new String[] {ASTPath.TYPE, ASTPath.TYPE_ARGUMENT},
              new String[] {ASTPath.TYPE_ARGUMENT});
    } else if (matchKeyword("Parenthesized")) {
      entry = newASTEntry(Kind.PARENTHESIZED, new String[] {ASTPath.EXPRESSION});
    } else if (matchKeyword("Return")) {
      entry = newASTEntry(Kind.RETURN, new String[] {ASTPath.EXPRESSION});
    } else if (matchKeyword("Switch")) {
      entry =
          newASTEntry(
              Kind.SWITCH,
              new String[] {ASTPath.EXPRESSION, ASTPath.CASE},
              new String[] {ASTPath.CASE});
    } else if (matchKeyword("Synchronized")) {
      entry = newASTEntry(Kind.SYNCHRONIZED, new String[] {ASTPath.EXPRESSION, ASTPath.BLOCK});
    } else if (matchKeyword("Throw")) {
      entry = newASTEntry(Kind.THROW, new String[] {ASTPath.EXPRESSION});
    } else if (matchKeyword("Try")) {
      entry =
          newASTEntry(
              Kind.TRY,
              new String[] {ASTPath.BLOCK, ASTPath.CATCH, ASTPath.FINALLY_BLOCK},
              new String[] {ASTPath.CATCH});
    } else if (matchKeyword("TypeCast")) {
      entry = newASTEntry(Kind.TYPE_CAST, new String[] {ASTPath.TYPE, ASTPath.EXPRESSION});
    } else if (matchKeyword("TypeParameter")) {
      entry =
          newASTEntry(
              Kind.TYPE_PARAMETER, new String[] {ASTPath.BOUND}, new String[] {ASTPath.BOUND});
    } else if (matchKeyword("Unary")) {
      // Always use Kind.UNARY_PLUS for Unary
      entry = newASTEntry(Kind.UNARY_PLUS, new String[] {ASTPath.EXPRESSION});
    } else if (matchKeyword("UnionType")) {
      entry =
          newASTEntry(
              Kind.UNION_TYPE,
              new String[] {ASTPath.TYPE_ALTERNATIVE},
              new String[] {ASTPath.TYPE_ALTERNATIVE});
    } else if (matchKeyword("Variable")) {
      entry = newASTEntry(Kind.VARIABLE, new String[] {ASTPath.TYPE, ASTPath.INITIALIZER});
    } else if (matchKeyword("WhileLoop")) {
      entry = newASTEntry(Kind.WHILE_LOOP, new String[] {ASTPath.CONDITION, ASTPath.STATEMENT});
    } else if (matchKeyword("Wildcard")) {
      // Always use Kind.UNBOUNDED_WILDCARD for Wildcard
      entry = newASTEntry(Kind.UNBOUNDED_WILDCARD, new String[] {ASTPath.BOUND});
    } else {
      throw new ParseException("Invalid AST path type: " + st.sval);
    }
    return entry;
  }

  /**
   * Parses and constructs a new AST entry, where none of the child selections require arguments.
   * For example, the call:
   *
   * <pre>{@code
   * newASTEntry(Kind.WHILE_LOOP, new String[] {"condition", "statement"});
   * }</pre>
   *
   * constructs a while loop AST entry, where the valid child selectors are "condition" or
   * "statement".
   *
   * @param kind the kind of this AST entry
   * @param legalChildSelectors a list of the legal child selectors for this AST entry
   * @return a new {@link ASTPath.ASTEntry}
   * @throws ParseException if an illegal argument is found
   */
  private ASTPath.ASTEntry newASTEntry(Kind kind, String[] legalChildSelectors)
      throws IOException, ParseException {
    return newASTEntry(kind, legalChildSelectors, null);
  }

  /**
   * Parses and constructs a new AST entry. For example, the call:
   *
   * <pre>{@code
   * newASTEntry(Kind.CASE, new String[] {"expression", "statement"}, new String[] {"statement"});
   * }</pre>
   *
   * constructs a case AST entry, where the valid child selectors are "expression" or "statement"
   * and the "statement" child selector requires an argument.
   *
   * @param kind the kind of this AST entry
   * @param legalChildSelectors a list of the legal child selectors for this AST entry
   * @param argumentChildSelectors a list of the child selectors that also require an argument.
   *     Entries here should also be in the legalChildSelectors list.
   * @return a new {@link ASTPath.ASTEntry}
   * @throws ParseException if an illegal argument is found
   */
  private ASTPath.ASTEntry newASTEntry(
      Kind kind, String[] legalChildSelectors, String[] argumentChildSelectors)
      throws IOException, ParseException {
    expectChar('.');
    for (String arg : legalChildSelectors) {
      if (matchKeyword(arg)) {
        if (argumentChildSelectors != null
            && ArraysPlume.indexOf(argumentChildSelectors, arg) >= 0) {
          int index = matchNNInteger();
          return new ASTPath.ASTEntry(kind, arg, index);
        } else {
          return new ASTPath.ASTEntry(kind, arg);
        }
      }
    }
    throw new ParseException(
        "Invalid argument for "
            + kind
            + " (legal arguments - "
            + Arrays.toString(legalChildSelectors)
            + "): "
            + st.sval);
  }

  /** Parses the next tokens as a Java type. */
  private Type parseType() throws IOException, ParseException {
    DeclaredType declaredType;
    if (matchChar('?')) {
      declaredType = new DeclaredType(DeclaredType.WILDCARD);
    } else {
      declaredType = parseDeclaredType();
    }
    if (checkKeyword("extends") || checkKeyword("super")) {
      return parseBoundedType(declaredType);
    } else {
      Type type = declaredType;
      while (matchChar('[')) {
        expectChar(']');
        type = new ArrayType(type);
      }
      return type;
    }
  }

  /** Parses the next tokens as a declared type. */
  private DeclaredType parseDeclaredType() throws IOException, ParseException {
    String type = matchIdentifier();
    if (type == null) {
      type = matchPrimitiveType();
      if (type == null) {
        throw new ParseException("Expected identifier or primitive type");
      }
    }
    return parseDeclaredType(type);
  }

  /**
   * Parses the next tokens as a declared type.
   *
   * @param name the name of the initial identifier
   */
  private DeclaredType parseDeclaredType(String name) throws IOException, ParseException {
    DeclaredType type = new DeclaredType(name);
    if (matchChar('<')) {
      type.addTypeParameter(parseType());
      while (matchChar(',')) {
        type.addTypeParameter(parseType());
      }
      expectChar('>');
    }
    if (matchChar('.')) {
      type.setInnerType(parseDeclaredType());
    }
    return type;
  }

  /**
   * Parses the next tokens as a bounded type.
   *
   * @param type the name, which precedes "extends" or "super"
   */
  private BoundedType parseBoundedType(DeclaredType type) throws IOException, ParseException {
    BoundKind kind;
    if (matchKeyword("extends")) {
      kind = BoundKind.EXTENDS;
    } else if (matchKeyword("super")) {
      kind = BoundKind.SUPER;
    } else {
      throw new ParseException("Illegal bound kind: " + st.sval);
    }
    return new BoundedType(type, kind, parseDeclaredType());
  }

  private void parseClass() throws IOException, ParseException {
    expectKeyword("class");
    String basename = expectIdentifier();
    String fullName = curPkgPrefix + basename;

    AClass c = scene.classes.getVivify(fullName);
    expectChar(':');

    parseAnnotations(c);
    parseBounds(c.bounds);

    while (checkKeyword("extends")) {
      parseExtends(c);
    }
    while (checkKeyword("implements")) {
      parseImplements(c);
    }
    parseASTInsertions(c);

    while (checkKeyword("field")) {
      parseField(c);
    }
    while (checkKeyword("staticinit")) {
      parseStaticInit(c);
    }
    while (checkKeyword("instanceinit")) {
      parseInstanceInit(c);
    }
    while (checkKeyword("method")) {
      parseMethod(c);
    }
    c.methods.prune();
  }

  // Reads the index file in this.st and puts the information in this.scene.
  private void parse() throws ParseException, IOException {
    st.nextToken();

    while (st.ttype != TT_EOF) {
      expectKeyword("package");

      String pkg;
      if (checkIdentifier() == null) {
        pkg = null;
        // the default package cannot be annotated
        matchChar(':');
      } else {
        pkg = expectQualifiedName();
        expectChar(':');
        AClass p = scene.classes.getVivify(pkg + ".package-info");
        parseAnnotations(p);
      }

      if (pkg != null) {
        curPkgPrefix = pkg + ".";
      } else {
        curPkgPrefix = "";
      }

      for (; ; ) {
        if (checkKeyword("annotation")) {
          parseAnnotationDef();
        } else if (checkKeyword("class")) {
          parseClass();
        } else if (checkKeyword("package") || st.ttype == TT_EOF) {
          break;
        } else {
          throw new ParseException(
              "Expected: `annotation', `class', or `package'. Found: `"
                  + st.sval
                  + "', ttype:"
                  + st.ttype);
        }
      }
    }

    /*
            for (Map.Entry<String, AnnotationDef> entry : defs.entrySet()) {
                final String annotationType = entry.getKey();
                AnnotationDef def = entry.getValue();
                for (AnnotationFieldType aft : def.fieldTypes.values()) {
                    aft.accept(new AFTVisitor<Void, Void>() {
                        @Override
                        public Void visitAnnotationAFT(AnnotationAFT aft,
                                Void arg) {
                            for (AnnotationFieldType t : aft.annotationDef.fieldTypes.values()) {
                                t.accept(this, arg);
                            }
                            return null;
                        }

                        @Override
                        public Void visitArrayAFT(ArrayAFT aft, Void arg) {
                          return aft.elementType == null ? null
                              : aft.elementType.accept(this, arg);
                        }

                        @Override
                        public Void visitBasicAFT(BasicAFT aft, Void arg) {
                          return null;
                        }

                        @Override
                        public Void visitClassTokenAFT(ClassTokenAFT aft, Void arg) {
                          return null;
                        }

                        @Override
                        public Void visitEnumAFT(EnumAFT aft, Void arg) {
                            importSet(annotationType, aft).add(aft.typeName);
                            return null;
                        }

                        private Set<String> importSet(final String annotationType,
                            AnnotationFieldType aft) {
                          Set<String> imps = scene.imports.get(annotationType);
                          if (imps == null) {
                              imps = new TreeSet<String>();
                              scene.imports.put(annotationType, imps);
                          }
                          return imps;
                        }
                    }, null);
                }
            }
    */
  }

  private IndexFileParser(Reader in, String source, AScene scene) {
    this.source = source;
    defs = new LinkedHashMap<>();
    for (AnnotationDef ad : Annotations.standardDefs) {
      try {
        addDef(ad);
      } catch (ParseException e) {
        throw new Error(e);
      }
    }

    st = new StreamTokenizer(in);
    st.slashSlashComments(true);

    // restrict numbers -- don't really need, could interfere with
    // annotation values
    // st.ordinaryChar('-');
    // HMMM this fixes fully-qualified-name strangeness but breaks
    // floating-point numbers
    st.ordinaryChar('.');

    // argggh!!! stupid default needs to be overridden! see java bug 4217680
    st.ordinaryChar('/');

    // for "type-argument"
    st.wordChars('-', '-');

    // java identifiers can contain numbers, _, and $
    st.wordChars('0', '9');
    st.wordChars('_', '_');
    st.wordChars('$', '$');

    this.scene = scene;

    // See if the nonnull analysis picks up on this:
    // curPkgPrefix == ""; // will get changed later anyway
  }

  /**
   * Reads annotations from <code>in</code> in index file format and merges them into <code>scene
   * </code>. Annotations from the input are merged into the scene; it is an error if both the scene
   * and the input contain annotations of the same type on the same element.
   *
   * <p>Since each annotation in a scene carries its own definition and the scene as a whole no
   * longer has a set of definitions, annotation definitions that are given in the input but never
   * used are not saved anywhere and will not be included if the scene is written back to an index
   * file. Similarly, retention policies on definitions of annotations that are never used at the
   * top level are dropped.
   *
   * <p>Caveat: Parsing of floating point numbers currently does not work.
   */
  public static Map<String, AnnotationDef> parse(LineNumberReader in, String filename, AScene scene)
      throws IOException, ParseException {
    IndexFileParser parser = new IndexFileParser(in, filename, scene);
    // no filename is available in the exception messages
    return parseAndReturnAnnotationDefs(null, in, parser);
  }

  /**
   * Reads annotations from the index file <code>filename</code> and merges them into <code>scene
   * </code>; see {@link #parse(LineNumberReader, String, AScene)}.
   */
  public static Map<String, AnnotationDef> parseFile(String filename, AScene scene)
      throws IOException {
    try (LineNumberReader in =
        new LineNumberReader(Files.newBufferedReader(Paths.get(filename), UTF_8))) {
      IndexFileParser parser = new IndexFileParser(in, filename, scene);
      return parseAndReturnAnnotationDefs(filename, in, parser);
    }
  }

  /**
   * Reads annotations from the string (in index file format) and merges them into <code>scene
   * </code>; see {@link #parse(LineNumberReader, String, AScene)}. Primarily for testing.
   */
  public static Map<String, AnnotationDef> parseString(
      String fileContents, String source, AScene scene) throws IOException {
    String filename =
        "While parsing string from "
            + source
            + ": \n----------------BEGIN----------------\n"
            + fileContents
            + "----------------END----------------\n";
    LineNumberReader in = new LineNumberReader(new StringReader(fileContents));
    IndexFileParser parser = new IndexFileParser(in, filename, scene);
    return parseAndReturnAnnotationDefs(filename, in, parser);
  }

  private static Map<String, AnnotationDef> parseAndReturnAnnotationDefs(
      String filename, LineNumberReader in, IndexFileParser parser) throws IOException {
    try {
      parser.parse();
      return Collections.unmodifiableMap(parser.defs);
    } catch (IOException e) {
      throw filename == null ? new FileIOException(in, e) : new FileIOException(in, filename, e);
    } catch (ParseException e) {
      throw filename == null ? new FileIOException(in, e) : new FileIOException(in, filename, e);
    }
  }

  /**
   * Parse the given text into a {@link Type}.
   *
   * @param text the text to parse
   * @return the type
   */
  public static Type parseType(String text, String filename) {
    StringReader in = new StringReader(text);
    IndexFileParser parser = new IndexFileParser(in, filename, null);
    try {
      parser.st.nextToken();
      return parser.parseType();
    } catch (Exception e) {
      throw new RuntimeException("Error parsing type from: '" + text + "'", e);
    }
  }
}
