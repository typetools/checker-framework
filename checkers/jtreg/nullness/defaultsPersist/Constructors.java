/*
 * @test
 * @summary Test that defaulted types are stored in bytecode.
 *
 * @compile Driver.java ReferenceInfoUtil.java Constructors.java
 * @run main Driver Constructors
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

public class Constructors {

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = METHOD_FORMAL_PARAMETER, paramIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = METHOD_FORMAL_PARAMETER, paramIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = METHOD_FORMAL_PARAMETER, paramIndex=0),
    })
    public String paramDefault1() {
        return "Test(Object o) {}";
    }

    @TADescriptions({
        // Should there be defaults?
        // @TADescription(annotation = "checkers/nullness/quals/NonNull", type = METHOD_RETURN),
        // @TADescription(annotation = "checkers/initialization/quals/Initialized", type = METHOD_RETURN),
        // @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = METHOD_RETURN),
    })
    public String retDefault1() {
        return "Test() { }";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = THROWS, typeIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = THROWS, typeIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = THROWS, typeIndex=0),
    })
    public String throwsDefault1() {
        return "Test() throws Throwable {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = THROWS, typeIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = THROWS, typeIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = THROWS, typeIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = THROWS, typeIndex=1),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = THROWS, typeIndex=1),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = THROWS, typeIndex=1),
    })
    public String throwsDefault2() {
        return "Test() throws ArrayIndexOutOfBoundsException, NullPointerException {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = METHOD_RECEIVER),
        @TADescription(annotation = "checkers/initialization/quals/UnderInitialization", type = METHOD_RECEIVER),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = METHOD_RECEIVER),
    })
    @TestClass("Outer$Inner")
    public String recvDefault1() {
        return "class Outer {" +
            "  class Inner {" +
            "    Inner(Outer Outer.this) {}" +
            "  }" +
            "}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/Nullable", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
    })
    public String typeParams1() {
        return "<M1> Test(M1 p) {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
    })
    public String typeParams2() {
        return "<M1 extends Object> Test(M1 p) {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=1),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=1),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = METHOD_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=1),
    })
    public String typeParams3() {
        return "<M2 extends Comparable<M2>> Test(M2 p) {}";
    }
}
