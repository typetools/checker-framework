// Test case for Issue 103

import java.util.ArrayList;
import java.util.List;

class CC {}

class HR {}

// Crazy: remove the "extends HR" and it compiles
public class Bug103 extends HR {

    // Crazy: add a 23th element as for example "hello" and it compiles
    // Crazy: replace IG.C with IG.C+"" and it compiles
    // Crazy: remove final and it compiles
    // Crazy: replace with new String[22] and it compiles
    // Crazy: reduce to less than 5 distinct values and it compiles  (replace IG.D with IG.C)
    final String[] ids = {
        IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C, IG.C,
        IG.C, IG.C, IG.C, IG.D, IG.E, IG.F, IG.G
    };

    // Crazy: remove arg u and it compiles
    // Crazy: remove any line of m1 and it compiles
    // Crazy: replace two o args by null and it compiles
    void m1(CC o, Object u) {
        String cc = m2(o);
        String dd = m2(o);
    }

    String m2(final CC c) {
        return "a";
    }

    // Crazy: remove ids.length and it compiles
    // replace return type List with ArrayList and it compiles
    List<CC> m3(CC c) {
        ArrayList<CC> lc = new ArrayList<>(ids.length);
        return lc;
    }

    // Crazy: comment out the whole unused LV class and it compiles
    // Crazy: comment one of the following 4 lines out and it compiles
    static class LV {
        static String a = "a";
        static String b = "a";
        static String c = "a";
        static String d = "a";
    }

    class IG {
        // Crazy: comment one of the following 8 lines out and it compiles
        String C1 = "1";
        String C2 = "1";
        String C3 = "1";
        String C4 = "1";
        String C5 = "1";
        String C6 = "1";
        String C7 = "1";
        String C8 = "1";

        static final String C = "c";
        static final String D = C;
        static final String E = C;
        static final String F = C;

        // Crazy: comment one of the following 18 lines out and it compiles
        static final String G = C;
        static final String H = C;
        static final String I = C;
        static final String J = C;
        static final String K = C;
        static final String L = C;
        static final String M = C;
        static final String N = C;
        static final String O = C;
        static final String P = C;
        static final String Q = C;
        static final String R = C;
        static final String S = C;
        static final String T = C;
        static final String U = C;
        static final String V = C;
        static final String W = C;
        static final String X = C;
    }
}
