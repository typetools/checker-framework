package checkers.units;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.AnnotationMirror;

import checkers.quals.Bottom;
import checkers.quals.Unqualified;
import checkers.types.QualifierHierarchy;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.units.quals.*;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.basetype.BaseTypeChecker;

/**
 * Units Checker main class.
 *
 * Supports "units" option to add support for additional units.
 */
@SupportedOptions( { "units" } )
public class UnitsChecker extends BaseTypeChecker {

    // Map from canonical class name to the corresponding UnitsRelations instance.
    // We use the string to prevent instantiating the UnitsRelations multiple times.
    protected Map<String, UnitsRelations> unitsRel = new HashMap<String, UnitsRelations>();
    protected AnnotationUtils utils;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        utils = AnnotationUtils.getInstance(env);
        super.initChecker(env);
    }

    /** Copied from BasicChecker and adapted "quals" to "units".
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        AnnotationUtils annoUtils = AnnotationUtils.getInstance(env);

        Set<Class<? extends Annotation>> qualSet =
                new HashSet<Class<? extends Annotation>>();

        String qualNames = env.getOptions().get("units");
        if (qualNames == null) {
        } else {
            for (String qualName : qualNames.split(",")) {
                try {
                    final Class<? extends Annotation> q =
                            (Class<? extends Annotation>) Class.forName(qualName);

                    qualSet.add(q);
                    addUnitsRelations(annoUtils, q);
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
                new UnitsRelationsDefault().init(annoUtils, env));

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
    private void addUnitsRelations(AnnotationUtils annoUtils, Class<? extends Annotation> qual) {
        AnnotationMirror am = annoUtils.fromClass(qual);

        for (AnnotationMirror ama : am.getAnnotationType().asElement().getAnnotationMirrors() ) {
            if (ama.getAnnotationType().toString().equals(UnitsRelations.class.getCanonicalName())) {
                @SuppressWarnings("unchecked")
                Class<? extends UnitsRelations> theclass = (Class<? extends UnitsRelations>)
                    AnnotationUtils.parseTypeValue(ama, "value");
                String classname = theclass.getCanonicalName();

                if (!unitsRel.containsKey(classname)) {
                    try {
                        unitsRel.put(classname, ((UnitsRelations) theclass.newInstance()).init(annoUtils, env));
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
    protected GraphQualifierHierarchy.GraphFactory createQualifierHierarchyFactory() {
        return new GraphQualifierHierarchy.GraphFactory(this, AnnotationUtils.getInstance(env).fromClass(Bottom.class));
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new UnitsQualifierHierarchy((GraphQualifierHierarchy)super.createQualifierHierarchy());
    }

    protected class UnitsQualifierHierarchy extends GraphQualifierHierarchy {

        public UnitsQualifierHierarchy(GraphQualifierHierarchy hierarchy) {
            super(hierarchy);
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
        return utils.fromName(anno.getAnnotationType().toString());
    }
}
