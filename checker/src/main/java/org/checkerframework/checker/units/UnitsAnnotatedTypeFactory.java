package org.checkerframework.checker.units;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.MixedUnits;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.UnitsBottom;
import org.checkerframework.checker.units.qual.UnitsMultiple;
import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFormatter;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotationClassLoader;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/**
 * Annotated type factory for the Units Checker.
 *
 * <p>Handles multiple names for the same unit, with different prefixes, e.g. @kg is the same
 * as @g(Prefix.kilo).
 *
 * <p>Supports relations between units, e.g. if "m" is a variable of type "@m" and "s" is a variable
 * of type "@s", the division "m/s" is automatically annotated as "mPERs", the correct unit for the
 * result.
 */
public class UnitsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private static final Class<org.checkerframework.checker.units.qual.UnitsRelations>
            unitsRelationsAnnoClass = org.checkerframework.checker.units.qual.UnitsRelations.class;

    protected final AnnotationMirror mixedUnits =
            AnnotationBuilder.fromClass(elements, MixedUnits.class);
    protected final AnnotationMirror TOP =
            AnnotationBuilder.fromClass(elements, UnknownUnits.class);
    protected final AnnotationMirror BOTTOM =
            AnnotationBuilder.fromClass(elements, UnitsBottom.class);

    /**
     * Map from canonical class name to the corresponding UnitsRelations instance. We use the string
     * to prevent instantiating the UnitsRelations multiple times.
     */
    private Map<String, UnitsRelations> unitsRel;

    private static final Map<String, Class<? extends Annotation>> externalQualsMap =
            new HashMap<>();

    private static final Map<String, AnnotationMirror> aliasMap = new HashMap<>();

    public UnitsAnnotatedTypeFactory(BaseTypeChecker checker) {
        // use true to enable flow inference, false to disable it
        super(checker, false);

        this.postInit();
    }

    // In Units Checker, we always want to print out the Invisible Qualifiers
    // (UnknownUnits), and to format the print out of qualifiers by removing
    // Prefix.one
    @Override
    protected AnnotatedTypeFormatter createAnnotatedTypeFormatter() {
        return new UnitsAnnotatedTypeFormatter(checker);
    }

    // Converts all metric-prefixed units' alias annotations (eg @kg) into base unit annotations
    // with prefix values (eg @g(Prefix.kilo))
    @Override
    public AnnotationMirror aliasedAnnotation(AnnotationMirror anno) {
        // Get the name of the aliased annotation
        String aname = anno.getAnnotationType().toString();

        // See if we already have a map from this aliased annotation to its corresponding base unit
        // annotation
        if (aliasMap.containsKey(aname)) {
            // if so return it
            return aliasMap.get(aname);
        }

        boolean built = false;
        AnnotationMirror result = null;
        // if not, look for the UnitsMultiple meta annotations of this aliased annotation
        for (AnnotationMirror metaAnno :
                anno.getAnnotationType().asElement().getAnnotationMirrors()) {
            // see if the meta annotation is UnitsMultiple
            if (isUnitsMultiple(metaAnno)) {
                // retrieve the Class of the base unit annotation
                Class<? extends Annotation> baseUnitAnnoClass =
                        AnnotationUtils.getElementValueClass(metaAnno, "quantity", true)
                                .asSubclass(Annotation.class);

                // retrieve the SI Prefix of the aliased annotation
                Prefix prefix =
                        AnnotationUtils.getElementValueEnum(metaAnno, "prefix", Prefix.class, true);

                // Build a base unit annotation with the prefix applied
                result =
                        UnitsRelationsTools.buildAnnoMirrorWithSpecificPrefix(
                                processingEnv, baseUnitAnnoClass, prefix);

                // TODO: assert that this annotation is a prefix multiple of a Unit that's in the
                // supported type qualifiers list currently this breaks for externally loaded
                // annotations if the order was an alias before a base annotation.
                // assert isSupportedQualifier(result);

                built = true;
                break;
            }
        }

        if (built) {
            // aliases shouldn't have Prefix.one, but if it does then clean it up here
            if (UnitsRelationsTools.getPrefix(result) == Prefix.one) {
                result = removePrefix(result);
            }

            // add this to the alias map
            aliasMap.put(aname, result);
            return result;
        }

        return super.aliasedAnnotation(anno);
    }

    protected Map<String, UnitsRelations> getUnitsRel() {
        if (unitsRel == null) {
            unitsRel = new HashMap<>();
            // Always add the default units relations, for the standard units.
            unitsRel.put(
                    UnitsRelationsDefault.class.getCanonicalName(),
                    new UnitsRelationsDefault().init(processingEnv));
        }
        return unitsRel;
    }

    @Override
    protected AnnotationClassLoader createAnnotationClassLoader() {
        // Use the UnitsAnnotationClassLoader instead of the default one
        return new UnitsAnnotationClassLoader(checker);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // get all the loaded annotations
        Set<Class<? extends Annotation>> qualSet = getBundledTypeQualifiersWithPolyAll();

        // load all the external units
        loadAllExternalUnits();

        // copy all loaded external Units to qual set
        qualSet.addAll(externalQualsMap.values());

        return qualSet;
    }

    private void loadAllExternalUnits() {
        // load external individually named units
        String qualNames = checker.getOption("units");
        if (qualNames != null) {
            for (String qualName : qualNames.split(",")) {
                loadExternalUnit(qualName);
            }
        }

        // load external directories of units
        String qualDirectories = checker.getOption("unitsDirs");
        if (qualDirectories != null) {
            for (String directoryName : qualDirectories.split(":")) {
                loadExternalDirectory(directoryName);
            }
        }
    }

    /** Loads and processes a single external units qualifier. */
    private void loadExternalUnit(String annoName) {
        // loadExternalAnnotationClass() returns null for alias units
        Class<? extends Annotation> loadedClass = loader.loadExternalAnnotationClass(annoName);
        if (loadedClass != null) {
            addUnitToExternalQualMap(loadedClass);
        }
    }

    /** Loads and processes the units qualifiers from a single external directory. */
    private void loadExternalDirectory(String directoryName) {
        Set<Class<? extends Annotation>> annoClassSet =
                loader.loadExternalAnnotationClassesFromDirectory(directoryName);

        for (Class<? extends Annotation> annoClass : annoClassSet) {
            addUnitToExternalQualMap(annoClass);
        }
    }

    /** Adds the annotation class to the external qualifier map if it is not an alias annotation. */
    private void addUnitToExternalQualMap(final Class<? extends Annotation> annoClass) {
        AnnotationMirror mirror =
                UnitsRelationsTools.buildAnnoMirrorWithNoPrefix(processingEnv, annoClass);

        // if it is not an aliased annotation, add to external quals map if it isn't already in map
        if (!isAliasedAnnotation(mirror)) {
            String unitClassName = annoClass.getCanonicalName();
            if (!externalQualsMap.containsKey(unitClassName)) {
                externalQualsMap.put(unitClassName, annoClass);
            }
        }
        // if it is an aliased annotation
        else {
            // ensure it has a base unit
            Class<? extends Annotation> baseUnitClass = getBaseUnitAnnoClass(mirror);
            if (baseUnitClass != null) {
                // if the base unit isn't already added, add that first
                String baseUnitClassName = baseUnitClass.getCanonicalName();
                if (!externalQualsMap.containsKey(baseUnitClassName)) {
                    loadExternalUnit(baseUnitClassName);
                }

                // then add the aliased annotation to the alias map
                // TODO: refactor so we can directly add to alias map, skipping the assert check in
                // aliasedAnnotation.
                aliasedAnnotation(mirror);
            } else {
                // error: somehow the aliased annotation has @UnitsMultiple meta annotation, but no
                // base class defined in that meta annotation
                // TODO: error abort
            }
        }

        // process the units annotation and add its corresponding units relations class
        addUnitsRelations(annoClass);
    }

    private boolean isAliasedAnnotation(AnnotationMirror anno) {
        // loop through the meta annotations of the annotation, look for UnitsMultiple
        for (AnnotationMirror metaAnno :
                anno.getAnnotationType().asElement().getAnnotationMirrors()) {
            // see if the meta annotation is UnitsMultiple
            if (isUnitsMultiple(metaAnno)) {
                // TODO: does every alias have to have Prefix?
                return true;
            }
        }

        // if we are unable to find UnitsMultiple meta annotation, then this is not an Aliased
        // Annotation
        return false;
    }

    private @Nullable Class<? extends Annotation> getBaseUnitAnnoClass(AnnotationMirror anno) {
        // loop through the meta annotations of the annotation, look for UnitsMultiple
        for (AnnotationMirror metaAnno :
                anno.getAnnotationType().asElement().getAnnotationMirrors()) {
            // see if the meta annotation is UnitsMultiple
            if (isUnitsMultiple(metaAnno)) {
                // TODO: does every alias have to have Prefix?
                // retrieve the Class of the base unit annotation
                Class<? extends Annotation> baseUnitAnnoClass =
                        AnnotationUtils.getElementValueClass(metaAnno, "quantity", true)
                                .asSubclass(Annotation.class);

                return baseUnitAnnoClass;
            }
        }
        return null;
    }

    private boolean isUnitsMultiple(AnnotationMirror metaAnno) {
        return AnnotationUtils.areSameByClass(metaAnno, UnitsMultiple.class);
    }

    /**
     * Look for an @UnitsRelations annotation on the qualifier and add it to the list of
     * UnitsRelations.
     *
     * @param qual the qualifier to investigate
     */
    private void addUnitsRelations(Class<? extends Annotation> qual) {
        AnnotationMirror am = AnnotationBuilder.fromClass(elements, qual);

        for (AnnotationMirror ama : am.getAnnotationType().asElement().getAnnotationMirrors()) {
            if (AnnotationUtils.areSameByClass(ama, unitsRelationsAnnoClass)) {
                Class<? extends UnitsRelations> theclass;
                try {
                    theclass =
                            AnnotationUtils.getElementValueClass(ama, "value", true)
                                    .asSubclass(UnitsRelations.class);
                } catch (ClassCastException ex) {
                    Class<?> clazz = AnnotationUtils.getElementValueClass(ama, "value", true);
                    throw new BugInCF(
                            "Invalid @UnitsRelations meta-annotation found in %s. @UnitsRelations value,"
                                    + " %s, is not a subclass of org.checkerframework.checker.units.UnitsRelations.",
                            qual, clazz);
                }
                String classname = theclass.getCanonicalName();

                if (!getUnitsRel().containsKey(classname)) {
                    try {
                        unitsRel.put(
                                classname,
                                theclass.getDeclaredConstructor()
                                        .newInstance()
                                        .init(processingEnv));
                    } catch (NoSuchMethodException e) {
                        // TODO
                        e.printStackTrace();
                        throw new BugInCF("Exception NoSuchMethodException");
                    } catch (InvocationTargetException e) {
                        // TODO
                        e.printStackTrace();
                        throw new BugInCF("Exception InvocationTargetException");
                    } catch (InstantiationException e) {
                        // TODO
                        e.printStackTrace();
                        throw new BugInCF("Exception InstantiationException");
                    } catch (IllegalAccessException e) {
                        // TODO
                        e.printStackTrace();
                        throw new BugInCF("Exception IllegalAccessException");
                    }
                }
            }
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        // Don't call super.createTreeAnnotator because it includes PropagationTreeAnnotator which
        // is incorrect.
        return new ListTreeAnnotator(
                new UnitsPropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this),
                new UnitsTreeAnnotator(this));
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

    /** A class for adding annotations based on tree. */
    private class UnitsTreeAnnotator extends TreeAnnotator {

        UnitsTreeAnnotator(UnitsAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror lht = getAnnotatedType(node.getLeftOperand());
            AnnotatedTypeMirror rht = getAnnotatedType(node.getRightOperand());
            Tree.Kind kind = node.getKind();

            // Remove Prefix.one
            if (UnitsRelationsTools.getPrefix(lht) == Prefix.one) {
                lht = UnitsRelationsTools.removePrefix(elements, lht);
            }
            if (UnitsRelationsTools.getPrefix(rht) == Prefix.one) {
                rht = UnitsRelationsTools.removePrefix(elements, rht);
            }

            AnnotationMirror bestres = null;
            for (UnitsRelations ur : getUnitsRel().values()) {
                AnnotationMirror res = useUnitsRelation(kind, ur, lht, rht);

                if (bestres != null && res != null && !bestres.equals(res)) {
                    checker.message(
                            Kind.WARNING,
                            "UnitsRelation mismatch, taking neither! Previous: "
                                    + bestres
                                    + " and current: "
                                    + res);
                    return null; // super.visitBinary(node, type);
                }

                if (res != null) {
                    bestres = res;
                }
            }

            if (bestres != null) {
                type.replaceAnnotation(bestres);
            } else {
                // If none of the units relations classes could resolve the units, then apply
                // default rules

                switch (kind) {
                    case MINUS:
                    case PLUS:
                        if (lht.getAnnotations().equals(rht.getAnnotations())) {
                            // The sum or difference has the same units as both operands.
                            type.replaceAnnotations(lht.getAnnotations());
                        } else {
                            // otherwise it results in mixed
                            type.replaceAnnotation(mixedUnits);
                        }
                        break;
                    case DIVIDE:
                        if (lht.getAnnotations().equals(rht.getAnnotations())) {
                            // If the units of the division match, return TOP
                            type.replaceAnnotation(TOP);
                        } else if (UnitsRelationsTools.hasNoUnits(rht)) {
                            // any unit divided by a scalar keeps that unit
                            type.replaceAnnotations(lht.getAnnotations());
                        } else if (UnitsRelationsTools.hasNoUnits(lht)) {
                            // scalar divided by any unit returns mixed
                            type.replaceAnnotation(mixedUnits);
                        } else {
                            // else it is a division of two units that have no defined relations
                            // from a relations class
                            // return mixed
                            type.replaceAnnotation(mixedUnits);
                        }
                        break;
                    case MULTIPLY:
                        if (UnitsRelationsTools.hasNoUnits(lht)) {
                            // any unit multiplied by a scalar keeps the unit
                            type.replaceAnnotations(rht.getAnnotations());
                        } else if (UnitsRelationsTools.hasNoUnits(rht)) {
                            // any scalar multiplied by a unit becomes the unit
                            type.replaceAnnotations(lht.getAnnotations());
                        } else {
                            // else it is a multiplication of two units that have no defined
                            // relations from a relations class
                            // return mixed
                            type.replaceAnnotation(mixedUnits);
                        }
                        break;
                    case REMAINDER:
                        // in modulo operation, it always returns the left unit regardless of what
                        // it is (unknown, or some unit)
                        type.replaceAnnotations(lht.getAnnotations());
                        break;
                    default:
                        // Placeholders for unhandled binary operations
                        // Do nothing
                }
            }

            return null;
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            ExpressionTree var = node.getVariable();
            AnnotatedTypeMirror varType = getAnnotatedType(var);

            type.replaceAnnotations(varType.getAnnotations());
            return null;
        }

        private AnnotationMirror useUnitsRelation(
                Tree.Kind kind,
                UnitsRelations ur,
                AnnotatedTypeMirror lht,
                AnnotatedTypeMirror rht) {

            AnnotationMirror res = null;
            if (ur != null) {
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

    /** Set the Bottom qualifier as the bottom of the hierarchy. */
    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new UnitsQualifierHierarchy(
                factory, AnnotationBuilder.fromClass(elements, UnitsBottom.class));
    }

    protected class UnitsQualifierHierarchy extends GraphQualifierHierarchy {

        public UnitsQualifierHierarchy(MultiGraphFactory mgf, AnnotationMirror bottom) {
            super(mgf, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameIgnoringValues(superAnno, subAnno)) {
                return AnnotationUtils.areSame(superAnno, subAnno);
            }
            superAnno = removePrefix(superAnno);
            subAnno = removePrefix(subAnno);

            return super.isSubtype(subAnno, superAnno);
        }

        // Overriding leastUpperBound due to the fact that alias annotations are
        // not placed in the Supported Type Qualifiers set, instead, their base
        // SI units are in the set.
        // Whenever an alias annotation or prefix-multiple of a base SI unit is
        // used in ternary statements or through mismatched PolyUnit method
        // parameters, we handle the LUB resolution here so that these units can
        // correctly resolve to an LUB Unit.
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            AnnotationMirror result;

            // if the prefix is Prefix.one, automatically strip it for LUB checking
            if (UnitsRelationsTools.getPrefix(a1) == Prefix.one) {
                a1 = removePrefix(a1);
            }
            if (UnitsRelationsTools.getPrefix(a2) == Prefix.one) {
                a2 = removePrefix(a2);
            }

            // if the two units have the same base SI unit
            // TODO: it is possible to rewrite these two lines to use UnitsRelationsTools, will it
            // have worse performance?
            if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                // and if they have the same Prefix, it means it is the same unit
                if (AnnotationUtils.areSame(a1, a2)) {
                    // return the unit
                    result = a1;
                }

                // if they don't have the same Prefix, find the LUB
                else {
                    // check if a1 is a prefixed multiple of a base unit
                    boolean a1Prefixed = !UnitsRelationsTools.hasNoPrefix(a1);
                    // check if a2 is a prefixed multiple of a base unit
                    boolean a2Prefixed = !UnitsRelationsTools.hasNoPrefix(a2);

                    // when calling findLub(), the left AnnoMirror has to be a type within the
                    // supertypes Map this means it has to be one of the base SI units, so always
                    // strip the left unit or ensure it has no prefix
                    if (a1Prefixed && a2Prefixed) {
                        // if both are prefixed, strip the left and find LUB
                        result = this.findLub(removePrefix(a1), a2);
                    } else if (a1Prefixed && !a2Prefixed) {
                        // if only the left is prefixed, swap order and find LUB
                        result = this.findLub(a2, a1);
                    } else {
                        // else (only right is prefixed), just find the LUB
                        result = this.findLub(a1, a2);
                    }
                }
            } else {
                // if they don't have the same base SI unit, let super find it
                result = super.leastUpperBound(a1, a2);
            }

            return result;
        }
    }

    private AnnotationMirror removePrefix(AnnotationMirror anno) {
        return UnitsRelationsTools.removePrefix(elements, anno);
    }
}
