package disbaruse.records;

// The @DisbarUse annotation on the "barred" record component is written in disbarnested.astub.
class DisbarredNestedRecordByStubs {

  record Nested(String barred, String fine) {

    Nested {
      // :: error: [disbar.use]
      int x = barred.length();
    }

    void invalid() {
      // :: error: [disbar.use]
      barred();
      // :: error: [disbar.use]
      int x = barred.length();
    }

    void valid() {
      fine();
      int x = fine.length();
    }
  }
}
