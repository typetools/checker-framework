// Keep somewhat in sync with
// ../defaultsPersist/Driver.java and ../PersistUtil.

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.ClassFile;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
            List<String> expected = expectedOf(method);
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
                List<Annotation> actual = ReferenceInfoUtil.extendedAnnotationsOf(cf);
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

    private List<String> expectedOf(Method m) {
        ADescription ta = m.getAnnotation(ADescription.class);
        ADescriptions tas = m.getAnnotation(ADescriptions.class);

        if (ta == null && tas == null) {
            return null;
        }

        List<String> result = new ArrayList<>();

        if (ta != null) {
            result.add(expectedOf(ta));
        }

        if (tas != null) {
            for (ADescription a : tas.value()) {
                result.add(expectedOf(a));
            }
        }

        return result;
    }

    private String expectedOf(ADescription d) {
        return d.annotation();
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ADescription {
    String annotation();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ADescriptions {
    ADescription[] value() default {};
}
