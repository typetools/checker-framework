// Tests that assigning to a @Confidential location is always permitted, but that the type
// arguments of the assigned value are still checked.

import java.util.List;
import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;
import org.checkerframework.checker.confidential.qual.UnknownConfidential;

public class ConfidentialAssignment {

  /** A container whose type parameter permits a confidential type argument. */
  static class Box<T extends @UnknownConfidential Object> {
    T get() {
      throw new AssertionError("not called");
    }
  }

  void takesConfidential(@Confidential String s) {}

  void primaryQualifierIsRelaxed(
      @NonConfidential String nc, @Confidential String c, @UnknownConfidential String u) {
    // Anything may be assigned to a @Confidential location.
    @Confidential String s1 = nc;
    @Confidential String s2 = c;
    @Confidential String s3 = u;

    takesConfidential(nc);
    takesConfidential(c);
    takesConfidential(u);
  }

  void typeArgumentsAreStillChecked(
      @NonConfidential Box<@NonConfidential String> ncBox,
      @NonConfidential Box<@Confidential String> cBox) {
    // The primary qualifier is relaxed, so these are permitted.
    @Confidential Box<@NonConfidential String> ok1 = ncBox;
    @Confidential Box<@Confidential String> ok2 = cBox;

    // The type arguments are invariant, so these are errors even though the variable's primary
    // qualifier is @Confidential.  Relaxing the primary qualifier must not relax the arguments.
    // :: error: [assignment]
    @Confidential Box<@Confidential String> bad1 = ncBox;
    // :: error: [assignment]
    @Confidential Box<@NonConfidential String> bad2 = cBox;
  }

  void nonConfidentialIsNotRelaxed(@Confidential String c) {
    // :: error: [assignment]
    @NonConfidential String nc = c;
  }

  /**
   * Documents a current limitation: because {@code @NonConfidential} is the default for upper
   * bounds, an unannotated generic class such as {@code java.util.List} cannot hold a confidential
   * type argument.
   */
  void confidentialTypeArgumentInUnannotatedGeneric() {
    // :: error: [type.argument]
    List<@Confidential String> passwords = null;
  }
}
