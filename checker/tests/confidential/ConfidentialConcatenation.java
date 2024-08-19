import org.checkerframework.checker.confidential.qual.NonConfidential;

public class ConfidentialConcatenation {

  void executeNonConfidential(@NonConfidential String s) {}

  void executeConfidential(String s) {}

  void concatenation(@NonConfidential String s1, String s2) {
    executeNonConfidential(s1 + s1);
    // :: error: (argument)
    executeNonConfidential(s1 + s2);
    // :: error: (argument)
    executeNonConfidential(s2 + s1);
    // :: error: (argument)
    executeNonConfidential(s2 + s2);

    executeConfidential(s1 + s1);
    executeConfidential(s1 + s2);
    executeConfidential(s2 + s1);
    executeConfidential(s2 + s2);
  }

  void compoundConcatenation(@NonConfidential String s1, String s2) {
    s1 += s1;
    // :: error: (compound.assignment)
    s1 += s2;

    s2 += s2;
    s2 += s1;
  }
}
