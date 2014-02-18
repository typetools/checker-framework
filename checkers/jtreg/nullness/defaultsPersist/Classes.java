/*
 * @test
 * @summary Test that defaulted types are stored in bytecode.
 *
 * @compile Driver.java ReferenceInfoUtil.java Classes.java
 * @run main Driver Classes
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

public class Classes {

    /* TODO: store extends/implements in TypesIntoElements.
    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = CLASS_EXTENDS, typeIndex=-1),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_EXTENDS, typeIndex=-1),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=-1),
    })
    public String extendsDefault1() {
        return "class Test {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = CLASS_EXTENDS, typeIndex=-1),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_EXTENDS, typeIndex=-1),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=-1),
    })
    public String extendsDefault2() {
        return "class Test extends Object {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = CLASS_EXTENDS, typeIndex=-1),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_EXTENDS, typeIndex=-1),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=-1),
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = CLASS_EXTENDS, typeIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_EXTENDS, typeIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=0),
    })
    public String extendsDefault3() {
        return "class Test implements java.io.Serializable {}";
    }
    */

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/Nullable", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
    })
    public String typeParams1() {
        return "class Test <T1> {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
    })
    public String typeParams2() {
        return "class Test<T1 extends Object> {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=1),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=1),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=1),
    })
    public String typeParams3() {
        return "class Test<T2 extends Comparable<T2>> {}";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/Nullable", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=0, boundIndex=0),

        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=1, boundIndex=1),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=1, boundIndex=1),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = CLASS_TYPE_PARAMETER_BOUND, paramIndex=1, boundIndex=1),
    })
    public String typeParams4() {
        return "class Test<T1, T2 extends Comparable<T2>> {}";
    }
}
