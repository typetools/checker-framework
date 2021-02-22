public class Issue607 extends Issue607SuperClass {
    static String simpleString = "a";

    Issue607() {
        super(Issue607SuperClass.issue, string -> simpleString);
    }
}
