package org.checkerframework.framework.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.StandardLocation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link JavaParserUtil}. */
public class JavaParserUtilTest {

  /** The package that declares the member types that the tests resolve. */
  private static final String SUPERPKG =
      "org.checkerframework.framework.util.javaparserutil.superpkg";

  /** A package other than {@link #SUPERPKG}. */
  private static final String SUBPKG = "org.checkerframework.framework.util.javaparserutil.subpkg";

  /** Creates a new JavaParserUtilTest. */
  public JavaParserUtilTest() {}

  /**
   * Tests that {@link JavaParserUtil#resolveTypeNameAsTypeElement} resolves the simple name of an
   * inherited member type only when the subtype really inherits it.
   */
  @Test
  public void testResolveInheritedMemberType() {
    // A public member type is inherited, even across packages.
    assertResolvesTo(
        SUPERPKG + ".Base.Visible",
        SUBPKG,
        "class Sub extends " + SUPERPKG + ".Base { Visible f; }",
        "Visible");
    assertResolvesTo(
        SUPERPKG + ".Base.Visible",
        SUPERPKG,
        "class SamePackageSub extends Base { Visible f; }",
        "Visible");

    // A package-private member type is inherited only within the package that declares it.
    assertResolvesTo(
        SUPERPKG + ".Base.Hidden",
        SUPERPKG,
        "class SamePackageSub extends Base { Hidden f; }",
        "Hidden");
    assertResolvesTo(
        null, SUBPKG, "class Sub extends " + SUPERPKG + ".Base { Hidden f; }", "Hidden");

    // A private member type is never inherited.
    assertResolvesTo(null, SUPERPKG, "class SamePackageSub extends Base { Secret f; }", "Secret");

    // `Intermediate.Visible` hides `Base.Visible`.  A subtype of `Intermediate` therefore does not
    // inherit `Base.Visible`, whether or not it inherits `Intermediate.Visible`.
    assertResolvesTo(
        SUPERPKG + ".Intermediate.Visible",
        SUPERPKG,
        "class SamePackageIntermediateSub extends Intermediate { Visible f; }",
        "Visible");
    assertResolvesTo(
        null,
        SUBPKG,
        "class IntermediateSub extends " + SUPERPKG + ".Intermediate { Visible f; }",
        "Visible");
  }

  /**
   * Tests that {@link JavaParserUtil#resolveTypeNameAsTypeParameter} resolves a name to a type
   * variable exactly when no type declaration shadows the type variable.
   */
  @Test
  public void testResolveTypeVariableName() {
    // A type parameter of an enclosing class or of an enclosing method.
    assertNamesTypeVariable(
        "T extends CharSequence",
        SUPERPKG,
        "class SamePackageSub<T extends CharSequence> { T f; }",
        "T");
    assertNamesTypeVariable(
        "T extends CharSequence",
        SUPERPKG,
        "class SamePackageSub { <T extends CharSequence> void m(T p) {} }",
        "T");
    // A type parameter need not have a bound.
    assertNamesTypeVariable("T", SUPERPKG, "class SamePackageSub<T> { T f; }", "T");

    // A member type that the class declares shadows a type parameter of the same name, so the name
    // names the member type rather than the type variable.
    String shadowing =
        "class Shadowing<Visible> extends Base {"
            + " public static class Visible { public static class Inner {} }"
            + " Visible f; Visible.Inner g; Visible.OnlyInBase h; }";
    assertNamesTypeVariable(null, SUPERPKG, shadowing, "Visible");
    assertResolvesTo(SUPERPKG + ".Shadowing.Visible", SUPERPKG, shadowing, "Visible");
    // A name with more components is resolved within the member type.
    assertNamesTypeVariable(null, SUPERPKG, shadowing, "Visible.Inner");
    assertResolvesTo(SUPERPKG + ".Shadowing.Visible.Inner", SUPERPKG, shadowing, "Visible.Inner");
    // If the member type has no such member, then the name names nothing, even though the hidden
    // `Base.Visible` has such a member.
    assertNamesTypeVariable(null, SUPERPKG, shadowing, "Visible.OnlyInBase");
    assertResolvesTo(null, SUPERPKG, shadowing, "Visible.OnlyInBase");

    // A member type that the class only inherits does not shadow a type parameter of the same
    // name, so the name names the type variable.
    String inherited = "class TypeParameterSub<Visible> extends Base { Visible f; }";
    assertNamesTypeVariable("Visible", SUPERPKG, inherited, "Visible");
    assertResolvesTo(null, SUPERPKG, inherited, "Visible");

    // A local class shadows a type parameter of the same name.
    assertNamesTypeVariable(
        null,
        SUPERPKG,
        "class SamePackageSub<T extends CharSequence> { void m() { class T {} T f; } }",
        "T");

    // A member type that a local class declares shadows a type parameter of the local class.  The
    // member type has no name that `Elements` can look up, so the name resolves to nothing.
    String localShadowing =
        "class SamePackageSub { void m() { class Local<T> { class T {} T f; } } }";
    assertNamesTypeVariable(null, SUPERPKG, localShadowing, "T");
    assertResolvesTo(null, SUPERPKG, localShadowing, "T");

    // A name that names no type variable.
    assertNamesTypeVariable(
        null, SUPERPKG, "class SamePackageSub extends Base { Visible f; }", "Visible");

    // A type variable has no member types, so a name whose first component names a type variable
    // and that has more components, as in `T.Inner`, names nothing at all.
    String memberOfTypeVariable = "class SamePackageSub<T extends CharSequence> { T.Inner f; }";
    assertNamesTypeVariable(null, SUPERPKG, memberOfTypeVariable, "T.Inner");
    assertResolvesTo(null, SUPERPKG, memberOfTypeVariable, "T.Inner");
  }

