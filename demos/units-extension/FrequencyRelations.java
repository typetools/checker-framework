import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotatedTypeMirror;
import checkers.util.AnnotationUtils;
import checkers.util.AnnotationBuilder;

import checkers.units.*;

/** Relations among units of frequency. */
public class FrequencyRelations implements UnitsRelations {

    protected AnnotationMirror hz, s;

    public UnitsRelations init(ProcessingEnvironment env) {
        AnnotationBuilder builder = new AnnotationBuilder(env, Hz.class);
        builder.setValue("value", checkers.units.quals.Prefix.one);
        hz = builder.build();

        builder = new AnnotationBuilder(env, checkers.units.quals.s.class);
        builder.setValue("value", checkers.units.quals.Prefix.one);
        s = builder.build();

        return this;
    }

    // No multiplications yield Hertz.
    public AnnotationMirror multiplication(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        return null;
    }

    // Division of a scalar by seconds yields Hertz.
    // Other divisions yield an unannotated value.
    public AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        if (p1.getAnnotations().isEmpty() && p2.getAnnotations().contains(s)) {
            return hz;
        }

        return null;
    }

}
