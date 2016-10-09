import polyall.quals.H1S2;
// test case for Issue 681
// https://github.com/typetools/checker-framework/issues/681
/**
 * This class is shows that non-explicitly written annotations on Inner types are not
 * inserted correctly
 * The PolyAllAnnotatedTypeFactory adds @H1S2 to the type of any variable
 * whose name contains "addH1S2"
 * javacheck -cp tests/build/ -processor polyall.PolyAllChecker tests/polyall/IntoElement.java
 * javap -verbose Outer\$Inner.class
 * Outputs:
 * ...
 Outer$Inner addH1S2;
 descriptor: LOuter$Inner;
 flags:
 RuntimeVisibleTypeAnnotations:
 0: #10(): FIELD
 1: #11(): FIELD

 Outer$Inner explicitH1S2;
 descriptor: LOuter$Inner;
 flags:
 RuntimeVisibleTypeAnnotations:
 0: #11(): FIELD, location=[INNER_TYPE]
 1: #10(): FIELD
 2: #11(): FIELD

 */
class Outer {
    class Inner {
        @H1S2 Inner explicitH1S2;
        Outer.@H1S2 Inner explicitNestedH1S2;
        @H1S2 Outer.Inner explicitOneOuterH1S2;
        Inner addH1S2;

        @H1S2 Inner method (@H1S2 Inner paramExplicit, Inner nonAnno) {
            return paramExplicit;
        }
    }
}