  /**
   * Tests that {@link JavaParserUtil#typeToTypeMirrorUsingTypeVariableBounds} replaces a use of a
   * type variable by the type variable's upper bound, and that {@link
   * JavaParserUtil#typeToTypeMirror} does not.
   */
  @Test
  public void testTypeToTypeMirrorUsingTypeVariableBounds() {
    String bounded = "class SamePackageSub<T extends Number> { T f; T[] g; String h; int i; }";
    assertTypeMirror("java.lang.Number", bounded, "T");
    assertTypeMirror("java.lang.Number[]", bounded, "T[]");
    assertTypeMirror("java.lang.String", bounded, "String");
    assertTypeMirror("int", bounded, "int");

    // The implicit upper bound is `Object`.
    assertTypeMirror("java.lang.Object", "class SamePackageSub<T> { T f; }", "T");

    // A bound that is itself a type variable is replaced by its own upper bound.
    assertTypeMirror(
        "java.lang.CharSequence",
        "class SamePackageSub<T extends U, U extends CharSequence> { T f; }",
        "T");

    // The upper bound of `T` is an intersection type, whose erasure is its leftmost bound.
    assertTypeMirror(
        "java.lang.Number", "class SamePackageSub<T extends Number & Runnable> { T f; }", "T");

    // The result is the type of the declaration, which is not erased.
    assertTypeMirror(
        "java.lang.Comparable<T>", "class SamePackageSub<U extends Comparable<U>> { U f; }", "U");
    assertTypeMirror(
        "java.util.List<E>",
        "import java.util.List; class SamePackageSub<T> { List<T> f; }",
        "List<T>");

    // A type parameter of a method shadows a type parameter of the class.
    assertTypeMirror(
        "java.lang.Number",
        "class SamePackageSub<T extends CharSequence> { <T extends Number> void m(T p) {} }",
        "T");

    // A bound that is not on the classpath cannot be determined.
    assertTypeMirror(null, "class SamePackageSub<T extends NotOnClasspath> { T f; }", "T");

    // A type parameter declaration has no TypeMirror.
    String source = "package " + SUPERPKG + "; class SamePackageSub<T extends Number> {}";
    TypeParameter declaration =
        StaticJavaParser.parse(source).findFirst(TypeParameter.class).orElseThrow();
    Assert.assertNull(
        source,
        JavaParserUtil.typeToTypeMirrorUsingTypeVariableBounds(
            elements, types, declaration, new HashMap<>()));
  }

  /**
   * Asserts that {@link JavaParserUtil#typeToTypeMirrorUsingTypeVariableBounds} converts the given
   * type, as written in the given compilation unit, to a TypeMirror whose string representation is
   * {@code expected}. Also asserts that {@link JavaParserUtil#typeToTypeMirror} converts the type
   * to the same TypeMirror, or to null if the type contains a use of a type variable.
   *
   * @param expected the string representation of the expected TypeMirror, or null if the TypeMirror
   *     should not be determined
   * @param typeDeclaration the type declaration that the compilation unit, which is in package
   *     {@link #SUPERPKG}, contains, possibly preceded by imports; it must be the declaration of a
   *     type that is on the classpath, and it must contain {@code typeString}
   * @param typeString the type to convert, as written in {@code typeDeclaration}
   */
  private static void assertTypeMirror(
      @Nullable String expected, String typeDeclaration, String typeString) {
    String source = "package " + SUPERPKG + "; " + typeDeclaration;
    Type type =
        StaticJavaParser.parse(source).findAll(Type.class).stream()
            .filter(t -> !(t instanceof TypeParameter) && t.toString().equals(typeString))
            .findFirst()
            .orElseThrow(() -> new BugInCF("No type %s in %s", typeString, source));

    TypeMirror withBounds =
        JavaParserUtil.typeToTypeMirrorUsingTypeVariableBounds(
            elements, types, type, new HashMap<>());
    Assert.assertEquals(source, expected, withBounds == null ? null : withBounds.toString());

    boolean usesTypeVariable =
        type.getElementType() instanceof ClassOrInterfaceType classType
            && JavaParserUtil.resolveTypeNameAsTypeParameter(elements, classType) != null;
    TypeMirror withoutBounds =
        JavaParserUtil.typeToTypeMirror(elements, types, type, new HashMap<>());
    Assert.assertEquals(
        source,
        usesTypeVariable ? null : expected,
        withoutBounds == null ? null : withoutBounds.toString());
  }

