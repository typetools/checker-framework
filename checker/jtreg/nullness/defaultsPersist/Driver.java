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
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.TargetType;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Driver {

  private static final PrintStream out = System.out;

  // The argument is in the format expected by Class.forName().
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: java Driver <test-name>");
    }
    String name = args[0];
    Class<?> clazz = Class.forName(name);
    new Driver().runDriver(clazz.newInstance());
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
        ClassModel cm = PersistUtil.compileAndReturn(fullFile, testClass);
        boolean ignoreConstructors = !clazz.getName().equals("Constructors");
        List<TypeAnnotation> actual =
            ReferenceInfoUtil.extendedAnnotationsOf(cm, ignoreConstructors);
        String diagnostic =
            String.join(
                "; ",
                "Tests for " + clazz.getName(),
                "compact=" + compact,
                "fullFile=" + fullFile,
                "testClass=" + testClass);
        ReferenceInfoUtil.compare(expected, actual, diagnostic);
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
    String annoName = d.annotation();

    Position p = new Position();
    p.type = d.type();
    if (d.boundIndex() != NOT_SET) {
      p.boundIndex = d.boundIndex();
    }
    if (d.paramIndex() != NOT_SET) {
      p.parameterIndex = d.paramIndex();
    }
    if (d.typeIndex() != NOT_SET) {
      p.typeIndex = d.typeIndex();
    }
    if (d.genericLocation().length != 0) {
      p.location = Position.typePathFromBinary(d.genericLocation());
    }

    return AnnoPosPair.of(annoName, p);
  }

  public static final int NOT_SET = -888;
}

/** The position of a type annotation: what it applies to, and where within that type it appears. */
class Position {

  /** Indicates that an index is not applicable to the target that this position denotes. */
  public static final int NOT_APPLICABLE = Integer.MIN_VALUE;

  /** The kind of program element that the annotation applies to. */
  public TargetType type = TargetType.METHOD_RETURN;

  /** The path from the target's type to the annotated type; empty if they are the same type. */
  public List<TypePathComponent> location = List.of();

  /** The index of the annotated type parameter or formal parameter. */
  public int parameterIndex = NOT_APPLICABLE;

  /** The index of the annotated bound of a type parameter. */
  public int boundIndex = NOT_APPLICABLE;

  /** The index of the annotated type within a {@code throws} clause. */
  public int typeIndex = NOT_APPLICABLE;

  /**
   * Converts a type path from its class file representation -- a sequence of (kind tag, type
   * argument index) pairs -- into a list of type path components.
   *
   * @param binary alternating type path kind tags and type argument indices
   * @return the type path that {@code binary} represents
   */
  public static List<TypePathComponent> typePathFromBinary(int[] binary) {
    List<TypePathComponent> result = new ArrayList<>(binary.length / 2);
    for (int i = 0; i < binary.length; i += 2) {
      result.add(TypePathComponent.of(kindOfTag(binary[i]), binary[i + 1]));
    }
    return result;
  }

  /**
   * Returns the type path kind with the given class file tag.
   *
   * @param tag a type path kind tag
   * @return the type path kind whose tag is {@code tag}
   */
  private static TypePathComponent.Kind kindOfTag(int tag) {
    for (TypePathComponent.Kind kind : TypePathComponent.Kind.values()) {
      if (kind.tag() == tag) {
        return kind;
      }
    }
    throw new IllegalArgumentException("No type path kind with tag " + tag);
  }

  @Override
  public String toString() {
    return String.join(
        ", ",
        "type = " + type,
        "location = " + location,
        "parameterIndex = " + parameterIndex,
        "boundIndex = " + boundIndex,
        "typeIndex = " + typeIndex);
  }
}

/** A pair of an annotation name and a position. */
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
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescription {
  String annotation();

  TargetType type();

  int boundIndex() default Driver.NOT_SET;

  int paramIndex() default Driver.NOT_SET;

  int typeIndex() default Driver.NOT_SET;

  int[] genericLocation() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescriptions {
  TADescription[] value() default {};
}
