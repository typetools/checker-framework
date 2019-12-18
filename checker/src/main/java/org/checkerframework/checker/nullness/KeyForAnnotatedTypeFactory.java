package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.ComplexHierarchy;
import org.checkerframework.framework.util.QualifierKindHierarchy.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

public class KeyForAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                KeyForValue, KeyForStore, KeyForTransfer, KeyForAnalysis> {

    /** The @{@link KeyFor} annotation. */
    protected final AnnotationMirror KEYFOR = AnnotationBuilder.fromClass(elements, KeyFor.class);
    /** The @{@link UnknownKeyFor} annotation. */
    protected final AnnotationMirror UNKNOWNKEYFOR =
            AnnotationBuilder.fromClass(elements, UnknownKeyFor.class);
    /** The @{@link KeyForBottom} annotation. */
    protected final AnnotationMirror KEYFORBOTTOM =
            AnnotationBuilder.fromClass(elements, KeyForBottom.class);

    /** The canonical name of the KeyFor class. */
    protected final String KEYFOR_NAME = KeyFor.class.getCanonicalName();

    /** The Map.containsKey method. */
    private final ExecutableElement mapContainsKey =
            TreeUtils.getMethod("java.util.Map", "containsKey", 1, processingEnv);
    /** The Map.get method. */
    private final ExecutableElement mapGet =
            TreeUtils.getMethod("java.util.Map", "get", 1, processingEnv);
    /** The Map.put method. */
    private final ExecutableElement mapPut =
            TreeUtils.getMethod("java.util.Map", "put", 2, processingEnv);

    private final KeyForPropagator keyForPropagator = new KeyForPropagator(UNKNOWNKEYFOR);

