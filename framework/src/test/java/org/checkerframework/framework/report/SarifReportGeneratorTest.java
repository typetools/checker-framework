package org.checkerframework.framework.report;

import javax.tools.Diagnostic;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link SarifReportGenerator}. */
public class SarifReportGeneratorTest {

  /** Creates a new SarifReportGeneratorTest. */
  public SarifReportGeneratorTest() {}

  /**
   * Returns a SarifReportGenerator. Its processing environment is null, so the returned generator
   * supports only the {@code addResult} overload that takes a file URI and a region.
   *
   * @return a SarifReportGenerator
   */
  @SuppressWarnings("nullness:argument") // the processing environment is not used by these tests
  private static SarifReportGenerator newGenerator() {
    return new SarifReportGenerator(null);
  }

  /** Tests a report with no results. */
  @Test
  public void testNoResults() {
    String expected =
        String.join(
            "\n",
            "{",
            "  \"version\": \"2.1.0\",",
            "  \"runs\": [",
            "    {",
            "      \"tool\": {",
            "        \"driver\": {",
            "          \"name\": \"Checker Framework\",",
            "          \"version\": \"1.2.3\"",
            "        }",
            "      },",
            "      \"results\": []",
            "    }",
            "  ]",
            "}",
            "");
    Assert.assertEquals(expected, newGenerator().toJson("1.2.3"));
  }

  /**
   * Tests a report with an error, a warning, and a note (which is omitted), including characters
   * that JSON requires to be escaped.
   */
  @Test
  public void testResults() {
    SarifReportGenerator generator = newGenerator();
    generator.addResult(
        Diagnostic.Kind.ERROR,
        "[return] incompatible types in return.\ntype of expression: null (NullType)",
        "return",
        "file:///home/me/Foo.java",
        9,
        12,
        9,
        16);
    generator.addResult(
        Diagnostic.Kind.NOTE, "a note", "note.key", "file:///home/me/Foo.java", 1, 1, 1, 1);
    generator.addResult(
        Diagnostic.Kind.MANDATORY_WARNING,
        "quote \" backslash \\ tab \t return \r control \u0001 non-ASCII \u00e9",
        "some.warning",
        "file:///C:/dir%20with%20space/Bar.java",
        3,
        5,
        4,
        6);
    String expected =
        String.join(
            "\n",
            "{",
            "  \"version\": \"2.1.0\",",
            "  \"runs\": [",
            "    {",
            "      \"tool\": {",
            "        \"driver\": {",
            "          \"name\": \"Checker Framework\",",
            "          \"version\": \"Unknown\"",
            "        }",
            "      },",
            "      \"results\": [",
            "        {",
            "          \"ruleId\": \"return\",",
            "          \"level\": \"error\",",
            "          \"message\": {",
            "            \"text\": \"[return] incompatible types in return.\\ntype of expression:"
                + " null (NullType)\"",
            "          },",
            "          \"locations\": [",
            "            {",
            "              \"physicalLocation\": {",
            "                \"artifactLocation\": {",
            "                  \"uri\": \"file:///home/me/Foo.java\"",
            "                },",
            "                \"region\": {",
            "                  \"startLine\": 9,",
            "                  \"startColumn\": 12,",
            "                  \"endLine\": 9,",
            "                  \"endColumn\": 16",
            "                }",
            "              }",
            "            }",
            "          ]",
            "        },",
            "        {",
            "          \"ruleId\": \"some.warning\",",
            "          \"level\": \"warning\",",
            "          \"message\": {",
            "            \"text\": \"quote \\\" backslash \\\\ tab \\t return \\r control \\u0001"
                + " non-ASCII \u00e9\"",
            "          },",
            "          \"locations\": [",
            "            {",
            "              \"physicalLocation\": {",
            "                \"artifactLocation\": {",
            "                  \"uri\": \"file:///C:/dir%20with%20space/Bar.java\"",
            "                },",
            "                \"region\": {",
            "                  \"startLine\": 3,",
            "                  \"startColumn\": 5,",
            "                  \"endLine\": 4,",
            "                  \"endColumn\": 6",
            "                }",
            "              }",
            "            }",
            "          ]",
            "        }",
            "      ]",
            "    }",
            "  ]",
            "}",
            "");
    Assert.assertEquals(expected, generator.toJson("Unknown"));
  }
}
