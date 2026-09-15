// Tests of @UnknownConfidential, the top qualifier, and of concatenation involving it.

import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;
import org.checkerframework.checker.confidential.qual.UnknownConfidential;

public class ConfidentialHierarchy {

  void executeNonConfidential(@NonConfidential String s) {}

  void executeConfidential(@Confidential String s) {}

  void executeUnknown(@UnknownConfidential String s) {}

  void topIsSupertypeOfBoth(@NonConfidential String nc, @Confidential String c) {
    @UnknownConfidential String u1 = nc;
    @UnknownConfidential String u2 = c;

    executeUnknown(nc);
    executeUnknown(c);
  }

  void topIsNotSubtype(@UnknownConfidential String u) {
    // :: error: [assignment]
    @NonConfidential String nc = u;

    // :: error: [argument]
    executeNonConfidential(u);

    // Assignment to @Confidential is always permitted.
    @Confidential String c = u;
    executeConfidential(u);
  }

  void concatenationWithUnknown(
      @NonConfidential String nc, @Confidential String c, @UnknownConfidential String u) {
    // @Confidential dominates, even over @UnknownConfidential.
    @Confidential String r1 = u + c;
    @Confidential String r2 = c + u;

    // Otherwise the result is the least upper bound, which is @UnknownConfidential.
    @UnknownConfidential String r3 = u + nc;
    @UnknownConfidential String r4 = nc + u;
    @UnknownConfidential String r5 = u + u;

    // :: error: [assignment]
    @NonConfidential String r6 = u + nc;
    // :: error: [assignment]
    @NonConfidential String r7 = u + u;
  }
}
