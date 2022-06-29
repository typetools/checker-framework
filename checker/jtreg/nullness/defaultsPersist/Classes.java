/*
 * @test
 * @summary Test that defaulted types are stored in bytecode.
 *
 * @compile  ../PersistUtil.java Driver.java ReferenceInfoUtil.java Classes.java
 * @run main Driver Classes
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.CLASS_TYPE_PARAMETER;
import static com.sun.tools.classfile.TypeAnnotation.TargetType.CLASS_TYPE_PARAMETER_BOUND;

public class Classes {

  /* TODO: store extends/implements in TypesIntoElements.
  @TADescriptions({
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = CLASS_EXTENDS, typeIndex=-1),
      @TADescription(annotation = "org/checkerframework/checker/initialization/qual/Initialized", type = CLASS_EXTENDS, typeIndex=-1),
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=-1),
  })
  public String extendsDefault1() {
      return "class Test {}";
  }

  @TADescriptions({
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = CLASS_EXTENDS, typeIndex=-1),
      @TADescription(annotation = "org/checkerframework/checker/initialization/qual/Initialized", type = CLASS_EXTENDS, typeIndex=-1),
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=-1),
  })
  public String extendsDefault2() {
      return "class Test extends Object {}";
  }

  @TADescriptions({
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = CLASS_EXTENDS, typeIndex=-1),
      @TADescription(annotation = "org/checkerframework/checker/initialization/qual/Initialized", type = CLASS_EXTENDS, typeIndex=-1),
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=-1),
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/NonNull", type = CLASS_EXTENDS, typeIndex=0),
      @TADescription(annotation = "org/checkerframework/checker/initialization/qual/Initialized", type = CLASS_EXTENDS, typeIndex=0),
      @TADescription(annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor", type = CLASS_EXTENDS, typeIndex=0),
  })
  public String extendsDefault3() {
      return "class Test implements java.io.Serializable {}";
  }
  */

  @TADescriptions({
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/Nullable",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    // Annotations on the implicit constructor, which the test ignores.
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/NonNull",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/initialization/qual/UnderInitialization",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
    //     type = METHOD_RETURN),
  })
  public String typeParams1() {
    return "class Test <T1> {}";
  }

  @TADescriptions({
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    // Annotations on the implicit constructor, which the test ignores.
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/NonNull",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/initialization/qual/UnderInitialization",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
    //     type = METHOD_RETURN),
  })
  public String typeParams2() {
    return "class Test<T1 extends Object> {}";
  }

  @TADescriptions({
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 1),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 1),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 1),
    // Annotations on the implicit constructor, which the test ignores.
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/NonNull",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/initialization/qual/UnderInitialization",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
    //     type = METHOD_RETURN),
  })
  public String typeParams3() {
    return "class Test<T2 extends Comparable<T2>> {}";
  }

  @TADescriptions({
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/Nullable",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 0,
        boundIndex = 0),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 1),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 1),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER,
        paramIndex = 1),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/NonNull",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 1,
        boundIndex = 1),
    @TADescription(
        annotation = "org/checkerframework/checker/initialization/qual/Initialized",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 1,
        boundIndex = 1),
    @TADescription(
        annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
        type = CLASS_TYPE_PARAMETER_BOUND,
        paramIndex = 1,
        boundIndex = 1),
    // Annotations on the implicit constructor, which the test ignores.
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/NonNull",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/initialization/qual/UnderInitialization",
    //     type = METHOD_RETURN),
    // @TADescription(
    //     annotation = "org/checkerframework/checker/nullness/qual/UnknownKeyFor",
    //     type = METHOD_RETURN),
  })
  public String typeParams4() {
    return "class Test<T1, T2 extends Comparable<T2>> {}";
  }
}
