package org.checkerframework.checker.units;

import org.checkerframework.checker.units.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic.Kind;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

/**
 * Annotated type factory for the Units Checker.
 *
 * Handles multiple names for the same unit, with different prefixes,
 * e.g. @kg is the same as @g(Prefix.kilo).
 *
 * Supports relations between units, e.g. if "m" is a variable of type "@m" and
 * "s" is a variable of type "@s", the division "m/s" is automatically annotated
 * as "mPERs", the correct unit for the result.
 */
public class UnitsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror mixedUnits = AnnotationUtils.fromClass(elements, MixedUnits.class);
    protected AnnotationMirror BOTTOM;

    // Map from canonical class name to the corresponding UnitsRelations instance.
    // We use the string to prevent instantiating the UnitsRelations multiple times.
    private Map<String, UnitsRelations> unitsRel;

    private final Map<String, AnnotationMirror> aliasMap = new HashMap<String, AnnotationMirror>();

    public UnitsAnnotatedTypeFactory(BaseTypeChecker checker) {
        // use true for flow inference
        super(checker, false);

        BOTTOM = AnnotationUtils.fromClass(elements, UnitsBottom.class);
        this.postInit();

        addTypeNameImplicit(java.lang.Void.class, BOTTOM);
    }

    protected Map<String, UnitsRelations> getUnitsRel() {
        if (unitsRel == null) {
            unitsRel = new HashMap<String, UnitsRelations>();
        }
        return unitsRel;
    }

    @Override
    public AnnotationMirror aliasedAnnotation(AnnotationMirror a) {
        String aname = a.getAnnotationType().toString();
        if (aliasMap.containsKey(aname)) {
            return aliasMap.get(aname);
        }
        for (AnnotationMirror aa : a.getAnnotationType().asElement().getAnnotationMirrors() ) {
            // TODO: Is using toString the best way to go?
            if (aa.getAnnotationType().toString().equals(UnitsMultiple.class.getCanonicalName())) {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> theclass = (Class<? extends Annotation>)
                                                    AnnotationUtils.getElementValueClass(aa, "quantity", true);
                Prefix prefix = AnnotationUtils.getElementValueEnum(aa, "prefix", Prefix.class, true);
                AnnotationBuilder builder = new AnnotationBuilder(processingEnv, theclass);
                builder.setValue("value", prefix);
                AnnotationMirror res = builder.build();
                aliasMap.put(aname, res);
                return res;
            }
        }
        return super.aliasedAnnotation(a);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        Set<Class<? extends Annotation>> qualSet =
                new HashSet<Class<? extends Annotation>>();

        String qualNames = checker.getOption("units");
        if (qualNames == null) {
        } else {
            for (String qualName : qualNames.split(",")) {
                try {
                    final Class<? extends Annotation> q =
                            (Class<? extends Annotation>) Class.forName(qualName);

                    qualSet.add(q);
                    addUnitsRelations(q);
                } catch (ClassNotFoundException e) {
                    checker.message(javax.tools.Diagnostic.Kind.WARNING,
                            "Could not find class for unit: " + qualName + ". Ignoring unit.");
                }
            }
        }

        // Always add the default units relations.
        // TODO: we assume that all the standard units only use this. For absolute correctness,
        // go through each and look for a UnitsRelations annotation.
        getUnitsRel().put("org.checkerframework.checker.units.UnitsRelationsDefault",
                new UnitsRelationsDefault().init(processingEnv));

        // Explicitly add the top type.
        qualSet.add(UnknownUnits.class);
        qualSet.add(PolyUnit.class);
        qualSet.add(PolyAll.class);

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

        qualSet.add(Acceleration.class);
        qualSet.add(mPERs2.class);

        qualSet.add(Angle.class);
        qualSet.add(degrees.class);
        qualSet.add(radians.class);

        // Use the framework-provided bottom qualifier. It will automatically be
        // at the bottom of the qualifier hierarchy.
        qualSet.add(UnitsBottom.class);

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

                if (!getUnitsRel().containsKey(classname)) {
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

    @Override
    public TreeAnnotator createTreeAnnotator() {
        ImplicitsTreeAnnotator implicitsTreeAnnotator = new ImplicitsTreeAnnotator(this);
        implicitsTreeAnnotator.addTreeKind(Tree.Kind.NULL_LITERAL, BOTTOM);
        return new ListTreeAnnotator(
                new UnitsPropagationTreeAnnotator(this),
                implicitsTreeAnnotator,
                new UnitsTreeAnnotator(this)
        );
    }

    private static class UnitsPropagationTreeAnnotator extends PropagationTreeAnnotator {

        public UnitsPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }


        // Handled completely by UnitsTreeAnnotator
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }

        // Handled completely by UnitsTreeAnnotator
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            return null;
        }
    }

    /**
     * A class for adding annotations based on tree
     */
    private class UnitsTreeAnnotator extends TreeAnnotator {

        UnitsTreeAnnotator(UnitsAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror lht = getAnnotatedType(node.getLeftOperand());
            AnnotatedTypeMirror rht = getAnnotatedType(node.getRightOperand());
            Tree.Kind kind = node.getKind();

            AnnotationMirror bestres = null;
            for (UnitsRelations ur : unitsRel.values()) {
                AnnotationMirror res = useUnitsRelation(kind, ur, lht, rht);

                if (bestres != null && res != null && !bestres.equals(res)) {
                    checker.message(Kind.WARNING,
                            "UnitsRelation mismatch, taking neither! Previous: "
                                    + bestres + " and current: " + res);
                    return null; // super.visitBinary(node, type);
                }

                if (res!=null) {
                    bestres = res;
                }
            }

            if (bestres!=null) {
                type.addAnnotation(bestres);
            } else {
                // Handle the binary operations that do not produce a UnitsRelation.

                switch (kind) {
                case MINUS:
                case PLUS:
                    if (lht.getAnnotations().equals(rht.getAnnotations())) {
                        // The sum or difference has the same units as both
                        // operands.
                        type.addAnnotations(lht.getAnnotations());
                        break;
                    } else {
                        type.addAnnotation(mixedUnits);
                        break;
                    }
                case DIVIDE:
                    if (lht.getAnnotations().equals(rht.getAnnotations())) {
                        // If the units of the division match,
                        // do not add an annotation to the result type, keep it
                        // unqualified.
                        break;
                    }
                    break;
                case MULTIPLY:
                    if (noUnits(lht)) {
                        type.addAnnotations(rht.getAnnotations());
                        break;
                    }
                    if (noUnits(rht)) {
                        type.addAnnotations(lht.getAnnotations());
                        break;
                    }
                    type.addAnnotation(mixedUnits);
                    break;

                    // Placeholders for unhandled binary operations
                case REMAINDER:
                    // The checker disallows the following:
                    //     @Length int q = 10 * UnitTools.m;
                    //     @Length int r = q % 3;
                    // This seems wrong because it allows this:
                    //     @Length int r = q - (q / 3) * 3;
                    // TODO: We agreed to treat remainder like division.
                    break;
                default:
                    // Do nothing
                }
            }

            return null; // super.visitBinary(node, type);
        }

        private boolean noUnits(AnnotatedTypeMirror t) {
            Set<AnnotationMirror> annos = t.getAnnotations();
            return annos.isEmpty() ||
                    (annos.size() == 1 &&
                    AnnotationUtils.areSameByClass(annos.iterator().next(), UnknownUnits.class));
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            ExpressionTree var = node.getVariable();
            AnnotatedTypeMirror varType = getAnnotatedType(var);

            type.replaceAnnotations(varType.getAnnotations());
            return null;
        }

        private AnnotationMirror useUnitsRelation(Tree.Kind kind, UnitsRelations ur,
                AnnotatedTypeMirror lht, AnnotatedTypeMirror rht) {

            AnnotationMirror res = null;
            if (ur!=null) {
                switch (kind) {
                case DIVIDE:
                    res = ur.division(lht, rht);
                    break;
                case MULTIPLY:
                    res = ur.multiplication(lht, rht);
                    break;
                default:
                    // Do nothing
                }
            }
            return res;
        }

    }


    /* Set the Bottom qualifier as the bottom of the hierarchy.
     */
    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UnitsQualifierHierarchy(factory, AnnotationUtils.fromClass(elements, UnitsBottom.class));
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
