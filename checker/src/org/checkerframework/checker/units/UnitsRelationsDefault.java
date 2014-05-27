package org.checkerframework.checker.units;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.km2;
import org.checkerframework.checker.units.qual.kmPERh;
import org.checkerframework.checker.units.qual.m;
import org.checkerframework.checker.units.qual.m2;
import org.checkerframework.checker.units.qual.mPERs;
import org.checkerframework.checker.units.qual.mPERs2;
import org.checkerframework.checker.units.qual.s;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Default relations between SI units.
 * TODO: what relations are missing?
 */
public class UnitsRelationsDefault implements UnitsRelations {

    protected AnnotationMirror m, km, m2, km2, s, h, mPERs, mPERs2, kmPERh;

    @Override
    public UnitsRelations init(ProcessingEnvironment env) {
        AnnotationBuilder builder = new AnnotationBuilder(env, m.class);
        Elements elements = env.getElementUtils();

        builder.setValue("value", Prefix.one);
        m = builder.build();

        builder = new AnnotationBuilder(env, m.class);
        builder.setValue("value", Prefix.kilo);
        km = builder.build();

        m2 = AnnotationUtils.fromClass(elements, m2.class);
        km2 = AnnotationUtils.fromClass(elements, km2.class);

        builder = new AnnotationBuilder(env, s.class);
        builder.setValue("value", Prefix.one);
        s = builder.build();
        h = AnnotationUtils.fromClass(elements, h.class);

        mPERs = AnnotationUtils.fromClass(elements, mPERs.class);
        kmPERh = AnnotationUtils.fromClass(elements, kmPERh.class);

        mPERs2 = AnnotationUtils.fromClass(elements, mPERs2.class);

        return this;
    }

    @Override
    public /*@Nullable*/ AnnotationMirror multiplication(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
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
    public /*@Nullable*/ AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
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
