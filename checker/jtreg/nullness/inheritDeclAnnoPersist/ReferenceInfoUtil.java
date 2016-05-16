// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handled the same type qualifier appearing multiple times.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.TypeAnnotation;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.RuntimeAnnotations_attribute;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPool.UnexpectedEntry;

public class ReferenceInfoUtil {

    public static final int IGNORE_VALUE = -321;

    public static List<Annotation> extendedAnnotationsOf(ClassFile cf) {
        List<Annotation> annos = new ArrayList<Annotation>();
        findAnnotations(cf, annos);
        return annos;
    }

    /////////////////// Extract annotations //////////////////
    private static void findAnnotations(ClassFile cf, List<Annotation> annos) {
        for (Method m: cf.methods) {
            findAnnotations(cf, m, Attribute.RuntimeVisibleAnnotations, annos);
        }
    }

    /**
     * Test the result of Attributes.getIndex according to expectations
     * encoded in the method's name.
     */
    private static void findAnnotations(ClassFile cf, Method m, String name, List<Annotation> annos) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeAnnotations_attribute;
            RuntimeAnnotations_attribute tAttr = (RuntimeAnnotations_attribute)attr;
            for (Annotation an : tAttr.annotations) {
                if (!containsName(annos, an, cf)) {
                    annos.add(an);
                }
            }
        }
    }

    private static Annotation findAnnotation(String name, List<Annotation> annotations, ClassFile cf) throws InvalidIndex, UnexpectedEntry {
        String properName = "L" + name + ";";
        for (Annotation anno : annotations) {
            String actualName = cf.constant_pool.getUTF8Value(anno.type_index);
            if (properName.equals(actualName)) {
                return anno;
            }
        }
        return null;
    }

    public static boolean compare(List<String> expectedAnnos,
            List<Annotation> actualAnnos, ClassFile cf) throws InvalidIndex, UnexpectedEntry {
        if (actualAnnos.size() != expectedAnnos.size()) {
            throw new ComparisionException("Wrong number of annotations",
                    expectedAnnos,
                    actualAnnos, cf);
        }
        for (String annoName : expectedAnnos) {
            Annotation anno = findAnnotation(annoName, actualAnnos, cf);
            if (anno == null) {
                throw new ComparisionException("Expected annotation not found: "
                        + annoName, expectedAnnos, actualAnnos, cf);
            }
        }
        return true;
    }

    private static boolean containsName(List<Annotation> annos, Annotation anno,
            ClassFile cf) {
        try {
            for (Annotation an : annos) {
                if (cf.constant_pool.getUTF8Value(an.type_index).equals(cf.
                        constant_pool.getUTF8Value(anno.type_index))) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return false;
    }
}

class ComparisionException extends RuntimeException {
    private static final long serialVersionUID = -3930499712333815821L;

    public final List<String> expected;
    public final List<Annotation> found;
    public final ClassFile cf;

    public ComparisionException(String message, List<String> expected, List<Annotation> found, ClassFile cf) {
        super(message);
        this.expected = expected;
        this.found = found;
        this.cf = cf;
    }

    public String toString() {
        String str = super.toString();
        try {
            if (expected != null && found != null) {
                str += "\n\tExpected: " + expected.size() + " annotations; but found: " + found.size() + " annotations\n" +
                       "  Expected: " + expected +
                       "\n  Found: ";
                for (Annotation anno : found) {
                    str += cf.constant_pool.getUTF8Value(anno.type_index) + ",";
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return str;
    }
}
