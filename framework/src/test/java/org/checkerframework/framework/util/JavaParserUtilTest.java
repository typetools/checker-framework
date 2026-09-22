package org.checkerframework.framework.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
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
   * Tests that {@link JavaParserUtil#resolveTypeName} resolves the simple name of an inherited
   * member type only when the subtype really inherits it.
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
   * Asserts that {@link JavaParserUtil#resolveTypeName} resolves {@code typeName}, as written in
   * the given compilation unit, to a type element with the given fully-qualified name.
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

    TypeElement resolved = JavaParserUtil.resolveTypeName(elements, type);
    String resolvedName = resolved == null ? null : resolved.getQualifiedName().toString();
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
