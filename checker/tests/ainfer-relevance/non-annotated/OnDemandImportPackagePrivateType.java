import irrelevantpkg.*;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import relevantpkg.*;

// An import on demand imports only the types that are accessible where it appears, and a
// package-private type is accessible only within the package that declares it.  This compilation
// unit is in the unnamed package, so `import irrelevantpkg.*;` does not import
// `irrelevantpkg.Region`.  An inaccessible type does not resolve the name, and it does not prevent
// a later import on demand from resolving it.
//
// Inference must therefore resolve the name "Region" to `relevantpkg.Region`, which is relevant
// because it is a subtype of `CharSequence`, rather than to `irrelevantpkg.Region`, which is
// irrelevant.  If inference resolves the name incorrectly and therefore discards the annotation,
// then the second (validation) pass of this test issues the warning that is written below.
public class OnDemandImportPackagePrivateType {

  Region regionField;

  void assignField() {
    regionField = getSibling1Region();
  }

  void useField() {
    // :: warning: [argument]
    expectsSibling1Region(regionField);
  }

  void expectsSibling1Region(@AinferSibling1 Region r) {}

  @AinferSibling1 Region getSibling1Region() {
    return null;
  }
}