    /** Create a new KeyForAnnotatedTypeFactory. */
    public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        // Add compatibility annotations:
        addAliasedAnnotation(
                "org.checkerframework.checker.nullness.compatqual.KeyForDecl", KeyFor.class, true);
        addAliasedAnnotation(
                "org.checkerframework.checker.nullness.compatqual.KeyForType", KeyFor.class, true);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        KeyFor.class, UnknownKeyFor.class, KeyForBottom.class, PolyKeyFor.class));
    }

    @Override
    public ParameterizedExecutableType constructorFromUse(NewClassTree tree) {
        ParameterizedExecutableType result = super.constructorFromUse(tree);
        keyForPropagator.propagateNewClassTree(tree, result.executableType.getReturnType(), this);
        return result;
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new KeyForTypeHierarchy(
                checker,
                getQualifierHierarchy(),
                checker.getBooleanOption("ignoreRawTypeArguments", true),
                checker.hasOption("invariantArrays"));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new KeyForPropagationTreeAnnotator(this, keyForPropagator));
    }

    // TODO: work on removing this class
    protected static class KeyForTypeHierarchy extends DefaultTypeHierarchy {

        public KeyForTypeHierarchy(
                BaseTypeChecker checker,
                QualifierHierarchy qualifierHierarchy,
                boolean ignoreRawTypes,
                boolean invariantArrayComponents) {
            super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
        }

        @Override
        protected boolean isSubtype(
                AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
            // TODO: THIS IS FROM THE OLD TYPE HIERARCHY.  WE SHOULD FIX DATA-FLOW/PROPAGATION TO DO
            // THE RIGHT THING
            if (supertype.getKind() == TypeKind.TYPEVAR && subtype.getKind() == TypeKind.TYPEVAR) {
                // TODO: Investigate whether there is a nicer and more proper way to
                // get assignments between two type variables working.
                if (supertype.getAnnotations().isEmpty()) {
                    return true;
                }
            }

            // Otherwise Covariant would cause trouble.
            if (subtype.hasAnnotation(KeyForBottom.class)) {
                return true;
            }
            return super.isSubtype(subtype, supertype, top);
        }
    }

    /*
     * Given a string array 'values', returns an AnnotationMirror corresponding to @KeyFor(values)
     */
    public AnnotationMirror createKeyForAnnotationMirrorWithValue(LinkedHashSet<String> values) {
        // Create an AnnotationBuilder with the ArrayList
        AnnotationBuilder builder = new AnnotationBuilder(getProcessingEnv(), KeyFor.class);
        builder.setValue("value", values.toArray());

        // Return the resulting AnnotationMirror
        return builder.build();
    }

    /*
     * Given a string 'value', returns an AnnotationMirror corresponding to @KeyFor(value)
     */
    public AnnotationMirror createKeyForAnnotationMirrorWithValue(String value) {
        // Create an ArrayList with the value
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(value);
        return createKeyForAnnotationMirrorWithValue(values);
    }

    /**
     * Returns true if the expression tree is a key for the map.
     *
     * @param mapExpression expression that has type Map
     * @param tree expression that might be a key for the map
     * @return whether or not the expression is a key for the map
     */
    public boolean isKeyForMap(String mapExpression, ExpressionTree tree) {
        Collection<String> maps = null;
        AnnotatedTypeMirror type = getAnnotatedType(tree);
        AnnotationMirror keyForAnno = type.getAnnotation(KeyFor.class);
        if (keyForAnno != null) {
            maps = AnnotationUtils.getElementValueArray(keyForAnno, "value", String.class, false);
        } else {
            KeyForValue value = getInferredValueFor(tree);
            if (value != null) {
                maps = value.getKeyForMaps();
            }
        }

        return maps != null && maps.contains(mapExpression);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        return new KeyForQualifierHierarchy(getSupportedTypeQualifiers(), elements);
    }

    private final class KeyForQualifierHierarchy extends ComplexHierarchy {
        private final QualifierKind KEYFOR_KIND;

        public KeyForQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            this.KEYFOR_KIND = qualifierKindHierarchy.getQualifierKindMap().get(KEYFOR_NAME);
        }

        private List<String> extractValues(AnnotationMirror anno) {
            Map<? extends ExecutableElement, ? extends AnnotationValue> valMap =
                    anno.getElementValues();

            List<String> res;
            if (valMap.isEmpty()) {
                res = new ArrayList<>();
            } else {
                res = AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
            }
            return res;
        }

        @Override
        protected boolean isSubtype(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (subKind == KEYFOR_KIND && superKind == KEYFOR_KIND) {
                List<String> lhsValues = extractValues(superAnno);
                List<String> rhsValues = extractValues(subAnno);
                return rhsValues.containsAll(lhsValues);
            }
            return subKind.isSubtype(superKind);
        }

        @Override
        protected AnnotationMirror leastUpperBound(
                AnnotationMirror a1,
                QualifierKind qual1,
                AnnotationMirror a2,
                QualifierKind qual2) {
            if (qual1 == KEYFOR_KIND && qual2 == KEYFOR_KIND) {
                List<String> a1Values = extractValues(a1);
                List<String> a2Values = extractValues(a2);
                LinkedHashSet<String> set = new LinkedHashSet<>(a1Values);
                set.retainAll(a2Values);
                return createKeyForAnnotationMirrorWithValue(set);
            } else if (qual1 == KEYFOR_KIND) {
                return a1;
            } else if (qual2 == KEYFOR_KIND) {
                return a2;
            }
            throw new BugInCF("Unexpected QualifierKinds %s %s", qual1, qual2);
        }

        @Override
        protected AnnotationMirror greatestLowerBound(
                AnnotationMirror a1,
                QualifierKind qual1,
                AnnotationMirror a2,
                QualifierKind qual2) {
            if (qual1 == KEYFOR_KIND && qual2 == KEYFOR_KIND) {

                List<String> a1Values = extractValues(a1);
                List<String> a2Values = extractValues(a2);
                LinkedHashSet<String> set = new LinkedHashSet<>(a1Values);
                set.addAll(a2Values);
                return createKeyForAnnotationMirrorWithValue(set);
            } else if (qual1 == KEYFOR_KIND) {
                return a1;
            } else if (qual2 == KEYFOR_KIND) {
                return a2;
            }
            throw new BugInCF("Unexpected QualifierKinds %s %s", qual1, qual2);
        }
    }

    /** Returns true if the node is an invocation of Map.containsKey. */
    boolean isMapContainsKey(Tree tree) {
        return TreeUtils.isMethodInvocation(tree, mapContainsKey, getProcessingEnv());
    }

    /** Returns true if the node is an invocation of Map.get. */
    boolean isMapGet(Tree tree) {
        return TreeUtils.isMethodInvocation(tree, mapGet, getProcessingEnv());
    }

    /** Returns true if the node is an invocation of Map.put. */
    boolean isMapPut(Tree tree) {
        return TreeUtils.isMethodInvocation(tree, mapPut, getProcessingEnv());
    }

    /** Returns true if the node is an invocation of Map.containsKey. */
    boolean isMapContainsKey(Node node) {
        return NodeUtils.isMethodInvocation(node, mapContainsKey, getProcessingEnv());
    }

    /** Returns true if the node is an invocation of Map.get. */
    boolean isMapGet(Node node) {
        return NodeUtils.isMethodInvocation(node, mapGet, getProcessingEnv());
    }

    /** Returns true if the node is an invocation of Map.put. */
    boolean isMapPut(Node node) {
        return NodeUtils.isMethodInvocation(node, mapPut, getProcessingEnv());
    }

    /** Returns false. Redundancy in the KeyFor hierarchy is not worth warning about. */
    @Override
    public boolean shouldWarnIfStubRedundantWithBytecode() {
        return false;
    }
}
