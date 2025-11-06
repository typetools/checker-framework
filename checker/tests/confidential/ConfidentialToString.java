import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;

public class ConfidentialToString {
  void confObj(@Confidential Object confObj) {
    // :: error: (assignment)
    @NonConfidential String nonConfRes = confObj.toString();
    @Confidential String confRes = confObj.toString();
  }

  void nonConfObj(@NonConfidential Object nonConfObj) {
    @NonConfidential String nonConfRes = nonConfObj.toString();
  }
}
