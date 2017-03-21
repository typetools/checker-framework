package org.checkerframework.common.value;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * AnnotatedTypeFactory for the Value type system.
 *
 * @author plvines
 * @author smillst
 */
public class ValueAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The maximum number of values allowed in an annotation's array */
    protected static final int MAX_VALUES = 10;

    /**
     * The domain of the Constant Value Checker: the types for which it estimates possible values.
     */
    protected static final Set<String> coveredClassStrings;

    /** The top type for this hierarchy. */
    protected final AnnotationMirror UNKNOWNVAL;

    /** The bottom type for this hierarchy. */
    protected final AnnotationMirror BOTTOMVAL;

    /** Should this type factory report warnings? */
    private final boolean reportEvalWarnings;

    /** Helper class that evaluates statically executable methods, constructors, and fields. */
    private final ReflectiveEvalutator evalutator;

    static {
        Set<String> backingSet = new HashSet<String>(18);
        backingSet.add("int");
        backingSet.add("java.lang.Integer");
        backingSet.add("double");
        backingSet.add("java.lang.Double");
        backingSet.add("byte");
        backingSet.add("java.lang.Byte");
        backingSet.add("java.lang.String");
        backingSet.add("char");
        backingSet.add("java.lang.Character");
        backingSet.add("float");
        backingSet.add("java.lang.Float");
        backingSet.add("boolean");
        backingSet.add("java.lang.Boolean");
        backingSet.add("long");
        backingSet.add("java.lang.Long");
        backingSet.add("short");
        backingSet.add("java.lang.Short");
        backingSet.add("byte[]");
        coveredClassStrings = Collections.unmodifiableSet(backingSet);
    }

    public ValueAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        BOTTOMVAL = AnnotationUtils.fromClass(elements, BottomVal.class);
        UNKNOWNVAL = AnnotationUtils.fromClass(elements, UnknownVal.class);

        reportEvalWarnings = checker.hasOption(ValueChecker.REPORT_EVAL_WARNS);
        evalutator = new ReflectiveEvalutator(checker, this, reportEvalWarnings);

        if (this.getClass().equals(ValueAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new ValueTransfer(analysis);
    }

    /**
     * Creates an annotation of the given name with the given set of values.
     *
     * <p>If values.size &gt; MAX_VALUES, issues a checker warning and returns UNKNOWNVAL.
     *
     * <p>If values.size == 0, issues a checker warning and returns BOTTOMVAL.
     *
     * @return annotation given by name with values=values, or UNKNOWNVAL
     */
    private AnnotationMirror createAnnotation(String name, Set<?> values) {
        if (values.size() == 0) {
            return BOTTOMVAL;
        }
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, name);
        List<Object> valuesList = new ArrayList<Object>(values);
        builder.setValue("value", valuesList);
        return builder.build();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithoutPolyAll();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new ValueQualifierHierarchy(factory);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(new ValueTypeAnnotator(this), super.createTypeAnnotator());
    }

    /**
     * Creates array length annotations for the result of the Enum.values() method, which is the
     * number of possible values of the enum.
     */
    @Override
    public Pair<AnnotatedTypeMirror.AnnotatedExecutableType, List<AnnotatedTypeMirror>>
            methodFromUse(
                    ExpressionTree tree,
                    ExecutableElement methodElt,
                    AnnotatedTypeMirror receiverType) {

        Pair<AnnotatedTypeMirror.AnnotatedExecutableType, List<AnnotatedTypeMirror>> superPair =
                super.methodFromUse(tree, methodElt, receiverType);
        if (ElementUtils.matchesElement(methodElt, "values")
                && methodElt.getEnclosingElement().getKind() == ElementKind.ENUM
                && ElementUtils.isStatic(methodElt)) {
            int count = 0;
            List<? extends Element> l = methodElt.getEnclosingElement().getEnclosedElements();
            for (Element el : l) {
                if (el.getKind() == ElementKind.ENUM_CONSTANT) {
                    count++;
                }
            }
            AnnotationMirror am = createArrayLenAnnotation(Collections.singletonList(count));
            superPair.first.getReturnType().replaceAnnotation(am);
        }
        return superPair;
    }

    /**
     * Performs pre-processing on annotations written by users, replacing illegal annotations by
     * legal ones.
     */
    private class ValueTypeAnnotator extends TypeAnnotator {

        public ValueTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Void p) {
            replaceWithUnknownValIfTooManyValues(type);

            return super.visitPrimitive(type, p);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
            replaceWithUnknownValIfTooManyValues(type);

            return super.visitDeclared(type, p);
        }

        /**
         * If any constant-value annotation has &gt; MAX_VALUES number of values provided, treats
         * the value as UnknownVal. Works together with {@link ValueVisitor#visitAnnotation}, which
         * issues a warning to the user in this case.
         */
        private void replaceWithUnknownValIfTooManyValues(AnnotatedTypeMirror atm) {
            AnnotationMirror anno = atm.getAnnotationInHierarchy(UNKNOWNVAL);

            if (anno != null && anno.getElementValues().size() > 0) {
                List<Object> values =
                        AnnotationUtils.getElementValueArray(anno, "value", Object.class, false);
                if (values != null && values.size() > MAX_VALUES) {
                    atm.replaceAnnotation(UNKNOWNVAL);
                }
            }
        }
    }

    /** The qualifier hierarchy for the Value type system */
    private final class ValueQualifierHierarchy extends MultiGraphQualifierHierarchy {

        /** @param factory MultiGraphFactory to use to construct this */
        public ValueQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            } else {
                // If the two are unrelated, then bottom is the GLB.
                return BOTTOMVAL;
            }
        }

        /**
         * Determines the least upper bound of a1 and a2. If a1 and a2 are both the same type of
         * Value annotation, then the LUB is the result of taking all values from both a1 and a2 and
         * removing duplicates. If a1 and a2 are not the same type of Value annotation they may
         * still be mergeable because some values can be implicitly cast as others. If a1 and a2 are
         * both in {DoubleVal, IntVal} then they will be converted upwards: IntVal &rarr; DoubleVal
         * to arrive at a common annotation type.
         *
         * @return the least upper bound of a1 and a2
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (!AnnotationUtils.areSameIgnoringValues(
                    getTopAnnotation(a1), getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            }
            // If both are the same type, determine the type and merge:
            else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                List<Object> a1Values =
                        AnnotationUtils.getElementValueArray(a1, "value", Object.class, true);
                List<Object> a2Values =
                        AnnotationUtils.getElementValueArray(a2, "value", Object.class, true);
                Set<Object> newValues = new TreeSet<>();
                newValues.addAll(a1Values);
                newValues.addAll(a2Values);

                return createAnnotation(a1.getAnnotationType().toString(), newValues);
            }
            // Annotations are in this hierarchy, but they are not the same
            else {
                // If either is UNKNOWNVAL, ARRAYLEN, STRINGVAL, or BOOLEAN then the LUB is
                // UnknownVal.
                if (!((AnnotationUtils.areSameByClass(a1, IntVal.class)
                                || AnnotationUtils.areSameByClass(a1, DoubleVal.class))
                        && (AnnotationUtils.areSameByClass(a2, IntVal.class)
                                || AnnotationUtils.areSameByClass(a2, DoubleVal.class)))) {
                    return UNKNOWNVAL;
                } else {
                    // At this point one of them must be a DoubleVal and one an IntVal.
                    AnnotationMirror doubleAnno;
                    AnnotationMirror intAnno;

                    if (AnnotationUtils.areSameByClass(a2, DoubleVal.class)) {
                        doubleAnno = a2;
                        intAnno = a1;
                    } else {
                        doubleAnno = a1;
                        intAnno = a2;
                    }
                    List<Long> intVals = getIntValues(intAnno);
                    List<Double> doubleVals = getDoubleValues(doubleAnno);

                    for (Long n : intVals) {
                        doubleVals.add(n.doubleValue());
                    }

                    return createDoubleValAnnotation(doubleVals);
                }
            }
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
         * annotations are Value. In this case, rhs is a subtype of lhs iff lhs contains at least
         * every element of rhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {

            if (AnnotationUtils.areSameByClass(superAnno, UnknownVal.class)
                    || AnnotationUtils.areSameByClass(subAnno, BottomVal.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(subAnno, UnknownVal.class)
                    || AnnotationUtils.areSameByClass(superAnno, BottomVal.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(superAnno, subAnno)) {
                // Same type, so might be subtype
                List<Object> lhsValues =
                        AnnotationUtils.getElementValueArray(
                                superAnno, "value", Object.class, true);
                List<Object> rhsValues =
                        AnnotationUtils.getElementValueArray(subAnno, "value", Object.class, true);
                return lhsValues.containsAll(rhsValues);
            } else if (AnnotationUtils.areSameByClass(superAnno, DoubleVal.class)
                    && AnnotationUtils.areSameByClass(subAnno, IntVal.class)) {
                List<Double> superValues = getDoubleValues(superAnno);
                List<Long> subValues = getIntValues(subAnno);
                if (subValues.size() < superValues.size()) {
                    return false;
                }
                for (int i = 0; i < superValues.size(); i++) {
                    double lhsDbl = superValues.get(i);
                    double rhsDbl = subValues.get(i).doubleValue();
                    if (lhsDbl != rhsDbl) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        // The ValueTreeAnnotator handles propagation differently,
        // so it doesn't need PropgationTreeAnnotator.
        return new ListTreeAnnotator(
                new ValueTreeAnnotator(this), new ImplicitsTreeAnnotator(this));
    }

    /** The TreeAnnotator for this AnnotatedTypeFactory. It adds/replaces annotations. */
    protected class ValueTreeAnnotator extends TreeAnnotator {

        public ValueTreeAnnotator(ValueAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {

            List<? extends ExpressionTree> dimensions = tree.getDimensions();
            List<? extends ExpressionTree> initializers = tree.getInitializers();

            // Array construction can provide dimensions or use an initializer.

            // Dimensions provided
            if (!dimensions.isEmpty()) {
                handleDimensions(dimensions, (AnnotatedArrayType) type);
            } else {
                // Initializer used
                handleInitializers(initializers, (AnnotatedArrayType) type);

                AnnotationMirror newQual;
                Class<?> clazz = ValueCheckerUtils.getClassFromType(type.getUnderlyingType());
                String stringVal = null;
                if (clazz.equals(byte[].class)) {
                    stringVal = getByteArrayStringVal(initializers);
                } else if (clazz.equals(char[].class)) {
                    stringVal = getCharArrayStringVal(initializers);
                }

                if (stringVal != null) {
                    newQual = createStringAnnotation(Collections.singletonList(stringVal));
                    type.replaceAnnotation(newQual);
                }
            }

            return null;
        }

        /**
         * Recursive method to handle array initializations. Recursively descends the initializer to
         * find each dimension's size and create the appropriate annotation for it.
         *
         * @param dimensions a list of ExpressionTrees where each ExpressionTree is a specifier of
         *     the size of that dimension (should be an IntVal)
         * @param type the AnnotatedTypeMirror of the array
         */
        private void handleDimensions(
                List<? extends ExpressionTree> dimensions, AnnotatedArrayType type) {
            if (dimensions.size() > 1) {
                handleDimensions(
                        dimensions.subList(1, dimensions.size()),
                        (AnnotatedArrayType) type.getComponentType());
            }

            AnnotationMirror dimType =
                    getAnnotatedType(dimensions.get(0)).getAnnotationInHierarchy(UNKNOWNVAL);
            if (!AnnotationUtils.areSameIgnoringValues(dimType, UNKNOWNVAL)) {
                List<Long> longLengths = getIntValues(dimType);

                HashSet<Integer> lengths = new HashSet<Integer>(longLengths.size());
                for (Long l : longLengths) {
                    lengths.add(l.intValue());
                }
                AnnotationMirror newQual = createArrayLenAnnotation(new ArrayList<>(lengths));
                type.replaceAnnotation(newQual);
            }
        }

        /**
         * Adds the ArrayLen annotation from the array initializers to {@code type}.
         *
         * <p>If type is a multi-dimensional array, the the initializers might also contain arrays,
         * so this method adds the annotations for those initializers, too.
         *
         * @param initializers initializer trees
         * @param type array type to which annotations are added
         */
        private void handleInitializers(
                List<? extends ExpressionTree> initializers, AnnotatedArrayType type) {

            List<Integer> array = new ArrayList<>();
            array.add(initializers.size());
            type.replaceAnnotation(createArrayLenAnnotation(array));

            if (type.getComponentType().getKind() != TypeKind.ARRAY) {
                return;
            }

            // A list of arrayLens.  arrayLenOfDimensions.get(i) is the array lengths for the ith
            // dimension.
            List<List<Integer>> arrayLenOfDimensions = new ArrayList<>();
            for (ExpressionTree init : initializers) {
                AnnotatedTypeMirror componentType = getAnnotatedType(init);
                int dimension = 0;
                while (componentType.getKind() == TypeKind.ARRAY) {
                    List<Integer> arrayLens;
                    if (dimension == arrayLenOfDimensions.size()) {
                        arrayLens = new ArrayList<>();
                        arrayLenOfDimensions.add(arrayLens);
                    } else {
                        arrayLens = arrayLenOfDimensions.get(dimension);
                    }
                    AnnotationMirror arrayLen = componentType.getAnnotation(ArrayLen.class);
                    if (arrayLen != null) {
                        List<Integer> currentLengths = getArrayLength(arrayLen);
                        arrayLens.addAll(currentLengths);
                    }
                    dimension++;
                    componentType = ((AnnotatedArrayType) componentType).getComponentType();
                }
            }

            AnnotatedTypeMirror componentType = type.getComponentType();
            int i = 0;
            while (componentType.getKind() == TypeKind.ARRAY && i < arrayLenOfDimensions.size()) {
                componentType.addAnnotation(createArrayLenAnnotation(arrayLenOfDimensions.get(i)));
                componentType = ((AnnotatedArrayType) componentType).getComponentType();
                i++;
            }
        }

        /** Convert a byte array to a String. Return null if unable to convert. */
        private String getByteArrayStringVal(List<? extends ExpressionTree> initializers) {
            // True iff every element of the array is a literal.
            boolean allLiterals = true;
            byte[] bytes = new byte[initializers.size()];
            for (int i = 0; i < initializers.size(); i++) {
                ExpressionTree e = initializers.get(i);
                if (e.getKind() == Tree.Kind.INT_LITERAL) {
                    bytes[i] = (byte) (((Integer) ((LiteralTree) e).getValue()).intValue());
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    bytes[i] = (byte) (((Character) ((LiteralTree) e).getValue()).charValue());
                } else {
                    allLiterals = false;
                }
            }
            if (allLiterals) {
                return new String(bytes);
            }
            // If any part of the initializer isn't known,
            // the stringval isn't known.
            return null;
        }

        /** Convert a char array to a String. Return null if unable to convert. */
        private String getCharArrayStringVal(List<? extends ExpressionTree> initializers) {
            boolean allLiterals = true;
            StringBuilder stringVal = new StringBuilder();
            for (ExpressionTree e : initializers) {
                if (e.getKind() == Tree.Kind.INT_LITERAL) {
                    char charVal = (char) (((Integer) ((LiteralTree) e).getValue()).intValue());
                    stringVal.append(charVal);
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    char charVal = (((Character) ((LiteralTree) e).getValue()));
                    stringVal.append(charVal);
                } else {
                    allLiterals = false;
                }
            }
            if (allLiterals) {
                return stringVal.toString();
            }
            // If any part of the initializer isn't known,
            // the stringval isn't known.
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror type) {
            if (handledByValueChecker(type)) {
                AnnotatedTypeMirror castedAnnotation = getAnnotatedType(tree.getExpression());
                List<?> values = getValues(castedAnnotation, type.getUnderlyingType());
                type.replaceAnnotation(createResultingAnnotation(type.getUnderlyingType(), values));
            } else if (type.getKind() == TypeKind.ARRAY) {
                if (tree.getExpression().getKind() == Kind.NULL_LITERAL) {
                    type.replaceAnnotation(BOTTOMVAL);
                }
            }
            return null;
        }

        /**
         * Get the "value" field of the given annotation, casted to the given type. Empty list means
         * no value is possible (dead code). Null means no information is known -- any value is
         * possible.
         */
        private List<?> getValues(AnnotatedTypeMirror type, TypeMirror castTo) {
            AnnotationMirror anno = type.getAnnotationInHierarchy(UNKNOWNVAL);
            if (anno == null) {
                // If type is an AnnotatedTypeVariable (or other type without a primary annotation)
                // then anno will be null. It would be safe to use the annotation on the upper bound;
                // however, unless the upper bound was explicitly annotated, it will be unknown.
                // AnnotatedTypes.findEffectiveAnnotationInHierarchy(, toSearch, top)
                return null;
            }
            return ValueCheckerUtils.getValuesCastedToType(anno, castTo);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!handledByValueChecker(type)) {
                return null;
            }
            Object value = tree.getValue();
            switch (tree.getKind()) {
                case BOOLEAN_LITERAL:
                    AnnotationMirror boolAnno =
                            createBooleanAnnotation(Collections.singletonList((Boolean) value));
                    type.replaceAnnotation(boolAnno);
                    return null;

                case CHAR_LITERAL:
                    AnnotationMirror charAnno =
                            createCharAnnotation(Collections.singletonList((Character) value));
                    type.replaceAnnotation(charAnno);
                    return null;

                case DOUBLE_LITERAL:
                case FLOAT_LITERAL:
                case INT_LITERAL:
                case LONG_LITERAL:
                    AnnotationMirror numberAnno =
                            createNumberAnnotationMirror(Collections.singletonList((Number) value));
                    type.replaceAnnotation(numberAnno);
                    return null;
                case STRING_LITERAL:
                    AnnotationMirror stringAnno =
                            createStringAnnotation(Collections.singletonList((String) value));
                    type.replaceAnnotation(stringAnno);
                    return null;
                default:
                    return null;
            }
        }

        /**
         * Given a MemberSelectTree representing a method call, return true if the method's
         * declaration is annotated with {@code @StaticallyExecutable}.
         */
        private boolean methodIsStaticallyExecutable(Element method) {
            return getDeclAnnotation(method, StaticallyExecutable.class) != null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (handledByValueChecker(type)
                    && methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree))) {
                // Get argument values
                List<? extends ExpressionTree> arguments = tree.getArguments();
                ArrayList<List<?>> argValues;
                if (arguments.size() > 0) {
                    argValues = new ArrayList<List<?>>();
                    for (ExpressionTree argument : arguments) {
                        AnnotatedTypeMirror argType = getAnnotatedType(argument);
                        List<?> values = getValues(argType, argType.getUnderlyingType());
                        if (values == null || values.isEmpty()) {
                            // Values aren't known, so don't try to evaluate the method.
                            return null;
                        }
                        argValues.add(values);
                    }
                } else {
                    argValues = null;
                }

                // Get receiver values
                AnnotatedTypeMirror receiver = getReceiverType(tree);
                List<?> receiverValues;

                if (receiver != null && !ElementUtils.isStatic(TreeUtils.elementFromUse(tree))) {
                    receiverValues = getValues(receiver, receiver.getUnderlyingType());
                    if (receiverValues == null || receiverValues.isEmpty()) {
                        // Values aren't known, so don't try to evaluate the method.
                        return null;
                    }
                } else {
                    receiverValues = null;
                }

                // Evaluate method
                List<?> returnValues =
                        evalutator.evaluateMethodCall(argValues, receiverValues, tree);
                AnnotationMirror returnType =
                        createResultingAnnotation(type.getUnderlyingType(), returnValues);
                type.replaceAnnotation(returnType);
            }

            return null;
        }

        @Override
        public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
            boolean wrapperClass =
                    TypesUtils.isBoxedPrimitive(type.getUnderlyingType())
                            || TypesUtils.isDeclaredOfName(
                                    type.getUnderlyingType(), "java.lang.String");

            if (wrapperClass
                    || (handledByValueChecker(type)
                            && methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree)))) {
                // get argument values
                List<? extends ExpressionTree> arguments = tree.getArguments();
                ArrayList<List<?>> argValues;
                if (arguments.size() > 0) {
                    argValues = new ArrayList<List<?>>();
                    for (ExpressionTree argument : arguments) {
                        AnnotatedTypeMirror argType = getAnnotatedType(argument);
                        List<?> values = getValues(argType, argType.getUnderlyingType());
                        if (values == null || values.isEmpty()) {
                            // Values aren't known, so don't try to evaluate the method.
                            return null;
                        }
                        argValues.add(values);
                    }
                } else {
                    argValues = null;
                }
                // Evaluate method
                List<?> returnValues =
                        evalutator.evaluteConstrutorCall(argValues, tree, type.getUnderlyingType());
                AnnotationMirror returnType =
                        createResultingAnnotation(type.getUnderlyingType(), returnValues);
                type.replaceAnnotation(returnType);
            }

            return null;
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isFieldAccess(tree) && handledByValueChecker(type)) {
                VariableElement elem = (VariableElement) InternalUtils.symbol(tree);
                Object value = elem.getConstantValue();
                if (value != null) {
                    // The field is a compile time constant.
                    type.replaceAnnotation(
                            createResultingAnnotation(type.getUnderlyingType(), value));
                    return null;
                }
                if (ElementUtils.isStatic(elem) && ElementUtils.isFinal(elem)) {
                    // The field is static and final.
                    Element e = InternalUtils.symbol(tree.getExpression());
                    if (e != null) {
                        String classname = ElementUtils.getQualifiedClassName(e).toString();
                        String fieldName = tree.getIdentifier().toString();
                        value = evalutator.evaluateStaticFieldAccess(classname, fieldName, tree);
                        if (value != null) {
                            type.replaceAnnotation(
                                    createResultingAnnotation(type.getUnderlyingType(), value));
                        }
                        return null;
                    }
                }

                if (TreeUtils.isArrayLengthAccess(tree)) {
                    // The field access is to the length field, as in "someArrayExpression.length"
                    AnnotatedTypeMirror receiverType = getAnnotatedType(tree.getExpression());
                    if (receiverType.getKind() == TypeKind.ARRAY) {
                        AnnotationMirror arrayAnno = receiverType.getAnnotation(ArrayLen.class);
                        if (arrayAnno != null) {
                            // array.length, where array : @ArrayLen(x)
                            List<Integer> lengths =
                                    ValueAnnotatedTypeFactory.getArrayLength(arrayAnno);
                            type.replaceAnnotation(
                                    createNumberAnnotationMirror(new ArrayList<Number>(lengths)));
                            return null;
                        }
                    }
                }
            }
            return null;
        }

        /** Returns true iff the given type is in the domain of the Constant Value Checker. */
        private boolean handledByValueChecker(AnnotatedTypeMirror type) {
            return coveredClassStrings.contains(type.getUnderlyingType().toString());
        }

        /**
         * Returns a constant value annotation with the {@code value}. The class of the annotation
         * reflects the {@code resultType} given.
         *
         * @param resultType used to selecte which kind of value annotation is returned
         * @param value value to use
         * @return a constant value annotation with the {@code value}
         */
        private AnnotationMirror createResultingAnnotation(TypeMirror resultType, Object value) {
            return createResultingAnnotation(resultType, Collections.singletonList(value));
        }

        /**
         * Returns a constant value annotation with the {@code values}. The class of the annotation
         * reflects the {@code resultType} given.
         *
         * @param resultType used to selected which kind of value annotation is returned
         * @param values must be a homogeneous list: every element of it has the same class
         * @return a constant value annotation with the {@code values}
         */
        private AnnotationMirror createResultingAnnotation(TypeMirror resultType, List<?> values) {
            if (values == null) {
                return UNKNOWNVAL;
            }
            // For some reason null is included in the list of values,
            // so remove it so that it does not cause a NPE elsewhere.
            values.remove(null);
            if (values.size() == 0) {
                return BOTTOMVAL;
            }

            if (TypesUtils.isString(resultType)) {
                List<String> stringVals = new ArrayList<>(values.size());
                for (Object o : values) {
                    stringVals.add((String) o);
                }
                return createStringAnnotation(stringVals);
            } else if (ValueCheckerUtils.getClassFromType(resultType) == byte[].class) {
                List<String> stringVals = new ArrayList<>(values.size());
                for (Object o : values) {
                    if (o instanceof byte[]) {
                        stringVals.add(new String((byte[]) o));
                    } else {
                        stringVals.add(o.toString());
                    }
                }
                return createStringAnnotation(stringVals);
            }

            TypeKind primitiveKind;
            if (TypesUtils.isPrimitive(resultType)) {
                primitiveKind = resultType.getKind();
            } else if (TypesUtils.isBoxedPrimitive(resultType)) {
                primitiveKind = types.unboxedType(resultType).getKind();
            } else {
                return UNKNOWNVAL;
            }

            switch (primitiveKind) {
                case BOOLEAN:
                    List<Boolean> boolVals = new ArrayList<>(values.size());
                    for (Object o : values) {
                        boolVals.add((Boolean) o);
                    }
                    return createBooleanAnnotation(boolVals);
                case DOUBLE:
                case FLOAT:
                case INT:
                case LONG:
                case SHORT:
                case BYTE:
                    List<Number> numberVals = new ArrayList<>(values.size());
                    List<Character> characterVals = new ArrayList<>(values.size());
                    for (Object o : values) {
                        if (o instanceof Character) {
                            characterVals.add((Character) o);
                        } else {
                            numberVals.add((Number) o);
                        }
                    }
                    if (numberVals.isEmpty()) {
                        return createCharAnnotation(characterVals);
                    }
                    return createNumberAnnotationMirror(new ArrayList<>(numberVals));
                case CHAR:
                    List<Character> charVals = new ArrayList<>(values.size());
                    for (Object o : values) {
                        if (o instanceof Number) {
                            charVals.add((char) ((Number) o).intValue());
                        } else {
                            charVals.add((char) o);
                        }
                    }
                    return createCharAnnotation(charVals);
            }

            throw new UnsupportedOperationException("Unexpected kind:" + resultType);
        }
    }

    /**
     * Returns a {@link IntVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of longs; duplicates are allowed and the values may be in any order
     * @return a {@link IntVal} annotation using the values
     */
    public AnnotationMirror createIntValAnnotation(List<Long> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, IntVal.class);
        builder.setValue("value", values);
        return builder.build();
    }

    /**
     * Returns a {@link DoubleVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of doubles; duplicates are allowed and the values may be in any order
     * @return a {@link DoubleVal} annotation using the values
     */
    public AnnotationMirror createDoubleValAnnotation(List<Double> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, DoubleVal.class);
        builder.setValue("value", values);
        return builder.build();
    }

    /**
     * Returns a {@link StringVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of strings; duplicates are allowed and the values may be in any order
     * @return a {@link StringVal} annotation using the values
     */
    public AnnotationMirror createStringAnnotation(List<String> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, StringVal.class);
        builder.setValue("value", values);
        return builder.build();
    }

    /**
     * Returns a {@link ArrayLen} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of integers; duplicates are allowed and the values may be in any order
     * @return a {@link ArrayLen} annotation using the values
     */
    public AnnotationMirror createArrayLenAnnotation(List<Integer> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, ArrayLen.class);
        builder.setValue("value", values);
        return builder.build();
    }

    /**
     * Returns a {@link BoolVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of booleans; duplicates are allowed and the values may be in any order
     * @return a {@link BoolVal} annotation using the values
     */
    public AnnotationMirror createBooleanAnnotation(List<Boolean> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, BoolVal.class);
        builder.setValue("value", values);
        return builder.build();
    }

    /**
     * Returns a {@link IntVal} annotation using the values. If {@code values} is null, then
     * UnknownVal is returned; if {@code values} is empty, then bottom is returned. The values are
     * sorted and duplicates are removed before the annotation is created.
     *
     * @param values list of characters; duplicates are allowed and the values may be in any order
     * @return a {@link IntVal} annotation using the values
     */
    public AnnotationMirror createCharAnnotation(List<Character> values) {
        if (values == null) {
            return UNKNOWNVAL;
        }
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        List<Long> longValues = new ArrayList<>();
        for (char value : values) {
            longValues.add((long) value);
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, IntVal.class);
        builder.setValue("value", longValues);
        return builder.build();
    }

    /** @param values must be a homogeneous list: every element of it has the same class. */
    private AnnotationMirror createNumberAnnotationMirror(List<Number> values) {
        if (values.isEmpty()) {
            return UNKNOWNVAL;
        }
        Number first = values.get(0);
        if (first instanceof Integer
                || first instanceof Short
                || first instanceof Long
                || first instanceof Byte) {
            List<Long> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.longValue());
            }
            return createIntValAnnotation(intValues);
        }
        if (first instanceof Double || first instanceof Float) {
            List<Double> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.doubleValue());
            }
            return createDoubleValAnnotation(intValues);
        }
        throw new UnsupportedOperationException(
                "ValueAnnotatedTypeFactory: unexpected class: " + first.getClass());
    }

    /**
     * Returns the set of possible values as a sorted listed with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * <p>The method returns a list of {@code Long} but is named {@code getIntValues} because it
     * supports the {@code @IntVal} annotation.
     *
     * @param intAnno an {@code @IntVal} annotation, or null
     */
    public static List<Long> getIntValues(AnnotationMirror intAnno) {
        if (intAnno == null) {
            return null;
        }
        List<Long> list = AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
        ValueCheckerUtils.removeDuplicates(list);
        return list;
    }

    /**
     * Returns the set of possible values as a sorted listed with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * @param doubleAnno a {@code @DoubleVal} annotation, or null
     */
    public static List<Double> getDoubleValues(AnnotationMirror doubleAnno) {
        if (doubleAnno == null) {
            return null;
        }
        List<Double> list =
                AnnotationUtils.getElementValueArray(doubleAnno, "value", Double.class, true);
        ValueCheckerUtils.removeDuplicates(list);
        return list;
    }

    /**
     * Returns the set of possible array lengths as a sorted listed with no duplicate values.
     * Returns the empty list if no values are possible (for dead code). Returns null if any value
     * is possible -- that is, if no estimate can be made -- and this includes when there is no
     * constant-value annotation so the argument is null.
     *
     * @param arrayAnno an {@code @ArrayLen} annotation, or null
     */
    public static List<Integer> getArrayLength(AnnotationMirror arrayAnno) {
        if (arrayAnno == null) {
            return null;
        }
        List<Integer> list =
                AnnotationUtils.getElementValueArray(arrayAnno, "value", Integer.class, true);
        ValueCheckerUtils.removeDuplicates(list);
        return list;
    }

    /**
     * Returns the set of possible values as a sorted listed with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * @param intAnno an {@code @IntVal} annotation, or null
     */
    public static List<Character> getCharValues(AnnotationMirror intAnno) {
        if (intAnno == null) {
            return new ArrayList<>();
        }
        List<Long> intValues =
                AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
        TreeSet<Character> charValues = new TreeSet<>();
        for (Long i : intValues) {
            charValues.add((char) i.intValue());
        }
        return new ArrayList<>(charValues);
    }

    /**
     * Returns the set of possible values as a sorted listed with no duplicate values. Returns the
     * empty list if no values are possible (for dead code). Returns null if any value is possible
     * -- that is, if no estimate can be made -- and this includes when there is no constant-value
     * annotation so the argument is null.
     *
     * @param boolAnno a {@code @BoolVal} annotation, or null
     */
    public static List<Boolean> getBooleanValues(AnnotationMirror boolAnno) {
        if (boolAnno == null) {
            return new ArrayList<>();
        }
        List<Boolean> boolValues =
                AnnotationUtils.getElementValueArray(boolAnno, "value", Boolean.class, true);
        Set<Boolean> boolSet = new TreeSet<>(boolValues);
        if (boolSet.size() > 1) {
            // boolSet={true,false};
            return null;
        }
        return new ArrayList<>(boolSet);
    }

    /**
     * Empty list means dead code -- no values are possible. Null means no information in available
     * -- all values are possible.
     */
    public List<Long> getIntValuesFromExpression(
            String expression, Tree tree, TreePath currentPath) {
        AnnotationMirror intValAnno = null;
        try {
            intValAnno =
                    getAnnotationFromJavaExpressionString(
                            expression, tree, currentPath, IntVal.class);
        } catch (FlowExpressionParseException e) {
            // ignore parse errors
            return null;
        }
        return getIntValues(intValAnno);
    }
}
