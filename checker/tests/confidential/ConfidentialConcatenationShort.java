// TEMPORARY TEST FILE; remove before merging.

// NonConfidential <: Confidential

import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;

public class ConfidentialConcatenationShort {

  @Confidential String confidentialField;

  void concatenation(@NonConfidential String s1, String s2) {
    confidentialField = s1 + s1;
  }
}
