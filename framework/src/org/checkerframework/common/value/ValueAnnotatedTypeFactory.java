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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
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
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * @author plvines
 * @author smillst
 *
 *         AnnotatedTypeFactory for the Value type system.
 *
 */
public class ValueAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror UNKNOWNVAL, BOTTOMVAL;
    /** The maximum number of values allowed in an annotation's array */
    protected static final int MAX_VALUES = 10;
    protected Set<String> coveredClassStrings;

    /** should this type factory report warnings? **/
    private final boolean reportEvalWarnings;

    /** Helper class that evaluates statically executable methods, constructor, and fields.*/
    private final ReflectiveEvalutator evalutator;

    public ValueAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        BOTTOMVAL = AnnotationUtils.fromClass(elements, BottomVal.class);
        UNKNOWNVAL = AnnotationUtils.fromClass(elements, UnknownVal.class);

        coveredClassStrings = new HashSet<String>(19);
        coveredClassStrings.add("int");
        coveredClassStrings.add("java.lang.Integer");
        coveredClassStrings.add("double");
        coveredClassStrings.add("java.lang.Double");
        coveredClassStrings.add("byte");
        coveredClassStrings.add("java.lang.Byte");
        coveredClassStrings.add("java.lang.String");
        coveredClassStrings.add("char");
        coveredClassStrings.add("java.lang.Character");
        coveredClassStrings.add("float");
        coveredClassStrings.add("java.lang.Float");
        coveredClassStrings.add("boolean");
        coveredClassStrings.add("java.lang.Boolean");
        coveredClassStrings.add("long");
        coveredClassStrings.add("java.lang.Long");
        coveredClassStrings.add("short");
        coveredClassStrings.add("java.lang.Short");
        coveredClassStrings.add("byte[]");
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
     * Creates an annotation of the name given with the set of values given.
     * Issues a checker warning and return UNKNOWNVAL if values.size &gt;
     * MAX_VALUES
     *
     * @return annotation given by name with values=values, or UNKNOWNVAL
     */
    private AnnotationMirror createAnnotation(String name, Set<?> values) {

        if (values.size() > 0 && values.size() <= MAX_VALUES) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv, name);
            List<Object> valuesList = new ArrayList<Object>(values);
            builder.setValue("value", valuesList);
            return builder.build();
        } else {
            return UNKNOWNVAL;
        }
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
         * If any constant-value annotation has &gt; MAX_VALUES number of values
         * provided, treats the value as UnknownVal. Works together with
         * ValueVisitor.visitAnnotation, which issues a warning to the user in
         * this case.
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

    /**
     * The qualifier hierarchy for the Value type system
     */
    private final class ValueQualifierHierarchy extends MultiGraphQualifierHierarchy {

        /**
         * @param factory
         *            MultiGraphFactory to use to construct this
         */
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
         * Determines the least upper bound of a1 and a2. If a1 and a2 are both
         * the same type of Value annotation, then the LUB is the result of
         * taking all values from both a1 and a2 and removing duplicates. If a1
         * and a2 are not the same type of Value annotation they may still be
         * mergeable because some values can be implicitly cast as others. If a1
         * and a2 are both in {DoubleVal, IntVal} then they will be converted
         * upwards: IntVal &rarr; DoubleVal to arrive at a common annotation type.
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
                HashSet<Object> newValues = new HashSet<Object>(a1Values.size() + a2Values.size());

                newValues.addAll(a1Values);
                newValues.addAll(a2Values);

                return createAnnotation(a1.getAnnotationType().toString(), newValues);
            }
            // Annotations are in this hierarchy, but they are not the same
            else {
                // If either is UNKNOWNVAL, ARRAYLEN, STRINGVAL, or BOOLEAN then
                // the LUB is
                // UnknownVal
                if (!((AnnotationUtils.areSameByClass(a1, IntVal.class)
                                || AnnotationUtils.areSameByClass(a1, DoubleVal.class))
                        && (AnnotationUtils.areSameByClass(a2, IntVal.class)
                                || AnnotationUtils.areSameByClass(a2, DoubleVal.class)))) {
                    return UNKNOWNVAL;
                } else {
                    // At this point one of them must be a DoubleVal and one an
                    // IntVal
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
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are Value. In this case, rhs is a
         * subtype of lhs iff lhs contains at least every element of rhs
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {

            if (AnnotationUtils.areSameByClass(lhs, UnknownVal.class)
                    || AnnotationUtils.areSameByClass(rhs, BottomVal.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, UnknownVal.class)
                    || AnnotationUtils.areSameByClass(lhs, BottomVal.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
                // Same type, so might be subtype
                List<Object> lhsValues =
                        AnnotationUtils.getElementValueArray(lhs, "value", Object.class, true);
                List<Object> rhsValues =
                        AnnotationUtils.getElementValueArray(rhs, "value", Object.class, true);
                return lhsValues.containsAll(rhsValues);
            } else if (AnnotationUtils.areSameByClass(lhs, DoubleVal.class)
                    && AnnotationUtils.areSameByClass(rhs, IntVal.class)) {
                List<Long> rhsValues;
                rhsValues = AnnotationUtils.getElementValueArray(rhs, "value", Long.class, true);
                List<Double> lhsValues =
                        AnnotationUtils.getElementValueArray(lhs, "value", Double.class, true);
                boolean same = false;
                for (Long rhsLong : rhsValues) {
                    for (Double lhsDbl : lhsValues) {
                        if (lhsDbl.doubleValue() == rhsLong.doubleValue()) {
                            same = true;
                            break;
                        }
                    }
                    if (!same) {
                        return false;
                    }
                }
                return same;
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

    /**
     * The TreeAnnotator for this AnnotatedTypeFactory
     */
    protected class ValueTreeAnnotator extends TreeAnnotator {

        public ValueTreeAnnotator(ValueAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {

            List<? extends ExpressionTree> dimensions = tree.getDimensions();
            List<? extends ExpressionTree> initializers = tree.getInitializers();

            // Dimensions provided
            if (!dimensions.isEmpty()) {
                handleDimensions(dimensions, (AnnotatedArrayType) type);
            } else {
                // Initializer used
                handleInitalizers(initializers, (AnnotatedArrayType) type);

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
         * Recursive method to handle array initializations. Recursively
         * descends the initializer to find each dimension's size and create the
         * appropriate annotation for it.
         *
         * @param dimensions
         *            a list of ExpressionTrees where each ExpressionTree is a
         *            specifier of the size of that dimension (should be an
         *            IntVal).
         * @param type
         *            the AnnotatedTypeMirror of the array
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

        private void handleInitalizers(
                List<? extends ExpressionTree> initializers, AnnotatedArrayType type) {

            List<Integer> array = new ArrayList<>();
            array.add(initializers.size());
            type.replaceAnnotation(createArrayLenAnnotation(array));

            boolean singleDem = type.getComponentType().getKind() != TypeKind.ARRAY;
            if (singleDem) {
                return;
            }
            List<List<Integer>> summarylengths = new ArrayList<>();

            for (ExpressionTree init : initializers) {
                AnnotatedTypeMirror componentType = getAnnotatedType(init);
                int count = 0;
                while (componentType.getKind() == TypeKind.ARRAY) {
                    AnnotationMirror arrayLen = componentType.getAnnotation(ArrayLen.class);
                    List<Integer> currentLengths;
                    if (arrayLen != null) {
                        currentLengths = getArrayLength(arrayLen);
                    } else {
                        currentLengths = (new ArrayList<Integer>());
                    }
                    if (count == summarylengths.size()) {
                        summarylengths.add(new ArrayList<Integer>());
                    }
                    summarylengths.get(count).addAll(currentLengths);
                    count++;
                    componentType = ((AnnotatedArrayType) componentType).getComponentType();
                }
            }

            AnnotatedTypeMirror componentType = type.getComponentType();
            int i = 0;
            while (componentType.getKind() == TypeKind.ARRAY && i < summarylengths.size()) {
                componentType.addAnnotation(createArrayLenAnnotation(summarylengths.get(i)));
                componentType = ((AnnotatedArrayType) componentType).getComponentType();
                i++;
            }
        }

        private String getByteArrayStringVal(List<? extends ExpressionTree> initializers) {
            boolean allLiterals = true;
            byte[] bytes = new byte[initializers.size()];
            int i = 0;
            for (ExpressionTree e : initializers) {
                if (e.getKind() == Tree.Kind.INT_LITERAL) {
                    bytes[i] = (byte) (((Integer) ((LiteralTree) e).getValue()).intValue());
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    bytes[i] = (byte) (((Character) ((LiteralTree) e).getValue()).charValue());
                } else {
                    allLiterals = false;
                }
                i++;
            }
            if (allLiterals) {
                return new String(bytes);
            }
            // If any part of the initialize isn't know,
            // the stringval isn't known.
            return null;
        }

        private String getCharArrayStringVal(List<? extends ExpressionTree> initializers) {
            boolean allLiterals = true;
            String stringVal = "";
            for (ExpressionTree e : initializers) {
                if (e.getKind() == Tree.Kind.INT_LITERAL) {
                    char charVal = (char) (((Integer) ((LiteralTree) e).getValue()).intValue());
                    stringVal += charVal;
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    char charVal = (((Character) ((LiteralTree) e).getValue()));
                    stringVal += charVal;
                } else {
                    allLiterals = false;
                }
            }
            if (allLiterals) {
                return stringVal;
            }
            // If any part of the initialize isn't know,
            // the stringval isn't known.
            return null;
        }

        @Override
        public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror type) {
            if (isUnderlyingTypeAValue(type)) {
                AnnotatedTypeMirror castedAnnotation = getAnnotatedType(tree.getExpression());
                List<?> values = getValues(castedAnnotation, type.getUnderlyingType());
                type.replaceAnnotation(
                        resultAnnotationHandler(type.getUnderlyingType(), values, tree));
            } else if (type.getKind() == TypeKind.ARRAY) {
                if (tree.getExpression().getKind() == Kind.NULL_LITERAL) {
                    type.replaceAnnotation(BOTTOMVAL);
                }
            }
            return null;
        }

        private List<?> getValues(AnnotatedTypeMirror type, TypeMirror castTo) {
            AnnotationMirror anno = type.getAnnotationInHierarchy(UNKNOWNVAL);
            if (anno == null) {
                // if type is an AnnotatedTypeVariable (or other type without a primary annotation)
                // then anno will be null. It would be safe to use the annotation on the upper bound;
                //  however, unless the upper bound was explicitly annotated, it will be unknown.
                // AnnotatedTypes.findEffectiveAnnotationInHierarchy(, toSearch, top)
                return new ArrayList<>();
            }
            return ValueCheckerUtils.getValuesCastedToType(anno, castTo);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (isUnderlyingTypeAValue(type)) {
                switch (tree.getKind()) {
                    case BOOLEAN_LITERAL:
                        AnnotationMirror boolAnno =
                                createBooleanAnnotation(
                                        Collections.singletonList((Boolean) tree.getValue()));
                        type.replaceAnnotation(boolAnno);
                        return null;

                    case CHAR_LITERAL:
                        AnnotationMirror charAnno =
                                createCharAnnotation(
                                        Collections.singletonList((Character) tree.getValue()));
                        type.replaceAnnotation(charAnno);
                        return null;

                    case DOUBLE_LITERAL:
                        AnnotationMirror doubleAnno =
                                createNumberAnnotationMirror(
                                        Collections.<Number>singletonList(
                                                (Double) tree.getValue()));
                        type.replaceAnnotation(doubleAnno);
                        return null;

                    case FLOAT_LITERAL:
                        AnnotationMirror floatAnno =
                                createNumberAnnotationMirror(
                                        Collections.<Number>singletonList((Float) tree.getValue()));
                        type.replaceAnnotation(floatAnno);
                        return null;
                    case INT_LITERAL:
                        AnnotationMirror intAnno =
                                createNumberAnnotationMirror(
                                        Collections.<Number>singletonList(
                                                (Integer) tree.getValue()));
                        type.replaceAnnotation(intAnno);
                        return null;
                    case LONG_LITERAL:
                        AnnotationMirror longAnno =
                                createNumberAnnotationMirror(
                                        Collections.<Number>singletonList((Long) tree.getValue()));
                        type.replaceAnnotation(longAnno);
                        return null;
                    case STRING_LITERAL:
                        AnnotationMirror stringAnno =
                                createStringAnnotation(
                                        Collections.singletonList((String) tree.getValue()));
                        type.replaceAnnotation(stringAnno);
                        return null;
                    default:
                        return null;
                }
            }
            return null;
        }

        /**
         * Simple method to take a MemberSelectTree representing a method call
         * and determine if the method's return is annotated with
         * {@code @StaticallyExecutable}.
         */
        private boolean methodIsStaticallyExecutable(Element method) {
            return getDeclAnnotation(method, StaticallyExecutable.class) != null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (isUnderlyingTypeAValue(type)
                    && methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree))) {
                // Get argument values
                List<? extends ExpressionTree> arguments = tree.getArguments();
                ArrayList<List<?>> argValues;
                if (arguments.size() > 0) {
                    argValues = new ArrayList<List<?>>();
                    for (ExpressionTree argument : arguments) {
                        AnnotatedTypeMirror argType = getAnnotatedType(argument);
                        List<?> values = getValues(argType, argType.getUnderlyingType());
                        if (values.isEmpty()) {
                            // values aren't known, so don't try to evaluate the
                            // method
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
                    if (receiverValues.isEmpty()) {
                        // values aren't known, so don't try to evaluate the
                        // method
                        return null;
                    }
                } else {
                    receiverValues = null;
                }

                // Evaluate method
                List<?> returnValues =
                        evalutator.evaluteMethodCall(argValues, receiverValues, tree);
                AnnotationMirror returnType =
                        resultAnnotationHandler(type.getUnderlyingType(), returnValues, tree);
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
                    || (isUnderlyingTypeAValue(type)
                            && methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree)))) {
                // get arugment values
                List<? extends ExpressionTree> arguments = tree.getArguments();
                ArrayList<List<?>> argValues;
                if (arguments.size() > 0) {
                    argValues = new ArrayList<List<?>>();
                    for (ExpressionTree argument : arguments) {
                        AnnotatedTypeMirror argType = getAnnotatedType(argument);
                        List<?> values = getValues(argType, argType.getUnderlyingType());
                        if (values.isEmpty()) {
                            // values aren't known, so don't try to evaluate the
                            // method
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
                        resultAnnotationHandler(type.getUnderlyingType(), returnValues, tree);
                type.replaceAnnotation(returnType);
            }

            return null;
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
            if (TreeUtils.isFieldAccess(tree) && isUnderlyingTypeAValue(type)) {
                VariableElement elem = (VariableElement) InternalUtils.symbol(tree);
                Object value = elem.getConstantValue();
                if (value != null) {
                    // compile time constant
                    type.replaceAnnotation(
                            resultAnnotationHandler(
                                    type.getUnderlyingType(),
                                    Collections.singletonList(value),
                                    tree));
                    return null;
                }
                if (ElementUtils.isStatic(elem) && ElementUtils.isFinal(elem)) {
                    Element e = InternalUtils.symbol(tree.getExpression());
                    if (e != null) {
                        String classname = ElementUtils.getQualifiedClassName(e).toString();
                        String fieldName = tree.getIdentifier().toString();
                        value = evalutator.evaluateStaticFieldAccess(classname, fieldName, tree);
                        if (value != null) {
                            type.replaceAnnotation(
                                    resultAnnotationHandler(
                                            type.getUnderlyingType(),
                                            Collections.singletonList(value),
                                            tree));
                        }
                        return null;
                    }
                }

                if (tree.getIdentifier().toString().equals("length")) {
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

        /**
         * Overloaded method for convenience of dealing with
         * AnnotatedTypeMirrors. See isClassCovered(TypeMirror type) below
         */
        private boolean isUnderlyingTypeAValue(AnnotatedTypeMirror type) {
            return coveredClassStrings.contains(type.getUnderlyingType().toString());
        }

        /**
         * Overloaded version to accept an AnnotatedTypeMirror
         *
         * @param resultType
         *            is evaluated using getClass to derived a Class object for
         *            passing to the other resultAnnotationHandler function
         * @param tree
         *            location for error reporting
         */
        private AnnotationMirror resultAnnotationHandler(
                TypeMirror resultType, List<?> results, Tree tree) {

            Class<?> resultClass = ValueCheckerUtils.getClassFromType(resultType);

            // For some reason null is included in the list of values,
            // so remove it so that it does not cause a NPE elsewhere.
            results.remove(null);
            if (results.size() == 0) {
                return UNKNOWNVAL;
            } else if (resultClass == Boolean.class || resultClass == boolean.class) {
                HashSet<Boolean> boolVals = new HashSet<Boolean>(results.size());
                for (Object o : results) {
                    boolVals.add((Boolean) o);
                }
                return createBooleanAnnotation(new ArrayList<Boolean>(boolVals));

            } else if (resultClass == Double.class
                    || resultClass == double.class
                    || resultClass == Float.class
                    || resultClass == float.class
                    || resultClass == Integer.class
                    || resultClass == int.class
                    || resultClass == Long.class
                    || resultClass == long.class
                    || resultClass == Short.class
                    || resultClass == short.class
                    || resultClass == Byte.class
                    || resultClass == byte.class) {
                HashSet<Number> numberVals = new HashSet<>(results.size());
                List<Character> charVals = new ArrayList<>();
                for (Object o : results) {
                    if (o instanceof Character) {
                        charVals.add((Character) o);
                    } else {
                        numberVals.add((Number) o);
                    }
                }
                if (numberVals.isEmpty()) {
                    return createCharAnnotation(charVals);
                }
                return createNumberAnnotationMirror(new ArrayList<Number>(numberVals));
            } else if (resultClass == char.class || resultClass == Character.class) {
                HashSet<Character> intVals = new HashSet<>(results.size());
                for (Object o : results) {
                    if (o instanceof Number) {
                        intVals.add((char) ((Number) o).intValue());
                    } else {
                        intVals.add((char) o);
                    }
                }
                return createCharAnnotation(new ArrayList<Character>(intVals));
            } else if (resultClass == String.class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    stringVals.add((String) o);
                }
                return createStringAnnotation(new ArrayList<String>(stringVals));
            } else if (resultClass == byte[].class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    if (o instanceof byte[]) {
                        stringVals.add(new String((byte[]) o));
                    } else {
                        stringVals.add(o.toString());
                    }
                }
                return createStringAnnotation(new ArrayList<String>(stringVals));
            }

            return UNKNOWNVAL;
        }
    }

    public AnnotationMirror createIntValAnnotation(List<Long> intValues) {
        intValues = ValueCheckerUtils.removeDuplicates(intValues);
        if (intValues.isEmpty() || intValues.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, IntVal.class);
        builder.setValue("value", intValues);
        return builder.build();
    }

    public AnnotationMirror createDoubleValAnnotation(List<Double> doubleValues) {
        doubleValues = ValueCheckerUtils.removeDuplicates(doubleValues);
        if (doubleValues.isEmpty() || doubleValues.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, DoubleVal.class);
        builder.setValue("value", doubleValues);
        return builder.build();
    }

    public AnnotationMirror createStringAnnotation(List<String> values) {
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.isEmpty() || values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, StringVal.class);
        builder.setValue("value", values);
        return builder.build();
    }

    public AnnotationMirror createArrayLenAnnotation(List<Integer> values) {
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.isEmpty() || values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, ArrayLen.class);
        builder.setValue("value", values);
        return builder.build();
    }

    public AnnotationMirror createBooleanAnnotation(List<Boolean> values) {
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.isEmpty() || values.size() > MAX_VALUES) {
            return UNKNOWNVAL;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, BoolVal.class);
        builder.setValue("value", values);
        return builder.build();
    }

    public AnnotationMirror createCharAnnotation(List<Character> values) {
        values = ValueCheckerUtils.removeDuplicates(values);
        if (values.isEmpty() || values.size() > MAX_VALUES) {
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

    public static List<Long> getIntValues(AnnotationMirror intAnno) {
        return AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
    }

    public static List<Double> getDoubleValues(AnnotationMirror doubleAnno) {
        return AnnotationUtils.getElementValueArray(doubleAnno, "value", Double.class, true);
    }

    public static List<Integer> getArrayLength(AnnotationMirror arrayAnno) {
        return AnnotationUtils.getElementValueArray(arrayAnno, "value", Integer.class, true);
    }

    public static List<Character> getCharValues(AnnotationMirror intAnno) {
        if (intAnno != null) {
            List<Long> intValues =
                    AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
            List<Character> charValues = new ArrayList<Character>();
            for (Long i : intValues) {
                charValues.add((char) i.intValue());
            }
            return charValues;
        }
        return new ArrayList<>();
    }

    public static List<Boolean> getBooleanValues(AnnotationMirror boolAnno) {
        if (boolAnno != null) {
            List<Boolean> boolValues =
                    AnnotationUtils.getElementValueArray(boolAnno, "value", Boolean.class, true);
            Set<Boolean> boolSet = new TreeSet<>(boolValues);
            if (boolSet.size() > 1) {
                // boolSet={true,false};
                return new ArrayList<>();
            }
            if (boolSet.size() == 0) {
                // boolSet={};
                return new ArrayList<>();
            }
            if (boolSet.size() == 1) {
                // boolSet={true} or boolSet={false}
                return new ArrayList<>(boolSet);
            }
        }
        return new ArrayList<>();
    }
}
