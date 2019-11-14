// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/Driver.java

// I removed some unnecessary code, e.g. declarations of @TA.
// I changed expected logic to handle multiple appearances
// of the same qualifier in different positions.

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.TypeAnnotation;
import com.sun.tools.classfile.TypeAnnotation.TargetType;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.javacutil.Pair;

public class Driver {

    private static final PrintStream out = System.out;

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length > 1) {
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
            List<Pair<String, TypeAnnotation.Position>> expected = expectedOf(method);
            if (expected == null) {
                continue;
            }
            if (method.getReturnType() != String.class) {
                throw new IllegalArgumentException(
                        "Test method needs to return a string: " + method);
            }
            String testClass = PersistUtil.testClassOf(method);

            try {
                String compact = (String) method.invoke(object);
                String fullFile = PersistUtil.wrap(compact);
                ClassFile cf = PersistUtil.compileAndReturn(fullFile, testClass);
                List<TypeAnnotation> actual = ReferenceInfoUtil.extendedAnnotationsOf(cf);
                ReferenceInfoUtil.compare(expected, actual, cf);
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

    private List<Pair<String, TypeAnnotation.Position>> expectedOf(Method m) {
        TADescription ta = m.getAnnotation(TADescription.class);
        TADescriptions tas = m.getAnnotation(TADescriptions.class);

        if (ta == null && tas == null) {
            return null;
        }

        List<Pair<String, TypeAnnotation.Position>> result = new ArrayList<>();

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

    private Pair<String, TypeAnnotation.Position> expectedOf(TADescription d) {
        String annoName = d.annotation();

        TypeAnnotation.Position p = new TypeAnnotation.Position();
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
            p.bound_index = d.boundIndex();
        }
        if (d.paramIndex() != NOT_SET) {
            p.parameter_index = d.paramIndex();
        }
        if (d.typeIndex() != NOT_SET) {
            p.type_index = d.typeIndex();
        }
        if (d.exceptionIndex() != NOT_SET) {
            p.exception_index = d.exceptionIndex();
        }
        if (d.genericLocation().length != 0) {
            p.location =
                    TypeAnnotation.Position.getTypePathFromBinary(
                            wrapIntArray(d.genericLocation()));
        }

        return Pair.of(annoName, p);
    }

    private List<Integer> wrapIntArray(int[] ints) {
        List<Integer> list = new ArrayList<>(ints.length);
        for (int i : ints) {
            list.add(i);
        }
        return list;
    }

    public static final int NOT_SET = -888;
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
