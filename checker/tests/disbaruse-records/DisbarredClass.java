import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUse;

class DisbarredClass {

  @DisbarUse String barred;
  String fine;

  DisbarredClass() {}

  @DisbarUse
  DisbarredClass(String param) {}

  @DisbarUse
  void disbarredMethod() {}

  void invalid() {
    // :: error: (disbar.use)
    disbarredMethod();
    // :: error: (disbar.use)
    new DisbarredClass("");
    // :: error: (disbar.use)
    barred = "";
    // :: error: (disbar.use)
    int x = barred.length();
  }

  void valid() {
    new DisbarredClass();
    fine = "";
    int x = fine.length();
  }
}
