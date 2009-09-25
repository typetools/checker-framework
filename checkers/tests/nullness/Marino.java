import checkers.nullness.quals.*;
// Test cases originally written by Dan Marino, UCLA, 10/8/2007.
@checkers.quals.DefaultQualifier("checkers.nullness.quals.Nullable")
public class Marino {

    @NonNull String m_str;
    static String ms_str;
    String m_nullableStr;

    void testWhile() throws Exception {
        String s = "foo";
        while (true) {
            @NonNull String a = s;
            System.out.println("a has length: " + a.length());
            break;
        }
        int i = 1;
        while (true){

            @NonNull String a = s;
            s = null;
            System.out.println("hi" + s.length());
            if (i > 2) break;
            a = null;
        }
        // Checker doesn't catch that m_str not initialized.
        // This is Caveat 2 in the manual, but note that it
        // is not limited to contructors.
        System.out.println("Member string has length: " + m_str.length());

        // Dereference of any static field is allowed.
        // I suppose this is a design decision
        // for practicality in interacting with libraries...?
        System.out.println("Member string has length: " + ms_str.length());
        System.out.println("Everyone should get this error: " +
                           m_nullableStr.length());

        s = null;
        @NonNull String b = "hi";
        try{
            System.out.println("b has length: " + b.length());
            methodThatThrowsEx();
            s = "bye";
        }finally{
            // Checker doesn't catch that s will be null here.
            b = s;
            System.out.println("b has length: " + b.length());
        }
    }

    void methodThatThrowsEx() throws Exception {
    throw new Exception();
    }

}
