import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUse;

record DisbarredRecord(@DisbarUse String barred, String fine) {

  DisbarredRecord {
    // :: error: (disbar.use)
    int x = barred.length();
  }

  void invalid() {
    // :: error: (disbar.use)
    barred();
    // :: error: (disbar.use)
    int x = barred.length();
  }

  void valid() {
    fine();
    int x = fine.length();
  }
}
