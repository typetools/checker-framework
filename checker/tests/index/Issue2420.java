import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

public class Issue2420 {
  static void str(String argStr) {
    if (argStr.isEmpty()) {
      return;
    }
    if (argStr == "abc") {
      return;
    }
    // :: error: (argument.type.incompatible)
    char c = "abc".charAt(argStr.length() - 1);
    // :: error: (argument.type.incompatible)
    char c2 = "abc".charAt(argStr.length());
  }
}
