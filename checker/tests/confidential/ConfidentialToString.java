import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;

public ConfidentialToString {
  void confParam(@Confidential Object confObj) {
    // :: error: (assignment)
    @NonConfidential String nonConfRes = confObj.toString();
    @Confidential String confRes = confObj.toString();
  }

  void nonConfParam(@NonConfidential Object nonConfObj) {
    @NonConfidential String nonConfRes = nonConfObj.toString();
  }
}
