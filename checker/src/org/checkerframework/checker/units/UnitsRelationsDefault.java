package org.checkerframework.checker.units;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.km2;
import org.checkerframework.checker.units.qual.kmPERh;
import org.checkerframework.checker.units.qual.m;
import org.checkerframework.checker.units.qual.m2;
import org.checkerframework.checker.units.qual.mPERs;
import org.checkerframework.checker.units.qual.mPERs2;
import org.checkerframework.checker.units.qual.mm2;
import org.checkerframework.checker.units.qual.s;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;

/**
 * Default relations between SI units.
 * TODO: what relations are missing?
 */
public class UnitsRelationsDefault implements UnitsRelations {

    protected AnnotationMirror m, km, mm, m2, km2, mm2, s, h, mPERs, mPERs2, kmPERh;

    @Override
    public UnitsRelations init(ProcessingEnvironment env) {
        AnnotationBuilder builder = new AnnotationBuilder(env, m.class);
        Elements elements = env.getElementUtils();

        builder.setValue("value", Prefix.one);
        m = builder.build();

        builder = new AnnotationBuilder(env, m.class);
        builder.setValue("value", Prefix.kilo);
        km = builder.build();

        builder = new AnnotationBuilder(env, m.class);
        builder.setValue("value", Prefix.milli);
        mm = builder.build();

        m2 = AnnotationUtils.fromClass(elements, m2.class);
        km2 = AnnotationUtils.fromClass(elements, km2.class);
        mm2 = AnnotationUtils.fromClass(elements, mm2.class);

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

        // length * length => area
        if (AnnotationUtils.containsSameIgnoringValues(p1.getAnnotations(), m) &&
                AnnotationUtils.containsSameIgnoringValues(p2.getAnnotations(), m)) {
            Prefix p1prefix = getTypeMirrorPrefix(p1);
            Prefix p2prefix = getTypeMirrorPrefix(p2);

            if (p1prefix == null || p2prefix == null) {
                // prefix of null means we couldn't find or assign any prefixes at all (error state)
                return null;
            } else if (p1prefix.equals(p2prefix) && p2prefix.equals(Prefix.kilo)) {
                // km * km
                return km2;
            } else if (p1prefix.equals(p2prefix) && p2prefix.equals(Prefix.milli)) {
                // mm * mm
                return mm2;
            } else if (p1prefix.equals(p2prefix) && p2prefix.equals(Prefix.one)) {
                // m * m
                return m2;
            } else {
                return null;
            }
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), s) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs)
                ||
                AnnotationUtils.containsSame(p1.getAnnotations(), mPERs) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)) {
            // s * mPERs or mPERs * s => m
            return m;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), s) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs2)
                ||
                AnnotationUtils.containsSame(p1.getAnnotations(), mPERs2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)) {
            // s * mPERs2 or mPERs2 * s => mPERs
            return mPERs;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), h) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), kmPERh)
                ||
                AnnotationUtils.containsSame(p1.getAnnotations(), kmPERh) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), h)) {
            // h * kmPERh or kmPERh * h => km
            return km;
        } else {
            return null;
        }
    }

    @Override
    public /*@Nullable*/ AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        if (AnnotationUtils.containsSame(p1.getAnnotations(), m) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)) {
            // m / s => mPERs
            return mPERs;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), km) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), h)) {
            // km / h => kmPERh
            return kmPERh;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), m2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), m)) {
            // m2 / m => m
            return m;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), km2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), km)) {
            // km2 / km => km
            return km;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), mm2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mm)) {
            // mm2 / mm => mm
            return mm;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), m) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs)) {
            // m / mPERs => s
            return s;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), km) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), kmPERh)) {
            // km / kmPERh => h
            return h;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), mPERs) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)) {
            // mPERs / s = mPERs2
            return mPERs2;
        } else if (AnnotationUtils.containsSame(p1.getAnnotations(), mPERs) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs2)) {
            // mPERs / mPERs2 => s  (velocity / acceleration == time)
            return s;
        } else {
            return null;
        }
    }

    // helper functions
    // go through each annotation of an annotated type, find the prefix and return it
    private Prefix getTypeMirrorPrefix(AnnotatedTypeMirror atm) {
        for (AnnotationMirror mirror : atm.getAnnotations()) {
            AnnotationValue annotationValue = getAnnotationMirrorPrefix(mirror);
            // annotation has no element value (ie no SI prefix)
            if (annotationValue == null) {
                return Prefix.one;
            }
            // if the annotation has a value, then detect the string name of the prefix and return the Prefix
            String prefixString = annotationValue.getValue().toString();
            if (prefixString.equals(Prefix.kilo.toString())) {
                return Prefix.kilo;
            } else if (prefixString.equals(Prefix.milli.toString())) {
                return Prefix.milli;
            } else if (prefixString.equals(Prefix.one.toString())) {
                return Prefix.one;
            }
        }
        return null;
    }

    // given an annotation, returns the prefix value (eg kilo) if there is any, otherwise returns null
    private AnnotationValue getAnnotationMirrorPrefix(AnnotationMirror mirror) {
        Map<? extends ExecutableElement,? extends AnnotationValue> elementValues = mirror.getElementValues();
        for (Map.Entry<? extends ExecutableElement,? extends AnnotationValue> entry : elementValues.entrySet()) {
            if ("value".equals(entry.getKey().getSimpleName().toString())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
