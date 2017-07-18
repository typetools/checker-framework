/*
 * @test
 * @summary Test that defaulted types are stored in bytecode.
 *
 * @compile -source 7 -target 7 ../PersistUtil.java Driver.java ReferenceInfoUtil.java Constructors.java
 * @run main Driver Constructors
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.METHOD_FORMAL_PARAMETER;
import static com.sun.tools.classfile.TypeAnnotation.TargetType.METHOD_RECEIVER;
import static com.sun.tools.classfile.TypeAnnotation.TargetType.METHOD_TYPE_PARAMETER;
import static com.sun.tools.classfile.TypeAnnotation.TargetType.METHOD_TYPE_PARAMETER_BOUND;
import static com.sun.tools.classfile.TypeAnnotation.TargetType.THROWS;

public class Constructors {

    @TADescriptions({
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = METHOD_FORMAL_PARAMETER,
            paramIndex = 0
        ),
    })
    public String paramDefault1() {
        return "Test(Object o) {}";
    }

    @TADescriptions({
        // Should there be defaults?
        // @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = METHOD_RETURN),
        // @TADescription(annotation = "org/checkerframework/checker/initialization/qual/Initialized", type = METHOD_RETURN),
        // @TADescription(annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor", type = METHOD_RETURN),
    })
    public String retDefault1() {
        return "Test() { }";
    }

    @TADescriptions({
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = THROWS,
            typeIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = THROWS,
            typeIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = THROWS,
            typeIndex = 0
        ),
    })
    public String throwsDefault1() {
        return "Test() throws Throwable {}";
    }

    @TADescriptions({
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = THROWS,
            typeIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = THROWS,
            typeIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = THROWS,
            typeIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = THROWS,
            typeIndex = 1
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = THROWS,
            typeIndex = 1
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = THROWS,
            typeIndex = 1
        ),
    })
    public String throwsDefault2() {
        return "Test() throws ArrayIndexOutOfBoundsException, NullPointerException {}";
    }

    @TADescriptions({
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = METHOD_RECEIVER
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_RECEIVER
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = METHOD_RECEIVER
        ),
    })
    @TestClass("Outer$Inner")
    public String recvDefault1() {
        return "class Outer {" + "  class Inner {" + "    Inner(Outer Outer.this) {}" + "  }" + "}";
    }

    @TADescriptions({
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/KeyForBottom",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/Nullable",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 0
        ),
    })
    public String typeParams1() {
        return "<M1> Test(M1 p) {}";
    }

    @TADescriptions({
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/KeyForBottom",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 0
        ),
    })
    public String typeParams2() {
        return "<M1 extends Object> Test(M1 p) {}";
    }

    @TADescriptions({
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/KeyForBottom",
            type = METHOD_TYPE_PARAMETER,
            paramIndex = 0
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/NonNull",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 1
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/initialization/qual/Initialized",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 1
        ),
        @TADescription(
            annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
            type = METHOD_TYPE_PARAMETER_BOUND,
            paramIndex = 0,
            boundIndex = 1
        ),
    })
    public String typeParams3() {
        return "<M2 extends Comparable<M2>> Test(M2 p) {}";
    }
}
