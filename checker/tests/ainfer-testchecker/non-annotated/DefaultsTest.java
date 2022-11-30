import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferDefaultType;

// The @AinferDefaultType annotation, which is the default for every location, is forbidden to be written
// anywhere. This class attempts to infer @AinferDefaultType in several locations, and the annotated
// version of this class (in the annotated folder) should have no explicit @AinferDefaultType annotations.
public class DefaultsTest {
  String defaultField = "";
  String defaultField2;

  void test() {
    @SuppressWarnings("all") // To allow the use of the explicit @AinferDefaultType.
    @AinferDefaultType String explicitDefault = "";
    defaultField2 = explicitDefault;
  }

  // This method's return type should not be updated by the whole-program inference
  // since it is the default.
  String lubTest() {
    if (Math.random() > 0.5) {
      return ""; // @AinferDefaultType
    } else {
      @SuppressWarnings("cast.unsafe")
      @AinferBottom String s = (@AinferBottom String) "";
      return s;
    }
  }
}
