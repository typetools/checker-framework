// @below-java21-jdk-skip-test

// The CFG wraps the selector of a switch without a `case null` label in a null check.

import java.io.*;

class SwitchSelectorNullCheck {

  static InputStream get() throws IOException {
    return new FileInputStream("x");
  }

  void leakInDefault() throws IOException {
    // :: error: [required.method.not.called]
    switch (get()) {
      case FileInputStream f -> f.close();
      default -> {}
    }
  }

  void closeAfter() throws IOException {
    InputStream is = get();
    switch (is) {
      case FileInputStream f -> {}
      default -> {}
    }
    is.close();
  }
}
