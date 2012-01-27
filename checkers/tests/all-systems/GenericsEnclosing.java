import java.util.TreeMap;

/** Resolution of outer classes must take substitution of generic types
 * into account.
 * Thanks to EMS for finding this problem.
 *
 * Also see regex/GenericsEnclosing for a test case for the Regex Checker.
 */
class GenericsEnclosing extends TreeMap<String, String> {
    class Inner {
        void foo() {
            put("string", "string".toString());
            put("string", "string");
            GenericsEnclosing.this.put("string", "string".toString());
            GenericsEnclosing.this.put("string", "string");
        }
    }
}

class OtherUse {
    void m(GenericsEnclosing p) {
        p.put("string", "string".toString());
        p.put("string", "string");
    }
}