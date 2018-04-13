package org.checkerframework.checker.units;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
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

/** Default relations between SI units. TODO: what relations are missing? */
public class UnitsRelationsDefault implements UnitsRelations {
    protected AnnotationMirror m, km, mm, m2, km2, mm2, s, h, mPERs, kmPERh, mPERs2;
    protected Elements elements;

    /**
     * Constructs various AnnotationMirrors representing specific checker-framework provided Units
     * involved in the rules resolved in this UnitsRelations implementation
     */
    @Override
    public UnitsRelations init(ProcessingEnvironment env) {
        elements = env.getElementUtils();

        m = UnitsRelationsTools.buildAnnoMirrorWithDefaultPrefix(env, m.class);
        km = UnitsRelationsTools.buildAnnoMirrorWithSpecificPrefix(env, m.class, Prefix.kilo);
        mm = UnitsRelationsTools.buildAnnoMirrorWithSpecificPrefix(env, m.class, Prefix.milli);

        m2 = UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(env, m2.class);
        km2 = UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(env, km2.class);
        mm2 = UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(env, mm2.class);

        s = UnitsRelationsTools.buildAnnoMirrorWithDefaultPrefix(env, s.class);
        h = UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(env, h.class);

        mPERs = UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(env, mPERs.class);
        kmPERh = UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(env, kmPERh.class);

        mPERs2 = UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(env, mPERs2.class);

        return this;
    }

    /**
     * Provides rules for resolving the result Unit of the multiplication of checker-framework
     * provided Units
     */
    @Override
    public @Nullable AnnotationMirror multiplication(
            AnnotatedTypeMirror lht, AnnotatedTypeMirror rht) {
        // TODO: does this handle scaling correctly?

        // length * length => area
        // checking SI units only
        if (UnitsRelationsTools.hasSpecificUnitIgnoringPrefix(lht, m)
                && UnitsRelationsTools.hasSpecificUnitIgnoringPrefix(rht, m)) {
            if (UnitsRelationsTools.hasNoPrefix(lht) && UnitsRelationsTools.hasNoPrefix(rht)) {
                // m * m
                return m2;
            }

            Prefix lhtPrefix = UnitsRelationsTools.getPrefix(lht);
            Prefix rhtPrefix = UnitsRelationsTools.getPrefix(rht);

            if (bothHaveSpecificPrefix(lhtPrefix, rhtPrefix, Prefix.kilo)) {
                // km * km
                return km2;
            } else if (bothHaveSpecificPrefix(lhtPrefix, rhtPrefix, Prefix.one)) {
                // m(Prefix.one) * m(Prefix.one)
                return m2;
            } else if (bothHaveSpecificPrefix(lhtPrefix, rhtPrefix, Prefix.milli)) {
                // mm * mm
                return mm2;
            } else {
                return null;
            }
        } else if (havePairOfUnitsIgnoringOrder(lht, s, rht, mPERs)) {
            // s * mPERs or mPERs * s => m
            return m;
        } else if (havePairOfUnitsIgnoringOrder(lht, s, rht, mPERs2)) {
            // s * mPERs2 or mPERs2 * s => mPERs
            return mPERs;
        } else if (havePairOfUnitsIgnoringOrder(lht, h, rht, kmPERh)) {
            // h * kmPERh or kmPERh * h => km
            return km;
        } else {
            return null;
        }
    }

    /**
     * Provides rules for resolving the result Unit of the division of checker-framework provided
     * Units
     */
    @Override
    public @Nullable AnnotationMirror division(AnnotatedTypeMirror lht, AnnotatedTypeMirror rht) {
        if (havePairOfUnits(lht, m, rht, s)) {
            // m / s => mPERs
            return mPERs;
        } else if (havePairOfUnits(lht, km, rht, h)) {
            // km / h => kmPERh
            return kmPERh;
        } else if (havePairOfUnits(lht, m2, rht, m)) {
            // m2 / m => m
            return m;
        } else if (havePairOfUnits(lht, km2, rht, km)) {
            // km2 / km => km
            return km;
        } else if (havePairOfUnits(lht, mm2, rht, mm)) {
            // mm2 / mm => mm
            return mm;
        } else if (havePairOfUnits(lht, m, rht, mPERs)) {
            // m / mPERs => s
            return s;
        } else if (havePairOfUnits(lht, km, rht, kmPERh)) {
            // km / kmPERh => h
            return h;
        } else if (havePairOfUnits(lht, mPERs, rht, s)) {
            // mPERs / s = mPERs2
            return mPERs2;
        } else if (havePairOfUnits(lht, mPERs, rht, mPERs2)) {
            // mPERs / mPERs2 => s  (velocity / acceleration == time)
            return s;
        } else {
            return null;
        }
    }

    /**
     * Checks to see if both lhtPrefix and rhtPrefix have the same prefix as specificPrefix
     *
     * @param lhtPrefix left hand side prefix
     * @param rhtPrefix right hand side prefix
     * @param specificPrefix specific desired prefix to match
     * @return true if all 3 Prefix are the same, false otherwise
     */
    protected boolean bothHaveSpecificPrefix(
            Prefix lhtPrefix, Prefix rhtPrefix, Prefix specificPrefix) {
        if (lhtPrefix == null || rhtPrefix == null || specificPrefix == null) {
            return false;
        }

        return (lhtPrefix.equals(rhtPrefix) && rhtPrefix.equals(specificPrefix));
    }

    /**
     * Checks to see if lht has the unit ul and if rht has the unit ur all at the same time
     *
     * @param lht left hand annotated type
     * @param ul left hand unit
     * @param rht right hand annotated type
     * @param ur right hand unit
     * @return true if lht has lu and rht has ru, false otherwise
     */
    protected boolean havePairOfUnits(
            AnnotatedTypeMirror lht,
            AnnotationMirror ul,
            AnnotatedTypeMirror rht,
            AnnotationMirror ur) {
        return UnitsRelationsTools.hasSpecificUnit(lht, ul)
                && UnitsRelationsTools.hasSpecificUnit(rht, ur);
    }

    /**
     * Checks to see if lht and rht have the pair of units u1 and u2 regardless of order
     *
     * @param lht left hand annotated type
     * @param u1 unit 1
     * @param rht right hand annotated type
     * @param u2 unit 2
     * @return true if lht and rht have the pair of units u1 and u2 regardless of order, false
     *     otherwise
     */
    protected boolean havePairOfUnitsIgnoringOrder(
            AnnotatedTypeMirror lht,
            AnnotationMirror u1,
            AnnotatedTypeMirror rht,
            AnnotationMirror u2) {
        return havePairOfUnits(lht, u1, rht, u2) || havePairOfUnits(lht, u2, rht, u1);
    }
}
