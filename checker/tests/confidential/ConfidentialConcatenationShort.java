// NonConfidential <: Confidential

import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;

public class ConfidentialConcatenationShort {

  void concatenation(@NonConfidential String s1, String s2) {
    @Confidential String s_1 = s1 + s1;
  }
}
