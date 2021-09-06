import org.checkerframework.checker.testchecker.disbaruse.qual.DisbarUse;

class DisbarredClass {

    @DisbarUse String barred;
    String fine;

    DisbarredClass() {}

    @DisbarUse
    DisbarredClass(String param) {}

    DisbarredClass(@DisbarUse Integer param) {}

    DisbarredClass(@DisbarUse Long param) {
        // :: error: (disbar.use)
        param = 7L;
        // :: error: (disbar.use)
        param.toString();
    }

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
