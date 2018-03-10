package qual;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.UnitsRelations;
import org.checkerframework.checker.units.UnitsRelationsTools;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.s;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/** Relations among units of frequency. */
public class FrequencyRelations implements UnitsRelations {

    protected AnnotationMirror hertz, kilohertz, second, millisecond;
    protected Elements elements;

    public UnitsRelations init(ProcessingEnvironment env) {
        elements = env.getElementUtils();

        // create Annotation Mirrors, each representing a particular Unit's Annotation
        hertz = UnitsRelationsTools.buildAnnoMirrorWithDefaultPrefix(env, Hz.class);
        kilohertz =
                UnitsRelationsTools.buildAnnoMirrorWithSpecificPrefix(env, Hz.class, Prefix.kilo);
        second = UnitsRelationsTools.buildAnnoMirrorWithDefaultPrefix(env, s.class);
        millisecond =
                UnitsRelationsTools.buildAnnoMirrorWithSpecificPrefix(env, s.class, Prefix.milli);

        return this;
    }

    /** No multiplications yield Hertz. */
    public @Nullable AnnotationMirror multiplication(
            AnnotatedTypeMirror lht, AnnotatedTypeMirror rht) {
        // return null so the default units relations can process multiplcations of other units
        return null;
    }

    /**
     * Division of a scalar by seconds yields Hertz. Division of a scalar by milliseconds yields
     * Kilohertz. Other divisions yield an unannotated value.
     */
    public @Nullable AnnotationMirror division(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht) {
        if (UnitsRelationsTools.hasNoUnits(lht)) {
            // scalar / millisecond => kilohertz
            if (UnitsRelationsTools.hasSpecificUnit(rht, millisecond)) {
                return kilohertz;
            }
            // scalar / second => hertz
            else if (UnitsRelationsTools.hasSpecificUnit(rht, second)) {
                return hertz;
            }
        }

        return null;
    }
}
