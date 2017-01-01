package org.checkerframework.checker.nullness;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.KeyForPropagator.PropagationDirection;
import org.checkerframework.checker.nullness.qual.Covariant;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.expressionannotations.ExpressionAnnotationHelper;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TypesUtils;

public class KeyForAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror UNKNOWNKEYFOR, KEYFOR, KEYFORBOTTOM;

    private final KeyForPropagator keyForPropagator;

    public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        KEYFOR = AnnotationUtils.fromClass(elements, KeyFor.class);
        UNKNOWNKEYFOR = AnnotationUtils.fromClass(elements, UnknownKeyFor.class);
        KEYFORBOTTOM = AnnotationUtils.fromClass(elements, KeyForBottom.class);
        keyForPropagator = new KeyForPropagator(UNKNOWNKEYFOR);

        // Add compatibility annotations:
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.KeyForDecl.class, KEYFOR);
        addAliasedAnnotation(
                org.checkerframework.checker.nullness.compatqual.KeyForType.class, KEYFOR);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        KeyFor.class,
                        UnknownKeyFor.class,
                        KeyForBottom.class,
                        PolyKeyFor.class,
                        PolyAll.class));
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(
            NewClassTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> result =
                super.constructorFromUse(tree);

        final AnnotatedTypeMirror returnType = result.first.getReturnType();

        // Can we square this with the KeyForPropagationTreeAnnotator
        Pair<Tree, AnnotatedTypeMirror> context = getVisitorState().getAssignmentContext();

        if (returnType.getKind() == TypeKind.DECLARED && context != null && context.first != null) {
            AnnotatedTypeMirror assignedTo = TypeArgInferenceUtil.assignedTo(this, getPath(tree));

            if (assignedTo != null) {
                // array types and boxed primitives etc don't require propagation
                if (assignedTo.getKind() == TypeKind.DECLARED) {
                    final AnnotatedDeclaredType newClassType = (AnnotatedDeclaredType) returnType;
                    keyForPropagator.propagate(
                            newClassType,
                            (AnnotatedDeclaredType) assignedTo,
                            PropagationDirection.TO_SUBTYPE,
                            this);
                }
            }
        }
        return result;
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new KeyForTypeHierarchy(
                checker,
                getQualifierHierarchy(),
                checker.getOption("ignoreRawTypeArguments", "true").equals("true"),
                checker.hasOption("invariantArrays"));
    }

    @Override
    protected ExpressionAnnotationHelper createExpressionAnnotationHelper() {
        return new ExpressionAnnotationHelper(this, KeyFor.class);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new KeyForPropagationTreeAnnotator(this, keyForPropagator));
    }

    protected class KeyForTypeHierarchy extends DefaultTypeHierarchy {

        public KeyForTypeHierarchy(
                BaseTypeChecker checker,
                QualifierHierarchy qualifierHierarchy,
                boolean ignoreRawTypes,
                boolean invariantArrayComponents) {
            super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
        }

        @Override
        public boolean isSubtype(
                AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {

            //TODO: THIS IS FROM THE OLD TYPE HIERARCHY.  WE SHOULD FIX DATA-FLOW/PROPAGATION TO DO THE RIGHT THING
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
            return super.isSubtype(subtype, supertype, visited);
        }

        protected boolean isCovariant(final int typeArgIndex, final int[] covariantArgIndexes) {
            if (covariantArgIndexes != null) {
                for (int covariantIndex : covariantArgIndexes) {
                    if (typeArgIndex == covariantIndex) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public Boolean visitTypeArgs(
                AnnotatedDeclaredType subtype,
                AnnotatedDeclaredType supertype,
                VisitHistory visited,
                boolean subtypeIsRaw,
                boolean supertypeIsRaw) {
            final boolean ignoreTypeArgs = ignoreRawTypes && (subtypeIsRaw || supertypeIsRaw);

            if (!ignoreTypeArgs) {

                //TODO: Make an option for honoring this annotation in DefaultTypeHierarchy?
                final TypeElement supertypeElem =
                        (TypeElement) supertype.getUnderlyingType().asElement();
                int[] covariantArgIndexes = null;
                if (supertypeElem.getAnnotation(Covariant.class) != null) {
                    covariantArgIndexes = supertypeElem.getAnnotation(Covariant.class).value();
                }

                final List<? extends AnnotatedTypeMirror> subtypeTypeArgs =
                        subtype.getTypeArguments();
                final List<? extends AnnotatedTypeMirror> supertypeTypeArgs =
                        supertype.getTypeArguments();

                if (subtypeTypeArgs.isEmpty() || supertypeTypeArgs.isEmpty()) {
                    return true;
                }

                if (supertypeTypeArgs.size() > 0) {
                    for (int i = 0; i < supertypeTypeArgs.size(); i++) {
                        final AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
                        final AnnotatedTypeMirror subTypeArg = subtypeTypeArgs.get(i);

                        if (subtypeIsRaw || supertypeIsRaw) {
                            rawnessComparer.isValidInHierarchy(
                                    subtype, supertype, currentTop, visited);
                        } else {
                            if (!isContainedBy(
                                    subTypeArg,
                                    superTypeArg,
                                    visited,
                                    isCovariant(i, covariantArgIndexes))) {
                                return false;
                            }
                        }
                    }
                }
            }

            return true;
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
        LinkedHashSet<String> values = new LinkedHashSet<String>();
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
        AnnotatedTypeMirror type = getAnnotatedType(tree);
        AnnotationMirror keyForAnno = type.getAnnotation(KeyFor.class);
        if (keyForAnno == null) {
            return false;
        }

        List<String> val =
                AnnotationUtils.getElementValueArray(keyForAnno, "value", String.class, false);
        return val.contains(mapExpression);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new KeyForQualifierHierarchy(factory);
    }

    private final class KeyForQualifierHierarchy extends GraphQualifierHierarchy {

        public KeyForQualifierHierarchy(MultiGraphFactory factory) {
            super(factory, KEYFORBOTTOM);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR)
                    && AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
                List<String> lhsValues = null;
                List<String> rhsValues = null;

                Map<? extends ExecutableElement, ? extends AnnotationValue> valMap =
                        lhs.getElementValues();

                if (valMap.isEmpty()) {
                    lhsValues = new ArrayList<String>();
                } else {
                    lhsValues =
                            AnnotationUtils.getElementValueArray(lhs, "value", String.class, true);
                }

                valMap = rhs.getElementValues();

                if (valMap.isEmpty()) {
                    rhsValues = new ArrayList<String>();
                } else {
                    rhsValues =
                            AnnotationUtils.getElementValueArray(rhs, "value", String.class, true);
                }

                return rhsValues.containsAll(lhsValues);
            }
            // Ignore annotation values to ensure that annotation is in supertype map.
            if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR)) {
                lhs = KEYFOR;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
                rhs = KEYFOR;
            }
            return super.isSubtype(rhs, lhs);
        }
    }

    private TypeMirror erasedMapType = null;

    protected boolean isInvocationOfMapMethod(MethodInvocationNode n, String methodName) {
        String invokedMethod = getMethodName(n);
        // First verify if the method name is correct. This is an inexpensive check.
        if (invokedMethod.equals(methodName)) {
            // Now verify that the receiver of the method invocation is of a type
            // that extends that java.util.Map interface. This is a more expensive check.
            if (erasedMapType == null) {
                TypeMirror mapType = TypesUtils.typeFromClass(types, elements, Map.class);
                erasedMapType = types.erasure(mapType);
            }

            TypeMirror receiverType = types.erasure(n.getTarget().getReceiver().getType());

            if (types.isSubtype(receiverType, erasedMapType)) {
                return true;
            }
        }
        return false;
    }

    protected String getMethodName(MethodInvocationNode n) {
        String invokedMethod = n.getTarget().getMethod().toString();
        int index = invokedMethod.indexOf("(");
        assert index != -1 : this.getClass() + ": expected method name to contain (";
        invokedMethod = invokedMethod.substring(0, index);
        return invokedMethod;
    }
}
