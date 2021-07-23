// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handle the same type qualifier appearing multiple times.

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPool.UnexpectedEntry;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.RuntimeAnnotations_attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ReferenceInfoUtil {

    public static final int IGNORE_VALUE = -321;

    public static List<Annotation> extendedAnnotationsOf(ClassFile cf) {
        List<Annotation> annos = new ArrayList<>();
        findAnnotations(cf, annos);
        return annos;
    }

    /////////////////// Extract annotations //////////////////
    private static void findAnnotations(ClassFile cf, List<Annotation> annos) {
        for (Method m : cf.methods) {
            findAnnotations(cf, m, Attribute.RuntimeVisibleAnnotations, annos);
        }
    }

    /**
     * Test the result of Attributes.getIndex according to expectations encoded in the method's
     * name.
     */
    private static void findAnnotations(
            ClassFile cf, Method m, String name, List<Annotation> annos) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeAnnotations_attribute;
            RuntimeAnnotations_attribute tAttr = (RuntimeAnnotations_attribute) attr;
            for (Annotation an : tAttr.annotations) {
                if (!containsName(annos, an, cf)) {
                    annos.add(an);
                }
            }
        }
    }

    private static Annotation findAnnotation(
            String name, List<Annotation> annotations, ClassFile cf)
            throws InvalidIndex, UnexpectedEntry {
        String properName = "L" + name + ";";
        for (Annotation anno : annotations) {
            String actualName = cf.constant_pool.getUTF8Value(anno.type_index);
            if (properName.equals(actualName)) {
                return anno;
            }
        }
        return null;
    }

    public static boolean compare(
            List<String> expectedAnnos,
            List<Annotation> actualAnnos,
            ClassFile cf,
            String diagnostic)
            throws InvalidIndex, UnexpectedEntry {
        if (actualAnnos.size() != expectedAnnos.size()) {
            throw new ComparisonException(
                    "Wrong number of annotations; " + diagnostic, expectedAnnos, actualAnnos, cf);
        }
        for (String annoName : expectedAnnos) {
            Annotation anno = findAnnotation(annoName, actualAnnos, cf);
            if (anno == null) {
                throw new ComparisonException(
                        "Expected annotation not found: " + annoName + "; " + diagnostic,
                        expectedAnnos,
                        actualAnnos,
                        cf);
            }
        }
        return true;
    }

    private static boolean containsName(List<Annotation> annos, Annotation anno, ClassFile cf) {
        try {
            for (Annotation an : annos) {
                if (cf.constant_pool
                        .getUTF8Value(an.type_index)
                        .equals(cf.constant_pool.getUTF8Value(anno.type_index))) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return false;
    }
}

class ComparisonException extends RuntimeException {
    private static final long serialVersionUID = -3930499712333815821L;

    public final List<String> expected;
    public final List<Annotation> found;
    public final ClassFile cf;

    public ComparisonException(
            String message, List<String> expected, List<Annotation> found, ClassFile cf) {
        super(message);
        this.expected = expected;
        this.found = found;
        this.cf = cf;
    }

    public String toString() {
        StringJoiner foundString = new StringJoiner(",");
        for (Annotation anno : found) {
            try {
                foundString.add(cf.constant_pool.getUTF8Value(anno.type_index));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return String.join(
                System.lineSeparator(),
                super.toString(),
                "\tExpected: "
                        + expected.size()
                        + " annotations; but found: "
                        + found.size()
                        + " annotations",
                "  Expected: " + expected,
                "  Found: " + foundString);
    }
}
