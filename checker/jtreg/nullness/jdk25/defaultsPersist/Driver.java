// Keep in sync with ../../jdk24/defaultsPersist/Driver.java, which uses the
// com.sun.tools.classfile API that was removed in Java 25.  This version uses the
// java.lang.classfile API.

// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/Driver.java

// I removed some unnecessary code, e.g. declarations of @TA.
// I changed expected logic to handle multiple appearances
// of the same qualifier in different positions.

import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Label;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.TargetType;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;

public class Driver {

  private static final PrintStream out = System.out;

  // The argument is in the format expected by Class.forName().
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: java Driver <test-name>");
    }
    String name = args[0];
    Class<?> clazz = Class.forName(name);
    new Driver().runDriver(clazz.getDeclaredConstructor().newInstance());
  }

  protected void runDriver(Object object) throws Exception {
    int passed = 0, failed = 0;
    Class<?> clazz = object.getClass();
    out.println("Tests for " + clazz.getName());

    // Find methods
    for (Method method : clazz.getMethods()) {
      List<AnnoPosPair> expected = expectedOf(method);
      if (expected == null) {
        continue;
      }
      if (method.getReturnType() != String.class) {
        throw new IllegalArgumentException("Test method needs to return a string: " + method);
      }
      String testClass = PersistUtil.testClassOf(method);

      try {
        String compact = (String) method.invoke(object);
        String fullFile = PersistUtil.wrap(compact);
        ClassModel c = PersistUtil.compileAndReturn(fullFile, testClass);
        boolean ignoreConstructors = !clazz.getName().equals("Constructors");
        List<AnnoPosPair> actual = ReferenceInfoUtil.extendedAnnotationsOf(c, ignoreConstructors);
        String diagnostic =
            String.join(
                "; ",
                "Tests for " + clazz.getName(),
                "compact=" + compact,
                "fullFile=" + fullFile,
                "testClass=" + testClass);
        ReferenceInfoUtil.compare(expected, actual, c, diagnostic);
        out.println("PASSED:  " + method.getName());
        ++passed;
      } catch (Throwable e) {
        out.println("FAILED:  " + method.getName());
        out.println("    " + e);
        ++failed;
      }
    }

    out.println();
    int total = passed + failed;
    out.println(total + " total tests: " + passed + " PASSED, " + failed + " FAILED");

    out.flush();

    if (failed != 0) {
      throw new RuntimeException(failed + " tests failed");
    }
  }

  private List<AnnoPosPair> expectedOf(Method m) {
    TADescription ta = m.getAnnotation(TADescription.class);
    TADescriptions tas = m.getAnnotation(TADescriptions.class);

    if (ta == null && tas == null) {
      return null;
    }

    List<AnnoPosPair> result = new ArrayList<>();

    if (ta != null) {
      result.add(expectedOf(ta));
    }

    if (tas != null) {
      for (TADescription a : tas.value()) {
        result.add(expectedOf(a));
      }
    }

    return result;
  }

  private AnnoPosPair expectedOf(TADescription d) {
    String annoName = "L" + d.annotation() + ";";

    Position p = new Position();
    p.type = d.type();
    if (d.offset() != NOT_SET) {
      p.offset = d.offset();
    }
    if (d.lvarOffset().length != 0) {
      p.lvarOffset = d.lvarOffset();
    }
    if (d.lvarLength().length != 0) {
      p.lvarLength = d.lvarLength();
    }
    if (d.lvarIndex().length != 0) {
      p.lvarIndex = d.lvarIndex();
    }
    if (d.boundIndex() != NOT_SET) {
      p.boundIndex = d.boundIndex();
    }
    if (d.paramIndex() != NOT_SET) {
      p.parameterIndex = d.paramIndex();
    }
    if (d.typeIndex() != NOT_SET) {
      p.typeIndex = d.typeIndex();
    }
    if (d.exceptionIndex() != NOT_SET) {
      p.exceptionIndex = d.exceptionIndex();
    }
    if (d.genericLocation().length != 0) {
      p.location = getTypePathFromBinary(d.genericLocation());
    }

    return AnnoPosPair.of(annoName, p);
  }

  /**
   * Converts a type path from its binary representation -- a sequence of (type path kind, type
   * argument index) pairs -- into a list of type path components.
   *
   * @param binary a type path in binary representation
   * @return the corresponding list of type path components
   */
  private static List<TypePathComponent> getTypePathFromBinary(int[] binary) {
    List<TypePathComponent> result = new ArrayList<>(binary.length / 2);
    for (int i = 0; i < binary.length; i += 2) {
      TypePathComponent.Kind kind =
          switch (binary[i]) {
            case 0 -> TypePathComponent.Kind.ARRAY;
            case 1 -> TypePathComponent.Kind.INNER_TYPE;
            case 2 -> TypePathComponent.Kind.WILDCARD;
            case 3 -> TypePathComponent.Kind.TYPE_ARGUMENT;
            default -> throw new IllegalArgumentException("Bad type path kind: " + binary[i]);
          };
      result.add(TypePathComponent.of(kind, binary[i + 1]));
    }
    return result;
  }

  public static final int NOT_SET = -888;
}

