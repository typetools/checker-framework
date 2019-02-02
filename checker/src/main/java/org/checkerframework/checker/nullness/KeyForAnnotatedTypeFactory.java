package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

public class KeyForAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                KeyForValue, KeyForStore, KeyForTransfer, KeyForAnalysis> {

    protected final AnnotationMirror UNKNOWNKEYFOR, KEYFOR, KEYFORBOTTOM;

    /** A parameter list consisting of just Object. */
    public final List<TypeMirror> PARAMS_OBJECT;

    /** A parameter list consisting of [Object, Object]. */
    public final List<TypeMirror> PARAMS_OBJECT_OBJECT;

    private final KeyForPropagator keyForPropagator;

    private final TypeMirror erasedMapType;

    public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        KEYFOR = AnnotationBuilder.fromClass(elements, KeyFor.class);
        UNKNOWNKEYFOR = AnnotationBuilder.fromClass(elements, UnknownKeyFor.class);
        KEYFORBOTTOM = AnnotationBuilder.fromClass(elements, KeyForBottom.class);
        keyForPropagator = new KeyForPropagator(UNKNOWNKEYFOR);

        TypeMirror OBJECT_TYPE = TypesUtils.typeFromClass(Object.class, types, elements);
        PARAMS_OBJECT = Collections.singletonList(OBJECT_TYPE);
        List<TypeMirror> paramsObjectObject = new ArrayList<>(2);
        paramsObjectObject.add(OBJECT_TYPE);
        paramsObjectObject.add(OBJECT_TYPE);
        PARAMS_OBJECT_OBJECT = Collections.unmodifiableList(paramsObjectObject);

        // Add compatibility annotations:
        addAliasedAnnotation("org.checkerframework.checker.nullness.compatqual.KeyForDecl", KEYFOR);
        addAliasedAnnotation("org.checkerframework.checker.nullness.compatqual.KeyForType", KEYFOR);

        TypeMirror mapType = TypesUtils.typeFromClass(Map.class, types, elements);
        erasedMapType = types.erasure(mapType);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(
                        KeyFor.class,
                        UnknownKeyFor.class,
                        KeyForBottom.class,
                        PolyKeyFor.class,
                        PolyAll.class));
    }

    @Override
    public ParameterizedMethodType constructorFromUse(NewClassTree tree) {
        ParameterizedMethodType result = super.constructorFromUse(tree);
        keyForPropagator.propagateNewClassTree(tree, result.methodType.getReturnType(), this);
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
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new KeyForQualifierHierarchy(factory);
    }

    private final class KeyForQualifierHierarchy extends GraphQualifierHierarchy {

        public KeyForQualifierHierarchy(MultiGraphFactory factory) {
            super(factory, KEYFORBOTTOM);
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
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameByName(superAnno, KEYFOR)
                    && AnnotationUtils.areSameByName(subAnno, KEYFOR)) {
                List<String> lhsValues = extractValues(superAnno);
                List<String> rhsValues = extractValues(subAnno);

                return rhsValues.containsAll(lhsValues);
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameByName(superAnno, KEYFOR)) {
                superAnno = KEYFOR;
            }
            if (AnnotationUtils.areSameByName(subAnno, KEYFOR)) {
                subAnno = KEYFOR;
            }
            return super.isSubtype(subAnno, superAnno);
        }
    }

    /**
     * Returns true if the node is an invocation of the named method in the Map interface, which has
     * one formal parameter of Object type.
     *
     * @param n the method invocation
     * @param methodName the method to check for, which has one formal parameter
     */
    protected boolean isInvocationOfMapMethodWithOneObjectParameter(
            MethodInvocationNode n, String methodName) {
        return isInvocationOfMapMethod(n, methodName, PARAMS_OBJECT);
    }

    /**
     * Returns true if the node is an invocation of the named method in the Map interface, which has
     * two formal parameters of Object type.
     *
     * @param n the method invocation
     * @param methodName the method to check for, which has two formal parameters
     */
    protected boolean isInvocationOfMapMethodWithTwoObjectParameters(
            MethodInvocationNode n, String methodName) {
        return isInvocationOfMapMethod(n, methodName, PARAMS_OBJECT_OBJECT);
    }

    /**
     * Returns true if the node is an invocation of the named method in the Map interface.
     *
     * @param n the method invocation
     * @param methodName the method to check for
     * @param paramTypes the parameter types in methodName's declaration, to resolve overloading
     */
    protected boolean isInvocationOfMapMethod(
            MethodInvocationNode n, String methodName, List<TypeMirror> paramTypes) {
        String invokedMethodName = getMethodName(n);

        // First verify if the method name is correct. This is an inexpensive check.
        if (!invokedMethodName.equals(methodName)) {
            return false;
        }

        // Verify the argument types.  These are more expensive checks.

        // The receiver's type must extend the java.util.Map interface.
        TypeMirror receiverType = types.erasure(n.getTarget().getReceiver().getType());
        if (!types.isSubtype(receiverType, erasedMapType)) {
            return false;
        }

        // Also verify the other argument types
        List<Node> args = n.getArguments();
        if (args.size() != paramTypes.size()) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            Node arg = args.get(i);
            TypeMirror erasedArgType = types.erasure(n.getArguments().get(i).getType());
            TypeMirror paramType = paramTypes.get(i);
            // This test isn't right, because there could be a more specific overload that is
            // applied.  Exact equality wouldn't be right either.
            if (!types.isSubtype(erasedArgType, paramType)) {
                return false;
            }
        }

        return true;
    }

    protected String getMethodName(MethodInvocationNode n) {
        String invokedMethod = n.getTarget().getMethod().toString();
        int index = invokedMethod.indexOf("(");
        assert index != -1 : this.getClass() + ": expected method name to contain (";
        invokedMethod = invokedMethod.substring(0, index);
        return invokedMethod;
    }
}
