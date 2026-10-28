import java.nio.CharBuffer;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import otherpkg.PackagePrivateMemberTypes;

// A subclass does not inherit a package-private member type of a superclass that is declared in a
// different package, so such a member type is not in scope in the subclass.  Inference must resolve
// the name
// "CharBuffer" to the single-type import `java.nio.CharBuffer`, which is relevant because it is a
// subtype of `CharSequence`, rather than to `otherpkg.PackagePrivateMemberTypes.CharBuffer`, which
// is package-private, is not inherited, and is irrelevant.
//
// By contrast, a protected member type is inherited even from a different package, so inference
// must resolve the name "Protected" to `otherpkg.PackagePrivateMemberTypes.Protected`, which is
// relevant.
//
// If inference resolves either name incorrectly and therefore discards the annotation, then the
// second (validation) pass of this test issues the warnings that are written below.
public class CrossPackageMemberTypes extends PackagePrivateMemberTypes {

  CharBuffer charBufferField;

  Protected protectedField;

  void assignFields() {
    charBufferField = getSibling1CharBuffer();
    protectedField = getSibling1Protected();
  }

  void useFields() {
    // :: warning: [argument]
    expectsSibling1CharBuffer(charBufferField);
    // :: warning: [argument]
    expectsSibling1Protected(protectedField);
  }

  void expectsSibling1CharBuffer(@AinferSibling1 CharBuffer b) {}

  void expectsSibling1Protected(@AinferSibling1 Protected p) {}

  @AinferSibling1 CharBuffer getSibling1CharBuffer() {
    return null;
  }

  @AinferSibling1 Protected getSibling1Protected() {
    return null;
  }
}
