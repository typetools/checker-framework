// @below-java17-jdk-skip-test
// @infer-jaifs-skip-test The AFU's JAIF reading/writing libraries don't support records.
// @infer-ajava-skip-test The ajava backend stores inferences per source declaration, and a
// record's fields and canonical constructor have no declaration in the source code.

// Check that types can be inferred for a nested record: for the formal parameters of its
// canonical constructor, and for its components.
//
// A record component and the corresponding formal parameter of the canonical constructor must be
// given the same annotation, because the canonical constructor assigns the parameter to the field
// that the component declares.  See SceneToStubWriter.recordComponentAnnos.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

public class RecordTest {

  @SuppressWarnings("cast.unsafe")
  static @AinferSibling1 String getAinferSibling1() {
    return (@AinferSibling1 String) "foo";
  }

  record ConstructorRecord(int component) {}

  void testConstructorParameter() {
    @AinferTop int top = (@AinferTop int) 0;
    // :: warning: [argument]
    new ConstructorRecord(top);
  }

  record ComponentRecord(String component) {
    ComponentRecord(String component) {
      this.component = getAinferSibling1();
    }

    static void requireAinferSibling1(@AinferSibling1 String x) {}

    void testComponent() {
      // The inferred annotation on the component is written in the record header, which is
      // where a record's fields are declared.
      // :: warning: [argument]
      requireAinferSibling1(this.component);
    }
  }
}
