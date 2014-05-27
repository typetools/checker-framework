import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.units.UnitsRelations;
import org.checkerframework.checker.units.qual.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationBuilder;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

/** Relations among units of frequency. */
public class FrequencyRelations implements UnitsRelations {

    protected AnnotationMirror hz, s;

    public UnitsRelations init(ProcessingEnvironment env) {
        AnnotationBuilder builder = new AnnotationBuilder(env, Hz.class);
        builder.setValue("value", Prefix.one);
        hz = builder.build();

        builder = new AnnotationBuilder(env,  org.checkerframework.checker.units.qual.s.class);
        builder.setValue("value", Prefix.one);
        s = builder.build();

        return this;
    }

    // No multiplications yield Hertz.
    public /*@Nullable*/ AnnotationMirror multiplication(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        return null;
    }

    // Division of a scalar by seconds yields Hertz.
    // Other divisions yield an unannotated value.
    public /*@Nullable*/ AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        if (p1.getAnnotations().isEmpty() && p2.getAnnotations().contains(s)) {
            return hz;
        }

        return null;
    }

}
