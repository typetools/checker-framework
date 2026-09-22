package org.checkerframework.framework.test.junit;

import java.util.regex.Pattern;
import org.checkerframework.framework.stub.StubGenerator;
import org.checkerframework.framework.test.junit.StubGeneratorTestHelper.SourceFile;
import org.junit.Assert;
import org.junit.Test;
import org.plumelib.util.StringsP;

/** Tests for {@link StubGenerator}. */
public class StubGeneratorTest {

  /** Matches a package declaration, which starts a line. */
  private static final Pattern packageDeclarationPattern =
      Pattern.compile("^[ \t]*package\\s", Pattern.MULTILINE);

  /** Creates a new StubGeneratorTest. */
  public StubGeneratorTest() {}

  @Test
  public void nestedClassInDefaultPackage() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "Foo.java", "public class Foo { public static class Inner {} }", "Foo.Inner");
    Assert.assertTrue(stub, stub.contains("class Foo$Inner"));
    assertNoPackageDeclaration(stub);
  }

  @Test
  public void doublyNestedClass() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p/Qux.java",
            "package p;"
                + " public class Qux { public static class Inner { public static class Innermost"
                + " {} } }",
            "p.Qux");
    Assert.assertTrue(stub, stub.contains("class Qux$Inner"));
    Assert.assertTrue(stub, stub.contains("class Qux$Inner$Innermost"));
  }

  @Test
  public void nestedClassInNamedPackage() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p/Bar.java",
            "package p; public class Bar { public static class Inner {} }",
            "p.Bar.Inner");
    Assert.assertTrue(stub, stub.contains("package p;"));
    Assert.assertTrue(stub, stub.contains("class Bar$Inner"));
  }

  @Test
  public void topLevelClassInDefaultPackage() {
    String stub = StubGeneratorTestHelper.generateStub("Baz.java", "public class Baz {}", "Baz");
    Assert.assertTrue(stub, stub.contains("class Baz"));
    assertNoPackageDeclaration(stub);
  }

  @Test
  public void typeUseAnnotationWithClassLiteralArgument() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p/Uses.java",
            "package p;"
                + " public class Uses { public @Anno(Tgt.class) String field; }"
                + " @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)"
                + " @interface Anno { Class<?> value(); }"
                + " class Tgt {}",
            "p.Uses");
    Assert.assertTrue(stub, stub.contains("@p.Anno(p.Tgt.class) String field"));
  }

  @Test
  public void typeUseAnnotationWithStringArgumentContainingParentheses() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p/UsesString.java",
            "package p;"
                + " public class UsesString { public @Anno2(\"a.b.method()\") String field; }"
                + " @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)"
                + " @interface Anno2 { String value(); }",
            "p.UsesString");
    Assert.assertTrue(stub, stub.contains("@p.Anno2(\"a.b.method()\") String field"));
  }

  @Test
  public void typeUseAnnotationWithCharArgumentContainingParenthesis() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p/UsesChar.java",
            "package p;"
                + " public class UsesChar { public @Anno3(ch = ')', type = java.util.Map.class)"
                + " String field; }"
                + " @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)"
                + " @interface Anno3 { char ch(); Class<?> type(); }",
            "p.UsesChar");
    Assert.assertTrue(
        stub, stub.contains("@p.Anno3(ch=')', type=java.util.Map.class) String field"));
  }

  @Test
  public void nestedAnnotationType() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p/Outer.java",
            "package p; public class Outer { public @interface Ann { int value(); } }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("@interface Outer$Ann"));
    // An annotation type's superinterface is java.lang.annotation.Annotation, which may not
    // appear in an implements clause.
    Assert.assertFalse(stub, stub.contains("implements"));
    StubGeneratorTestHelper.assertParses(stub);
  }

  @Test
  public void typeParameterBounds() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p/Bounded.java",
            "package p;"
                + " public class Bounded<T extends Number & java.io.Serializable> {"
                + "   public <U extends CharSequence> void m(U u) {} }",
            "p.Bounded");
    Assert.assertTrue(stub, stub.contains("class Bounded<T extends Number & Serializable>"));
    Assert.assertTrue(stub, stub.contains("<U extends CharSequence> void m(U u)"));
    StubGeneratorTestHelper.assertParses(stub);
  }

  @Test
  public void annotatedTypeParameters() {
    String stub =
        StubGeneratorTestHelper.generateStub(
            "p.Annotated",
            StubGeneratorTestHelper.annotationDeclaration("TypeAnno", "TYPE_USE"),
            StubGeneratorTestHelper.annotationDeclaration("ParamAnno", "TYPE_PARAMETER"),
            StubGeneratorTestHelper.annotationDeclaration("BothAnno", "TYPE_USE", "TYPE_PARAMETER"),
            new SourceFile(
                "p/Annotated.java",
                "package p;"
                    + " public class Annotated<@TypeAnno T, @ParamAnno U, @BothAnno V> {"
                    + "   public <@TypeAnno A extends @TypeAnno Number> void m(A a) {} }"));
    Assert.assertTrue(
        stub, stub.contains("class Annotated<@p.TypeAnno T, @p.ParamAnno U, @p.BothAnno V>"));
    // An annotation that is applicable to both a type parameter and a type use is printed once.
    Assert.assertEquals(stub, 1, StringsP.count(stub, "@p.BothAnno"));
    Assert.assertTrue(
        stub, stub.contains("<@p.TypeAnno A extends @p.TypeAnno Number> void m(A a)"));
    StubGeneratorTestHelper.assertParses(stub);
  }

  /**
   * Asserts that the given stub file text contains no package declaration, as is correct for the
   * default package.
   *
   * @param stub the text of a stub file
   */
  private void assertNoPackageDeclaration(String stub) {
    Assert.assertFalse(stub, packageDeclarationPattern.matcher(stub).find());
  }
}
