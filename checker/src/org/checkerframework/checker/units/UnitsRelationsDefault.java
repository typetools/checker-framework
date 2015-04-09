package org.checkerframework.checker.units;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
 */

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import java.util.Map;

import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.m;
import org.checkerframework.checker.units.qual.m2;
import org.checkerframework.checker.units.qual.mPERs;
import org.checkerframework.checker.units.qual.mPERs2;
import org.checkerframework.checker.units.qual.km2;
import org.checkerframework.checker.units.qual.kmPERh;
import org.checkerframework.checker.units.qual.mm2;
import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.s;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

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

            // prefix of null means we couldn't find or assign any prefixes at all (error state)
            if(p1prefix == null || p2prefix == null) {
                return null;
            }
            // km * km
            else if(p1prefix.equals(p2prefix) && p2prefix.equals(Prefix.kilo)) {
                return km2;
            }
            // mm * mm
            else if(p1prefix.equals(p2prefix) && p2prefix.equals(Prefix.milli)) {
                return mm2;
            }
            // m * m
            else if(p1prefix.equals(p2prefix) && p2prefix.equals(Prefix.one)) {
                return m2;
            }
            else {
                return null;
            }
        }
        // s * mPERs or mPERs * s => m
        else if (    AnnotationUtils.containsSame(p1.getAnnotations(), s) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs)
                ||
                AnnotationUtils.containsSame(p1.getAnnotations(), mPERs) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)    ) {
            return m;
        }
        // s * mPERs2 or mPERs2 * s => mPERs
        else if (    AnnotationUtils.containsSame(p1.getAnnotations(), s) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs2)
                ||
                AnnotationUtils.containsSame(p1.getAnnotations(), mPERs2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)    ) {
            return mPERs;
        }
        // h * kmPERh or kmPERh * h => km
        else if (    AnnotationUtils.containsSame(p1.getAnnotations(), h) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), kmPERh)
                ||
                AnnotationUtils.containsSame(p1.getAnnotations(), kmPERh) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), h)) {
            return km;
        }
        else {
            return null;
        }
    }

    @Override
    public /*@Nullable*/ AnnotationMirror division(AnnotatedTypeMirror p1, AnnotatedTypeMirror p2) {
        // m / s => mPERs
        if (AnnotationUtils.containsSame(p1.getAnnotations(), m) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)) {
            return mPERs;
        }
        // km / h => kmPERh
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), km) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), h)) {
            return kmPERh;
        }
        // m2 / m => m
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), m2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), m)) {
            return m;
        }
        // km2 / km => km
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), km2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), km)) {
            return km;
        }
        // mm2 / mm => mm
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), mm2) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mm)) {
            return mm;
        }
        // m / mPERs => s
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), m) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs)) {
            return s;
        }
        // km / kmPERh => h
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), km) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), kmPERh)) {
            return h;
        }
        // mPERs / s = mPERs2
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), mPERs) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), s)) {
            return mPERs2;
        }
        // mPERs / mPERs2 => s  (velocity / acceleration == time)
        else if (AnnotationUtils.containsSame(p1.getAnnotations(), mPERs) &&
                AnnotationUtils.containsSame(p2.getAnnotations(), mPERs2)) {
            return s;
        }
        else {
            return null;
        }
    }

    // helper functions
    // go through each annotation of an annotated type, find the prefix and return it
    private Prefix getTypeMirrorPrefix(AnnotatedTypeMirror atm)
    {
        for(AnnotationMirror mirror : atm.getAnnotations())
        {
            AnnotationValue annotationValue = getAnnotationMirrorPrefix(mirror);
            // annotation has no element value (ie no SI prefix)
            if(annotationValue == null) {
                return Prefix.one;
            }
            // if the annotation has a value, then detect the string name of the prefix and return the Prefix
            String prefixString = annotationValue.getValue().toString();
            if(prefixString.equals(Prefix.kilo.toString())) {
                return Prefix.kilo;
            }
            else if(prefixString.equals(Prefix.milli.toString())) {
                return Prefix.milli;
            }
            else if(prefixString.equals(Prefix.one.toString())) {
                return Prefix.one;
            }
        }
        return null;
    }

    // given an annotation, returns the prefix value (eg kilo) if there is any, otherwise returns null
    private AnnotationValue getAnnotationMirrorPrefix(AnnotationMirror mirror)
    {
        Map<? extends ExecutableElement,? extends AnnotationValue> elementValues = mirror.getElementValues();
        for (Map.Entry<? extends ExecutableElement,? extends AnnotationValue> entry : elementValues.entrySet()) {
            if ("value".equals(entry.getKey().getSimpleName().toString())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
