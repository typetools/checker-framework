package org.checkerframework.checker.nullness;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.qual.Covariant;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GeneralAnnotatedTypeFactory;
import org.checkerframework.framework.type.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.ListTreeAnnotator;
import org.checkerframework.framework.type.PropagationTreeAnnotator;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TreeAnnotator;
import org.checkerframework.framework.type.TypeAnnotator;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.DependentTypes;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

/**
 * The annotated type factory for the nullness type-system.
 */
public class NullnessAnnotatedTypeFactory
    extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis> {

    /** Annotation constants */
    protected final AnnotationMirror NONNULL, NULLABLE, POLYNULL, MONOTONIC_NONNULL;
    protected final AnnotationMirror UNKNOWNKEYFOR, KEYFOR;

    /** Dependent types instance. */
    protected final DependentTypes dependentTypes;

    protected final SystemGetPropertyHandler systemGetPropertyHandler;
    protected final CollectionToArrayHeuristics collectionToArrayHeuristics;

    /**
     * Factory for arbitrary qualifiers, used for declarations and "unused"
     * qualifier.
     */
    protected final GeneralAnnotatedTypeFactory generalFactory;

    // Cache for the nullness annotations
    protected final Set<Class<? extends Annotation>> nullnessAnnos;

    protected final Class<? extends Annotation> checkerKeyForClass = org.checkerframework.checker.nullness.qual.KeyFor.class;

    /** Regular expression for an identifier */
    protected final String identifierRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";

    /** Matches an identifier */
    protected final Pattern identifierPattern = Pattern.compile("^"
            + identifierRegex + "$");

    protected final /*@CompilerMessageKey*/ String KEYFOR_VALUE_PARAMETER_VARIABLE_NAME = "keyfor.value.parameter.variable.name";
    protected final /*@CompilerMessageKey*/ String KEYFOR_VALUE_PARAMETER_VARIABLE_NAME_FORMAL_PARAM_NUM = "keyfor.value.parameter.variable.name.formal.param.num";

    @SuppressWarnings("deprecation") // aliasing to deprecated annotation
    public NullnessAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFbc) {
        super(checker, useFbc);

        NONNULL = AnnotationUtils.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(elements, Nullable.class);
        POLYNULL = AnnotationUtils.fromClass(elements, PolyNull.class);
        MONOTONIC_NONNULL = AnnotationUtils.fromClass(elements, MonotonicNonNull.class);

        KEYFOR = AnnotationUtils.fromClass(elements, KeyFor.class);
        UNKNOWNKEYFOR = AnnotationUtils.fromClass(elements, UnknownKeyFor.class);

        Set<Class<? extends Annotation>> tempNullnessAnnos = new HashSet<>();
        tempNullnessAnnos.add(NonNull.class);
        tempNullnessAnnos.add(MonotonicNonNull.class);
        tempNullnessAnnos.add(Nullable.class);
        tempNullnessAnnos.add(PolyNull.class);
        tempNullnessAnnos.add(PolyAll.class);
        nullnessAnnos = Collections.unmodifiableSet(tempNullnessAnnos);

        addAliasedAnnotation(org.checkerframework.checker.nullness.qual.LazyNonNull.class, MONOTONIC_NONNULL);

        // If you update the following, also update ../../../manual/nullness-checker.tex .
        // Aliases for @Nonnull:
        addAliasedAnnotation(com.sun.istack.internal.NotNull.class, NONNULL);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class, NONNULL);
        addAliasedAnnotation(javax.annotation.Nonnull.class, NONNULL);
        addAliasedAnnotation(javax.validation.constraints.NotNull.class, NONNULL);
        addAliasedAnnotation(org.eclipse.jdt.annotation.NonNull.class, NONNULL);
        addAliasedAnnotation(org.jetbrains.annotations.NotNull.class, NONNULL);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NonNull.class, NONNULL);
        addAliasedAnnotation(org.jmlspecs.annotation.NonNull.class, NONNULL);

        // Aliases for @Nullable:
        addAliasedAnnotation(com.sun.istack.internal.Nullable.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(edu.umd.cs.findbugs.annotations.UnknownNullness.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(javax.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.eclipse.jdt.annotation.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.jetbrains.annotations.Nullable.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.CheckForNull.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NullAllowed.class, NULLABLE);
        addAliasedAnnotation(org.netbeans.api.annotations.common.NullUnknown.class, NULLABLE);
        addAliasedAnnotation(org.jmlspecs.annotation.Nullable.class, NULLABLE);

        // Add compatibility annotations:
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.NullableDecl.class, NULLABLE);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.PolyNullDecl.class, POLYNULL);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.NonNullDecl.class, NONNULL);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.MonotonicNonNullDecl.class, MONOTONIC_NONNULL);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.NullableType.class, NULLABLE);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.PolyNullType.class, POLYNULL);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.NonNullType.class, NONNULL);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.MonotonicNonNullType.class, MONOTONIC_NONNULL);

        // TODO: These heuristics are just here temporarily. They all either
        // need to be replaced, or carefully checked for correctness.
        generalFactory = new GeneralAnnotatedTypeFactory(checker);
        // Alias the same generalFactory below and ensure that setRoot updates it.
        dependentTypes = new DependentTypes(checker, generalFactory);

        systemGetPropertyHandler = new SystemGetPropertyHandler(processingEnv, this);

        postInit();

        // Add Java 7 compatibility annotation:
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.KeyForDecl.class, KEYFOR);

        // do this last, as it might use the factory again.
        this.collectionToArrayHeuristics = new CollectionToArrayHeuristics(
                processingEnv, this);
    }

    @Override
    public void setRoot(CompilationUnitTree root) {
        generalFactory.setRoot(root);
        super.setRoot(root);
    }

    // handle dependent types
    @Override
    protected void annotateImplicit(Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
        super.annotateImplicit(tree, type, useFlow);
        dependentTypes.handle(tree, type);
    }


    @Override
    public AnnotatedTypeMirror getDefaultedAnnotatedType(Tree varTree,
            ExpressionTree valueTree) {
        AnnotatedTypeMirror result = super.getDefaultedAnnotatedType(varTree, valueTree);
        return handlePolyNull(result, valueTree);
    }

    /**
     * Replaces {@link PolyNull} with {@link Nullable} to be more permissive
     * (because {@code type} is usually a left-hand side) if the org.checkerframework.dataflow
     * analysis has determined that this is allowed soundly.
     */
    protected AnnotatedTypeMirror handlePolyNull(AnnotatedTypeMirror type,
            Tree context) {
        if (type.hasAnnotation(PolyNull.class)
                || type.hasAnnotation(PolyAll.class)) {
            NullnessValue inferred = getInferredValueFor(context);
            if (inferred != null && inferred.isPolyNullNull) {
                type.replaceAnnotation(NULLABLE);
            }
        }
        return type;
    }

    // handle dependent types
    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(
            NewClassTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> fromUse = super.constructorFromUse(tree);
        AnnotatedExecutableType constructor = fromUse.first;
        dependentTypes.handleConstructor(tree,
                generalFactory.getAnnotatedType(tree), constructor);
        return fromUse;
    }

    @Override
    public List<VariableTree> getUninitializedInvariantFields(
            NullnessStore store, TreePath path, boolean isStatic,
            List<? extends AnnotationMirror> receiverAnnotations) {
        List<VariableTree> candidates = super.getUninitializedInvariantFields(
                store, path, isStatic, receiverAnnotations);
        List<VariableTree> result = new ArrayList<>();
        for (VariableTree c : candidates) {
            AnnotatedTypeMirror type = getAnnotatedType(c);
            boolean isPrimitive = TypesUtils.isPrimitive(type.getUnderlyingType());
            if (!isPrimitive) {
                // primitives do not need to be initialized
                result.add(c);
            }
        }
        return result;
    }

    @Override
    protected NullnessAnalysis createFlowAnalysis(List<Pair<VariableElement, NullnessValue>> fieldValues) {
        return new NullnessAnalysis(checker, this, fieldValues);
    }

    @Override
    public NullnessTransfer createFlowTransferFunction(CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer> analysis) {
        return new NullnessTransfer((NullnessAnalysis) analysis);
    }

    @Override
    public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(
            MethodInvocationTree tree) {
        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super
                .methodFromUse(tree);
        AnnotatedExecutableType method = mfuPair.first;

        systemGetPropertyHandler.handle(tree, method);
        collectionToArrayHeuristics.handle(tree, method);

        return keyForMethodFromUse(tree, method, mfuPair.second);
    }

    private Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> keyForMethodFromUse(
            MethodInvocationTree tree, AnnotatedExecutableType method, List<AnnotatedTypeMirror> second) {

        Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

        // Modify parameters
        List<AnnotatedTypeMirror> params = method.getParameterTypes();
        for (AnnotatedTypeMirror param : params) {
          AnnotatedTypeMirror subst = keyForSubstituteCall(tree, param);
          mappings.put(param, subst);
        }

        // Modify return type
        AnnotatedTypeMirror returnType = method.getReturnType();
        if (returnType.getKind() != TypeKind.VOID ) {
          AnnotatedTypeMirror subst = keyForSubstituteCall(tree, returnType);
          mappings.put(returnType, subst);
        }

        // TODO: upper bounds, throws?

        method = (AnnotatedExecutableType)method.substitute(mappings);

        return Pair.of(method, second);
    }

    @Override
    public AnnotatedTypeMirror getMethodReturnType(MethodTree m, ReturnTree r) {
        return handlePolyNull(super.getMethodReturnType(m, r), r);
    }

    protected AnnotatedTypeMirror getDeclaredAndDefaultedAnnotatedType(Tree tree) {
        HACK_DONT_CALL_POST_AS_MEMBER = true;
        shouldCache = false;

        AnnotatedTypeMirror type = getAnnotatedType(tree);

        shouldCache = true;
        HACK_DONT_CALL_POST_AS_MEMBER = false;

        return type;
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new NullnessTypeAnnotator(this);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new NullnessPropagationAnnotator(this),
                new ImplicitsTreeAnnotator(this),
                new NullnessTreeAnnotator(this),
                new CommitmentTreeAnnotator(this)
        );
    }

    /**
     * If the element is {@link NonNull} when used in a static member access,
     * modifies the element's type (by adding {@link NonNull}).
     *
     * @param elt
     *            the element being accessed
     * @param type
     *            the type of the element {@code elt}
     */
    private void annotateIfStatic(Element elt, AnnotatedTypeMirror type) {
        if (elt == null)
            return;

        if (elt.getKind().isClass() || elt.getKind().isInterface()
        // Workaround for System.{out,in,err} issue: assume all static
        // fields in java.lang.System are nonnull.
                || isSystemField(elt)) {
            type.replaceAnnotation(NONNULL);
        }
    }

    private static boolean isSystemField(Element elt) {
        if (!elt.getKind().isField())
            return false;

        if (!ElementUtils.isStatic(elt) || !ElementUtils.isFinal(elt))
            return false;

        VariableElement var = (VariableElement) elt;

        // Heuristic: if we have a static final field in a system package,
        // treat it as NonNull (many like Boolean.TYPE and System.out
        // have constant value null but are set by the VM).
        boolean inJavaPackage = ElementUtils.getQualifiedClassName(var)
                .toString().startsWith("java.");

        return (var.getConstantValue() != null
                || var.getSimpleName().contentEquals("class") || inJavaPackage);
    }

    /**
     * Nullness doesn't call propagation on binary and unary because
     * the result is always @Initialized (the default qualifier).
     *
     * Would this be valid to move into CommitmentTreeAnnotator.
     */
    protected class NullnessPropagationAnnotator extends PropagationTreeAnnotator {

        public NullnessPropagationAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            return null;
        }

        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            return null;
        }
    }

    protected class NullnessTreeAnnotator extends TreeAnnotator
        /*extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTreeAnnotator*/ {

        public NullnessTreeAnnotator(NullnessAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node,
                AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;
            // case 8: class in static member access
            annotateIfStatic(elt, type);
            return null;
        }

        @Override
        public Void visitVariable(VariableTree node,
                AnnotatedTypeMirror type) {
            Element elt = InternalUtils.symbol(node);
            if (elt.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                if (!type.isAnnotatedInHierarchy(NONNULL)) {
                    // case 9. exception parameter
                    type.addAnnotation(NONNULL);
                }
            }
            return null;
        }

        @Override
        public Void visitIdentifier(IdentifierTree node,
                AnnotatedTypeMirror type) {

            Element elt = TreeUtils.elementFromUse(node);
            assert elt != null;

            // case 8. static method access
            annotateIfStatic(elt, type);

            if (elt.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                // TODO: It's surprising that we have to do this in
                // both visitVariable and visitIdentifier. This should
                // already be handled by applying the defaults anyway.
                // case 9. exception parameter
                type.replaceAnnotation(NONNULL);
            }

            return null;
        }

        // The result of a binary operation is always non-null.
        @Override
        public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }

        // The result of a compound operation is always non-null.
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node,
                AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            // Committment will run after for initialization defaults
            return null;
        }

        // The result of a unary operation is always non-null.
        @Override
        public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }

        // The result of newly allocated structures is always non-null.
        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(NONNULL);
            return null;
        }
    }

    protected class NullnessTypeAnnotator
        extends InitializationAnnotatedTypeFactory<NullnessValue, NullnessStore, NullnessTransfer, NullnessAnalysis>.CommitmentTypeAnnotator {

        public NullnessTypeAnnotator(InitializationAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
            super(atypeFactory);
        }
    }


    /**
     * @return The list of annotations of the non-null type system.
     */
    public Set<Class<? extends Annotation>> getNullnessAnnotations() {
        return nullnessAnnos;
    }

    @Override
    public Set<Class<? extends Annotation>> getInvalidConstructorReturnTypeAnnotations() {
        Set<Class<? extends Annotation>> l = new HashSet<>(
                super.getInvalidConstructorReturnTypeAnnotations());
        l.addAll(getNullnessAnnotations());
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        Elements elements = processingEnv.getElementUtils();
        return AnnotationUtils.fromClass(elements, NonNull.class);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new NullnessQualifierHierarchy(factory, (Object[]) null);
    }

    protected class NullnessQualifierHierarchy extends InitializationQualifierHierarchy {

        public NullnessQualifierHierarchy(MultiGraphFactory f, Object[] arg) {
            super(f, arg);
        }

        @Override
        public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
            return keyForGetPolymorphicAnnotation(start);
        }

        private AnnotationMirror keyForGetPolymorphicAnnotation(AnnotationMirror start) {
            AnnotationMirror top = getTopAnnotation(start);

            if (AnnotationUtils.areSameIgnoringValues(top, UNKNOWNKEYFOR)) {
                return null;
            }

            if (polyQualifiers.containsKey(top)) {
                return polyQualifiers.get(top);
            } else if (polyQualifiers.containsKey(polymorphicQualifier)) {
                return polyQualifiers.get(polymorphicQualifier);
            } else {
                // No polymorphic qualifier exists for that hierarchy.
                ErrorReporter.errorAbort("MultiGraphQualifierHierarchy: did not find the polymorphic qualifier corresponding to qualifier " + start +
                        "; all polymorphic qualifiers: " + polyQualifiers  + "; this: " + this);
                return null;
            }
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (isInitializationAnnotation(rhs) ||
                    isInitializationAnnotation(lhs)) {
                return this.isSubtypeInitialization(rhs, lhs);
            }

            // KeyFor handling

            if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR) &&
                AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
                List<String> lhsValues = null;
                List<String> rhsValues = null;

                Map<? extends ExecutableElement, ? extends AnnotationValue> valMap = lhs.getElementValues();

                if (valMap.isEmpty())
                    lhsValues = new ArrayList<String>();
                else
                    lhsValues = AnnotationUtils.getElementValueArray(lhs, "value", String.class, true);

                valMap = rhs.getElementValues();

                if (valMap.isEmpty())
                    rhsValues = new ArrayList<String>();
                else
                    rhsValues = AnnotationUtils.getElementValueArray(rhs, "value", String.class, true);

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

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (isInitializationAnnotation(a1) ||
                    isInitializationAnnotation(a2)) {
                return this.leastUpperBoundInitialization(a1, a2);
            }
            return super.leastUpperBound(a1, a2);
        }
    }

    /*
     * Given a string array 'values', returns an AnnotationMirror corresponding to @KeyFor(values)
     */
    public AnnotationMirror createKeyForAnnotationMirrorWithValue(ArrayList<String> values) {
        // Create an AnnotationBuilder with the ArrayList

        AnnotationBuilder builder =
                new AnnotationBuilder(getProcessingEnv(), KeyFor.class);
        builder.setValue("value", values);

        // Return the resulting AnnotationMirror

        return builder.build();
    }

    /*
     * Given a string 'value', returns an AnnotationMirror corresponding to @KeyFor(value)
     */
    public AnnotationMirror createKeyForAnnotationMirrorWithValue(String value) {
        // Create an ArrayList with the value

        ArrayList<String> values = new ArrayList<String>();

        values.add(value);

        return createKeyForAnnotationMirrorWithValue(values);
    }

    /*
     * This method uses FlowExpressionsParseUtil to attempt to recognize the variable names indicated in the values in KeyFor(values).
     *
     * This method modifies atm such that the values are replaced with the string representation of the Flow Expression Receiver
     * returned by FlowExpressionsParseUtil.parse. This ensures that when comparing KeyFor values later when doing subtype checking
     * that equivalent expressions (such as "field" and "this.field" when there is no local variable "field") are represented by the same
     * string so that string comparison will succeed.
     *
     * This is necessary because when KeyForTransfer generates KeyFor annotations, it uses FlowExpressions to generate the values in KeyFor(values).
     * canonicalizeKeyForValues ensures that user-provided KeyFor annotations will contain values that match the format of those in the generated
     * KeyFor annotations.
     *
     * Returns null if the values did not change.
     *
     */
    private ArrayList<String> canonicalizeKeyForValues(AnnotationMirror anno, FlowExpressionContext flowExprContext, TreePath path, Tree t, boolean returnNullIfUnchanged) {
        Receiver varTypeReceiver = null;

        CFAbstractStore<?, ?> store = null;
        boolean unknownReceiver = false;

        if (flowExprContext.receiver.containsUnknown()) {
            // If the receiver is unknown, we will try local variables

            store = getStoreBefore(t);
            unknownReceiver = true; // We could use store != null for this check, but this is clearer.
        }

        if (anno != null) {
            boolean valuesChanged = false; // Indicates that at least one value was changed in the list.
            ArrayList<String> newValues = new ArrayList<String>();

            List<String> values = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
            for (String s: values){
                boolean localVariableFound = false;

                if (unknownReceiver) {
                    // If the receiver is unknown, try a local variable
                    CFAbstractValue<?> val = store.getValueOfLocalVariableByName(s);

                    if (val != null) {
                        newValues.add(s);
                        // Don't set valuesChanged to true since local variable names are already canonicalized
                        localVariableFound = true;
                    }
                }

                if (localVariableFound == false) {
                    try {
                        varTypeReceiver = FlowExpressionParseUtil.parse(s, flowExprContext, path);
                    } catch (FlowExpressionParseException e) {
                    }

                    if (unknownReceiver // The receiver type was unknown initially, and ...
                            && (varTypeReceiver == null
                            || varTypeReceiver.containsUnknown()) // ... the receiver type is still unknown after a call to parse
                            ) {
                        // parse did not find a static member field. Try a nonstatic field.

                        try {
                            varTypeReceiver = FlowExpressionParseUtil.parse("this." + s, // Try a field in the current object. Do not modify s itself since it is used in the newValue.equals(s) check below.
                                    flowExprContext, path);
                        } catch (FlowExpressionParseException e) {
                        }
                    }

                    if (varTypeReceiver != null) {
                        String newValue = varTypeReceiver.toString();
                        newValues.add(newValue);

                        if (!newValue.equals(s)) {
                            valuesChanged = true;
                        }
                    }
                    else {
                        newValues.add(s); // This will get ignored if valuesChanged is false after exiting the for loop
                    }
                }
            }

            if (!returnNullIfUnchanged || valuesChanged) {
                return newValues; // There is no need to sort the resulting array because the subtype check will be a containsAll call, not an equals call.
            }
        }

        return null;
    }

    // Returns null if the AnnotationMirror did not change.
    private AnnotationMirror canonicalizeKeyForValuesGetAnnotationMirror(AnnotationMirror anno, FlowExpressionContext flowExprContext, TreePath path, Tree t) {
        ArrayList<String> newValues = canonicalizeKeyForValues(anno, flowExprContext, path, t, true);

        return newValues == null ? null : createKeyForAnnotationMirrorWithValue(newValues);
    }

    private void canonicalizeKeyForValues(AnnotatedTypeMirror atm, FlowExpressionContext flowExprContext, TreePath path, Tree t) {

        AnnotationMirror anno = canonicalizeKeyForValuesGetAnnotationMirror(atm.getAnnotation(KeyFor.class), flowExprContext, path, t);

        if (anno != null) {
            atm.replaceAnnotation(anno);
        }
    }

    // Build new varType and valueType with canonicalized expressions in the values
    private void keyForCanonicalizeValuesForMethodInvocationNode(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree t,
            TreePath path,
            MethodInvocationNode node) {

        Pair<ArrayList<String>, ArrayList<String>> valuesPair = keyForCanonicalizeValuesForMethodInvocationNode(
                varType.getAnnotation(KeyFor.class),
                valueType.getAnnotation(KeyFor.class),
                t, path, node, true
                );

        ArrayList<String> var = valuesPair.first;
        ArrayList<String> val = valuesPair.second;

        if (var != null) {
            varType.replaceAnnotation(createKeyForAnnotationMirrorWithValue(var));
        }

        if (val != null) {
            valueType.replaceAnnotation(createKeyForAnnotationMirrorWithValue(val));
        }
    }

    /* Deal with the special case where parameters were specified as
       variable names. This is a problem because those variable names are
       ambiguous and could refer to different variables at the call sites.
       Issue a warning to the user if the variable name is a plain identifier
       (with no preceding this. or classname.) */
    private void keyForIssueWarningIfArgumentValuesContainVariableName(List<Receiver> arguments, Tree t, Name methodName, MethodInvocationNode node) {

        ArrayList<String> formalParamNames = null;
        boolean formalParamNamesAreValid = true;

        for(int i = 0; i < arguments.size(); i++) {
            Receiver argument = arguments.get(i);

            List<? extends AnnotationMirror> keyForAnnos = argument.getType().getAnnotationMirrors();
            if (keyForAnnos != null) {
                for(AnnotationMirror anno : keyForAnnos) {
                    if (AnnotationUtils.areSameByClass(anno, checkerKeyForClass)) {
                        List<String> values = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
                        for (String s: values){
                            Matcher identifierMatcher = identifierPattern.matcher(s);

                            if (identifierMatcher.matches()) {
                                if (formalParamNames == null) { // Lazy initialization
                                    formalParamNames = new ArrayList<String>();
                                    ExecutableElement el = TreeUtils.elementFromUse(node.getTree());
                                    List<? extends VariableElement> varels = el.getParameters();
                                    for(VariableElement varel : varels) {
                                        String formalParamName = varel.getSimpleName().toString();

                                        // Heuristic: if the formal parameter name appears to be synthesized, and not the
                                        // original name, don't bother adding any parameter names to the list.
                                        if (formalParamName.equals("p0") || formalParamName.equals("arg0")) {
                                            formalParamNamesAreValid = false;
                                            break;
                                        }

                                        formalParamNames.add(formalParamName);
                                    }
                                }

                                int formalParamNum = -1;
                                if (formalParamNamesAreValid) {
                                    formalParamNum = formalParamNames.indexOf(s);
                                }

                                String paramNumString = Integer.toString(i + 1);

                                if (formalParamNum == -1) {
                                    checker.report(Result.warning(KEYFOR_VALUE_PARAMETER_VARIABLE_NAME, s, paramNumString, methodName), t);
                                }
                                else {
                                    String formalParamNumString = Integer.toString(formalParamNum + 1);

                                    checker.report(Result.warning(KEYFOR_VALUE_PARAMETER_VARIABLE_NAME_FORMAL_PARAM_NUM, s, paramNumString, methodName, formalParamNumString), t);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Pair<ArrayList<String>, ArrayList<String>> keyForCanonicalizeValuesForMethodInvocationNode(AnnotationMirror varType,
            AnnotationMirror valueType,
            Tree t,
            TreePath path,
            MethodInvocationNode node,
            boolean returnNullIfUnchanged) {

        /* The following code is best explained by example. Suppose we have the following:

        public static class Graph {
            private Map<String, Integer> adjList = new HashMap<String, Integer>();
            public static boolean addEdge(@KeyFor("#2.adjList") String theStr, Graph theGraph) {
                ...
            }
        }

        public static class TestClass {
            public void buildGraph(Graph myGraph, @KeyFor("#1.adjList") String myStr) {
                Graph.addEdge(myStr, myGraph);
            }
        }

        The challenge is to recognize that in the call to addEdge(myStr, myGraph), myGraph
        corresponds to theGraph formal parameter, even though one is labeled as
        parameter #1 and the other as #2.

        All we know at this point is:
        -We have a varType whose annotation is @KeyFor("#2.adjList")
        -We have a valueType whose annotation is @KeyFor("#1.adjList")
        -We are processing a method call Graph.addEdge(myStr, myGraph)

        We need to build flow expression contexts that will allow us
        to convert both annotations into @KeyFor("myGraph.adjList")
        so that we will know they are equivalent.
        */

        // Building the context for the varType is straightforward. We need it to be
        // the context of the call site (Graph.addEdge(myStr, myGraph)) so that the
        // formal parameters theStr and theGraph will be replaced with the actual
        // parameters myStr and myGraph. The call to
        // canonicalizeKeyForValues(varType, flowExprContextVarType, path, t);
        // will then be able to transform "#2.adjList" into "myGraph.adjList"
        // since myGraph is the second actual parameter in the call.

        FlowExpressionContext flowExprContextVarType = FlowExpressionParseUtil.buildFlowExprContextForUse(node, this),
                flowExprContextValueType = null;

        // Building the context for the valueType is more subtle. That's because
        // at the call site of Graph.addEdge(myStr, myGraph), we no longer have
        // any notion of what parameter #1 refers to. That information is found
        // at the declaration of the enclosing method.

        MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);

        if (enclosingMethod != null) {

            // An important piece of information when creating the Flow Context
            // is the receiver. If the enclosing method is static, we need the
            // receiver to be the class name (e.g. Graph). Otherwise we need
            // the receiver to be the instance of the class (e.g. someGraph,
            // if the call were someGraph.myMethod(...)

            // To be able to generate the receiver, we need the enclosing class.

            ClassTree enclosingClass = TreeUtils.enclosingClass(path);

            Node receiver = null;
            if (enclosingMethod.getModifiers().getFlags().contains(Modifier.STATIC)) {
                receiver = new ClassNameNode(enclosingClass);
            }
            else {
                receiver = new ImplicitThisLiteralNode(InternalUtils.typeOf(enclosingClass));
            }

            Receiver internalReceiver = FlowExpressions.internalReprOf(this, receiver);

            // Now we need to translate the method parameters. #1.adjList needs to
            // become myGraph.adjList. We do not do that translation here, as that
            // is handled by the call to canonicalizeKeyForValues(valueType, ...) below.
            // However, we indicate that the actual parameters are [myGraph, myStr]
            // so that canonicalizeKeyForValues can translate #1 to myGraph.

            List<Receiver> internalArguments = new ArrayList<>();

            // Note that we are not handling varargs as we assume that parameter numbers such as "#2" cannot refer to a vararg expanded argument.

            for (VariableTree vt : enclosingMethod.getParameters()) {
                internalArguments.add(FlowExpressions.internalReprOf(this,
                        new LocalVariableNode(vt, receiver)));
            }

            // Create the Flow Expression context in terms of the receiver and parameters.

            flowExprContextValueType = new FlowExpressionContext(internalReceiver, internalArguments, this);

            keyForIssueWarningIfArgumentValuesContainVariableName(flowExprContextValueType.arguments, t, enclosingMethod.getName(), node);
        }
        else {

            // If there is no enclosing method, then we are probably dealing with a field initializer.
            // In that case, we do not need to worry about transforming parameter numbers such as #1
            // since they are meaningless in this context. Create the usual Flow Expression context
            // as the context of the call site.

            flowExprContextValueType = FlowExpressionParseUtil.buildFlowExprContextForUse(node, this);
        }

        // If they are local variable names, they are already canonicalized. So we only need to canonicalize
        // the names of static and instance fields.

        ArrayList<String> var = canonicalizeKeyForValues(varType, flowExprContextVarType, path, t, returnNullIfUnchanged);
        ArrayList<String> val = canonicalizeKeyForValues(valueType, flowExprContextValueType, path, t, returnNullIfUnchanged);

        return Pair.of(var, val);
    }

    public boolean keyForValuesSubtypeCheck(AnnotationMirror varType,
            AnnotationMirror valueType,
            Tree t,
            MethodInvocationNode node
            ) {
        TreePath path = getPath(t);

        Pair<ArrayList<String>, ArrayList<String>> valuesPair = keyForCanonicalizeValuesForMethodInvocationNode(varType, valueType, t, path, node, false);

        ArrayList<String> var = valuesPair.first;
        ArrayList<String> val = valuesPair.second;

        if (var == null && val == null) {
            return true;
        }
        else if (var == null || val == null) {
            return false;
        }

        return val.containsAll(var) && var.containsAll(var); // This condition will be relaxed later.
    }

    public void keyForCanonicalizeValues(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, TreePath path) {

        Tree t = path.getLeaf();

        Node node = getNodeForTree(t);

        if (node != null) {
            if (node instanceof MethodInvocationNode) {
                keyForCanonicalizeValuesForMethodInvocationNode(varType, valueType, t, path, (MethodInvocationNode) node);
            }
            else {
                Receiver r = FlowExpressions.internalReprOf(this, node);

                FlowExpressionContext flowExprContext = new FlowExpressionContext(r, null, this);

                canonicalizeKeyForValues(varType, flowExprContext, path, t);
                canonicalizeKeyForValues(valueType, flowExprContext, path, t);
            }
        }
    }

    /* TODO: doc
     * This pattern and the logic how to use it is copied from NullnessFlow.
     * NullnessFlow already contains four exact copies of the logic for handling this
     * pattern and should really be refactored.
     */
    private static final Pattern parameterPtn = Pattern.compile("#(\\d+)");

    // TODO: copied from NullnessFlow, but without the "." at the end.
    private String receiver(MethodInvocationTree node) {
      ExpressionTree sel = node.getMethodSelect();
      if (sel.getKind() == Tree.Kind.IDENTIFIER)
        return "";
      else if (sel.getKind() == Tree.Kind.MEMBER_SELECT)
        return ((MemberSelectTree)sel).getExpression().toString();
      ErrorReporter.errorAbort("NullnessAnnotatedTypeFactory.receiver: cannot be here");
      return null; // dead code
    }

    // TODO: doc
    // TODO: "this" should be implicitly prepended
    // TODO: substitutions also need to be applied to argument types
    private AnnotatedTypeMirror keyForSubstituteCall(MethodInvocationTree call, AnnotatedTypeMirror inType) {

      // System.out.println("input type: " + inType);
      AnnotatedTypeMirror outType = inType.getCopy(true);

      AnnotationMirror anno = inType.getAnnotation(KeyFor.class);
      if (anno != null) {

        List<String> inMaps = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
        List<String> outMaps = new ArrayList<String>();

        String receiver = receiver(call);

        for (String inMapName : inMaps) {
          if (parameterPtn.matcher(inMapName).matches()) {
            int param = Integer.valueOf(inMapName.substring(1));
            if (param <= 0 || param > call.getArguments().size()) {
              // The failure should already have been reported, when the
              // method declaration was processed.
              // checker.report(Result.failure("param.index.nullness.parse.error", inMapName), call);
            } else {
              String res = call.getArguments().get(param-1).toString();
              outMaps.add(res);
            }
          } else if (inMapName.equals("this")) {
            outMaps.add(receiver);
          } else {
            // TODO: look at the code below, copied from NullnessFlow
            // System.out.println("KeyFor argument unhandled: " + inMapName + " using " + receiver + "." + inMapName);
            // do not always add the receiver, e.g. for local variables this creates a mess
            // outMaps.add(receiver + "." + inMapName);
            // just copy name for now, better than doing nothing
            outMaps.add(inMapName);
          }
          // TODO: look at code in NullnessFlow and decide whether there
          // are more cases to copy.
        }

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, KeyFor.class);
        builder.setValue("value", outMaps);
        AnnotationMirror newAnno =  builder.build();

        outType.removeAnnotation(KeyFor.class);
        outType.addAnnotation(newAnno);
      }

      if (outType.getKind() == TypeKind.DECLARED) {
        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) outType;
        Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

        // Get the substituted type arguments
        for (AnnotatedTypeMirror typeArgument : declaredType.getTypeArguments()) {
          AnnotatedTypeMirror substTypeArgument = keyForSubstituteCall(call, typeArgument);
          mapping.put(typeArgument, substTypeArgument);
        }

        outType = declaredType.substitute(mapping);
      } else if (outType.getKind() == TypeKind.ARRAY) {
        AnnotatedArrayType  arrayType = (AnnotatedArrayType) outType;

        // Get the substituted component type
        AnnotatedTypeMirror elemType = arrayType.getComponentType();
        AnnotatedTypeMirror substElemType = keyForSubstituteCall(call, elemType);

        arrayType.setComponentType(substElemType);
        // outType aliases arrayType
      } else if(outType.getKind().isPrimitive() ||
                outType.getKind() == TypeKind.WILDCARD ||
                outType.getKind() == TypeKind.TYPEVAR) {
        // TODO: for which of these should we also recursively substitute?
        // System.out.println("KeyForATF: Intentionally unhandled Kind: " + outType.getKind());
      } else {
        // System.err.println("KeyForATF: Unknown getKind(): " + outType.getKind());
        // assert false;
      }

      // System.out.println("result type: " + outType);
      return outType;
    }

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new KeyForTypeHierarchy(checker, getQualifierHierarchy());
    }

    private class KeyForTypeHierarchy extends TypeHierarchy {

        public KeyForTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy) {
            super(checker, qualifierHierarchy);
        }

        @Override
        public final boolean isSubtype(AnnotatedTypeMirror rhs, AnnotatedTypeMirror lhs) {

            if (lhs.getAnnotation(KeyFor.class) != null) {
                if (lhs.getKind() == TypeKind.TYPEVAR &&
                        rhs.getKind() == TypeKind.TYPEVAR) {
                    // TODO: Investigate whether there is a nicer and more proper way to
                    // get assignments between two type variables working.
                    if (lhs.getAnnotations().isEmpty()) {
                        return true;
                    }
                }
                // Otherwise Covariant would cause trouble.
                if (rhs.hasAnnotation(KeyForBottom.class)) {
                    return true;
                }
            }

            return super.isSubtype(rhs, lhs);
        }

        @Override
        protected boolean isSubtypeTypeArguments(AnnotatedDeclaredType rhs, AnnotatedDeclaredType lhs) {
            if (lhs.getAnnotation(KeyFor.class) != null) {

                if (ignoreRawTypeArguments(rhs, lhs)) {
                    return true;
                }

                List<AnnotatedTypeMirror> rhsTypeArgs = rhs.getTypeArguments();
                List<AnnotatedTypeMirror> lhsTypeArgs = lhs.getTypeArguments();

                if (rhsTypeArgs.isEmpty() || lhsTypeArgs.isEmpty())
                    return true;

                TypeElement lhsElem = (TypeElement) lhs.getUnderlyingType().asElement();
                // TypeElement rhsElem = (TypeElement) lhs.getUnderlyingType().asElement();
                // the following would be needed if Covariant were per type parameter
                // AnnotatedDeclaredType lhsDecl = currentATF.fromElement(lhsElem);
                // AnnotatedDeclaredType rhsDecl = currentATF.fromElement(rhsElem);
                // List<AnnotatedTypeMirror> lhsTVs = lhsDecl.getTypeArguments();
                // List<AnnotatedTypeMirror> rhsTVs = rhsDecl.getTypeArguments();

                // TODO: implementation of @Covariant should be done in the standard TypeHierarchy
                int[] covarVals = null;
                if (lhsElem.getAnnotation(Covariant.class) != null) {
                    covarVals = lhsElem.getAnnotation(Covariant.class).value();
                }


                if (lhsTypeArgs.size() != rhsTypeArgs.size()) {
                    // This test fails e.g. for casts from a type with one type
                    // argument to a type with two type arguments.
                    // See test case nullness/generics/GenericsCasts
                    // TODO: shouldn't the type be brought to a common type before
                    // this?
                    return true;
                }

                for (int i = 0; i < lhsTypeArgs.size(); ++i) {
                    boolean covar = false;
                    if (covarVals != null) {
                        for (int cvv = 0; cvv < covarVals.length; ++cvv) {
                            if (covarVals[cvv] == i) {
                                covar = true;
                            }
                        }
                    }

                    if (covar) {
                        if (!isSubtype(rhsTypeArgs.get(i), lhsTypeArgs.get(i)))
                            // TODO: still check whether isSubtypeAsTypeArgument returns true.
                            // This handles wildcards better.
                            return isSubtypeAsTypeArgument(rhsTypeArgs.get(i), lhsTypeArgs.get(i));
                    } else {
                        if (!isSubtypeAsTypeArgument(rhsTypeArgs.get(i), lhsTypeArgs.get(i)))
                            return false;
                    }
                }

                return true;
            }

            return super.isSubtypeTypeArguments(rhs, lhs);
        }
    }

}
