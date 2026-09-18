package otherpkg;

import java.nio.CharBuffer;
import middlepkg.Middle;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A package-private member type is inherited only by a subclass in the package that declares it,
// so whether a class inherits one depends on the package of every class between the two, not only
// on the package that contains the use of its name.  `PackagePrivateMemberTypes.CharBuffer` is
// package-private, so `middlepkg.Middle` does not inherit it, and therefore this class does not
// inherit it either -- even though this class is in the package that declares it.
//
// Inference must resolve the name "CharBuffer" to the single-type import `java.nio.CharBuffer`,
// which is relevant because it is a subtype of `CharSequence`, rather than to
// `PackagePrivateMemberTypes.CharBuffer`, which is irrelevant.  Inference must therefore write the
// annotation on `charBufferField`, as the goal file shows.  If inference instead resolves the name
// to `PackagePrivateMemberTypes.CharBuffer`, then it discards the annotation.
//
// The second (validation) pass of this test does not detect a discarded annotation here, because
// the test harness passes to that pass only the ajava files that inference wrote into the
// top-level output directory, and inference writes the ajava file for a class in a named package
// into a subdirectory.  The goal file is therefore this test's only assertion.
public class NotInheritedThroughOtherPackage extends Middle {

  CharBuffer charBufferField;

  void assignField() {
    charBufferField = getSibling1CharBuffer();
  }

  @AinferSibling1 CharBuffer getSibling1CharBuffer() {
    return null;
  }
}
