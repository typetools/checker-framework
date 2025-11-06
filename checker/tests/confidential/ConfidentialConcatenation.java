// NonConfidential <: Confidential

import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;

public class ConfidentialConcatenation {

  void executeNonConfidential(@NonConfidential String s) {}

  void executeConfidential(@Confidential String s) {}

  void concatenation(@NonConfidential String s1, @Confidential String s2) {
    @Confidential String s_1 = s1 + s1;
    @Confidential String s_2 = s1 + s2;
    @Confidential String s_3 = s2 + s1;
    @Confidential String s_4 = s2 + s2;

    @NonConfidential String s_5 = s1 + s1;
    // :: error: (assignment)
    @NonConfidential String s_6 = s1 + s2;
    // :: error: (assignment)
    @NonConfidential String s_7 = s2 + s1;
    // :: error: (assignment)
    @NonConfidential String s_8 = s2 + s2;
  }

  void concatenationInvocation(@NonConfidential String s1, @Confidential String s2) {
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

  void compoundConcatenation(@NonConfidential String s1, @Confidential String s2) {
    s1 += s1;
    // :: error: (compound.assignment)
    s1 += s2;

    s2 += s2;
    s2 += s1;
  }
}
