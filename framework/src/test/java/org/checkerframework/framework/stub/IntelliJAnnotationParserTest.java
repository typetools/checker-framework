package org.checkerframework.framework.stub;

import java.util.Arrays;
import java.util.Collections;
import org.checkerframework.framework.stub.IntelliJAnnotationParser.ParsedItemSignature;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for the string-manipulation routines of {@link IntelliJAnnotationParser}. */
public class IntelliJAnnotationParserTest {

  @Test
  public void testParseSignatureClass() {
    ParsedItemSignature parsed = IntelliJAnnotationParser.parseSignature("java.lang.String");
    Assert.assertTrue(parsed.isClass);
    Assert.assertFalse(parsed.isMalformed());
    Assert.assertEquals("java.lang.String", parsed.className);
  }

  @Test
  public void testParseSignatureField() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature(
            "java.lang.String java.util.Comparator CASE_INSENSITIVE_ORDER");
    Assert.assertTrue(parsed.isField);
    Assert.assertEquals("java.lang.String", parsed.className);
    Assert.assertEquals("CASE_INSENSITIVE_ORDER", parsed.memberName);
  }

  @Test
  public void testParseSignatureMethod() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature(
            "java.lang.String java.lang.String substring(int, int)");
    Assert.assertTrue(parsed.isMethodOrConstructor);
    Assert.assertFalse(parsed.isConstructor);
    Assert.assertEquals("substring", parsed.memberName);
    Assert.assertEquals(Arrays.asList("int", "int"), parsed.paramTypes);
    Assert.assertEquals(-1, parsed.paramIndex);
  }

  @Test
  public void testParseSignatureMethodParameter() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature(
            "java.lang.String java.lang.String concat(java.lang.String) 0");
    Assert.assertTrue(parsed.isMethodOrConstructor);
    Assert.assertEquals(0, parsed.paramIndex);
  }

  @Test
  public void testParseSignatureConstructor() {
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature("java.lang.String java.lang.String(byte[], int)");
    Assert.assertTrue(parsed.isConstructor);
    Assert.assertEquals("String", parsed.memberName);
    Assert.assertEquals(Arrays.asList("byte[]", "int"), parsed.paramTypes);
  }

  @Test
  public void testParseSignatureMalformed() {
    // The closing parenthesis has no matching opening parenthesis.
    ParsedItemSignature parsed =
        IntelliJAnnotationParser.parseSignature("java.lang.String substring int, int)");
    Assert.assertTrue(parsed.isMalformed());
  }

  @Test
  public void testParseSignatureBadParameterIndex() {
    // Only a parameter index may follow the parameter list.  A non-numeric or negative trailer
    // is not silently treated as naming the return type.
    Assert.assertTrue(
        IntelliJAnnotationParser.parseSignature(
                "java.lang.String java.lang.String concat(java.lang.String) bogus")
            .isMalformed());
    Assert.assertTrue(
        IntelliJAnnotationParser.parseSignature(
                "java.lang.String java.lang.String concat(java.lang.String) -1")
            .isMalformed());
    Assert.assertTrue(
        IntelliJAnnotationParser.parseSignature(
                "java.lang.String java.lang.String concat(java.lang.String) 0 1")
            .isMalformed());
  }

  @Test
  public void testParseChar() {
    Assert.assertEquals(Character.valueOf('a'), IntelliJAnnotationParser.parseChar("a"));
    Assert.assertEquals(Character.valueOf('\n'), IntelliJAnnotationParser.parseChar("\\n"));
    Assert.assertEquals(Character.valueOf('\''), IntelliJAnnotationParser.parseChar("\\'"));
    Assert.assertEquals(Character.valueOf('\\'), IntelliJAnnotationParser.parseChar("\\\\"));
    // stripQuotes has already interpreted the escape sequence in a quoted value such as '\\'.
    Assert.assertEquals(Character.valueOf('\\'), IntelliJAnnotationParser.parseChar("\\"));
    Assert.assertEquals(Character.valueOf(' '), IntelliJAnnotationParser.parseChar("\\s"));
    // Unicode escapes, which may contain more than one 'u'.
    Assert.assertEquals(Character.valueOf('A'), IntelliJAnnotationParser.parseChar("\\u0041"));
    Assert.assertEquals(Character.valueOf('A'), IntelliJAnnotationParser.parseChar("\\uuu0041"));
    // Octal escapes.
    Assert.assertEquals(Character.valueOf('\0'), IntelliJAnnotationParser.parseChar("\\0"));
    Assert.assertEquals(Character.valueOf('!'), IntelliJAnnotationParser.parseChar("\\041"));
    Assert.assertEquals(Character.valueOf('\u00ff'), IntelliJAnnotationParser.parseChar("\\377"));
  }

  @Test
  public void testParseCharMalformed() {
    // A value that is not a char literal is not silently treated as some char.
    Assert.assertNull(IntelliJAnnotationParser.parseChar(""));
    Assert.assertNull(IntelliJAnnotationParser.parseChar("ab"));
    // Not a Java escape sequence.
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\q"));
    // Malformed unicode escapes.
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\u"));
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\u041"));
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\u004g"));
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\u00041"));
    // Malformed octal escapes.
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\400"));
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\0000"));
    Assert.assertNull(IntelliJAnnotationParser.parseChar("\\08"));
  }

  @Test
  public void testStripQuotes() {
    Assert.assertEquals("abc", IntelliJAnnotationParser.stripQuotes("\"abc\""));
    Assert.assertEquals("abc", IntelliJAnnotationParser.stripQuotes("  \"abc\"  "));
    Assert.assertEquals("abc", IntelliJAnnotationParser.stripQuotes("'abc'"));
    Assert.assertEquals(
        "java.lang.String.class", IntelliJAnnotationParser.stripQuotes("java.lang.String.class"));
  }

  @Test
  public void testStripQuotesEscapes() {
    Assert.assertEquals("a\nb", IntelliJAnnotationParser.stripQuotes("\"a\\nb\""));
    Assert.assertEquals("a\tb", IntelliJAnnotationParser.stripQuotes("\"a\\tb\""));
    Assert.assertEquals("a\\b", IntelliJAnnotationParser.stripQuotes("\"a\\\\b\""));
    Assert.assertEquals("a\"b", IntelliJAnnotationParser.stripQuotes("\"a\\\"b\""));
    Assert.assertEquals("a'b", IntelliJAnnotationParser.stripQuotes("\"a\\'b\""));
    Assert.assertEquals("aAb", IntelliJAnnotationParser.stripQuotes("\"a\\u0041b\""));
    Assert.assertEquals("aAb", IntelliJAnnotationParser.stripQuotes("\"a\\uuu0041b\""));
    Assert.assertEquals("a\0b", IntelliJAnnotationParser.stripQuotes("\"a\\0b\""));
    Assert.assertEquals("a!b", IntelliJAnnotationParser.stripQuotes("\"a\\041b\""));
    // A string that ends with a backslash.
    Assert.assertEquals("a\\", IntelliJAnnotationParser.stripQuotes("\"a\\\\\""));
    // An unterminated string literal is left alone.
    Assert.assertEquals("\"a\\\"", IntelliJAnnotationParser.stripQuotes("\"a\\\""));
  }

  @Test
  public void testParseBoolean() {
    Assert.assertEquals(Boolean.TRUE, IntelliJAnnotationParser.parseBoolean("true"));
    Assert.assertEquals(Boolean.TRUE, IntelliJAnnotationParser.parseBoolean("TRUE"));
    Assert.assertEquals(Boolean.FALSE, IntelliJAnnotationParser.parseBoolean("false"));
    // A string that is not a boolean literal is not silently treated as false.
    Assert.assertNull(IntelliJAnnotationParser.parseBoolean("ture"));
    Assert.assertNull(IntelliJAnnotationParser.parseBoolean("1"));
    Assert.assertNull(IntelliJAnnotationParser.parseBoolean("yes"));
    Assert.assertNull(IntelliJAnnotationParser.parseBoolean(""));
  }

  @Test
  public void testParseArrayLiteral() {
    Assert.assertEquals(Collections.emptyList(), IntelliJAnnotationParser.parseArrayLiteral("{}"));
    Assert.assertEquals(
        Arrays.asList("\"a\"", "\"b\""),
        IntelliJAnnotationParser.parseArrayLiteral("{\"a\", \"b\"}"));
    // A comma within a string literal does not separate array elements.
    Assert.assertEquals(
        Arrays.asList("\"a,b\"", "\"c\""),
        IntelliJAnnotationParser.parseArrayLiteral("{\"a,b\", \"c\"}"));
    // A single value need not be surrounded by braces.
    Assert.assertEquals(
        Arrays.asList("\"a\""), IntelliJAnnotationParser.parseArrayLiteral("\"a\""));
  }
}
