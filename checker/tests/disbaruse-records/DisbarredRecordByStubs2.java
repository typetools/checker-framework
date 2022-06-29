record DisbarredRecordByStubs2(String barred, String fine) {

  // Annotation isn't copied to explicitly declared constructor parameters:
  DisbarredRecordByStubs2(String barred, String fine) {
    int x = barred.length();
    // :: error: (disbar.use)
    this.barred = "";
    this.fine = fine;
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
