package org.checkerframework.framework.stub;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.afu.scenelib.el.AClass;
import org.checkerframework.afu.scenelib.el.AElement;
import org.checkerframework.afu.scenelib.el.AField;
import org.checkerframework.afu.scenelib.el.AMethod;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.afu.scenelib.el.ATypeElement;
import org.checkerframework.afu.scenelib.el.TypePathEntry;
import org.checkerframework.framework.util.StaticJavaParserUtil;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link ToIndexFileConverter}. */
public class ToIndexFileConverterTest {

  // The asm library is on this test's runtime classpath but not on its compile classpath, so the
  // following two constants are written literally rather than as references to fields of
  // org.objectweb.asm.TypePath.

  /**
   * The {@code step} value of a {@link TypePathEntry} that steps from a parameterized type to one
   * of its type arguments; that is, {@code org.objectweb.asm.TypePath.TYPE_ARGUMENT}.
   */
  private static final int TYPE_ARGUMENT = 3;

  /**
   * The {@code step} value of a {@link TypePathEntry} that steps from a wildcard to its bound; that
   * is, {@code org.objectweb.asm.TypePath.WILDCARD_BOUND}.
   */
  private static final int WILDCARD_BOUND = 2;

  /** The type path entry for the first type argument of a type. */
  private static final TypePathEntry firstTypeArgument = TypePathEntry.create(TYPE_ARGUMENT, 0);

  /** The type path entry for the bound of a wildcard. */
  private static final TypePathEntry wildcardBound = TypePathEntry.create(WILDCARD_BOUND, 0);

  /** The type path entry for the component type of an array type. */
  private static final TypePathEntry arrayElement = TypePathEntry.ARRAY_ELEMENT;

  /** The type path of the type argument of a type: {@code List<HERE>}. */
  private static final List<TypePathEntry> outerTypeArgument = Arrays.asList(firstTypeArgument);

  /** The type path of the type argument of a type argument: {@code List<List<HERE>>}. */
  private static final List<TypePathEntry> innerTypeArgument =
      Arrays.asList(firstTypeArgument, firstTypeArgument);

  /** The converter used by the {@code getJVML} tests. */
  private final ToIndexFileConverter converter =
      new ToIndexFileConverter(null, Collections.emptyList(), new AScene());

  /**
   * Tests that an annotation on a nested type argument is recorded even when no annotation appears
   * on the type argument that encloses it.
   */
  @Test
  public void testNestedTypeArgumentWithoutEnclosingAnnotation() {
    AField field =
        fieldOfStub(
            "package p;",
            "import java.util.List;",
            "class C {",
            "  List<List<@Nullable String>> f;",
            "}");
    ATypeElement inner = field.type.innerTypes.get(innerTypeArgument);
    Assert.assertNotNull("no entry for the nested type argument", inner);
    Assert.assertNotNull(
        "@Nullable was not recorded on the nested type argument", inner.lookup("Nullable"));
  }

  /** Tests that annotations at several levels of nesting are each recorded at their own level. */
  @Test
  public void testAnnotationsAtEveryLevel() {
    AField field =
        fieldOfStub(
            "package p;",
            "import java.util.List;",
            "class C {",
            "  List<@A List<@B String>> f;",
            "}");
    ATypeElement outer = field.type.innerTypes.get(outerTypeArgument);
    Assert.assertNotNull("no entry for the outer type argument", outer);
    Assert.assertNotNull("@A was not recorded on the outer type argument", outer.lookup("A"));
    Assert.assertNull("@B was recorded on the outer type argument", outer.lookup("B"));
    ATypeElement inner = field.type.innerTypes.get(innerTypeArgument);
    Assert.assertNotNull("no entry for the nested type argument", inner);
    Assert.assertNotNull("@B was not recorded on the nested type argument", inner.lookup("B"));
    Assert.assertNull("@A was recorded on the nested type argument", inner.lookup("A"));
  }

