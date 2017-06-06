// Test case for Issue 681:
// https://github.com/typetools/checker-framework/issues/681

import polyall.quals.H1S2;

// TODO: Issue is fixed, but test needs to be re-written in
// a way that actually checks the behavior.

/**
 * This class shows that non-explicitly written annotations on
 * Inner types are not inserted correctly.
 *
 * The PolyAllAnnotatedTypeFactory adds @H1S2 to the type of any variable
 * whose name contains "addH1S2".
 *
 * <pre>
 * javacheck -cp tests/build/ -processor polyall.PolyAllChecker tests/polyall/Issue681.java
 * javap -verbose Issue681\$Inner.class
 * <pre>
 *
 * Outputs:
 * ...
 * <pre>
 * Issue681$Inner addH1S2;
 * descriptor: LIssue681$Inner;
 * flags:
 * RuntimeVisibleTypeAnnotations:
 * 0: #10(): FIELD
 * 1: #11(): FIELD
 *
 * Issue681$Inner explicitH1S2;
 * descriptor: LIssue681$Inner;
 * flags:
 * RuntimeVisibleTypeAnnotations:
 * 0: #11(): FIELD, location=[INNER_TYPE]
 * 1: #10(): FIELD
 * 2: #11(): FIELD
 * </pre>
 */
class Issue681 {
    class Inner {
        @H1S2 Inner explicitH1S2;
        Issue681.@H1S2 Inner explicitNestedH1S2;
        @H1S2 Issue681.Inner explicitOneOuterH1S2;
        Inner addH1S2;

        @H1S2 Inner method(@H1S2 Inner paramExplicit, Inner nonAnno) {
            return paramExplicit;
        }
    }
}
