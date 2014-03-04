/*
 * @test
 * @summary Test that defaulted types are stored in bytecode.
 *
 * @compile Driver.java ReferenceInfoUtil.java Fields.java
 * @run main Driver Fields
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

public class Fields {

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD),
    })
    public String fieldDefault() {
        return "Object f = new Object();";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD),
    })
    public String fieldDefaultOneExplicit() {
        return "@NonNull Object f = new Object();";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/Nullable", type = FIELD),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD),
    })
    public String fieldWithDefaultQualifier() {
        return "@DefaultQualifier(Nullable.class)\n " +
            "Object f;";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD, genericLocation = {0, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {0, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {0, 0}),
    })
    public String fieldArray1() {
        return "String[] sa = new String[1];";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD),

        @TADescription(annotation = "checkers/nullness/quals/Nullable", type = FIELD, genericLocation = {0, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {0, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {0, 0}),

        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD, genericLocation = {0, 0, 0, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {0, 0, 0, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {0, 0, 0, 0}),

        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD, genericLocation = {0, 0, 0, 0, 0, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {0, 0, 0, 0, 0, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {0, 0, 0, 0, 0, 0}),
    })
    public String fieldArray2() {
        return "String[] @Nullable [][] saaa = new String[1][][];";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD, genericLocation = {3, 0, 2, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {3, 0, 2, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {3, 0, 2, 0}),
    })
    public String wildcards1() {
        return "java.util.List<? extends Object> f = new java.util.ArrayList<>();";
    }

    @TADescriptions({
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD),
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD, genericLocation = {3, 0, 2, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {3, 0, 2, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {3, 0, 2, 0}),
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD, genericLocation = {3, 0, 2, 0, 3, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {3, 0, 2, 0, 3, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {3, 0, 2, 0, 3, 0}),
        @TADescription(annotation = "checkers/nullness/quals/NonNull", type = FIELD, genericLocation = {3, 0, 2, 0, 3, 0, 0, 0}),
        @TADescription(annotation = "checkers/initialization/quals/Initialized", type = FIELD, genericLocation = {3, 0, 2, 0, 3, 0, 0, 0}),
        @TADescription(annotation = "checkers/nullness/quals/UnknownKeyFor", type = FIELD, genericLocation = {3, 0, 2, 0, 3, 0, 0, 0}),
    })
    public String wildcards2() {
        return "java.util.List<? extends java.util.List<String[]>> f = new java.util.ArrayList<>();";
    }

}