  /** Tests that an annotation on the bound of a wildcard type argument is recorded. */
  @Test
  public void testWildcardBound() {
    AField field =
        fieldOfStub(
            "package p;",
            "import java.util.List;",
            "class C {",
            "  List<? extends @A String> f;",
            "}");
    Assert.assertEquals("wrong type paths", Arrays.asList(bound()), typePaths(field));
    ATypeElement inner = field.type.innerTypes.get(bound());
    Assert.assertNotNull("@A was not recorded on the wildcard bound", inner.lookup("A"));
  }

  /**
   * Tests that the annotations of an array type that is a type argument are recorded, each at the
   * array level to which it applies.
   */
  @Test
  public void testArrayTypeArgument() {
    AField field =
        fieldOfStub(
            "package p;", "import java.util.List;", "class C {", "  List<@A String @B []> f;", "}");
    Assert.assertEquals(
        "wrong type paths",
        Arrays.asList(outerTypeArgument, element(outerTypeArgument)),
        typePaths(field));
    // `@B` applies to `String[]`, the type argument itself.
    ATypeElement arrayType = field.type.innerTypes.get(outerTypeArgument);
    Assert.assertNotNull("@B was not recorded on the array type", arrayType.lookup("B"));
    Assert.assertNull("@A was recorded on the array type", arrayType.lookup("A"));
    // `@A` applies to `String`, the component type.
    ATypeElement componentType = field.type.innerTypes.get(element(outerTypeArgument));
    Assert.assertNotNull("@A was not recorded on the component type", componentType.lookup("A"));
    Assert.assertNull("@B was recorded on the component type", componentType.lookup("B"));
  }

  /**
   * Tests that an annotation on an inner array level of a type argument is recorded at that level
   * rather than at a deeper one.
   */
  @Test
  public void testNestedArrayTypeArgument() {
    AField field =
        fieldOfStub(
            "package p;", "import java.util.List;", "class C {", "  List<String[] @A []> f;", "}");
    // `@A` applies to `String[]`, which is the component type of the type argument `String[][]`.
    Assert.assertEquals(
        "wrong type paths", Arrays.asList(element(outerTypeArgument)), typePaths(field));
    ATypeElement componentType = field.type.innerTypes.get(element(outerTypeArgument));
    Assert.assertNotNull("@A was not recorded on the component type", componentType.lookup("A"));
  }

  /**
   * Tests that the annotations of a field whose type is an array type are recorded, each at the
   * array level to which it applies.
   */
  @Test
  public void testArrayFieldType() {
    AField field = fieldOfStub("package p;", "class C {", "  String @A [] @B [] f;", "}");
    // `@A` applies to `String[][]`, the type of the field itself.
    Assert.assertNotNull("@A was not recorded on the field's type", field.type.lookup("A"));
    Assert.assertNull("@B was recorded on the field's type", field.type.lookup("B"));
    // `@B` applies to `String[]`, the component type.
    Assert.assertEquals(
        "wrong type paths", Arrays.asList(element(Collections.emptyList())), typePaths(field));
    ATypeElement componentType = field.type.innerTypes.get(element(Collections.emptyList()));
    Assert.assertNotNull("@B was not recorded on the component type", componentType.lookup("B"));
    Assert.assertNull("@A was recorded on the component type", componentType.lookup("A"));
  }

  /**
   * Tests that no entry is created for a nested type that bears no annotation. Such an entry would
   * be written to the JAIF as a content-free {@code inner-type} line.
   */
  @Test
  public void testNoEntryForUnannotatedNestedType() {
    AField field =
        fieldOfStub(
            "package p;",
            "import java.util.List;",
            "import java.util.Map;",
            "class C {",
            "  List<Map<@A String, Integer>> f;",
            "}");
    // There is no entry for the unannotated `Map<...>` or `Integer`, only for `@A String`.
    Assert.assertEquals("wrong type paths", Arrays.asList(innerTypeArgument), typePaths(field));
    ATypeElement inner = field.type.innerTypes.get(innerTypeArgument);
    Assert.assertNotNull("@A was not recorded on the nested type argument", inner.lookup("A"));
  }

