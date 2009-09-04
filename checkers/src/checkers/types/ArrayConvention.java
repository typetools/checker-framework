package checkers.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;

import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.visitors.AnnotatedTypeScanner;

/**
 * A private utility class that handles the array convention and handles the
 * convertion between them.
 *
 *
 */
/*package-scope*/ enum ArrayConvention {
    CANONICAL,
    ELTS_IN,
    ELTS_PRE,
    ARRAYS_IN,
    ARRAYS_PRE;

    public final boolean isArrays() {
        return (this == ARRAYS_IN || this == ARRAYS_PRE);
    }

    /** JSR 308: should we use the ELTS array convention?
     */
    public static final ArrayConvention USED_CONVENTION;

    /** JSR 308: default array convention */
    private static final ArrayConvention JSR308_DEFAULT_ARRAY_CONVENTION = CANONICAL;

    static {
        // Determine the convention ("elts" or "arrays") to use for JSR 308
        // annotations on array types
        String arrayConv = System.getProperty("jsr308.arrays");
        if (arrayConv == null)
            arrayConv = System.getProperty("jsr308_arrays");
        if (arrayConv == null)
            arrayConv = System.getenv("jsr308.arrays");
        if (arrayConv == null)
            arrayConv = System.getenv("jsr308_arrays");

        // Decide on the convention
        ArrayConvention foundConvention = null;
        for (ArrayConvention ar : ArrayConvention.values()) {
            if (ar.toString().equalsIgnoreCase(arrayConv)) {
                foundConvention = ar;
                break;
            }
        }
        if (foundConvention == null)
            USED_CONVENTION = JSR308_DEFAULT_ARRAY_CONVENTION;
        else
            USED_CONVENTION = foundConvention;
    }

    /**
     * Convert the array type from following ELT convention to the ARRAYS
     * convention, and visa versa
     */
    public static final void applyArrayConvention(AnnotatedTypeMirror result) {
        applyArrayConvention(result, USED_CONVENTION);
    }

    public static final void applyArrayConvention(AnnotatedTypeMirror result, ArrayConvention conv) {
        if (conv == CANONICAL) {
            eltsToCanonical.visit(result, null);
        } else if (conv.isArrays()) {
            eltsToArrays.visit(result, null);
        }
    }

    public static AnnotatedTypeScanner<Void, Void> eltsToCanonical
    = new AnnotatedTypeScanner<Void, Void>() {
        public Void visitArray(AnnotatedArrayType type, Void p) {
            LinkedList<AnnotatedTypeMirror> arrays = new LinkedList<AnnotatedTypeMirror>();
            List<List<AnnotationMirror>> annotations = new ArrayList<List<AnnotationMirror>>();

            AnnotatedTypeMirror innerComponent = type;
            while (innerComponent.getKind() == TypeKind.ARRAY) {
                arrays.add(innerComponent);
                // TODO: replace .annotations with .getAnnotations()
                // However, change is problamatic with type
                annotations.add(new ArrayList<AnnotationMirror>(innerComponent.annotations));
                innerComponent = ((AnnotatedArrayType)innerComponent).getComponentType();
            }
            // add the most inner component
            arrays.addFirst(innerComponent);
            annotations.add(new ArrayList<AnnotationMirror>(innerComponent.annotations));

            for (int i = 0; i < arrays.size(); ++i) {
                AnnotatedTypeMirror t = arrays.get(i);
                t.clearAnnotations();
                t.addAnnotations(annotations.get(i));
            }

            // Avoid visiting components in the middle
            return visit(innerComponent);
        }
    };

    public static AnnotatedTypeScanner<Void, Void> eltsToArrays
    = new AnnotatedTypeScanner<Void, Void>() {
        public Void visitArray(AnnotatedArrayType type, Void p) {
            List<AnnotatedTypeMirror> arrays = new ArrayList<AnnotatedTypeMirror>();
            List<List<AnnotationMirror>> annotations = new ArrayList<List<AnnotationMirror>>();

            AnnotatedTypeMirror innerComponent = type;
            while (innerComponent.getKind() == TypeKind.ARRAY) {
                arrays.add(innerComponent);
                // TODO: replace .annotations with .getAnnotations()
                // However, change is problamatic with type
                annotations.add(new ArrayList<AnnotationMirror>(innerComponent.annotations));
                innerComponent = ((AnnotatedArrayType)innerComponent).getComponentType();
            }
            // add the most inner component
            arrays.add(innerComponent);
            annotations.add(new ArrayList<AnnotationMirror>(innerComponent.annotations));

            Collections.reverse(annotations);

            for (int i = 0; i < arrays.size(); ++i) {
                AnnotatedTypeMirror t = arrays.get(i);
                t.clearAnnotations();
                t.addAnnotations(annotations.get(i));
            }

            // Avoid visiting components in the middle
            return visit(innerComponent);
        }
    };

    public static AnnotatedTypeScanner<Void, Void> arraysToCanonical
    = new AnnotatedTypeScanner<Void, Void>() {
        public Void visitArray(AnnotatedArrayType type, Void p) {
            List<AnnotatedArrayType> arrays = new ArrayList<AnnotatedArrayType>();
            List<List<AnnotationMirror>> annotations = new ArrayList<List<AnnotationMirror>>();

            AnnotatedTypeMirror innerComponent = type;
            while (innerComponent.getKind() == TypeKind.ARRAY) {
                arrays.add((AnnotatedArrayType)innerComponent);
                // TODO: replace .annotations with .getAnnotations()
                // However, change is problamatic with type
                annotations.add(new ArrayList<AnnotationMirror>(innerComponent.annotations));
                innerComponent = ((AnnotatedArrayType)innerComponent).getComponentType();
            }

            Collections.reverse(annotations);

            for (int i = 0; i < arrays.size(); ++i) {
                AnnotatedTypeMirror t = arrays.get(i);
                t.clearAnnotations();
                t.addAnnotations(annotations.get(i));
            }

            // Avoid visiting components in the middle
            return visit(innerComponent, p);
        }
    };

}
