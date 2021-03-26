/*
 * @test
 * @summary Test that defaulted types are stored in bytecode.
 *
 * @compile ../PersistUtil.java Driver.java ReferenceInfoUtil.java Fields.java
 * @run main Driver Fields
 * @ignore This fails for Java 11. See Issue 2816.
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.FIELD;

public class Fields {

  @TADescriptions({
    @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD),
  })
  public String fieldDefault() {
    return "Object f = new Object();";
  }

  @TADescriptions({
    @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD),
  })
  public String fieldDefaultOneExplicit() {
    return "@NonNull Object f = new Object();";
  }

  @TADescriptions({
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/Nullable",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD),
  })
  public String fieldWithDefaultQualifier() {
    return "@DefaultQualifier(Nullable.class)" + System.lineSeparator() + " Object f;";
  }

  @TADescriptions({
    @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {0, 0}),
  })
  public String fieldArray1() {
    return "String[] sa = new String[1];";
  }

  @TADescriptions({
    @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/Nullable",
        type = FIELD,
        genericLocation = {0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {0, 0, 0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {0, 0, 0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {0, 0, 0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {0, 0, 0, 0, 0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {0, 0, 0, 0, 0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {0, 0, 0, 0, 0, 0}),
  })
  public String fieldArray2() {
    return "String[] @Nullable [][] saaa = new String[1][][];";
  }

  @TADescriptions({
    // in front of the java.util.List
    @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD),

    // in front of Object //TODO: NEXT ANNO CHANGE TO NULLABLE WHEN WE GET JDK WORKING WITH THIS
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {3, 0, 2, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {3, 0, 2, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {3, 0, 2, 0}),

    // in front of the wildcard (?)
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {3, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {3, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {3, 0}),
  })
  public String wildcards1() {
    return "java.util.List<? extends Object> f = new java.util.ArrayList<>();";
  }

  @TADescriptions({
    // in front of the first java.util.List
    @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD),

    // in front of the wildcard (?)
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {3, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {3, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {3, 0}),

    // in front of the second java.util.List
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {3, 0, 2, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {3, 0, 2, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {3, 0, 2, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {3, 0, 2, 0, 3, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {3, 0, 2, 0, 3, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {3, 0, 2, 0, 3, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = FIELD,
        genericLocation = {3, 0, 2, 0, 3, 0, 0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = FIELD,
        genericLocation = {3, 0, 2, 0, 3, 0, 0, 0}),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = FIELD,
        genericLocation = {3, 0, 2, 0, 3, 0, 0, 0}),
  })
  public String wildcards2() {
    return "java.util.List<? extends java.util.List<String[]>> f = new java.util.ArrayList<>();";
  }
}