  /**
   * Tests that an annotation that {@code extractAnnotation} discards is not recorded as null, and
   * that an annotation next to it is still recorded.
   */
  @Test
  public void testDiscardedAnnotation() {
    // `extractAnnotation` discards an annotation whose text contains `+`.
    AClass clazz =
        classOfStub(
            "package p;",
            "@A(\"x+y\") @B class C {",
            "  @A(\"x+y\") @B int f;",
            "  @A(\"x+y\") @B C() {}",
            "  @A(\"x+y\") @B void m(@A(\"x+y\") @B C this) {}",
            "}");
    assertOnlyB("the class", clazz);
    assertOnlyB("field f", clazz.fields.get("f"));
    Assert.assertEquals("wrong methods", 2, clazz.methods.size());
    for (AMethod method : clazz.methods.values()) {
      assertOnlyB(method.methodSignature, method);
      Assert.assertFalse(
          "null was recorded on the receiver of " + method.methodSignature,
          method.receiver.tlAnnotationsHere.contains(null));
    }
    assertOnlyB("the receiver type of m", clazz.methods.get("m()V").receiver.type);
  }

  /**
   * Asserts that {@code @B} is recorded on {@code elem}, and that null is not.
   *
   * @param description a description of {@code elem}, for failure messages
   * @param elem a scene element
   */
  private static void assertOnlyB(String description, AElement elem) {
    Assert.assertNotNull("no scene element for " + description, elem);
    Assert.assertFalse(
        "null was recorded on " + description, elem.tlAnnotationsHere.contains(null));
    Assert.assertNotNull("@B was not recorded on " + description, elem.lookup("B"));
  }

  /**
   * Tests that the annotations of a receiver parameter, including those on its type arguments, are
   * recorded on the receiver, and that a constructor's declaration annotations are not.
   */
  @Test
  public void testReceiverParameter() {
    AScene scene =
        sceneOfStub(
            "package p;",
            "class C<T> {",
            "  @A C() {}",
            "  void m(@B C<@D T> this) {}",
            "  @G class Inner {",
            "    Inner(@E C<@F T> C.this, int i) {}",
            "  }",
            "}");
    AClass clazz = scene.classes.get("p.C");
    AMethod constructor = clazz.methods.get("<init>()V");
    Assert.assertNotNull("@A was not recorded on the constructor", constructor.lookup("A"));
    Assert.assertTrue(
        "an annotation was recorded on the constructor's receiver",
        constructor.receiver.tlAnnotationsHere.isEmpty());
    Assert.assertTrue(
        "an annotation was recorded on the constructor's receiver type",
        constructor.receiver.type.tlAnnotationsHere.isEmpty());

    ATypeElement methodRcvr = clazz.methods.get("m()V").receiver.type;
    Assert.assertNotNull("@B was not recorded on the receiver type of m", methodRcvr.lookup("B"));
    Assert.assertEquals(
        "wrong type paths for the receiver of m",
        Arrays.asList(outerTypeArgument),
        new ArrayList<>(methodRcvr.innerTypes.keySet()));
    Assert.assertNotNull(
        "@D was not recorded on the receiver's type argument",
        methodRcvr.innerTypes.get(outerTypeArgument).lookup("D"));

    AClass innerClass = scene.classes.get("p.C$Inner");
    Assert.assertNotNull("no scene element for class p.C$Inner", innerClass);
    Assert.assertNotNull("@G was not recorded on class Inner", innerClass.lookup("G"));
    Assert.assertNull("@G was recorded on class C", clazz.lookup("G"));
    AMethod innerConstructor = innerClass.methods.get("<init>(I)V");
    Assert.assertNotNull("no scene element for the constructor of Inner", innerConstructor);
    Assert.assertFalse(
        "the constructor of Inner was recorded on p.C", clazz.methods.containsKey("<init>(I)V"));
    ATypeElement innerRcvr = innerConstructor.receiver.type;
    Assert.assertNotNull(
        "@E was not recorded on the receiver type of Inner's constructor", innerRcvr.lookup("E"));
    Assert.assertNotNull(
        "@F was not recorded on the type argument of the receiver of Inner's constructor",
        innerRcvr.innerTypes.get(outerTypeArgument).lookup("F"));
  }

  /** Tests {@code getJVML} on wildcard types. */
  @Test
  public void testGetJVMLWildcard() {
    // The erasure of a wildcard is the erasure of its upper bound.
    assertJVML("Ljava/lang/Object;", parseWildcard("?"));
    assertJVML("Ljava/lang/Object;", parseWildcard("? super Number"));
    assertJVML("Ljava/lang/Number;", parseWildcard("? extends Number"));
  }