/**
 * The location of a type annotation. This is the java.lang.classfile analogue of {@code
 * com.sun.tools.classfile.TypeAnnotation.Position}: it flattens the {@link
 * java.lang.classfile.TypeAnnotation.TargetInfo} hierarchy into a single class, whose fields that
 * do not apply to a given target retain their default values.
 */
class Position {

  /** The kind of program element that the annotation targets. */
  public TargetType type;

  /** The path from the annotated type to the type that is the target of the annotation. */
  public List<TypePathComponent> location = List.of();

  /** The bytecode offset of the targeted instruction, or -1. */
  public int offset = -1;

  /** The start offsets of the ranges of a targeted local variable, or null. */
  public int[] lvarOffset = null;

  /** The lengths of the ranges of a targeted local variable, or null. */
  public int[] lvarLength = null;

  /** The local variable table indices of a targeted local variable, or null. */
  public int[] lvarIndex = null;

  /** The index of a targeted type parameter bound, or -1. */
  public int boundIndex = -1;

  /** The index of a targeted type parameter or formal parameter, or -1. */
  public int parameterIndex = -1;

  /** The index of a targeted supertype, thrown type, or type argument, or -1. */
  public int typeIndex = -1;

  /** The exception table index of a targeted exception parameter, or -1. */
  public int exceptionIndex = -1;

  /**
   * Returns the position of the given type annotation.
   *
   * @param ta a type annotation
   * @param labelToBci maps a label to a bytecode index; it is called only for annotations within a
   *     method body, which are the only ones that refer to a label
   * @return the position of {@code ta}
   */
  public static Position of(TypeAnnotation ta, ToIntFunction<Label> labelToBci) {
    Position p = new Position();
    p.location = ta.targetPath();
    TypeAnnotation.TargetInfo target = ta.targetInfo();
    p.type = target.targetType();
    switch (target) {
      case TypeAnnotation.TypeParameterTarget t -> p.parameterIndex = t.typeParameterIndex();
      case TypeAnnotation.SupertypeTarget t -> p.typeIndex = t.supertypeIndex();
      case TypeAnnotation.TypeParameterBoundTarget t -> {
        p.parameterIndex = t.typeParameterIndex();
        p.boundIndex = t.boundIndex();
      }
      case TypeAnnotation.EmptyTarget unused -> {}
      case TypeAnnotation.FormalParameterTarget t -> p.parameterIndex = t.formalParameterIndex();
      case TypeAnnotation.ThrowsTarget t -> p.typeIndex = t.throwsTargetIndex();
      case TypeAnnotation.LocalVarTarget t -> {
        List<TypeAnnotation.LocalVarTargetInfo> table = t.table();
        p.lvarOffset = new int[table.size()];
        p.lvarLength = new int[table.size()];
        p.lvarIndex = new int[table.size()];
        for (int i = 0; i < table.size(); i++) {
          TypeAnnotation.LocalVarTargetInfo lv = table.get(i);
          p.lvarOffset[i] = labelToBci.applyAsInt(lv.startLabel());
          p.lvarLength[i] = labelToBci.applyAsInt(lv.endLabel()) - p.lvarOffset[i];
          p.lvarIndex[i] = lv.index();
        }
      }
      case TypeAnnotation.CatchTarget t -> p.exceptionIndex = t.exceptionTableIndex();
      case TypeAnnotation.OffsetTarget t -> p.offset = labelToBci.applyAsInt(t.target());
      case TypeAnnotation.TypeArgumentTarget t -> {
        p.offset = labelToBci.applyAsInt(t.target());
        p.typeIndex = t.typeArgumentIndex();
      }
      default -> throw new IllegalArgumentException("Unexpected target: " + target);
    }
    return p;
  }

  @Override
  public String toString() {
    return String.join(
        ", ",
        "type = " + type,
        "location = " + location,
        "offset = " + offset,
        "lvarOffset = " + Arrays.toString(lvarOffset),
        "lvarLength = " + Arrays.toString(lvarLength),
        "lvarIndex = " + Arrays.toString(lvarIndex),
        "boundIndex = " + boundIndex,
        "parameterIndex = " + parameterIndex,
        "typeIndex = " + typeIndex,
        "exceptionIndex = " + exceptionIndex);
  }
}

/** A pair of an annotation and a position. */
class AnnoPosPair {
  /** The first element of the pair. */
  public final String first;

  /** The second element of the pair. */
  public final Position second;

  /**
   * Creates a new immutable pair. Clients should use {@link #of}.
   *
   * @param first the first element of the pair
   * @param second the second element of the pair
   */
  private AnnoPosPair(String first, Position second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Creates a new immutable pair.
   *
   * @param first first argument
   * @param second second argument
   * @return a pair of the values (first, second)
   */
  public static AnnoPosPair of(String first, Position second) {
    return new AnnoPosPair(first, second);
  }

  @Override
  public String toString() {
    return first + " @ " + second;
  }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescription {
  String annotation();

  TargetType type();

  int offset() default Driver.NOT_SET;

  int[] lvarOffset() default {};

  int[] lvarLength() default {};

  int[] lvarIndex() default {};

  int boundIndex() default Driver.NOT_SET;

  int paramIndex() default Driver.NOT_SET;

  int typeIndex() default Driver.NOT_SET;

  int exceptionIndex() default Driver.NOT_SET;

  int[] genericLocation() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescriptions {
  TADescription[] value() default {};
}
