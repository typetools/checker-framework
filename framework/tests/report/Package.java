import java.util.regex.*;

// File reporttest.astub contains an annotation on
// the java.util.regex package.

// :: error: (usage)
class Package extends PatternSyntaxException {
    public Package(String desc, String regex, int index) {
        // :: error: (usage)
        super(desc, regex, index);
    }

    @Override
    @org.checkerframework.dataflow.qual.Pure
    public String getPattern() {
        // :: error: (usage)
        return super.getPattern();
    }

    // :: error: (usage)
    void m(Pattern p) {
        // Access to a constant.
        // :: error: (usage)
        int i = Pattern.CANON_EQ;

        // Use of inherited method.
        // :: error: (usage)
        String msg = getMessage();

        // No report for use of overridden method -
        // we get a message when we call super in the overriding method.
        // TODO: Would we want "transitive" behavior? I.e. a few levels higher
        // in the inheritance hierarchy we could see the class to report.
        String pat = this.getPattern();

        try {
            // :: error: (usage)
            p.compile("test(((");
        } catch (Package pe) {
            // We don't look at supertypes of the types we analyze.
            // TODO: Should we?
            System.out.println("OK!");
            // :: error: (usage)
        } catch (PatternSyntaxException pse) {
            // We do get a report for direct uses.
            System.out.println("Ha!");
        }
    }
}
