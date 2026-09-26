package org.checkerframework.framework.stub;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.StubUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.afu.scenelib.el.AClass;
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
   * Converts a stub file to a JAIF.
   *
   * @param stubFileLines the lines of the stub file
   * @return the JAIF that {@link ToIndexFileConverter} produces for the stub file
   */
  private static String convert(String... stubFileLines) throws Exception {
    String stubFile = String.join(System.lineSeparator(), stubFileLines);
    ByteArrayInputStream in = new ByteArrayInputStream(stubFile.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ToIndexFileConverter.convert(new AScene(), in, out);
    return out.toString(StandardCharsets.UTF_8.name());
  }

  /**
   * Asserts that {@code jaif} declares {@code method} in {@code className}.
   *
   * @param jaif a JAIF
   * @param className the name of a class, without its package; a {@code $} separates a nested class
   *     from its enclosing class
   * @param method the JVML representation of a method, such as {@code "myMethod(I)V"}
   */
  private static void assertMethod(String jaif, String className, String method) {
    String currentClass = null;
    for (String line : jaif.split("\\R")) {
      if (line.startsWith("class ") && line.endsWith(":")) {
        currentClass = line.substring("class ".length(), line.length() - 1);
      } else if (line.strip().equals("method " + method + ":") && className.equals(currentClass)) {
        return;
      }
    }
    Assert.fail("no method " + method + " in class " + className + System.lineSeparator() + jaif);
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
    AField field = clazz.fields.get("f");
    Assert.assertNotNull("no scene element for field f", field);
    return field;
  }

  /** A method's JVML descriptor uses the erasure of each type variable. */
  @Test
  public void testTypeVariableErasure() throws Exception {
    String jaif =
        convert(
            "package p;",
            "class MyClass<S extends CharSequence> {",
            "  <T extends Number, U> void myMethod(T t, U u, S s, Object o) {}",
            "}");
    assertMethod(
        jaif,
        "MyClass",
        "myMethod(Ljava/lang/Number;Ljava/lang/Object;Ljava/lang/CharSequence;Ljava/lang/Object;)V");
  }

  /** A method's JVML descriptor uses the fully qualified name of a class on the classpath. */
  @Test
  public void testResolveClassOnClasspath() throws Exception {
    String jaif =
        convert(
            "package p;",
            "import org.checkerframework.framework.stub.ToIndexFileConverter;",
            "class MyClass {",
            "  void myMethod(ToIndexFileConverter c) {}",
            "}");
    assertMethod(
        jaif, "MyClass", "myMethod(Lorg/checkerframework/framework/stub/ToIndexFileConverter;)V");
  }

  /** A single-type import shadows a class of the same name in the stub file's own package. */
  @Test
  public void testSingleTypeImportShadowsOwnPackage() throws Exception {
    // Both org.checkerframework.framework.util.PurityChecker and
    // org.checkerframework.dataflow.util.PurityChecker are on the classpath.
    String jaif =
        convert(
            "package org.checkerframework.framework.util;",
            "import org.checkerframework.dataflow.util.PurityChecker;",
            "class MyClass {",
            "  void myMethod(PurityChecker c) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lorg/checkerframework/dataflow/util/PurityChecker;)V");
  }

  /** A varargs parameter's JVML descriptor is an array type. */
  @Test
  public void testVarargs() throws Exception {
    String jaif =
        convert(
            "package p;",
            "class MyClass<S extends CharSequence> {",
            "  MyClass(int i, String... ss) {}",
            "  <T extends Number> void myMethod(T... ts) {}",
            "  void myOtherMethod(S[]... ss) {}",
            "}");
    assertMethod(jaif, "MyClass", "<init>(I[Ljava/lang/String;)V");
    assertMethod(jaif, "MyClass", "myMethod([Ljava/lang/Number;)V");
    assertMethod(jaif, "MyClass", "myOtherMethod([[Ljava/lang/CharSequence;)V");
  }

  /** A method's JVML descriptor uses a fully qualified name that appears in the stub file. */
  @Test
  public void testFullyQualifiedName() throws Exception {
    String jaif =
        convert("package p;", "class MyClass {", "  void myMethod(java.util.List<?> l) {}", "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/List;)V");
  }

  /** A method's JVML descriptor uses the binary name of a nested class. */
  @Test
  public void testNestedClass() throws Exception {
    String jaif =
        convert(
            "package p;",
            "import java.util.Map;",
            "class MyClass {",
            "  void myMethod(java.util.Map.Entry<?, ?> e1, Map.Entry<?, ?> e2) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/Map$Entry;Ljava/util/Map$Entry;)V");
  }

  /** An unresolvable unqualified name is assumed to be in the stub file's own package. */
  @Test
  public void testUnresolvedTypeInOwnPackage() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MyClass {",
            "  void myMethod(MyOtherClass c) {}",
            "}",
            "class MyOtherClass {",
            "  void myOtherMethod(MyClass c) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lmypackage/MyOtherClass;)V");
    assertMethod(jaif, "MyOtherClass", "myOtherMethod(Lmypackage/MyClass;)V");
  }

  /**
   * In an unresolvable name, an identifier that starts with an uppercase letter is assumed to be a
   * class name and one that starts with a lowercase letter is assumed to be a package name.
   */
  @Test
  public void testUnresolvedQualifiedType() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MyClass {",
            "  void myMethod(MyOtherClass.MyNestedClass c, other.pkg.MyOtherClass o) {}",
            "}");
    assertMethod(
        jaif,
        "MyClass",
        "myMethod(Lmypackage/MyOtherClass$MyNestedClass;Lother/pkg/MyOtherClass;)V");
  }

  /** A single-type import qualifies an unresolvable name, including a nested one. */
  @Test
  public void testUnresolvedImportedType() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "import other.pkg.MyOtherClass;",
            "class MyClass {",
            "  void myMethod(MyOtherClass c, MyOtherClass.MyNestedClass n) {}",
            "}");
    assertMethod(
        jaif,
        "MyClass",
        "myMethod(Lother/pkg/MyOtherClass;Lother/pkg/MyOtherClass$MyNestedClass;)V");
  }

  /** A member type is inherited through a superclass that the stub file names by package. */
  @Test
  public void testInheritedThroughPackageQualifiedSuperclass() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class Parent {",
            "  class Nested {}",
            "}",
            "class Child extends mypackage.Parent {",
            "  void myMethod(Nested n) {}",
            "}");
    assertMethod(jaif, "Child", "myMethod(Lmypackage/Parent$Nested;)V");
  }

  /**
   * The first identifier of a qualified name is not a subpackage of the stub file's package: in
   * package {@code java}, the name {@code util.List} does not refer to {@code java.util.List}.
   */
  @Test
  public void testQualifiedNameIsNotRelativeToPackage() throws Exception {
    String jaif =
        convert("package java;", "class MyClass {", "  void myMethod(util.List l) {}", "}");
    assertMethod(jaif, "MyClass", "myMethod(Lutil/List;)V");
  }

  /**
   * A single-type import determines the type that a name refers to, even if the imported type is
   * not on the classpath and a type of the same name is in {@code java.lang}.
   */
  @Test
  public void testUnloadableSingleTypeImportShadowsJavaLang() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "import other.pkg.Module;",
            "class MyClass {",
            "  void myMethod(Module m) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lother/pkg/Module;)V");
  }

  /** A package-private member type of a class in another package is not inherited. */
  @Test
  public void testPackagePrivateMemberTypeIsNotInherited() throws Exception {
    // java.util.TreeMap declares a package-private member type named Entry.
    String jaif =
        convert(
            "package mypackage;",
            "class MyClass extends java.util.TreeMap {",
            "  void myMethod(Entry e) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/Map$Entry;)V");
  }

  /**
   * A member type that a class inherits from a class on the classpath shadows a type that the stub
   * file declares in an enclosing scope.
   */
  @Test
  public void testInheritedMemberTypeShadowsTopLevelStubType() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class Entry {}",
            "abstract class MyClass extends java.util.AbstractMap {",
            "  void myMethod(Entry e) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/Map$Entry;)V");
  }

  /** A class's own member types are not in scope in its {@code extends} clause. */
  @Test
  public void testSupertypeIsResolvedOutsideClassBody() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MySuperClass {",
            "  class MyNestedClass {}",
            "}",
            "class MyClass extends MySuperClass {",
            "  static class MySuperClass {}",
            "  void myMethod(MyNestedClass c) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lmypackage/MySuperClass$MyNestedClass;)V");
  }

  /**
   * The annotations of a varargs parameter are recorded: one that precedes the type as a
   * declaration annotation, one within the element type on the array's component type, and one that
   * precedes the {@code ...} on the array type.
   */
  @Test
  public void testVarargsParameterAnnotations() throws Exception {
    String stubFile =
        String.join(
            System.lineSeparator(),
            "package p;",
            "class C {",
            "  void m(@A java.lang.@B String @C ... args) {}",
            "}");
    AScene scene = new AScene();
    ToIndexFileConverter.convert(
        scene,
        new ByteArrayInputStream(stubFile.getBytes(StandardCharsets.UTF_8)),
        new ByteArrayOutputStream());
    AMethod method = scene.classes.get("p.C").methods.get("m([Ljava/lang/String;)V");
    Assert.assertNotNull("no scene element for method m", method);
    AField param = method.parameters.get(0);
    Assert.assertNotNull("no scene element for parameter args", param);
    Assert.assertNotNull("@A was not recorded on the parameter", param.lookup("A"));
    Assert.assertNotNull("@C was not recorded on the array type", param.type.lookup("C"));
    Assert.assertNull("@B was recorded on the array type", param.type.lookup("B"));
    ATypeElement componentType = param.type.innerTypes.get(element(Collections.emptyList()));
    Assert.assertNotNull("no scene element for the component type", componentType);
    Assert.assertNotNull("@B was not recorded on the component type", componentType.lookup("B"));
    Assert.assertNull("@C was recorded on the component type", componentType.lookup("C"));
  }

  /** A record's members belong to the record, not to the class that encloses it. */
  @Test
  public void testNestedRecordMembers() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MyClass {",
            "  record MyRecord(int x) {",
            "    void myMethod() {}",
            "  }",
            "}");
    assertMethod(jaif, "MyClass$MyRecord", "myMethod()V");
  }

  /** A type variable whose bound is a fully qualified name erases to that name. */
  @Test
  public void testFullyQualifiedTypeVariableBound() throws Exception {
    String jaif =
        convert(
            "package p;",
            "class MyClass {",
            "  <T extends java.util.List<?>, U extends java.util.Map.Entry<?, ?>>",
            "  void myMethod(T t, U u) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/List;Ljava/util/Map$Entry;)V");
  }

  /** A class declared in the stub file shadows a class of the same name on the classpath. */
  @Test
  public void testStubFileDeclarationShadowsClasspath() throws Exception {
    // org.checkerframework.dataflow.util.PurityChecker is on the classpath.
    String jaif =
        convert(
            "package mypackage;",
            "import org.checkerframework.dataflow.util.PurityChecker;",
            "class MyClass {",
            "  class PurityChecker {}",
            "  void myMethod(PurityChecker c) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lmypackage/MyClass$PurityChecker;)V");
  }

  /** A nested class shadows a type parameter of an enclosing class. */
  @Test
  public void testNestedClassShadowsTypeParameter() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MyClass<E> {",
            "  void myMethod(E e) {}",
            "  class MyNestedClass {",
            "    class E {}",
            "    void myNestedMethod(E e) {}",
            "  }",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/lang/Object;)V");
    assertMethod(
        jaif, "MyClass$MyNestedClass", "myNestedMethod(Lmypackage/MyClass$MyNestedClass$E;)V");
  }

  /** A method's JVML descriptor uses the binary name of an inherited member type. */
  @Test
  public void testInheritedNestedClass() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MySuperClass {",
            "  class MyNestedClass {}",
            "}",
            "interface MyInterface {",
            "  class MyInterfaceNestedClass {}",
            "}",
            "class MyMiddleClass extends MySuperClass implements MyInterface {}",
            "class MyClass extends MyMiddleClass {",
            "  void myMethod(MyNestedClass c, MyInterfaceNestedClass i) {}",
            "  void myOtherMethod(MyMiddleClass.MyNestedClass c) {}",
            "}");
    assertMethod(
        jaif,
        "MyClass",
        "myMethod(Lmypackage/MySuperClass$MyNestedClass;"
            + "Lmypackage/MyInterface$MyInterfaceNestedClass;)V");
    assertMethod(jaif, "MyClass", "myOtherMethod(Lmypackage/MySuperClass$MyNestedClass;)V");
  }

  /** A member type that is inherited from a class on the classpath is resolved. */
  @Test
  public void testInheritedNestedClassOnClasspath() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "import java.util.HashMap;",
            "class MyClass extends HashMap {",
            "  void myMethod(Entry e) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/Map$Entry;)V");
  }

  /**
   * A member type that is inherited, through a supertype that the stub file declares, from a class
   * on the classpath is resolved.
   */
  @Test
  public void testIndirectlyInheritedNestedClassOnClasspath() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "import java.util.HashMap;",
            "class MyMiddleClass extends HashMap {}",
            "class MyClass extends MyMiddleClass {",
            "  void myMethod(Entry e) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/Map$Entry;)V");
  }

  /**
   * A member type that is inherited, through a chain of supertypes that the stub file declares,
   * from an interface on the classpath is resolved.
   */
  @Test
  public void testIndirectlyInheritedNestedClassOnClasspathViaInterface() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "interface MyInterface extends java.util.Map {}",
            "abstract class MyMiddleClass implements MyInterface {}",
            "abstract class MyClass extends MyMiddleClass {",
            "  void myMethod(Entry e) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Ljava/util/Map$Entry;)V");
  }

  /**
   * A stub file that declares a cyclic inheritance hierarchy, which is not legal Java, does not
   * cause infinite recursion while resolving an inherited member type.
   */
  @Test
  public void testCyclicInheritanceOfNestedClassOnClasspath() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MyFirstClass extends MySecondClass {}",
            "class MySecondClass extends MyFirstClass {",
            "  void myMethod(Entry e) {}",
            "}");
    assertMethod(jaif, "MySecondClass", "myMethod(Lmypackage/Entry;)V");
  }

  /** A nested class declared in the stub file is qualified with the stub file's package. */
  @Test
  public void testUnresolvedNestedTypeInOwnPackage() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MyClass {",
            "  void myMethod(MyOtherClass.MyNestedClass c) {}",
            "}",
            "class MyOtherClass {",
            "  class MyNestedClass {",
            "    class MyDoublyNestedClass {}",
            "    void myNestedMethod(MyDoublyNestedClass c, MyOtherClass o) {}",
            "  }",
            "  void myOtherMethod(MyNestedClass c) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lmypackage/MyOtherClass$MyNestedClass;)V");
    assertMethod(jaif, "MyOtherClass", "myOtherMethod(Lmypackage/MyOtherClass$MyNestedClass;)V");
    assertMethod(
        jaif,
        "MyOtherClass$MyNestedClass",
        "myNestedMethod(Lmypackage/MyOtherClass$MyNestedClass$MyDoublyNestedClass;"
            + "Lmypackage/MyOtherClass;)V");
  }

  /** The stub file may refer to one of its own types by the type's fully qualified name. */
  @Test
  public void testFullyQualifiedNameOfStubFileType() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "class MyClass {",
            "  void myMethod(mypackage.MyOtherClass.MyNestedClass c) {}",
            "}",
            "class MyOtherClass {",
            "  class MyNestedClass {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lmypackage/MyOtherClass$MyNestedClass;)V");
  }

  /** A single-type import of a type that the stub file declares is resolved. */
  @Test
  public void testSingleTypeImportOfStubFileType() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "import mypackage.MyOtherClass.MyNestedClass;",
            "class MyClass {",
            "  void myMethod(MyNestedClass c) {}",
            "}",
            "class MyOtherClass {",
            "  class MyNestedClass {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lmypackage/MyOtherClass$MyNestedClass;)V");
  }

  /** An import-on-demand of types that the stub file declares is resolved. */
  @Test
  public void testOnDemandImportOfStubFileType() throws Exception {
    String jaif =
        convert(
            "package mypackage;",
            "import mypackage.MyOtherClass.*;",
            "class MyClass {",
            "  void myMethod(MyNestedClass c) {}",
            "}",
            "class MyOtherClass {",
            "  class MyNestedClass {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lmypackage/MyOtherClass$MyNestedClass;)V");
  }

  /** A single-type import does not supply part of a package name. */
  @Test
  public void testImportDoesNotSupplyPartialPackage() throws Exception {
    // `util.Map` does not refer to java.util.Map, so the name is unresolvable.
    String jaif =
        convert(
            "package mypackage;",
            "import java.util.Map;",
            "class MyClass {",
            "  void myMethod(util.Map m) {}",
            "}");
    assertMethod(jaif, "MyClass", "myMethod(Lutil/Map;)V");
  }

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
}
