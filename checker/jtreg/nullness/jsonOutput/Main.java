/*
 * @test
 * @summary Check the -AjsonOutput command-line argument
 *
 * @compile/fail/timeout=60 -AjsonOutput=actual.json -processor org.checkerframework.checker.nullness.NullnessChecker JsonOutputTestCase.java Main.java
 * @run main Main
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        assertFileContents("actual.json");
    }

    public static void assertFileContents(String file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            for (String expected : expectedLines) {
                String actual = reader.readLine();
                if (actual == null) {
                    throw new Error("Actual file ended, expected " + expected);
                }
                if (!expected.equals(actual)) {
                    throw new Error(
                            "Expected: "
                                    + expected
                                    + System.lineSeparator()
                                    + "Actual  : "
                                    + actual);
                }
            }
            String actual = reader.readLine();
            if (actual != null) {
                throw new Error("Extra content: " + actual);
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    // A jtreg test should not depend on other files, because there is no guarantee where it will be
    // run.
    static String[] expectedLines =
            new String[] {
                "[",
                "  {",
                "    \"uri\": \"file:///home/mernst/research/types/checker-framework-fork-mernst-branch-json-output-2/checker/jtreg/nullness/jsonOutput/JsonOutputTestCase.java\",",
                "    \"diagnostics\": [",
                "      {",
                "        \"range\": {",
                "          \"start\": {",
                "            \"line\": 13,",
                "            \"character\": 15",
                "          },",
                "          \"end\": {",
                "            \"line\": 13,",
                "            \"character\": 15",
                "          }",
                "        },",
                "        \"severity\": 1,",
                "        \"code\": \"argument.type.incompatible\",",
                "        \"source\": \"Nullness Checker\",",
                "        \"message\": \"[argument.type.incompatible] incompatible types in argument.\\nfound   : @Initialized @Nullable InputStream\\nrequired: @Initialized @NonNull InputStream\"",
                "      }",
                "    ]",
                "  }",
                "]"
            };
}
