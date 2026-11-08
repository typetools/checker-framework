package org.checkerframework.framework.stub;

import java.util.Arrays;
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
}
