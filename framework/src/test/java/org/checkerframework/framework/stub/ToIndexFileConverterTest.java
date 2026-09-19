package org.checkerframework.framework.stub;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.Collections;
import org.checkerframework.afu.scenelib.el.AScene;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link ToIndexFileConverter}. */
public class ToIndexFileConverterTest {

  /** The converter under test. */
  private final ToIndexFileConverter converter =
      new ToIndexFileConverter(null, Collections.emptyList(), new AScene());

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
}
