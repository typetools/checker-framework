package checkers.units;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import checkers.quals.Bottom;
import checkers.quals.Unqualified;
import checkers.types.QualifierHierarchy;
import checkers.units.quals.*;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import checkers.basetype.BaseTypeChecker;

/**
 * Units Checker main class.
 *
 * Supports "units" option to add support for additional units.
 */
@SupportedOptions( { "units" } )
public class UnitsChecker extends BaseTypeChecker {

    protected Elements elements;

    // Map from canonical class name to the corresponding UnitsRelations instance.
    // We use the string to prevent instantiating the UnitsRelations multiple times.
    protected Map<String, UnitsRelations> unitsRel = new HashMap<String, UnitsRelations>();

    @Override
    public void initChecker() {
        elements = processingEnv.getElementUtils();
        super.initChecker();
    }

    /** Copied from BasicChecker and adapted "quals" to "units".
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> qualSet =
                new HashSet<Class<? extends Annotation>>();

        String qualNames = processingEnv.getOptions().get("units");
        if (qualNames == null) {
        } else {
            for (String qualName : qualNames.split(",")) {
                try {
                    final Class<? extends Annotation> q =
                            (Class<? extends Annotation>) Class.forName(qualName);

                    qualSet.add(q);
                    addUnitsRelations(q);
                } catch (ClassNotFoundException e) {
                    messager.printMessage(javax.tools.Diagnostic.Kind.WARNING,
                    		"Could not find class for unit: " + qualName + ". Ignoring unit.");
                }
            }
        }

        // Always add the default units relations.
        // TODO: we assume that all the standard units only use this. For absolute correctness,
        // go through each and look for a UnitsRelations annotation.
        unitsRel.put("checkers.units.UnitsRelationsDefault",
                new UnitsRelationsDefault().init(processingEnv));

        // Explicitly add the Unqualified type.
        qualSet.add(Unqualified.class);

        // Only add the directly supported units. Shorthands like kg are
        // handled automatically by aliases.

        qualSet.add(Length.class);
        // qualSet.add(mm.class);
        // qualSet.add(Meter.class);
        qualSet.add(m.class);
        // qualSet.add(km.class);

        qualSet.add(Time.class);
        // qualSet.add(Second.class);
        qualSet.add(s.class);
        qualSet.add(min.class);
        qualSet.add(h.class);

        qualSet.add(Speed.class);
        qualSet.add(mPERs.class);
        qualSet.add(kmPERh.class);

        qualSet.add(Area.class);
        qualSet.add(mm2.class);
        qualSet.add(m2.class);
        qualSet.add(km2.class);

        qualSet.add(Current.class);
        qualSet.add(A.class);

        qualSet.add(Mass.class);
        qualSet.add(g.class);
        // qualSet.add(kg.class);

        qualSet.add(Substance.class);
        qualSet.add(mol.class);

        qualSet.add(Luminance.class);
        qualSet.add(cd.class);

        qualSet.add(Temperature.class);
        qualSet.add(C.class);
        qualSet.add(K.class);

        // Use the framework-provided bottom qualifier. It will automatically be
        // at the bottom of the qualifier hierarchy.
        qualSet.add(Bottom.class);

        return Collections.unmodifiableSet(qualSet);
    }

    /**
     * Look for an @UnitsRelations annotation on the qualifier and
     * add it to the list of UnitsRelations.
     *
     * @param annoUtils The AnnotationUtils instance to use.
     * @param qual The qualifier to investigate.
     */
    private void addUnitsRelations(Class<? extends Annotation> qual) {
        AnnotationMirror am = AnnotationUtils.fromClass(elements, qual);

        for (AnnotationMirror ama : am.getAnnotationType().asElement().getAnnotationMirrors() ) {
            if (ama.getAnnotationType().toString().equals(UnitsRelations.class.getCanonicalName())) {
                @SuppressWarnings("unchecked")
                Class<? extends UnitsRelations> theclass = (Class<? extends UnitsRelations>)
                    AnnotationUtils.getElementValueClass(ama, "value", true);
                String classname = theclass.getCanonicalName();

                if (!unitsRel.containsKey(classname)) {
                    try {
                        unitsRel.put(classname, ((UnitsRelations) theclass.newInstance()).init(processingEnv));
                    } catch (InstantiationException e) {
                        // TODO
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /** Copied from BasicChecker; cannot reuse it, because BasicChecker is final.
     * TODO: BasicChecker might also want to always call super.
     */
    @Override
    public Collection<String> getSuppressWarningsKey() {
        Set<String> swKeys = new HashSet<String>(super.getSuppressWarningsKey());
        Set<Class<? extends Annotation>> annos = getSupportedTypeQualifiers();

        for (Class<? extends Annotation> anno : annos) {
            swKeys.add(anno.getSimpleName().toLowerCase());
        }

        return swKeys;
    }

    /* Set the Bottom qualifier as the bottom of the hierarchy.
     */
    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UnitsQualifierHierarchy(factory, AnnotationUtils.fromClass(elements, Bottom.class));
    }

    protected class UnitsQualifierHierarchy extends GraphQualifierHierarchy {

        public UnitsQualifierHierarchy(MultiGraphFactory f,
                AnnotationMirror bottom) {
            super(f, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
                return AnnotationUtils.areSame(lhs, rhs);
            }
            lhs = stripValues(lhs);
            rhs = stripValues(rhs);

            return super.isSubtype(rhs, lhs);
        }
    }

    private AnnotationMirror stripValues(AnnotationMirror anno) {
        return AnnotationUtils.fromName(elements, anno.getAnnotationType().toString());
    }
}
