package org.checkerframework.framework.stub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.checkerframework.framework.stub.IntelliJAnnotationParser.ParsedItemSignature;
import org.junit.Test;

/** Unit tests for the string-manipulation routines of {@link IntelliJAnnotationParser}. */
public class IntelliJAnnotationParserTest {

  @Test
  public void testParseSignatureClass() {
    ParsedItemSignature parsed = IntelliJAnnotationParser.parseSignature("java.lang.String");
    assertTrue(parsed.isClass);
    assertFalse(parsed.isMalformed());
    assertEquals("java.lang.String", parsed.className);
  }

  @Test
  public void testParseSignatureField() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature(
            "java.lang.String java.util.Comparator CASE_INSENSITIVE_ORDER");
    assertTrue(parsed.isField);
    assertEquals("java.lang.String", parsed.className);
    assertEquals("CASE_INSENSITIVE_ORDER", parsed.memberName);
  }

  @Test
  public void testParseSignatureMethod() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature(
            "java.lang.String java.lang.String substring(int, int)");
    assertTrue(parsed.isMethodOrConstructor);
    assertFalse(parsed.isConstructor);
    assertEquals("substring", parsed.memberName);
    assertEquals(Arrays.asList("int", "int"), parsed.paramTypes);
    assertEquals(-1, parsed.paramIndex);
  }

  @Test
  public void testParseSignatureMethodParameter() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature(
            "java.lang.String java.lang.String concat(java.lang.String) 0");
    assertTrue(parsed.isMethodOrConstructor);
    assertEquals(0, parsed.paramIndex);
  }

  @Test
  public void testParseSignatureConstructor() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature("java.lang.String java.lang.String(byte[], int)");
    assertTrue(parsed.isConstructor);
    assertEquals("String", parsed.memberName);
    assertEquals(Arrays.asList("byte[]", "int"), parsed.paramTypes);
  }

  @Test
  public void testParseSignatureMalformed() {
    // The closing parenthesis has no matching opening parenthesis.
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature("java.lang.String substring int, int)");
    assertTrue(parsed.isMalformed());
  }

  @Test
  public void testStripQuotes() {
    assertEquals("abc", IntelliJAnnotationParser.stripQuotes("\"abc\""));
    assertEquals("abc", IntelliJAnnotationParser.stripQuotes("  \"abc\"  "));
    assertEquals("abc", IntelliJAnnotationParser.stripQuotes("'abc'"));
    assertEquals(
        "java.lang.String.class", IntelliJAnnotationParser.stripQuotes("java.lang.String.class"));
  }

  @Test
  public void testStripQuotesEscapes() {
    assertEquals("a\nb", IntelliJAnnotationParser.stripQuotes("\"a\\nb\""));
    assertEquals("a\tb", IntelliJAnnotationParser.stripQuotes("\"a\\tb\""));
    assertEquals("a\\b", IntelliJAnnotationParser.stripQuotes("\"a\\\\b\""));
    assertEquals("a\"b", IntelliJAnnotationParser.stripQuotes("\"a\\\"b\""));
    assertEquals("a'b", IntelliJAnnotationParser.stripQuotes("\"a\\'b\""));
    assertEquals("aAb", IntelliJAnnotationParser.stripQuotes("\"a\\u0041b\""));
    assertEquals("aAb", IntelliJAnnotationParser.stripQuotes("\"a\\uuu0041b\""));
    assertEquals("a\0b", IntelliJAnnotationParser.stripQuotes("\"a\\0b\""));
    assertEquals("a!b", IntelliJAnnotationParser.stripQuotes("\"a\\041b\""));
    // A string that ends with a backslash.
    assertEquals("a\\", IntelliJAnnotationParser.stripQuotes("\"a\\\\\""));
    // An unterminated string literal is left alone.
    assertEquals("\"a\\\"", IntelliJAnnotationParser.stripQuotes("\"a\\\""));
  }

  @Test
  public void testParseBoolean() {
    assertEquals(Boolean.TRUE, IntelliJAnnotationParser.parseBoolean("true"));
    assertEquals(Boolean.TRUE, IntelliJAnnotationParser.parseBoolean("TRUE"));
    assertEquals(Boolean.FALSE, IntelliJAnnotationParser.parseBoolean("false"));
    // A string that is not a boolean literal is not silently treated as false.
    assertNull(IntelliJAnnotationParser.parseBoolean("ture"));
    assertNull(IntelliJAnnotationParser.parseBoolean("1"));
    assertNull(IntelliJAnnotationParser.parseBoolean("yes"));
    assertNull(IntelliJAnnotationParser.parseBoolean(""));
  }

  @Test
  public void testParseArrayLiteral() {
    assertEquals(Collections.emptyList(), IntelliJAnnotationParser.parseArrayLiteral("{}"));
    assertEquals(
        Arrays.asList("\"a\"", "\"b\""),
        IntelliJAnnotationParser.parseArrayLiteral("{\"a\", \"b\"}"));
    // A comma within a string literal does not separate array elements.
    assertEquals(
        Arrays.asList("\"a,b\"", "\"c\""),
        IntelliJAnnotationParser.parseArrayLiteral("{\"a,b\", \"c\"}"));
    // A single value need not be surrounded by braces.
    assertEquals(Arrays.asList("\"a\""), IntelliJAnnotationParser.parseArrayLiteral("\"a\""));
  }
}
