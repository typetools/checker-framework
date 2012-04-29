package checkers.units;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotatedTypeMirror;
import checkers.units.quals.Prefix;
import checkers.units.quals.h;
import checkers.units.quals.km2;
import checkers.units.quals.kmPERh;
import checkers.units.quals.m;
import checkers.units.quals.m2;
import checkers.units.quals.mPERs;
import checkers.units.quals.s;
import checkers.util.AnnotationUtils;
import checkers.util.AnnotationUtils.AnnotationBuilder;

/**
 * Default relations between SI units.
 * TODO: what relations are missing?
 */
public class UnitsRelationsDefault implements UnitsRelations {

    protected AnnotationMirror m, km, m2, km2, s, h, mPERs, kmPERh;

    @Override
    public UnitsRelations init(AnnotationUtils annos, ProcessingEnvironment env) {
        AnnotationBuilder builder = new AnnotationBuilder(env, m.class);
        builder.setValue("value", Prefix.one);
        m = builder.build();

        builder = new AnnotationBuilder(env, m.class);
        builder.setValue("value", Prefix.kilo);
        km = builder.build();

        m2 = annos.fromClass(m2.class);
        km2 = annos.fromClass(km2.class);

        builder = new AnnotationBuilder(env, s.class);
        builder.setValue("value", Prefix.one);
        s = builder.build();
        h = annos.fromClass(h.class);

        mPERs = annos.fromClass(mPERs.class);
        kmPERh = annos.fromClass(kmPERh.class);

        return this;
    }

    @Override
    public AnnotationMirror multiplication(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        // TODO: does this handle scaling correctly?
        if (AnnotationUtils.containsSameIgnoringValues(p1.getAnnotations(), m) &&
                AnnotationUtils.containsSameIgnoringValues(p2.getAnnotations(), m)) {
            return m2;
        }

        if (AnnotationUtils.containsSameIgnoringValues(p1.getAnnotations(), km) &&
                AnnotationUtils.containsSameIgnoringValues(p2.getAnnotations(), km)) {
            return km2;
        }

        return null;
    }

    @Override
    public AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        if (AnnotationUtils.containsSameIgnoringValues(p1.getAnnotations(), m) &&
                AnnotationUtils.containsSameIgnoringValues(p2.getAnnotations(), s)) {
            return mPERs;
        }

        if (AnnotationUtils.containsSameIgnoringValues(p1.getAnnotations(), km) &&
                AnnotationUtils.containsSameIgnoringValues(p2.getAnnotations(), h)) {
            return kmPERh;
        }

        return null;
    }

}