  /** Tests {@code getJVML} on types other than wildcards. */
  @Test
  public void testGetJVMLNonWildcard() {
    assertJVML("I", StaticJavaParser.parseType("int"));
    assertJVML("V", StaticJavaParser.parseType("void"));
    assertJVML("[[Ljava/lang/String;", StaticJavaParser.parseType("String[][]"));
  }

  /**
   * Parses a wildcard type. JavaParser cannot parse a wildcard on its own, so this parses a type
   * that has the wildcard as its sole type argument.
   *
   * @param wildcard a wildcard, in Java syntax
   * @return the AST node for {@code wildcard}
   */
  private static Type parseWildcard(String wildcard) {
    ClassOrInterfaceType type =
        (ClassOrInterfaceType) StaticJavaParser.parseType("java.util.List<" + wildcard + ">");
    return type.getTypeArguments().get().get(0);
  }

  /**
   * Asserts that {@code type} has the given JVML representation.
   *
   * @param expected the expected JVML representation
   * @param type a type
   */
  private void assertJVML(String expected, Type type) {
    Assert.assertEquals(type.asString(), expected, converter.getJVML(type));
  }

  /**
   * Returns the type path of the bound of the first type argument: {@code List<? extends HERE>}.
   *
   * @return the type path of the bound of the first type argument
   */
  private static List<TypePathEntry> bound() {
    return Arrays.asList(firstTypeArgument, wildcardBound);
  }

  /**
   * Returns the given type path, extended by one step from an array type to its component type.
   *
   * @param loc a type path that denotes an array type
   * @return the type path of the component type of the array type that {@code loc} denotes
   */
  private static List<TypePathEntry> element(List<TypePathEntry> loc) {
    List<TypePathEntry> result = new ArrayList<>(loc);
    result.add(arrayElement);
    return result;
  }

  /**
   * Returns the type paths at which the given field's type has annotations, in the order that the
   * JAIF writer would print them.
   *
   * @param field a scene element for a field
   * @return the type paths of the field's type's inner types
   */
  private static List<List<TypePathEntry>> typePaths(AField field) {
    return new ArrayList<>(field.type.innerTypes.keySet());
  }

  /**
   * Converts a stub file that declares a single class {@code p.C} with a single field {@code f},
   * and returns the field.
   *
   * @param lines the lines of the stub file
   * @return the scene element for field {@code f} of class {@code p.C}
   */
  private static AField fieldOfStub(String... lines) {
    AField field = classOfStub(lines).fields.get("f");
    Assert.assertNotNull("no scene element for field f", field);
    return field;
  }

  /**
   * Converts a stub file that declares a single class {@code p.C}, and returns the class.
   *
   * @param lines the lines of the stub file
   * @return the scene element for class {@code p.C}
   */
  private static AClass classOfStub(String... lines) {
    AScene scene = sceneOfStub(lines);
    AClass clazz = scene.classes.get("p.C");
    Assert.assertNotNull("no scene element for class p.C", clazz);
    return clazz;
  }

  /**
   * Converts a stub file whose first type declaration is class {@code p.C}, and returns the scene.
   *
   * @param lines the lines of the stub file
   * @return the scene that the converter populated from the stub file
   */
  private static AScene sceneOfStub(String... lines) {
    String stubContents = String.join(System.lineSeparator(), lines);
    StubUnit stubUnit =
        StaticJavaParserUtil.parseStubUnit(
            new ByteArrayInputStream(stubContents.getBytes(StandardCharsets.UTF_8)));
    CompilationUnit cu = stubUnit.getCompilationUnits().get(0);
    AScene scene = new AScene();
    ToIndexFileConverter converter =
        new ToIndexFileConverter(cu.getPackageDeclaration().get(), cu.getImports(), scene);
    TypeDeclaration<?> typeDecl = cu.getType(0);
    AClass clazz = scene.classes.getVivify("p." + typeDecl.getNameAsString());
    typeDecl.accept(converter, clazz);
    return scene;
  }
}
