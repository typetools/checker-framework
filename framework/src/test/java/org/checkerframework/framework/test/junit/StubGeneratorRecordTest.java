package org.checkerframework.framework.test.junit;

import static org.checkerframework.framework.test.junit.StubGeneratorTestHelper.annotationDeclaration;
import static org.checkerframework.framework.test.junit.StubGeneratorTestHelper.assertParses;
import static org.checkerframework.framework.test.junit.StubGeneratorTestHelper.generateStub;

import org.checkerframework.framework.stub.StubGenerator;
import org.checkerframework.framework.test.junit.StubGeneratorTestHelper.SourceFile;
import org.junit.Assert;
import org.junit.Test;

/** Tests that {@link StubGenerator} generates a parseable stub file for a record or an enum. */
public class StubGeneratorRecordTest {

  /** Creates a new StubGeneratorRecordTest. */
  public StubGeneratorRecordTest() {}

  @Test
  public void recordComponents() {
    String stub =
        generateStub(
            "p/Outer.java",
            "package p; public class Outer { public record Point(int x, String name) {} }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("record Outer$Point(int x, String name)"));
    // A record's superclass is java.lang.Record, which must not appear in an extends clause.
    Assert.assertFalse(stub, stub.contains("extends Record"));
    // A constructor's name is the name of the class declaration that contains it.
    Assert.assertTrue(stub, stub.contains("Outer$Point(int x, String name);"));
    assertParses(stub);
  }

  @Test
  public void genericRecordComponents() {
    String stub =
        generateStub(
            "p/Outer.java",
            "package p;"
                + " public class Outer {"
                + "   public record Pair<K, V>(K key, V value) implements Cloneable {} }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("record Outer$Pair<K, V>(K key, V value)"));
    Assert.assertTrue(stub, stub.contains("implements Cloneable"));
    assertParses(stub);
  }

  @Test
  public void boundedRecordTypeParameters() {
    String stub =
        generateStub(
            "p/Box.java",
            "package p; public record Box<T extends Number & Comparable<T>>(T t) {}",
            "p.Box");
    Assert.assertTrue(stub, stub.contains("record Box<T extends Number & Comparable<T>>(T t)"));
    assertParses(stub);
  }

  @Test
  public void topLevelRecord() {
    String stub =
        generateStub(
            "p/Point.java", "package p; public record Point(int x, String name) {}", "p.Point");
    Assert.assertTrue(stub, stub.contains("package p;"));
    Assert.assertTrue(stub, stub.contains("record Point(int x, String name)"));
    assertParses(stub);
  }

  @Test
  public void typeAnnotatedRecordComponents() {
    String stub =
        generateStub(
            "p.Outer",
            annotationDeclaration("TypeAnno", "TYPE_USE"),
            new SourceFile(
                "p/Outer.java",
                "package p;"
                    + " public class Outer {"
                    + "   public record Annotated(@TypeAnno int x, @TypeAnno String s) {} }"));
    Assert.assertTrue(
        stub, stub.contains("record Outer$Annotated(@p.TypeAnno int x, @p.TypeAnno String s)"));
    assertParses(stub);
  }

  @Test
  public void declarationAnnotatedRecordComponents() {
    String stub =
        generateStub(
            "p.Outer",
            annotationDeclaration("CompAnno", "RECORD_COMPONENT"),
            new SourceFile(
                "p/Outer.java",
                "package p;"
                    + " public class Outer { public record Annotated(@CompAnno int x) {} }"));
    Assert.assertTrue(stub, stub.contains("record Outer$Annotated(@p.CompAnno int x)"));
    assertParses(stub);
  }

  /**
   * An annotation that is applicable to both a record component and a type use is reported both as
   * a declaration annotation and as a type annotation, but it should be printed only once.
   */
  @Test
  public void recordComponentAnnotationPrintedOnce() {
    String stub =
        generateStub(
            "p.Outer",
            annotationDeclaration("BothAnno", "RECORD_COMPONENT", "TYPE_USE"),
            new SourceFile(
                "p/Outer.java",
                "package p;"
                    + " public class Outer { public record Annotated(@BothAnno String s) {} }"));
    Assert.assertTrue(stub, stub.contains("record Outer$Annotated(@p.BothAnno String s)"));
    assertParses(stub);
  }

  /**
   * An annotation on the element type of an array-typed record component is printed as part of the
   * component's type, so it should not also be printed as a declaration annotation.
   */
  @Test
  public void arrayRecordComponentAnnotationPrintedOnce() {
    String stub =
        generateStub(
            "p.Outer",
            annotationDeclaration("BothAnno", "RECORD_COMPONENT", "TYPE_USE"),
            new SourceFile(
                "p/Outer.java",
                "package p;"
                    + " public class Outer { public record Annotated(@BothAnno String[] s) {} }"));
    Assert.assertTrue(stub, stub.contains("record Outer$Annotated(@p.BothAnno String[] s)"));
    assertParses(stub);
  }

  /**
   * An annotation on an array-typed record component's array type is printed as part of the
   * component's type, so it should not also be printed as a declaration annotation.
   */
  @Test
  public void arrayTypeRecordComponentAnnotationPrintedOnce() {
    String stub =
        generateStub(
            "p.Outer",
            annotationDeclaration("BothAnno", "RECORD_COMPONENT", "TYPE_USE"),
            new SourceFile(
                "p/Outer.java",
                "package p;"
                    + " public class Outer { public record Annotated(String @BothAnno [] s) {} }"));
    Assert.assertTrue(stub, stub.contains("record Outer$Annotated(String @p.BothAnno [] s)"));
    assertParses(stub);
  }

  @Test
  public void nestedEnum() {
    String stub =
        generateStub(
            "p/Outer.java",
            "package p; public class Outer { public enum Color { RED, BLUE } }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("enum Outer$Color"));
    Assert.assertTrue(stub, stub.contains("RED, BLUE;"));
    // An enum's superclass is java.lang.Enum, which must not appear in an extends clause.
    Assert.assertFalse(stub, stub.contains("extends Enum"));
    assertParses(stub);
  }

  @Test
  public void nestedEnumWithoutConstants() {
    String stub =
        generateStub(
            "p/Outer.java", "package p; public class Outer { public enum Empty {} }", "p.Outer");
    Assert.assertTrue(stub, stub.contains("enum Outer$Empty"));
    assertParses(stub);
  }

  @Test
  public void topLevelEnum() {
    String stub =
        generateStub("p/Color.java", "package p; public enum Color { RED, BLUE }", "p.Color");
    Assert.assertTrue(stub, stub.contains("package p;"));
    Assert.assertTrue(stub, stub.contains("enum Color"));
    Assert.assertTrue(stub, stub.contains("RED, BLUE;"));
    assertParses(stub);
  }

  @Test
  public void annotatedEnumConstants() {
    String stub =
        generateStub(
            "p.Color",
            annotationDeclaration("FieldAnno", "FIELD"),
            new SourceFile(
                "p/Color.java", "package p; public enum Color { @FieldAnno RED, BLUE }"));
    Assert.assertTrue(stub, stub.contains("@p.FieldAnno RED, BLUE;"));
    assertParses(stub);
  }

  @Test
  public void unnamedPackageRecord() {
    String stub = generateStub("Point.java", "public record Point(int x, String name) {}", "Point");
    // The unnamed package has no package declaration.
    Assert.assertFalse(stub, stub.contains("package"));
    Assert.assertTrue(stub, stub.contains("record Point(int x, String name)"));
    assertParses(stub);
  }

  @Test
  public void unnamedPackageNestedRecord() {
    String stub =
        generateStub("Outer.java", "public class Outer { public record Point(int x) {} }", "Outer");
    Assert.assertFalse(stub, stub.contains("package"));
    Assert.assertTrue(stub, stub.contains("record Outer$Point(int x)"));
    assertParses(stub);
  }

  @Test
  public void emptyRecordComponents() {
    String stub =
        generateStub(
            "p/Outer.java",
            "package p; public class Outer { public record Empty() {} }",
            "p.Outer");
    Assert.assertTrue(stub, stub.contains("record Outer$Empty()"));
    assertParses(stub);
  }
}
