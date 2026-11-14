// Tests of @PolyConfidential.

import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;
import org.checkerframework.checker.confidential.qual.PolyConfidential;

public class ConfidentialPoly {

  @PolyConfidential
  String identity(@PolyConfidential String s) {
    return s;
  }

  @PolyConfidential
  String firstOf(@PolyConfidential String s1, @PolyConfidential String s2) {
    return s1;
  }

  void executeNonConfidential(@NonConfidential String s) {}

  void executeConfidential(@Confidential String s) {}

  void polyInstantiation(@NonConfidential String nc, @Confidential String c) {
    @NonConfidential String r1 = identity(nc);
    @Confidential String r2 = identity(c);

    // A @Confidential argument instantiates the polymorphic qualifier to @Confidential, so the
    // result may not be assigned to a @NonConfidential variable.
    // :: error: [assignment]
    @NonConfidential String r3 = identity(c);

    // Assigning a @NonConfidential result to a @Confidential variable is always permitted.
    @Confidential String r4 = identity(nc);
  }

  void polyAtCallSite(@NonConfidential String nc, @Confidential String c) {
    executeNonConfidential(identity(nc));
    // :: error: [argument]
    executeNonConfidential(identity(c));

    executeConfidential(identity(nc));
    executeConfidential(identity(c));
  }

  void polyMultipleArguments(@NonConfidential String nc, @Confidential String c) {
    executeNonConfidential(firstOf(nc, nc));

    // When any argument is @Confidential, the polymorphic qualifier resolves to @Confidential.
    // :: error: [argument]
    executeNonConfidential(firstOf(nc, c));
    // :: error: [argument]
    executeNonConfidential(firstOf(c, nc));
    // :: error: [argument]
    executeNonConfidential(firstOf(c, c));

    executeConfidential(firstOf(nc, c));
  }
}
