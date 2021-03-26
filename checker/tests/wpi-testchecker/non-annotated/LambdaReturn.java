// This test checks that WPI doesn't try to infer a return type for lambda expressions.
// This specific example came up in a case study.

import java.io.FileFilter;

public class LambdaReturn {
  void test() {
    FileFilter docxFilter =
        pathname -> {
          // We only want to process *.docx files, everything else can be skipped.
          if (pathname.isFile() && pathname.getName().matches(".*\\.docx")) {
            return true;
          }

          return false;
        };
  }
}