  /**
   * Asserts that {@link JavaParserUtil#resolveTypeNameAsTypeElement} resolves {@code typeName}, as
   * written in the given compilation unit, to a type element with the given fully-qualified name.
   *
   * @param expected the expected fully-qualified name, or null if the name should not resolve
   * @param packageName the package of the compilation unit
   * @param typeDeclaration the type declaration that the compilation unit contains; it must be the
   *     declaration of a type that is on the classpath, and it must contain {@code typeName}
   * @param typeName the type name to resolve
   */
  private static void assertResolvesTo(
      @Nullable String expected, String packageName, String typeDeclaration, String typeName) {
    String source = "package " + packageName + "; " + typeDeclaration;
    ClassOrInterfaceType type = findType(source, typeName);

    TypeElement resolved = JavaParserUtil.resolveTypeNameAsTypeElement(elements, type);
    String resolvedName = resolved == null ? null : resolved.getQualifiedName().toString();
    Assert.assertEquals(source, expected, resolvedName);
  }

  /**
   * Asserts that {@link JavaParserUtil#resolveTypeNameAsTypeParameter} resolves {@code typeName},
   * as written in the given compilation unit, to the declaration of a type variable that is written
   * as {@code expected}.
   *
   * @param expected the expected type parameter declaration, such as {@code "T extends
   *     CharSequence"}, or null if the name should not name a type variable
   * @param packageName the package of the compilation unit
   * @param typeDeclaration the type declaration that the compilation unit contains; it must be the
   *     declaration of a type that is on the classpath, and it must contain {@code typeName}
   * @param typeName the type name to resolve
   */
  private static void assertNamesTypeVariable(
      @Nullable String expected, String packageName, String typeDeclaration, String typeName) {
    String source = "package " + packageName + "; " + typeDeclaration;
    ClassOrInterfaceType type = findType(source, typeName);

    TypeParameter resolved = JavaParserUtil.resolveTypeNameAsTypeParameter(elements, type);
    String resolvedName = resolved == null ? null : resolved.toString();
    Assert.assertEquals(source, expected, resolvedName);
  }

  /**
   * Parses the given source code and returns the use of the given type name that it contains.
   *
   * @param source the source code of a compilation unit
   * @param typeName a type name that {@code source} uses
   * @return the first use of {@code typeName} in {@code source}
   */
  private static ClassOrInterfaceType findType(String source, String typeName) {
    CompilationUnit cu = StaticJavaParser.parse(source);
    return cu.findAll(ClassOrInterfaceType.class).stream()
        .filter(t -> t.getNameWithScope().equals(typeName))
        .findFirst()
        .orElseThrow(() -> new BugInCF("No type named %s in %s", typeName, source));
  }

  /** Used by the tests to look up names. */
  private static final Elements elements;

  /** Used by the tests to create types. */
  private static final Types types;

  static {
    Context context = new Context();
    // The file manager must be created, and its classpath set, before the compilation environment
    // reads them.  The classpath makes the test data classes, which are compiled along with this
    // test, available to `Elements#getTypeElement`.
    JavacFileManager fileManager = new JavacFileManager(context, true, StandardCharsets.UTF_8);
    try {
      fileManager.setLocation(StandardLocation.CLASS_PATH, classpathFiles());
    } catch (IOException e) {
      throw new BugInCF(e, "Cannot set the classpath");
    }
    JavacProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();
    elements = env.getElementUtils();
    types = env.getTypeUtils();
  }

  /**
   * Returns the entries of the classpath that this test is running on.
   *
   * @return the entries of the classpath that this test is running on
   */
  private static List<File> classpathFiles() {
    List<File> result = new ArrayList<>();
    for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
      result.add(new File(entry));
    }
    return result;
  }
}
