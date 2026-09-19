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
}
