import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;

public class SimpleConfidential {

  void executeNonConfidential(@NonConfidential String s) {}

  void executeConfidential(@Confidential String s) {}

  void nonConfidentialRef(@NonConfidential String s) {
    executeNonConfidential(s);
    executeConfidential(s);
  }

  void confidentialRef(@Confidential String s) {
    // :: error: (argument)
    executeNonConfidential(s);
    executeConfidential(s);
  }
}
