import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InferTypeArgsPolyChecker<OUTER_SCOPE_TV> {
    // ----------------------------------------------------------
    // Test Case - A
    <A> A methodA(@H2Top A a1, @H2Top A a2) {
        return null;
    }

    void contextA(@H1S1 @H2Bot String str, @H1Bot @H2Bot List<@H1S2 String> s) {
        @H2Bot Object a = methodA(str, s);
        @H1Top @H2Bot Object aTester = a;
    }

    // ----------------------------------------------------------
    // Test Case - B
    <B> B methodB(List<@H2S2 B> b1, List<@H1S2 B> b2) {
        return null;
    }

    void contextB(List<@H1S1 @H2S2 String> l1, List<@H1S2 @H2S1 String> l2) {
        @H1S1 @H2S1 String str = methodB(l1, l2);
    }

    // ----------------------------------------------------------
    // Test Case - C
    <C extends List<? extends Object>> C methodC(C c1, C c2) {
        return null;
    }

    void contextC(List<@H1S1 @H2S2 ? extends @H1S1 @H2S2 String> l1, List<@H1S1 @H2S2 String> l2) {
        List<@H1S1 @H2S2 ?> str = methodC(l1, l2);
    }

    // ----------------------------------------------------------
    // Test Case - D

    <D extends OUTER_SCOPE_TV, DD> D methodD(D d1, D d2, DD dd) {
        return null;
    }

    <D extends OUTER_SCOPE_TV, DD> DD methodD2(D d1, D d2, DD dd) {
        return null;
    }

    void contextD(OUTER_SCOPE_TV os1, @H1S1 @H2S1 OUTER_SCOPE_TV os2) {
        OUTER_SCOPE_TV osNaked1 = methodD(os1, os1, os2);

        // So for the next failure we correctly infer that for methodD to take both os1 and os2 as
        // arguments D must be @H1Top @H2Top OUTER_SCOPE_TV.  However, the UPPER_BOUND of D is
        // <@H1Bottom @H2Bottom OUTER_SCOPE_TV extends @H1Top @H2Top Object> notice that our
        // inferred type for D is above this bound.
        //
        // A similar, more useful example in the Nullness type system would be:
        /*
           class Gen<OUTER> {
              public List<OUTER> listo;

              <T extends OUTER> void addToListo(T t1, T T2) {
                  listo.add(t1);
                  listo.add(t2);
              }

              void launder(@NonNull OUTER arg1, @Nullable OUTER arg2) {
                  addToListo(arg1, arg2); // T is inferred to be <@Nullable OUTER>
                                          // if we did not mark this as type.argument.type.incompatible
                                          // then we would have no idea that in the last
                                          // line of this example we are putting a null value into
                                          // a List of @NonNull Strings
              }

           }

           Gen<@NonNull String> g = ...;
           g.listo = new ArrayList<@NonNull String>();
           g.launder("", null);    // during this method call null would be added to g.listo
        */
        // :: error: (type.argument.type.incompatible)
        OUTER_SCOPE_TV osNaked2 = methodD(os1, os2, "");

        // :: error: (type.argument.type.incompatible)
        OUTER_SCOPE_TV osAnnoed = methodD(os2, os2, "");

        // :: error: (type.argument.type.incompatible)
        String str = methodD2(os2, os1, "");
        OUTER_SCOPE_TV osNaked3 = methodD2(os1, os1, os2);
    }

    // ----------------------------------------------------------
    // Test Case - E
    <E> E methodE(E e1, E[] aos2) {
        return null;
    } // pass an array to one of these to cover A2FReducer.visitArray_Typevar

    void contextE(String[] strArr, String[][] strArrArr, OUTER_SCOPE_TV os, OUTER_SCOPE_TV[] aos) {
        String[] strArrLocal = methodE(strArr, strArrArr);
        OUTER_SCOPE_TV osLocal = methodE(os, aos);
    }

    // ----------------------------------------------------------
    // Test Case - C
    <F> List<? super F> methodF(List<? extends F> lExtF, List<? super F> lSupF) {
        return null;
    }

    void contextF(
            List<@H1Bot @H2Bot ? extends @H1Top @H2S1 String> l1,
            List<? super @H1S1 @H2S2 String> l2,
            List<@H1S1 @H2S2 ? extends @H1Top @H2Top String> l3) {

        // :: error: (argument.type.incompatible)
        List<? super @H1Bot @H2Bot String> lstr1 = methodF(l1, l2);

        // :: error: (argument.type.incompatible)
        List<? super @H1Top @H2S2 String> lstr2 = methodF(l3, l2);
    }

    <G extends List<H>, H extends List<G>> List<H> methodG(G g1, G G2) {
        return null;
    }

    class NodeList extends ArrayList<@H1Top @H2Top NodeList> {}

    void contextG(NodeList l1, NodeList l2) {
        List<@H1Top @H2Top NodeList> a = methodG(l1, l2);
    }

    // add test case for Array<String> vs. String[]
    // add test case of E extends F, F extends G, H extends List<? super E> and other craziness

    <M, N extends M> Map<M, N> method() {
        return null;
    }

    void contextMN() {

        // so I am not exactly sure how to create a meaningful test for this case
        // what occurs is the SubtypeSolver.propagateGlbs forces N to be a subtype of M
        // and it works (at least while I was debugging) but I don't know how to create
        // a check that fails or succeeds because of this
        Map<? super @H1S1 @H2S2 CharSequence, @H1Top @H2Top ? super String> mnl = method();
    }

    //    class Pair<PONE,PTWO> {
    //        PONE _1;
    //        PTWO _2;
    //    }

    <O extends P, P> @H1Top @H2Top P methodOP(@H1Top @H2Top P p, O o) {
        return null;
    }

    void contextOP(@H1S1 @H2S1 String s1, @H1Bot @H2Bot String s2) {
        // This test is actually here to test that the constraint P :> O is implied on p
        // :: error: (assignment.type.incompatible)
        @H1Bot @H2Bot String loc = methodOP(s1, s2);
    }

    //    class Triplet<L,M,N> {
    //        L l;
    //        M m;
    //        N n;
    //    }
    //
    //    <X extends Y, Y extends Z, Z> Triplet<X,Y,Z> methodXYZ() {
    //        return null;
    //    }
    //
    //    void contextXYZ() {
    //        Triplet<@H1S>
    //    }
}
