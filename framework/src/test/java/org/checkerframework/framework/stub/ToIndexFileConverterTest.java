package org.checkerframework.framework.stub;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.checkerframework.afu.scenelib.el.AScene;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link ToIndexFileConverter}. */
public class ToIndexFileConverterTest {

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

  /** A method's JVML descriptor uses the erasure of each type variable. */
  @Test
  public void testTypeVariableErasure() throws Exception {
    String jaif =
        convert(
            "package p;",
            "class MyClass<S extends CharSequence> {",
            "  <T extends Number, U> void myMethod(T t, U u, S s, Object o) {}",
            "}");
    Assert.assertTrue(
        jaif,
        jaif.contains(
            "method myMethod(Ljava/lang/Number;Ljava/lang/Object;Ljava/lang/CharSequence;"
                + "Ljava/lang/Object;)V"));
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
    Assert.assertTrue(
        jaif,
        jaif.contains(
            "method myMethod(Lorg/checkerframework/framework/stub/ToIndexFileConverter;)V"));
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
    Assert.assertTrue(
        jaif,
        jaif.contains("method myMethod(Lorg/checkerframework/dataflow/util/PurityChecker;)V"));
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
    Assert.assertTrue(jaif, jaif.contains("method <init>(I[Ljava/lang/String;)V"));
    Assert.assertTrue(jaif, jaif.contains("method myMethod([Ljava/lang/Number;)V"));
    Assert.assertTrue(jaif, jaif.contains("method myOtherMethod([[Ljava/lang/CharSequence;)V"));
  }

  /** A method's JVML descriptor uses a fully qualified name that appears in the stub file. */
  @Test
  public void testFullyQualifiedName() throws Exception {
    String jaif =
        convert("package p;", "class MyClass {", "  void myMethod(java.util.List<?> l) {}", "}");
    Assert.assertTrue(jaif, jaif.contains("method myMethod(Ljava/util/List;)V"));
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
    Assert.assertTrue(
        jaif, jaif.contains("method myMethod(Ljava/util/Map$Entry;Ljava/util/Map$Entry;)V"));
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
    Assert.assertTrue(jaif, jaif.contains("method myMethod(Lmypackage/MyOtherClass;)V"));
    Assert.assertTrue(jaif, jaif.contains("method myOtherMethod(Lmypackage/MyClass;)V"));
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
    Assert.assertTrue(
        jaif, jaif.contains("method myMethod(Ljava/util/List;Ljava/util/Map$Entry;)V"));
  }
}
