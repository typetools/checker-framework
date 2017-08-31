package org.checkerframework.common.basetype;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import static org.checkerframework.framework.util.AnnotatedTypes.getArrayDepth;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.BooleanLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.util.PurityChecker;
import org.checkerframework.dataflow.util.PurityChecker.PurityResult;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.Unused;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.VisitorState;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.ContractsUtils;
import org.checkerframework.framework.util.ContractsUtils.ConditionalPostcondition;
import org.checkerframework.framework.util.ContractsUtils.Contract;
import org.checkerframework.framework.util.ContractsUtils.Postcondition;
import org.checkerframework.framework.util.ContractsUtils.Precondition;
import org.checkerframework.framework.util.FieldInvariants;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.framework.util.QualifierPolymorphism;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A {@link SourceVisitor} that performs assignment and pseudo-assignment checking, method
 * invocation checking, and assignability checking.
 *
 * <p>This implementation uses the {@link AnnotatedTypeFactory} implementation provided by an
 * associated {@link BaseTypeChecker}; its visitor methods will invoke this factory on parts of the
 * AST to determine the "annotated type" of an expression. Then, the visitor methods will check the
 * types in assignments and pseudo-assignments using {@link #commonAssignmentCheck}, which
 * ultimately calls the {@link TypeHierarchy#isSubtype} method and reports errors that violate
 * Java's rules of assignment.
 *
 * <p>Note that since this implementation only performs assignment and pseudo-assignment checking,
 * other rules for custom type systems must be added in subclasses (e.g., dereference checking in
 * the {@link org.checkerframework.checker.nullness.NullnessChecker} is implemented in the {@link
 * org.checkerframework.checker.nullness.NullnessChecker}'s {@link TreeScanner#visitMemberSelect}
 * method).
 *
 * <p>This implementation does the following checks:
 *
 * <ol>
 *   <li><b>Assignment and Pseudo-Assignment Check</b>: It verifies that any assignment type-checks,
 *       using {@code TypeHierarchy.isSubtype} method. This includes method invocation and method
 *       overriding checks.
 *   <li><b>Type Validity Check</b>: It verifies that any user-supplied type is a valid type, using
 *       {@code isValidUse} method.
 *   <li><b>(Re-)Assignability Check</b>: It verifies that any assignment is valid, using {@code
 *       Checker.isAssignable} method.
 * </ol>
 *
 * @see "JLS $4"
 * @see TypeHierarchy#isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)
 * @see AnnotatedTypeFactory
 */
/*
 * Note how the handling of VisitorState is duplicated in AbstractFlow. In
 * particular, the handling of the assignment context has to be done correctly
 * in both classes. This is a pain and we should see how to handle this in the
 * DFF version.
 *
 * TODO: missing assignment context: array initializer
 * expressions should have the component type as context
 */
public class BaseTypeVisitor<Factory extends GenericAnnotatedTypeFactory<?, ?, ?, ?>>
        extends SourceVisitor<Void, Void> {

    /** The {@link BaseTypeChecker} for error reporting. */
    protected final BaseTypeChecker checker;

    /** The factory to use for obtaining "parsed" version of annotations. */
    protected final Factory atypeFactory;

    /** For obtaining line numbers in -Ashowchecks debugging output. */
    protected final SourcePositions positions;

    /** For storing visitor state. */
    protected final VisitorState visitorState;

    /** An instance of the {@link ContractsUtils} helper class. */
    protected final ContractsUtils contractsUtils;

    /**
     * @param checker the type-checker associated with this visitor (for callbacks to {@link
     *     TypeHierarchy#isSubtype})
     */
    public BaseTypeVisitor(BaseTypeChecker checker) {
        super(checker);

        this.checker = checker;
        this.atypeFactory = createTypeFactory();
        this.contractsUtils = ContractsUtils.getInstance(atypeFactory);
        this.positions = trees.getSourcePositions();
        this.visitorState = atypeFactory.getVisitorState();
        this.typeValidator = createTypeValidator();
        this.vectorType = atypeFactory.fromElement(elements.getTypeElement("java.util.Vector"));
    }

    protected BaseTypeVisitor(BaseTypeChecker checker, Factory typeFactory) {
        super(checker);

        this.checker = checker;
        this.atypeFactory = typeFactory;
        this.contractsUtils = ContractsUtils.getInstance(atypeFactory);
        this.positions = trees.getSourcePositions();
        this.visitorState = atypeFactory.getVisitorState();
        this.typeValidator = createTypeValidator();
        this.vectorType = atypeFactory.fromElement(elements.getTypeElement("java.util.Vector"));
    }

    /**
     * Constructs an instance of the appropriate type factory for the implemented type system.
     *
     * <p>The default implementation uses the checker naming convention to create the appropriate
     * type factory. If no factory is found, it returns {@link BaseAnnotatedTypeFactory}. It
     * reflectively invokes the constructor that accepts this checker and compilation unit tree (in
     * that order) as arguments.
     *
     * <p>Subclasses have to override this method to create the appropriate visitor if they do not
     * follow the checker naming convention.
     *
     * @return the appropriate type factory
     */
    @SuppressWarnings("unchecked") // unchecked cast to type variable
    protected Factory createTypeFactory() {
        // Try to reflectively load the type factory.
        Class<?> checkerClass = checker.getClass();
        while (checkerClass != BaseTypeChecker.class) {
            final String classToLoad =
                    checkerClass
                            .getName()
                            .replace("Checker", "AnnotatedTypeFactory")
                            .replace("Subchecker", "AnnotatedTypeFactory");

            AnnotatedTypeFactory result =
                    BaseTypeChecker.invokeConstructorFor(
                            classToLoad,
                            new Class<?>[] {BaseTypeChecker.class},
                            new Object[] {checker});
            if (result != null) {
                return (Factory) result;
            }
            checkerClass = checkerClass.getSuperclass();
        }
        return (Factory) new BaseAnnotatedTypeFactory(checker);
    }

    public final Factory getTypeFactory() {
        return atypeFactory;
    }

    // **********************************************************************
    // Responsible for updating the factory for the location (for performance)
    // **********************************************************************

    @Override
    public void setRoot(CompilationUnitTree root) {
        atypeFactory.setRoot(root);
        super.setRoot(root);
    }

    @Override
    public Void scan(Tree tree, Void p) {
        if (tree != null && getCurrentPath() != null) {
            this.visitorState.setPath(new TreePath(getCurrentPath(), tree));
        }
        return super.scan(tree, p);
    }

    /**
     * Type-check classTree and skips classes specified by the skipDef option. Subclasses should
     * override {@link #processClassTree(ClassTree)} instead of this method.
     *
     * @param classTree class to check
     * @param p null
     * @return null
     */
    @Override
    public final Void visitClass(ClassTree classTree, Void p) {
        if (checker.shouldSkipDefs(classTree)) {
            // Not "return super.visitClass(classTree, p);" because that would
            // recursively call visitors on subtrees; we want to skip the
            // class entirely.
            return null;
        }
        atypeFactory.preProcessClassTree(classTree);

        AnnotatedDeclaredType preACT = visitorState.getClassType();
        ClassTree preCT = visitorState.getClassTree();
        AnnotatedDeclaredType preAMT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();
        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
        visitorState.setClassType(atypeFactory.getAnnotatedType(classTree));
        visitorState.setClassTree(classTree);
        visitorState.setMethodReceiver(null);
        visitorState.setMethodTree(null);
        visitorState.setAssignmentContext(null);
        try {
            processClassTree(classTree);
            atypeFactory.postProcessClassTree(classTree);
        } finally {
            this.visitorState.setClassType(preACT);
            this.visitorState.setClassTree(preCT);
            this.visitorState.setMethodReceiver(preAMT);
            this.visitorState.setMethodTree(preMT);
            this.visitorState.setAssignmentContext(preAssCtxt);
        }
        return null;
    }

    /**
     * Type-check classTree. Subclasses should override this method instead of {@link
     * #visitClass(ClassTree, Void)}.
     *
     * @param classTree class to check
     */
    public void processClassTree(ClassTree classTree) {
        checkFieldInvariantDeclarations(classTree);
        if (!TreeUtils.hasExplicitConstructor(classTree)) {
            checkDefaultConstructor(classTree);
        }

        /* Visit the extends and implements clauses.
         * The superclass also visits them, but only calls visitParameterizedType, which
         * loses a main modifier.
         */
        Tree ext = classTree.getExtendsClause();
        if (ext != null) {
            validateTypeOf(ext);
        }

        List<? extends Tree> impls = classTree.getImplementsClause();
        if (impls != null) {
            for (Tree im : impls) {
                validateTypeOf(im);
            }
        }
        super.visitClass(classTree, null);
    }

    /**
     * Check that the field invariant declaration annotations meet the following requirements:
     *
     * <ol>
     *   <!-- The item numbering is referred to in the body of the method.-->
     *   <li value="1">If the superclass of {@code classTree} has a field invariant, then the field
     *       invariant for {@code classTree} must include all the fields in the superclass invariant
     *       and those fields' annotations must be a subtype (or equal) to the annotations for those
     *       fields in the superclass.
     *   <li value="2">The fields in the invariant must be a.) final and b.) declared in a
     *       superclass of {@code classTree}.
     *   <li value="3">The qualifier for each field must be a subtype of the annotation on the
     *       declaration of that field.
     *   <li value="4">The field invariant has an equal number of fields and qualifiers, or it has
     *       one qualifier and at least one field.
     * </ol>
     *
     * @param classTree class that might have a field invariant
     * @checker_framework.manual #field-invariants Field invariants
     */
    protected void checkFieldInvariantDeclarations(ClassTree classTree) {
        TypeElement elt = TreeUtils.elementFromDeclaration(classTree);
        FieldInvariants invariants = atypeFactory.getFieldInvariants(elt);
        if (invariants == null) {
            // No invariants to check
            return;
        }

        // Where to issue an error, if any.
        Tree errorTree =
                atypeFactory.getFieldInvariantAnnotationTree(
                        classTree.getModifiers().getAnnotations());
        if (errorTree == null) {
            // If the annotation was inherited, then there is no annotation tree, so issue the
            // error on the class.
            errorTree = classTree;
        }

        // Checks #4 (see method Javadoc)
        if (!invariants.isWellFormed()) {
            checker.report(Result.failure("field.invariant.not.wellformed"), errorTree);
            return;
        }

        TypeMirror superClass = elt.getSuperclass();
        List<String> fieldsNotFound = new ArrayList<>(invariants.getFields());
        Set<VariableElement> fieldElts =
                ElementUtils.findFieldsInTypeOrSuperType(superClass, fieldsNotFound);

        // Checks that fields are declared in super class. (#2b)
        if (!fieldsNotFound.isEmpty()) {
            String notFoundString = PluginUtil.join(", ", fieldsNotFound);
            checker.report(Result.failure("field.invariant.not.found", notFoundString), errorTree);
        }

        FieldInvariants superInvar =
                atypeFactory.getFieldInvariants(InternalUtils.getTypeElement(superClass));
        if (superInvar != null) {
            // Checks #3 (see method Javadoc)
            Result superError = invariants.isSuperInvariant(superInvar, atypeFactory);
            if (superError != null) {
                checker.report(superError, errorTree);
            }
        }

        List<String> notFinal = new ArrayList<>();
        for (VariableElement field : fieldElts) {
            String fieldName = field.getSimpleName().toString();
            if (!ElementUtils.isFinal(field)) {
                notFinal.add(fieldName);
            }
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(field);

            List<AnnotationMirror> annos = invariants.getQualifiersFor(field.getSimpleName());
            for (AnnotationMirror invariantAnno : annos) {
                AnnotationMirror declaredAnno =
                        type.getEffectiveAnnotationInHierarchy(invariantAnno);
                if (declaredAnno == null) {
                    // invariant anno isn't in this hierarchy
                    continue;
                }

                if (!atypeFactory.getQualifierHierarchy().isSubtype(invariantAnno, declaredAnno)) {
                    // Checks #3
                    checker.report(
                            Result.failure(
                                    "field.invariant.not.subtype",
                                    fieldName,
                                    invariantAnno,
                                    declaredAnno),
                            errorTree);
                }
            }
        }

        // Checks #2a
        if (!notFinal.isEmpty()) {
            String notFinalString = PluginUtil.join(", ", notFinal);
            checker.report(Result.failure("field.invariant.not.final", notFinalString), errorTree);
        }
    }

    protected void checkDefaultConstructor(ClassTree node) {}

    /**
     * Performs pseudo-assignment check: checks that the method obeys override and subtype rules to
     * all overridden methods.
     *
     * <p>The override rule specifies that a method, m1, may override a method m2 only if:
     *
     * <ul>
     *   <li>m1 return type is a subtype of m2
     *   <li>m1 receiver type is a supertype of m2
     *   <li>m1 parameters are supertypes of corresponding m2 parameters
     * </ul>
     *
     * Also, it issues a "missing.this" error for static method annotated receivers.
     */
    @Override
    public Void visitMethod(MethodTree node, Void p) {

        // We copy the result from getAnnotatedType to ensure that
        // circular types (e.g. K extends Comparable<K>) are represented
        // by circular AnnotatedTypeMirrors, which avoids problems with
        // later checks.
        // TODO: Find a cleaner way to ensure circular AnnotatedTypeMirrors.
        AnnotatedExecutableType methodType = atypeFactory.getAnnotatedType(node).deepCopy();
        AnnotatedDeclaredType preMRT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();
        visitorState.setMethodReceiver(methodType.getReceiverType());
        visitorState.setMethodTree(node);
        ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);

        try {
            if (InternalUtils.isAnonymousConstructor(node)) {
                // We shouldn't dig deeper
                return null;
            }

            // check method purity if needed
            {
                boolean anyPurityAnnotation = PurityUtils.hasPurityAnnotation(atypeFactory, node);
                boolean checkPurityAlways = checker.hasOption("suggestPureMethods");
                boolean checkPurityAnnotations = checker.hasOption("checkPurityAnnotations");

                if (checkPurityAnnotations && (anyPurityAnnotation || checkPurityAlways)) {
                    // check "no" purity
                    List<Pure.Kind> kinds = PurityUtils.getPurityKinds(atypeFactory, node);
                    // @Deterministic makes no sense for a void method or constructor
                    boolean isDeterministic = kinds.contains(Pure.Kind.DETERMINISTIC);
                    if (isDeterministic) {
                        if (TreeUtils.isConstructor(node)) {
                            checker.report(
                                    Result.warning("purity.deterministic.constructor"), node);
                        } else if (InternalUtils.typeOf(node.getReturnType()).getKind()
                                == TypeKind.VOID) {
                            checker.report(
                                    Result.warning("purity.deterministic.void.method"), node);
                        }
                    }

                    // Report errors if necessary.
                    PurityResult r =
                            PurityChecker.checkPurity(
                                    node.getBody(),
                                    atypeFactory,
                                    checker.hasOption("assumeSideEffectFree"));
                    if (!r.isPure(kinds)) {
                        reportPurityErrors(r, node, kinds);
                    }

                    // Issue a warning if the method is pure, but not annotated
                    // as such (if the feature is activated).
                    if (checkPurityAlways) {
                        Collection<Pure.Kind> additionalKinds = new HashSet<>(r.getTypes());
                        additionalKinds.removeAll(kinds);
                        if (TreeUtils.isConstructor(node)) {
                            additionalKinds.remove(Pure.Kind.DETERMINISTIC);
                        }
                        if (!additionalKinds.isEmpty()) {
                            if (additionalKinds.size() == 2) {
                                checker.report(
                                        Result.warning("purity.more.pure", node.getName()), node);
                            } else if (additionalKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
                                checker.report(
                                        Result.warning(
                                                "purity.more.sideeffectfree", node.getName()),
                                        node);
                            } else if (additionalKinds.contains(Pure.Kind.DETERMINISTIC)) {
                                checker.report(
                                        Result.warning("purity.more.deterministic", node.getName()),
                                        node);
                            } else {
                                assert false : "BaseTypeVisitor reached undesirable state";
                            }
                        }
                    }
                }
            }

            // Passing the whole method/constructor validates the return type
            validateTypeOf(node);

            // Validate types in throws clauses
            for (ExpressionTree thr : node.getThrows()) {
                validateTypeOf(thr);
            }

            if (atypeFactory.getDependentTypesHelper() != null) {
                atypeFactory.getDependentTypesHelper().checkMethod(node, methodType);
            }

            AnnotatedDeclaredType enclosingType =
                    (AnnotatedDeclaredType)
                            atypeFactory.getAnnotatedType(methodElement.getEnclosingElement());

            // Find which method this overrides!
            Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods =
                    AnnotatedTypes.overriddenMethods(elements, atypeFactory, methodElement);
            for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair :
                    overriddenMethods.entrySet()) {
                AnnotatedDeclaredType overriddenType = pair.getKey();
                AnnotatedExecutableType overriddenMethod =
                        AnnotatedTypes.asMemberOf(
                                types, atypeFactory, overriddenType, pair.getValue());
                if (!checkOverride(node, enclosingType, overriddenMethod, overriddenType, p)) {
                    // Stop at the first mismatch; this makes a difference only if
                    // -Awarns is passed, in which case multiple warnings might be raised on
                    // the same method, not adding any value. See Issue 373.
                    break;
                }
            }
            return super.visitMethod(node, p);
        } finally {
            boolean abstractMethod =
                    methodElement.getModifiers().contains(Modifier.ABSTRACT)
                            || methodElement.getModifiers().contains(Modifier.NATIVE);

            // check well-formedness of pre/postcondition
            List<String> formalParamNames = new ArrayList<String>();
            for (VariableTree param : node.getParameters()) {
                formalParamNames.add(param.getName().toString());
            }
            checkContractsAtMethodDeclaration(
                    node, methodElement, formalParamNames, abstractMethod);

            visitorState.setMethodReceiver(preMRT);
            visitorState.setMethodTree(preMT);
        }
    }

    /** Reports errors found during purity checking. */
    protected void reportPurityErrors(
            PurityResult result, MethodTree node, Collection<Pure.Kind> expectedTypes) {
        assert !result.isPure(expectedTypes);
        Collection<Pure.Kind> t = EnumSet.copyOf(expectedTypes);
        t.removeAll(result.getTypes());
        if (t.contains(Pure.Kind.DETERMINISTIC) || t.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
            String msgPrefix = "purity.not.deterministic.not.sideeffectfree.";
            if (!t.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
                msgPrefix = "purity.not.deterministic.";
            } else if (!t.contains(Pure.Kind.DETERMINISTIC)) {
                msgPrefix = "purity.not.sideeffectfree.";
            }
            for (Pair<Tree, String> r : result.getNotBothReasons()) {
                @SuppressWarnings("CompilerMessages")
                /*@CompilerMessageKey*/ String msg = msgPrefix + r.second;
                checker.report(Result.failure(msg), r.first);
            }
            if (t.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
                for (Pair<Tree, String> r : result.getNotSeFreeReasons()) {
                    @SuppressWarnings("CompilerMessages")
                    /*@CompilerMessageKey*/ String msg = "purity.not.sideeffectfree." + r.second;
                    checker.report(Result.failure(msg), r.first);
                }
            }
            if (t.contains(Pure.Kind.DETERMINISTIC)) {
                for (Pair<Tree, String> r : result.getNotDetReasons()) {
                    @SuppressWarnings("CompilerMessages")
                    /*@CompilerMessageKey*/ String msg = "purity.not.deterministic." + r.second;
                    checker.report(Result.failure(msg), r.first);
                }
            }
        }
    }

    private void checkContractsAtMethodDeclaration(
            MethodTree node,
            ExecutableElement methodElement,
            List<String> formalParamNames,
            boolean abstractMethod) {
        FlowExpressionContext flowExprContext = null;
        List<Contract> contracts = contractsUtils.getContracts(methodElement);

        for (Contract contract : contracts) {
            String expression = contract.expression;
            AnnotationMirror annotation = contract.annotation;

            if (flowExprContext == null) {
                flowExprContext =
                        FlowExpressionContext.buildContextForMethodDeclaration(
                                node, getCurrentPath(), checker.getContext());
            }
            FlowExpressions.Receiver expr = null;
            try {
                expr =
                        FlowExpressionParseUtil.parse(
                                expression, flowExprContext, getCurrentPath(), false);
            } catch (FlowExpressionParseException e) {
                checker.report(e.getResult(), node);
            }
            if (expr != null && !abstractMethod) {
                switch (contract.kind) {
                    case POSTCONDTION:
                        checkPostcondition(node, annotation, expr);
                        break;
                    case CONDITIONALPOSTCONDTION:
                        checkConditionalPostcondition(
                                node,
                                annotation,
                                expr,
                                ((ConditionalPostcondition) contract).annoResult);
                        break;
                    case PRECONDITION:
                        // Preconditions are checked at method invocations, not declarations
                        break;
                }
            }

            if (formalParamNames != null && formalParamNames.contains(expression)) {
                @SuppressWarnings("CompilerMessages")
                /*@CompilerMessageKey*/ String key =
                        "contracts." + contract.kind.errorKey + ".expression.parameter.name";
                checker.report(
                        Result.warning(
                                key,
                                node.getName().toString(),
                                expression,
                                formalParamNames.indexOf(expression) + 1,
                                expression),
                        node);
            }

            checkParametersAreEffectivelyFinal(node, methodElement, expression);
        }
    }

    /**
     * Check that the parameters used in {@code stringExpr} are effectively final for method {@code
     * method}.
     */
    private void checkParametersAreEffectivelyFinal(
            MethodTree node, ExecutableElement method, String stringExpr) {
        // check that all parameters used in the expression are
        // effectively final, so that they cannot be modified
        List<Integer> parameterIndices = FlowExpressionParseUtil.parameterIndices(stringExpr);
        for (Integer idx : parameterIndices) {
            if (idx > method.getParameters().size()) {
                // If the index is too big, a parse error was issued in checkContractsAtMethodDeclaration
                continue;
            }
            VariableElement parameter = method.getParameters().get(idx - 1);
            if (!ElementUtils.isEffectivelyFinal(parameter)) {
                checker.report(
                        Result.failure("flowexpr.parameter.not.final", "#" + idx, stringExpr),
                        node);
            }
        }
    }

    /**
     * Check that the expression's type is annotated with {@code annotation} at the regular exit
     * store.
     *
     * @param methodTree declaration of the method
     * @param annotation expression's type must have this annotation
     * @param expression the expression that the postcondition concerns
     */
    protected void checkPostcondition(
            MethodTree methodTree, AnnotationMirror annotation, Receiver expression) {
        CFAbstractStore<?, ?> exitStore = atypeFactory.getRegularExitStore(methodTree);
        if (exitStore == null) {
            // if there is no regular exitStore, then the method
            // cannot reach the regular exit and there is no need to
            // check anything
        } else {
            CFAbstractValue<?> value = exitStore.getValue(expression);
            AnnotationMirror inferredAnno = null;
            if (value != null) {
                QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
                Set<AnnotationMirror> annos = value.getAnnotations();
                inferredAnno = hierarchy.findAnnotationInSameHierarchy(annos, annotation);
            }
            if (!checkContract(expression, annotation, inferredAnno, exitStore)) {
                checker.report(
                        Result.failure(
                                "contracts.postcondition.not.satisfied", expression.toString()),
                        methodTree);
            }
        }
    }

    /**
     * Check that the expression's type is annotated with {@code annotation} at every regular exit
     * that returns {@code result}
     *
     * @param node tree of method with the postcondition
     * @param annotation expression's type must have this annotation
     * @param expression the expression that the postcondition concerns
     * @param result result for which the postcondition is valid
     */
    protected void checkConditionalPostcondition(
            MethodTree node, AnnotationMirror annotation, Receiver expression, boolean result) {
        boolean booleanReturnType =
                TypesUtils.isBooleanType(InternalUtils.typeOf(node.getReturnType()));
        if (!booleanReturnType) {
            checker.report(
                    Result.failure("contracts.conditional.postcondition.invalid.returntype"), node);
            // No reason to go ahead with further checking. The
            // annotation is invalid.
            return;
        }

        for (Pair<ReturnNode, ?> pair : atypeFactory.getReturnStatementStores(node)) {
            ReturnNode returnStmt = pair.first;

            Node retValNode = returnStmt.getResult();
            Boolean retVal =
                    retValNode instanceof BooleanLiteralNode
                            ? ((BooleanLiteralNode) retValNode).getValue()
                            : null;

            TransferResult<?, ?> transferResult = (TransferResult<?, ?>) pair.second;
            if (transferResult == null) {
                // Unreachable return statements have no stores, but there is no need to check them.
                continue;
            }
            CFAbstractStore<?, ?> exitStore =
                    (CFAbstractStore<?, ?>)
                            (result
                                    ? transferResult.getThenStore()
                                    : transferResult.getElseStore());
            CFAbstractValue<?> value = exitStore.getValue(expression);

            // don't check if return statement certainly does not match 'result'. at the moment,
            // this means the result is a boolean literal
            if (!(retVal == null || retVal == result)) {
                continue;
            }
            AnnotationMirror inferredAnno = null;
            if (value != null) {
                QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
                Set<AnnotationMirror> annos = value.getAnnotations();
                inferredAnno = hierarchy.findAnnotationInSameHierarchy(annos, annotation);
            }

            if (!checkContract(expression, annotation, inferredAnno, exitStore)) {
                checker.report(
                        Result.failure(
                                "contracts.conditional.postcondition.not.satisfied",
                                expression.toString()),
                        returnStmt.getTree());
            }
        }
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree node, Void p) {
        validateTypeOf(node);
        // Check the bounds here and not with every TypeParameterTree.
        // For the latter, we only need to check annotations on the type variable itself.
        // Why isn't this covered by the super call?
        for (Tree tpb : node.getBounds()) {
            validateTypeOf(tpb);
        }
        return super.visitTypeParameter(node, p);
    }

    // **********************************************************************
    // Assignment checkers and pseudo-assignments
    // **********************************************************************

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
        visitorState.setAssignmentContext(
                Pair.of((Tree) node, atypeFactory.getAnnotatedType(node)));

        try {
            if (atypeFactory.getDependentTypesHelper() != null) {
                atypeFactory
                        .getDependentTypesHelper()
                        .checkType(visitorState.getAssignmentContext().second, node);
            }
            // If there's no assignment in this variable declaration, skip it.
            if (node.getInitializer() != null) {
                commonAssignmentCheck(node, node.getInitializer(), "assignment.type.incompatible");
            } else {
                // commonAssignmentCheck validates the type of node,
                // so only validate if commonAssignmentCheck wasn't called
                validateTypeOf(node);
            }
            return super.visitVariable(node, p);
        } finally {
            visitorState.setAssignmentContext(preAssCtxt);
        }
    }

    /**
     * Performs two checks: subtyping and assignability checks, using {@link
     * #commonAssignmentCheck(Tree, ExpressionTree, String)}.
     *
     * <p>If the subtype check fails, it issues a "assignment.type.incompatible" error.
     */
    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
        visitorState.setAssignmentContext(
                Pair.of(
                        (Tree) node.getVariable(),
                        atypeFactory.getAnnotatedType(node.getVariable())));
        try {
            commonAssignmentCheck(
                    node.getVariable(), node.getExpression(), "assignment.type.incompatible");
            return super.visitAssignment(node, p);
        } finally {
            visitorState.setAssignmentContext(preAssCtxt);
        }
    }

    /**
     * Performs a subtype check, to test whether the node expression iterable type is a subtype of
     * the variable type in the enhanced for loop.
     *
     * <p>If the subtype check fails, it issues a "enhancedfor.type.incompatible" error.
     */
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        AnnotatedTypeMirror var = atypeFactory.getAnnotatedType(node.getVariable());
        AnnotatedTypeMirror iterableType = atypeFactory.getAnnotatedType(node.getExpression());
        AnnotatedTypeMirror iteratedType =
                AnnotatedTypes.getIteratedType(
                        checker.getProcessingEnvironment(), atypeFactory, iterableType);
        boolean valid = validateTypeOf(node.getVariable());
        if (valid) {
            commonAssignmentCheck(
                    var, iteratedType, node.getExpression(), "enhancedfor.type.incompatible");
        }
        return super.visitEnhancedForLoop(node, p);
    }

    /**
     * Performs a method invocation check.
     *
     * <p>An invocation of a method, m, on the receiver, r is valid only if:
     *
     * <ul>
     *   <li>passed arguments are subtypes of corresponding m parameters
     *   <li>r is a subtype of m receiver type
     *   <li>if m is generic, passed type arguments are subtypes of m type variables
     * </ul>
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        // Skip calls to the Enum constructor (they're generated by javac and
        // hard to check), also see CFGBuilder.visitMethodInvocation.
        if (TreeUtils.elementFromUse(node) == null || TreeUtils.isEnumSuper(node)) {
            return super.visitMethodInvocation(node, p);
        }

        if (shouldSkipUses(node)) {
            return super.visitMethodInvocation(node, p);
        }

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair =
                atypeFactory.methodFromUse(node);
        AnnotatedExecutableType invokedMethod = mfuPair.first;
        List<AnnotatedTypeMirror> typeargs = mfuPair.second;

        if (!atypeFactory.ignoreUninferredTypeArguments) {
            for (AnnotatedTypeMirror typearg : typeargs) {
                if (typearg.getKind() == TypeKind.WILDCARD
                        && ((AnnotatedWildcardType) typearg).isUninferredTypeArgument()) {
                    checker.report(
                            Result.failure(
                                    "type.arguments.not.inferred",
                                    invokedMethod.getElement().getSimpleName()),
                            node);
                    break; // only issue error once per method
                }
            }
        }

        List<AnnotatedTypeParameterBounds> paramBounds = new ArrayList<>();
        for (AnnotatedTypeVariable param : invokedMethod.getTypeVariables()) {
            paramBounds.add(param.getBounds());
        }

        checkTypeArguments(node, paramBounds, typeargs, node.getTypeArguments());

        List<AnnotatedTypeMirror> params =
                AnnotatedTypes.expandVarArgs(atypeFactory, invokedMethod, node.getArguments());
        checkArguments(params, node.getArguments());
        checkVarargs(invokedMethod, node);

        if (isVectorCopyInto(invokedMethod)) {
            typeCheckVectorCopyIntoArgument(node, params);
        }

        ExecutableElement invokedMethodElement = invokedMethod.getElement();
        if (!ElementUtils.isStatic(invokedMethodElement) && !TreeUtils.isSuperCall(node)) {
            checkMethodInvocability(invokedMethod, node);
        }

        // check precondition annotations
        checkPreconditions(node, contractsUtils.getPreconditions(invokedMethodElement));

        // Do not call super, as that would observe the arguments without
        // a set assignment context.
        scan(node.getMethodSelect(), p);
        return null; // super.visitMethodInvocation(node, p);
    }

    /**
     * A helper method to check that the array type of actual varargs is a subtype of the
     * corresponding required varargs, and issues "argument.invalid" error if it's not a subtype of
     * the required one.
     *
     * <p>Note it's required that type checking for each element in varargs is executed by the
     * caller before or after calling this method.
     *
     * @see #checkArguments(List, List)
     * @param invokedMethod the method type to be invoked
     * @param tree method or constructor invocation tree
     */
    protected void checkVarargs(AnnotatedExecutableType invokedMethod, Tree tree) {
        if (!invokedMethod.isVarArgs()) {
            return;
        }

        List<AnnotatedTypeMirror> formals = invokedMethod.getParameterTypes();
        int numFormals = formals.size();
        int lastArgIndex = numFormals - 1;
        AnnotatedArrayType lastParamAnnotatedType = (AnnotatedArrayType) formals.get(lastArgIndex);

        // We will skip type checking so that we avoid duplicating error message
        // if the last argument is same depth with the depth of formal varargs
        // because type checking is already done in checkArguments.
        List<? extends ExpressionTree> args;
        switch (tree.getKind()) {
            case METHOD_INVOCATION:
                args = ((MethodInvocationTree) tree).getArguments();
                break;
            case NEW_CLASS:
                args = ((NewClassTree) tree).getArguments();
                break;
            default:
                throw new AssertionError("Unexpected kind of tree: " + tree);
        }
        if (numFormals == args.size()) {
            AnnotatedTypeMirror lastArgType =
                    atypeFactory.getAnnotatedType(args.get(args.size() - 1));
            if (lastArgType.getKind() == TypeKind.ARRAY
                    && getArrayDepth(lastParamAnnotatedType)
                            == getArrayDepth((AnnotatedArrayType) lastArgType)) {
                return;
            }
        }

        AnnotatedTypeMirror wrappedVarargsType = atypeFactory.getAnnotatedTypeVarargsArray(tree);

        // When dataflow analysis is not enabled, it will be null and we can suppose there is no
        // annotation to be checked for generated varargs array.
        if (wrappedVarargsType == null) {
            return;
        }

        // The component type of wrappedVarargsType might not be a subtype of the component type of
        // lastParamAnnotatedType due to the difference of type inference between for an expression
        // and an invoked method element. We can consider that the component type of actual is same
        // with formal one because type checking for elements will be done in checkArguments. This
        // is also needed to avoid duplicating error message caused by elements in varargs
        if (wrappedVarargsType.getKind() == TypeKind.ARRAY) {
            ((AnnotatedArrayType) wrappedVarargsType)
                    .setComponentType(lastParamAnnotatedType.getComponentType());
        }

        commonAssignmentCheck(
                lastParamAnnotatedType, wrappedVarargsType, tree, "varargs.type.incompatible");
    }

    /**
     * Checks that all the given {@code preconditions} hold true immediately prior to the method
     * invocation or variable access at {@code tree}.
     *
     * @param tree the Tree immediately prior to which the preconditions must hold true
     * @param preconditions the preconditions to be checked
     */
    protected void checkPreconditions(MethodInvocationTree tree, Set<Precondition> preconditions) {
        // This check is needed for the GUI effects and Units Checkers tests to pass.
        // TODO: Remove this check and investigate the root cause.
        if (preconditions.isEmpty()) {
            return;
        }

        Node node = atypeFactory.getNodeForTree(tree);

        FlowExpressionContext flowExprContext =
                FlowExpressionContext.buildContextForMethodUse(
                        (MethodInvocationNode) node, checker.getContext());

        if (flowExprContext == null) {
            checker.report(Result.failure("flowexpr.parse.context.not.determined", node), tree);
            return;
        }

        for (Precondition p : preconditions) {
            String expression = p.expression;
            AnnotationMirror anno = p.annotation;

            try {
                FlowExpressions.Receiver expr =
                        FlowExpressionParseUtil.parse(
                                expression, flowExprContext, getCurrentPath(), false);

                CFAbstractStore<?, ?> store = atypeFactory.getStoreBefore(node);

                CFAbstractValue<?> value = store.getValue(expr);

                AnnotationMirror inferredAnno = null;
                if (value != null) {
                    QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
                    Set<AnnotationMirror> annos = value.getAnnotations();
                    inferredAnno = hierarchy.findAnnotationInSameHierarchy(annos, anno);
                }
                if (!checkContract(expr, anno, inferredAnno, store)) {
                    checker.report(
                            Result.failure(
                                    "contracts.precondition.not.satisfied",
                                    tree.toString(),
                                    expr == null ? expression : expr.toString()),
                            tree);
                }
            } catch (FlowExpressionParseException e) {
                // report errors here
                checker.report(e.getResult(), tree);
            }
        }
    }

    /**
     * Returns true if and only if {@code inferredAnnotation} is valid for a given expression to
     * match the {@code necessaryAnnotation}.
     *
     * <p>By default, {@code inferredAnnotation} must be a subtype of {@code necessaryAnnotation},
     * but subclasses might override this behavior.
     */
    protected boolean checkContract(
            Receiver expr,
            AnnotationMirror necessaryAnnotation,
            AnnotationMirror inferredAnnotation,
            CFAbstractStore<?, ?> store) {
        return inferredAnnotation != null
                && atypeFactory
                        .getQualifierHierarchy()
                        .isSubtype(inferredAnnotation, necessaryAnnotation);
    }

    // Handle case Vector.copyInto()
    private final AnnotatedDeclaredType vectorType;

    /** Returns true if the method symbol represents {@code Vector.copyInto} */
    protected boolean isVectorCopyInto(AnnotatedExecutableType method) {
        ExecutableElement elt = method.getElement();
        if (elt.getSimpleName().contentEquals("copyInto") && elt.getParameters().size() == 1)
            return true;

        return false;
    }

    /**
     * Type checks the method arguments of {@code Vector.copyInto()}.
     *
     * <p>The Checker Framework special-cases the method invocation, as it is type safety cannot be
     * expressed by Java's type system.
     *
     * <p>For a Vector {@code v} of type {@code Vectory<E>}, the method invocation {@code
     * v.copyInto(arr)} is type-safe iff {@code arr} is a array of type {@code T[]}, where {@code T}
     * is a subtype of {@code E}.
     *
     * <p>In other words, this method checks that the type argument of the receiver method is a
     * subtype of the component type of the passed array argument.
     *
     * @param node a method invocation of {@code Vector.copyInto()}
     * @param params the types of the parameters of {@code Vectory.copyInto()}
     */
    protected void typeCheckVectorCopyIntoArgument(
            MethodInvocationTree node, List<? extends AnnotatedTypeMirror> params) {
        assert params.size() == 1
                : "invalid no. of parameters " + params + " found for method invocation " + node;
        assert node.getArguments().size() == 1
                : "invalid no. of arguments in method invocation " + node;

        AnnotatedTypeMirror passed = atypeFactory.getAnnotatedType(node.getArguments().get(0));
        AnnotatedArrayType passedAsArray = (AnnotatedArrayType) passed;

        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(node);
        AnnotatedDeclaredType receiverAsVector =
                AnnotatedTypes.asSuper(atypeFactory, receiver, vectorType);
        if (receiverAsVector.getTypeArguments().isEmpty()) {
            return;
        }

        commonAssignmentCheck(
                passedAsArray.getComponentType(),
                receiverAsVector.getTypeArguments().get(0),
                node.getArguments().get(0),
                "vector.copyinto.type.incompatible");
    }

    /**
     * Performs a new class invocation check.
     *
     * <p>An invocation of a constructor, c, is valid only if:
     *
     * <ul>
     *   <li>passed arguments are subtypes of corresponding c parameters
     *   <li>if c is generic, passed type arguments are subtypes of c type variables
     * </ul>
     */
    @Override
    public Void visitNewClass(NewClassTree node, Void p) {
        if (checker.shouldSkipUses(InternalUtils.constructor(node))) {
            return super.visitNewClass(node, p);
        }

        Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> fromUse =
                atypeFactory.constructorFromUse(node);
        AnnotatedExecutableType constructor = fromUse.first;
        List<AnnotatedTypeMirror> typeargs = fromUse.second;

        List<? extends ExpressionTree> passedArguments = node.getArguments();
        List<AnnotatedTypeMirror> params =
                AnnotatedTypes.expandVarArgs(atypeFactory, constructor, passedArguments);

        checkArguments(params, passedArguments);
        checkVarargs(constructor, node);

        List<AnnotatedTypeParameterBounds> paramBounds = new ArrayList<>();
        for (AnnotatedTypeVariable param : constructor.getTypeVariables()) {
            paramBounds.add(param.getBounds());
        }

        checkTypeArguments(node, paramBounds, typeargs, node.getTypeArguments());

        boolean valid = validateTypeOf(node);

        if (valid) {
            AnnotatedDeclaredType dt = atypeFactory.getAnnotatedType(node);
            if (atypeFactory.getDependentTypesHelper() != null) {
                atypeFactory.getDependentTypesHelper().checkType(dt, node);
            }
            checkConstructorInvocation(dt, constructor, node);
        }
        // Do not call super, as that would observe the arguments without
        // a set assignment context.
        scan(node.getEnclosingExpression(), p);
        scan(node.getIdentifier(), p);
        scan(node.getClassBody(), p);

        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {

        Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result =
                atypeFactory.getFnInterfaceFromTree(node);
        AnnotatedExecutableType overridden = result.second;

        if (node.getBody().getKind() != Tree.Kind.BLOCK) {
            // Check return type for single statement returns here
            AnnotatedTypeMirror ret = overridden.getReturnType();
            if (ret.getKind() != TypeKind.VOID) {
                visitorState.setAssignmentContext(Pair.of((Tree) node, ret));
                commonAssignmentCheck(
                        ret, (ExpressionTree) node.getBody(), "return.type.incompatible");
            }
        }

        // Check parameters
        for (int i = 0; i < overridden.getParameterTypes().size(); ++i) {
            AnnotatedTypeMirror overridingParm =
                    atypeFactory.getAnnotatedType(node.getParameters().get(i));
            commonAssignmentCheck(
                    overridingParm,
                    overridden.getParameterTypes().get(i),
                    node.getParameters().get(i),
                    "lambda.param.type.incompatible");
        }

        // TODO: Post conditions?

        return super.visitLambdaExpression(node, p);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree node, Void p) {
        this.checkMethodReferenceAsOverride(node, p);
        return super.visitMemberReference(node, p);
    }

    /**
     * Checks that the type of the return expression is a subtype of the enclosing method required
     * return type. If not, it issues a "return.type.incompatible" error.
     */
    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        // Don't try to check return expressions for void methods.
        if (node.getExpression() == null) {
            return super.visitReturn(node, p);
        }

        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
        try {

            Tree enclosing =
                    TreeUtils.enclosingOfKind(
                            getCurrentPath(),
                            new HashSet<Tree.Kind>(
                                    Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)));

            AnnotatedTypeMirror ret = null;
            if (enclosing.getKind() == Tree.Kind.METHOD) {

                MethodTree enclosingMethod = TreeUtils.enclosingMethod(getCurrentPath());
                boolean valid = validateTypeOf(enclosing);
                if (valid) {
                    ret = atypeFactory.getMethodReturnType(enclosingMethod, node);
                }
            } else {
                Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result =
                        atypeFactory.getFnInterfaceFromTree((LambdaExpressionTree) enclosing);
                ret = result.second.getReturnType();
            }

            if (ret != null) {
                visitorState.setAssignmentContext(Pair.of((Tree) node, ret));

                commonAssignmentCheck(ret, node.getExpression(), "return.type.incompatible");
            }
            return super.visitReturn(node, p);
        } finally {
            visitorState.setAssignmentContext(preAssCtxt);
        }
    }

    /* TODO: something similar to visitReturn should be done.
     * public Void visitThrow(ThrowTree node, Void p) {
     * return super.visitThrow(node, p);
     * }
     */

    /**
     * Ensure that the annotation arguments comply to their declarations. This needs some special
     * casing, as annotation arguments form special trees.
     */
    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        List<? extends ExpressionTree> args = node.getArguments();
        if (args.isEmpty()) {
            // Nothing to do if there are no annotation arguments.
            return null;
        }

        Element anno = TreeInfo.symbol((JCTree) node.getAnnotationType());
        if (anno.toString().equals(DefaultQualifier.class.getName())
                || anno.toString().equals(SuppressWarnings.class.getName())) {
            // Skip these two annotations, as we don't care about the
            // arguments to them.
            return null;
        }

        // Mapping from argument simple name to its annotated type.
        Map<String, AnnotatedTypeMirror> annoTypes = new HashMap<String, AnnotatedTypeMirror>();
        for (Element encl : ElementFilter.methodsIn(anno.getEnclosedElements())) {
            AnnotatedExecutableType exeatm =
                    (AnnotatedExecutableType) atypeFactory.getAnnotatedType(encl);
            AnnotatedTypeMirror retty = exeatm.getReturnType();
            annoTypes.put(encl.getSimpleName().toString(), retty);
        }

        for (ExpressionTree arg : args) {
            if (!(arg instanceof AssignmentTree)) {
                // TODO: when can this happen?
                continue;
            }

            AssignmentTree at = (AssignmentTree) arg;
            // Ensure that we never ask for the annotated type of an annotation, because
            // we don't have a type for annotations.
            if (at.getExpression().getKind() == Tree.Kind.ANNOTATION) {
                visitAnnotation((AnnotationTree) at.getExpression(), p);
                continue;
            }
            if (at.getExpression().getKind() == Tree.Kind.NEW_ARRAY) {
                NewArrayTree nat = (NewArrayTree) at.getExpression();
                boolean isAnno = false;
                for (ExpressionTree init : nat.getInitializers()) {
                    if (init.getKind() == Tree.Kind.ANNOTATION) {
                        visitAnnotation((AnnotationTree) init, p);
                        isAnno = true;
                    }
                }
                if (isAnno) {
                    continue;
                }
            }

            AnnotatedTypeMirror expected = annoTypes.get(at.getVariable().toString());
            Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();

            {
                // Determine and set the new assignment context.
                ExpressionTree var = at.getVariable();
                assert var instanceof IdentifierTree
                        : "Expected IdentifierTree as context. Found: " + var;
                AnnotatedTypeMirror meth = atypeFactory.getAnnotatedType(var);
                assert meth instanceof AnnotatedExecutableType
                        : "Expected AnnotatedExecutableType as context. Found: " + meth;
                AnnotatedTypeMirror newctx = ((AnnotatedExecutableType) meth).getReturnType();
                visitorState.setAssignmentContext(
                        Pair.<Tree, AnnotatedTypeMirror>of((Tree) null, newctx));
            }

            try {
                AnnotatedTypeMirror actual = atypeFactory.getAnnotatedType(at.getExpression());
                if (expected.getKind() != TypeKind.ARRAY) {
                    // Expected is not an array -> direct comparison.
                    commonAssignmentCheck(
                            expected, actual, at.getExpression(), "annotation.type.incompatible");
                } else {
                    if (actual.getKind() == TypeKind.ARRAY) {
                        // Both actual and expected are arrays.
                        commonAssignmentCheck(
                                expected,
                                actual,
                                at.getExpression(),
                                "annotation.type.incompatible");
                    } else {
                        // The declaration is an array type, but just a single
                        // element is given.
                        commonAssignmentCheck(
                                ((AnnotatedArrayType) expected).getComponentType(),
                                actual,
                                at.getExpression(),
                                "annotation.type.incompatible");
                    }
                }
            } finally {
                visitorState.setAssignmentContext(preAssCtxt);
            }
        }
        return null;
    }

    /**
     * If the computation of the type of the ConditionalExpressionTree in
     * org.checkerframework.framework.type.TypeFromTree.TypeFromExpression.visitConditionalExpression(ConditionalExpressionTree,
     * AnnotatedTypeFactory) is correct, the following checks are redundant. However, let's add
     * another failsafe guard and do the checks.
     */
    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        AnnotatedTypeMirror cond = atypeFactory.getAnnotatedType(node);
        this.commonAssignmentCheck(cond, node.getTrueExpression(), "conditional.type.incompatible");
        this.commonAssignmentCheck(
                cond, node.getFalseExpression(), "conditional.type.incompatible");
        return super.visitConditionalExpression(node, p);
    }

    // **********************************************************************
    // Check for illegal re-assignment
    // **********************************************************************

    /**
     * Performs assignability check using {@link #checkAssignability(AnnotatedTypeMirror, Tree)}.
     */
    @Override
    public Void visitUnary(UnaryTree node, Void p) {
        if ((node.getKind() == Tree.Kind.PREFIX_DECREMENT)
                || (node.getKind() == Tree.Kind.PREFIX_INCREMENT)
                || (node.getKind() == Tree.Kind.POSTFIX_DECREMENT)
                || (node.getKind() == Tree.Kind.POSTFIX_INCREMENT)) {
            AnnotatedTypeMirror varType = atypeFactory.getAnnotatedTypeLhs(node.getExpression());
            AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedTypeRhsUnaryAssign(node);
            commonAssignmentCheck(
                    varType, valueType, node, "compound.assignment.type.incompatible");
        }
        return super.visitUnary(node, p);
    }

    /**
     * Performs assignability check using {@link #checkAssignability(AnnotatedTypeMirror, Tree)}.
     */
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        // If node is the tree representing the compounds assignment s += expr,
        // Then this method should check whether s + expr can be assigned to s,
        // but the "s + expr" tree does not exist.  So instead, check that
        // s += expr can be assigned to s.
        commonAssignmentCheck(node.getVariable(), node, "compound.assignment.type.incompatible");
        return super.visitCompoundAssignment(node, p);
    }

    // **********************************************************************
    // Check for invalid types inserted by the user
    // **********************************************************************

    @Override
    public Void visitNewArray(NewArrayTree node, Void p) {
        boolean valid = validateTypeOf(node);

        if (valid && node.getType() != null) {
            AnnotatedArrayType arrayType = atypeFactory.getAnnotatedType(node);
            if (atypeFactory.getDependentTypesHelper() != null) {
                atypeFactory.getDependentTypesHelper().checkType(arrayType, node);
            }
            if (node.getInitializers() != null) {
                checkArrayInitialization(arrayType.getComponentType(), node.getInitializers());
            }
        }

        return super.visitNewArray(node, p);
    }

    /**
     * Do not override this method! Previously, this method contained some logic, but the main
     * modifier of types was missing. It has been merged with the TypeValidator below. This method
     * doesn't need to do anything, as the type is already validated.
     */
    @Override
    public final Void visitParameterizedType(ParameterizedTypeTree node, Void p) {
        return null; // super.visitParameterizedType(node, p);
    }

    protected void checkTypecastRedundancy(TypeCastTree node, Void p) {
        if (!checker.getLintOption("cast:redundant", false)) {
            return;
        }

        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(node);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());

        if (AnnotatedTypes.areSame(castType, exprType)) {
            checker.report(Result.warning("cast.redundant", castType), node);
        }
    }

    protected void checkTypecastSafety(TypeCastTree node, Void p) {
        if (!checker.getLintOption("cast:unsafe", true)) {
            return;
        }
        AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(node);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());

        // We cannot do a simple test of casting, as isSubtypeOf requires
        // the input types to be subtypes according to Java
        if (!isTypeCastSafe(castType, exprType)) {
            checker.report(
                    Result.warning("cast.unsafe", exprType.toString(true), castType.toString(true)),
                    node);
        }
    }

    private boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType) {
        QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();

        if (castType.getKind() == TypeKind.DECLARED) {
            // eliminate false positives, where the annotations are
            // implicitly added by the declared type declaration
            AnnotatedDeclaredType castDeclared = (AnnotatedDeclaredType) castType;
            AnnotatedDeclaredType elementType =
                    atypeFactory.fromElement(
                            (TypeElement) castDeclared.getUnderlyingType().asElement());
            if (AnnotationUtils.areSame(
                    castDeclared.getAnnotations(), elementType.getAnnotations())) {
                return true;
            }
        }

        if (checker.hasOption("checkCastElementType")) {
            AnnotatedTypeMirror newCastType;
            if (castType.getKind() == TypeKind.TYPEVAR) {
                newCastType = ((AnnotatedTypeVariable) castType).getUpperBound();
            } else {
                newCastType = castType;
            }
            AnnotatedTypeMirror newExprType;
            if (exprType.getKind() == TypeKind.TYPEVAR) {
                newExprType = ((AnnotatedTypeVariable) exprType).getUpperBound();
            } else {
                newExprType = exprType;
            }

            if (!atypeFactory.getTypeHierarchy().isSubtype(newExprType, newCastType)) {
                return false;
            }
            if (newCastType.getKind() == TypeKind.ARRAY
                    && newExprType.getKind() != TypeKind.ARRAY) {
                // Always warn if the cast contains an array, but the expression
                // doesn't, as in "(Object[]) o" where o is of type Object
                return false;
            } else if (newCastType.getKind() == TypeKind.DECLARED
                    && newExprType.getKind() == TypeKind.DECLARED) {
                int castSize = ((AnnotatedDeclaredType) newCastType).getTypeArguments().size();
                int exprSize = ((AnnotatedDeclaredType) newExprType).getTypeArguments().size();

                if (castSize != exprSize) {
                    // Always warn if the cast and expression contain a different number of
                    // type arguments, e.g. to catch a cast from "Object" to "List<@NonNull Object>".
                    // TODO: the same number of arguments actually doesn't guarantee anything.
                    return false;
                }
            } else if (castType.getKind() == TypeKind.TYPEVAR
                    && exprType.getKind() == TypeKind.TYPEVAR) {
                // If both the cast type and the casted expression are type variables, then check the
                // bounds.
                Set<AnnotationMirror> lowerBoundAnnotationsCast =
                        AnnotatedTypes.findEffectiveLowerBoundAnnotations(
                                qualifierHierarchy, castType);
                Set<AnnotationMirror> lowerBoundAnnotationsExpr =
                        AnnotatedTypes.findEffectiveLowerBoundAnnotations(
                                qualifierHierarchy, exprType);
                return qualifierHierarchy.isSubtype(
                                lowerBoundAnnotationsExpr, lowerBoundAnnotationsCast)
                        && qualifierHierarchy.isSubtype(
                                exprType.getEffectiveAnnotations(),
                                castType.getEffectiveAnnotations());
            }
            Set<AnnotationMirror> castAnnos;
            if (castType.getKind() == TypeKind.TYPEVAR) {
                // If the cast type is a type var, but the expression is not, then check that the
                // type of the expression is a subtype of the lower bound.
                castAnnos =
                        AnnotatedTypes.findEffectiveLowerBoundAnnotations(
                                qualifierHierarchy, castType);
            } else {
                castAnnos = castType.getAnnotations();
            }

            return qualifierHierarchy.isSubtype(exprType.getEffectiveAnnotations(), castAnnos);
        } else {
            // checkCastElementType option wasn't specified, so only check effective annotations,
            return qualifierHierarchy.isSubtype(
                    exprType.getEffectiveAnnotations(), castType.getEffectiveAnnotations());
        }
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        // validate "node" instead of "node.getType()" to prevent duplicate errors.
        boolean valid = validateTypeOf(node) && validateTypeOf(node.getExpression());
        if (valid) {
            checkTypecastSafety(node, p);
            checkTypecastRedundancy(node, p);
        }
        if (atypeFactory.getDependentTypesHelper() != null) {
            AnnotatedTypeMirror type = atypeFactory.getAnnotatedType(node);
            atypeFactory.getDependentTypesHelper().checkType(type, node.getType());
        }
        return super.visitTypeCast(node, p);
        // return scan(node.getExpression(), p);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void p) {
        validateTypeOf(node.getType());
        return super.visitInstanceOf(node, p);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
        try {
            visitorState.setAssignmentContext(null);
            scan(node.getExpression(), p);
            scan(node.getIndex(), p);
        } finally {
            visitorState.setAssignmentContext(preAssCtxt);
        }
        return null;
    }

    /**
     * Checks the type of the exception parameter Subclasses should override
     * checkExceptionParameter(CatchTree node) rather than this method to change the behavior of
     * this check.
     */
    @Override
    public Void visitCatch(CatchTree node, Void p) {
        checkExceptionParameter(node);
        return super.visitCatch(node, p);
    }

    /**
     * Checks the type of a thrown exception. Subclasses should override
     * checkThrownExpression(ThrowTree node) rather than this method to change the behavior of this
     * check.
     */
    @Override
    public Void visitThrow(ThrowTree node, Void p) {
        checkThrownExpression(node);
        return super.visitThrow(node, p);
    }

    // **********************************************************************
    // Helper methods to provide a single overriding point
    // **********************************************************************

    /**
     * Issue error if the exception parameter is not a supertype of the annotation specified by
     * {@link #getExceptionParameterLowerBoundAnnotations()}, which is top by default.
     *
     * <p>Subclasses may override this method to change the behavior of this check. Subclasses
     * wishing to enforce that exception parameter be annotated with other annotations can just
     * override {@link #getExceptionParameterLowerBoundAnnotations()}.
     *
     * @param node CatchTree to check
     */
    protected void checkExceptionParameter(CatchTree node) {

        Set<? extends AnnotationMirror> requiredAnnotations =
                getExceptionParameterLowerBoundAnnotations();
        AnnotatedTypeMirror exPar = atypeFactory.getAnnotatedType(node.getParameter());

        for (AnnotationMirror required : requiredAnnotations) {
            AnnotationMirror found = exPar.getAnnotationInHierarchy(required);
            assert found != null;
            if (!atypeFactory.getQualifierHierarchy().isSubtype(required, found)) {
                checker.report(
                        Result.failure("exception.parameter.invalid", found, required),
                        node.getParameter());
            }

            if (exPar.getKind() == TypeKind.UNION) {
                AnnotatedUnionType aut = (AnnotatedUnionType) exPar;
                for (AnnotatedTypeMirror alterntive : aut.getAlternatives()) {
                    AnnotationMirror foundAltern = alterntive.getAnnotationInHierarchy(required);
                    if (!atypeFactory.getQualifierHierarchy().isSubtype(required, foundAltern)) {
                        checker.report(
                                Result.failure(
                                        "exception.parameter.invalid", foundAltern, required),
                                node.getParameter());
                    }
                }
            }
        }
    }

    /**
     * Returns a set of AnnotationMirrors that is a lower bound for exception parameters.
     *
     * <p>Note: by default this method is called by getThrowUpperBoundAnnotations(), so that this
     * annotation is enforced.
     *
     * <p>(Default is top)
     *
     * @return set of annotation mirrors, one per hierarchy, that from a lower bound of annotations
     *     that can be written on an exception parameter
     */
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        return atypeFactory.getQualifierHierarchy().getTopAnnotations();
    }

    /**
     * Checks the type of the thrown expression.
     *
     * <p>By default, this method checks that the thrown expression is a subtype of top.
     *
     * <p>Issue error if the thrown expression is not a sub type of the annotation given by {@link
     * #getThrowUpperBoundAnnotations()}, the same as {@link
     * #getExceptionParameterLowerBoundAnnotations()} by default.
     *
     * <p>Subclasses may override this method to change the behavior of this check. Subclasses
     * wishing to enforce that the thrown expression be a subtype of a type besides {@link
     * #getExceptionParameterLowerBoundAnnotations}, should override {@link
     * #getThrowUpperBoundAnnotations()}.
     *
     * @param node ThrowTree to check
     */
    protected void checkThrownExpression(ThrowTree node) {
        AnnotatedTypeMirror throwType = atypeFactory.getAnnotatedType(node.getExpression());
        Set<? extends AnnotationMirror> required = getThrowUpperBoundAnnotations();
        switch (throwType.getKind()) {
            case NULL:
            case DECLARED:
                Set<AnnotationMirror> found = throwType.getAnnotations();
                if (!atypeFactory.getQualifierHierarchy().isSubtype(found, required)) {
                    checker.report(
                            Result.failure("throw.type.invalid", found, required),
                            node.getExpression());
                }
                break;
            case TYPEVAR:
            case WILDCARD:
                //TODO: this code might change after the type var changes.
                Set<AnnotationMirror> foundEffective = throwType.getEffectiveAnnotations();
                if (!atypeFactory.getQualifierHierarchy().isSubtype(foundEffective, required)) {
                    checker.report(
                            Result.failure("throw.type.invalid", foundEffective, required),
                            node.getExpression());
                }
                break;
            case UNION:
                AnnotatedUnionType unionType = (AnnotatedUnionType) throwType;
                Set<AnnotationMirror> foundPrimary = unionType.getAnnotations();
                if (!atypeFactory.getQualifierHierarchy().isSubtype(foundPrimary, required)) {
                    checker.report(
                            Result.failure("throw.type.invalid", foundPrimary, required),
                            node.getExpression());
                }
                for (AnnotatedTypeMirror altern : unionType.getAlternatives()) {
                    if (!atypeFactory
                            .getQualifierHierarchy()
                            .isSubtype(altern.getAnnotations(), required)) {
                        checker.report(
                                Result.failure(
                                        "throw.type.invalid", altern.getAnnotations(), required),
                                node.getExpression());
                    }
                }
                break;
            default:
                ErrorReporter.errorAbort(
                        "Unexpected throw expression type: " + throwType.getKind());
                break;
        }
    }

    /**
     * Returns a set of AnnotationMirrors that is a upper bound for thrown exceptions.
     *
     * <p>Note: by default this method is returns by getExceptionParameterLowerBoundAnnotations(),
     * so that this annotation is enforced.
     *
     * <p>(Default is top)
     *
     * @return set of annotation mirrors, one per hierarchy, that form an upper bound of thrown
     *     expressions
     */
    protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
        return getExceptionParameterLowerBoundAnnotations();
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
     * emits an error message (through the compiler's messaging interface) if it is not valid.
     *
     * @param varTree the AST node for the lvalue (usually a variable)
     * @param valueExp the AST node for the rvalue (the new value)
     * @param errorKey the error message to use if the check fails (must be a compiler message key,
     *     see {@link org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey})
     */
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, /*@CompilerMessageKey*/ String errorKey) {
        AnnotatedTypeMirror var = atypeFactory.getAnnotatedTypeLhs(varTree);
        assert var != null : "no variable found for tree: " + varTree;

        if (!validateType(varTree, var)) {
            return;
        }

        checkAssignability(var, varTree);

        commonAssignmentCheck(var, valueExp, errorKey);
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
     * emits an error message (through the compiler's messaging interface) if it is not valid.
     *
     * @param varType the annotated type of the lvalue (usually a variable)
     * @param valueExp the AST node for the rvalue (the new value)
     * @param errorKey the error message to use if the check fails (must be a compiler message key,
     *     see {@link org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey})
     */
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueExp,
            /*@CompilerMessageKey*/ String errorKey) {
        if (shouldSkipUses(valueExp)) {
            return;
        }
        if (varType.getKind() == TypeKind.ARRAY
                && valueExp instanceof NewArrayTree
                && ((NewArrayTree) valueExp).getType() == null) {
            AnnotatedTypeMirror compType = ((AnnotatedArrayType) varType).getComponentType();
            NewArrayTree arrayTree = (NewArrayTree) valueExp;
            assert arrayTree.getInitializers() != null
                    : "array initializers are not expected to be null in: " + valueExp;
            checkArrayInitialization(compType, arrayTree.getInitializers());
        }
        if (!validateTypeOf(valueExp)) {
            return;
        }
        AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedType(valueExp);
        assert valueType != null : "null type for expression: " + valueExp;
        commonAssignmentCheck(varType, valueType, valueExp, errorKey);
    }

    /**
     * Checks the validity of an assignment (or pseudo-assignment) from a value to a variable and
     * emits an error message (through the compiler's messaging interface) if it is not valid.
     *
     * @param varType the annotated type of the variable
     * @param valueType the annotated type of the value
     * @param valueTree the location to use when reporting the error message
     * @param errorKey the error message to use if the check fails (must be a compiler message key,
     *     see {@link org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey})
     */
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            /*@CompilerMessageKey*/ String errorKey) {

        if (checker.hasOption("showchecks")) {
            long valuePos = positions.getStartPosition(root, valueTree);
            System.out.printf(
                    " %s (line %3d): %s %s%n     actual: %s %s%n   expected: %s %s%n",
                    "About to test whether actual is a subtype of expected",
                    (root.getLineMap() != null ? root.getLineMap().getLineNumber(valuePos) : -1),
                    valueTree.getKind(),
                    valueTree,
                    valueType.getKind(),
                    valueType.toString(),
                    varType.getKind(),
                    varType.toString());
        }

        boolean success = atypeFactory.getTypeHierarchy().isSubtype(valueType, varType);

        // TODO: integrate with subtype test.
        if (success) {
            for (Class<? extends Annotation> mono :
                    atypeFactory.getSupportedMonotonicTypeQualifiers()) {
                if (valueType.hasAnnotation(mono) && varType.hasAnnotation(mono)) {
                    checker.report(
                            Result.failure(
                                    "monotonic.type.incompatible",
                                    mono.getCanonicalName(),
                                    mono.getCanonicalName(),
                                    valueType.toString()),
                            valueTree);
                    return;
                }
            }
        }

        if (checker.hasOption("showchecks")) {
            long valuePos = positions.getStartPosition(root, valueTree);
            System.out.printf(
                    " %s (line %3d): %s %s%n     actual: %s %s%n   expected: %s %s%n",
                    (success
                            ? "success: actual is subtype of expected"
                            : "FAILURE: actual is not subtype of expected"),
                    (root.getLineMap() != null ? root.getLineMap().getLineNumber(valuePos) : -1),
                    valueTree.getKind(),
                    valueTree,
                    valueType.getKind(),
                    valueType.toString(),
                    varType.getKind(),
                    varType.toString());
        }

        // Use an error key only if it's overridden by a checker.
        if (!success) {
            String valueTypeString;
            String varTypeString;
            if (shouldPrintVerbose(varType, valueType)) {
                valueTypeString = valueType.toString(true);
                varTypeString = varType.toString(true);
            } else {
                valueTypeString = valueType.toString();
                varTypeString = varType.toString();
            }
            checker.report(Result.failure(errorKey, valueTypeString, varTypeString), valueTree);
        }
    }

    /**
     * Return whether or not the verbose toString should be used when printing the two annotated
     * types.
     *
     * @param atm1 the first AnnotatedTypeMirror
     * @param atm2 the second AnnotatedTypeMirror
     * @return true iff there are two annotated types (in either ATM) such that their toStrings are
     *     the same but their verbose toStrings differ
     */
    private boolean shouldPrintVerbose(AnnotatedTypeMirror atm1, AnnotatedTypeMirror atm2) {
        String atm1ToString = atm1.toString();
        String atm2ToString = atm2.toString();
        // If both types as strings are the same, use verbose toString.
        if (atm2ToString.equals(atm1ToString)
                // or if neither string contains an annotation
                || (!atm2ToString.contains("@") && !atm1ToString.contains("@"))) {
            return true;
        }

        SimpleAnnotatedTypeScanner<Boolean, Void> checkForMismatchedToStrings =
                new SimpleAnnotatedTypeScanner<Boolean, Void>() {
                    /** Maps from a type's toString to its verbose toString */
                    Map<String, String> map = new HashMap<>();

                    @Override
                    protected Boolean reduce(Boolean r1, Boolean r2) {
                        r1 = r1 == null ? false : r1;
                        r2 = r2 == null ? false : r2;
                        return r1 || r2;
                    }

                    @Override
                    protected Boolean defaultAction(AnnotatedTypeMirror type, Void avoid) {
                        if (type == null) {
                            return false;
                        }
                        String simple = type.toString();
                        String verbose = map.get(simple);
                        if (verbose == null) {
                            map.put(simple, type.toString(true));
                            return false;
                        } else {
                            return !verbose.equals(type.toString(true));
                        }
                    }
                };
        Boolean r1 = checkForMismatchedToStrings.visit(atm1);
        if (r1 != null && r1) {
            return true;
        }
        // Call reset to clear the visitor history, but not the map from Strings to types.
        checkForMismatchedToStrings.reset();
        Boolean r2 = checkForMismatchedToStrings.visit(atm2);

        // SimpleAnnotatedTypeScanner#scan returns null if it encounters a null AnnotatedTypeMirror.
        // This shouldn't happen if the atm1 and atm2 are well-formed.
        return r2 == null ? false : r2;
    }

    protected void checkArrayInitialization(
            AnnotatedTypeMirror type, List<? extends ExpressionTree> initializers) {
        // TODO: set assignment context like for method arguments?
        // Also in AbstractFlow.
        for (ExpressionTree init : initializers) {
            commonAssignmentCheck(type, init, "array.initializer.type.incompatible");
        }
    }

    /**
     * Checks that the annotations on the type arguments supplied to a type or a method invocation
     * are within the bounds of the type variables as declared, and issues the
     * "type.argument.type.incompatible" error if they are not.
     *
     * @param toptree the tree for error reporting, only used for inferred type arguments
     * @param paramBounds the bounds of the type parameters from a class or method declaration
     * @param typeargs the type arguments from the type or method invocation
     * @param typeargTrees the type arguments as trees, used for error reporting
     */
    // TODO: see updated version below that performs more well-formedness checks.
    protected void checkTypeArguments(
            Tree toptree,
            List<? extends AnnotatedTypeParameterBounds> paramBounds,
            List<? extends AnnotatedTypeMirror> typeargs,
            List<? extends Tree> typeargTrees) {

        // System.out.printf("BaseTypeVisitor.checkTypeArguments: %s, TVs: %s, TAs: %s, TATs: %s\n",
        //         toptree, paramBounds, typeargs, typeargTrees);

        // If there are no type variables, do nothing.
        if (paramBounds.isEmpty()) {
            return;
        }

        assert paramBounds.size() == typeargs.size()
                : "BaseTypeVisitor.checkTypeArguments: mismatch between type arguments: "
                        + typeargs
                        + " and type parameter bounds"
                        + paramBounds;

        Iterator<? extends AnnotatedTypeParameterBounds> boundsIter = paramBounds.iterator();
        Iterator<? extends AnnotatedTypeMirror> argIter = typeargs.iterator();

        while (boundsIter.hasNext()) {

            AnnotatedTypeParameterBounds bounds = boundsIter.next();
            AnnotatedTypeMirror typeArg = argIter.next();

            if (isIgnoredUninferredWildcard(bounds.getUpperBound())
                    || isIgnoredUninferredWildcard(typeArg)) {
                continue;
            }

            if (shouldBeCaptureConverted(typeArg, bounds)) {
                continue;
            }

            AnnotatedTypeMirror paramUpperBound = bounds.getUpperBound();
            if (typeArg.getKind() == TypeKind.WILDCARD) {
                paramUpperBound =
                        atypeFactory.widenToUpperBound(
                                paramUpperBound, (AnnotatedWildcardType) typeArg);
            }

            if (typeargTrees == null || typeargTrees.isEmpty()) {
                // The type arguments were inferred and we mark the whole method.
                // The inference fails if we provide invalid arguments,
                // therefore issue an error for the arguments.
                // I hope this is less confusing for users.
                commonAssignmentCheck(
                        paramUpperBound, typeArg, toptree, "type.argument.type.incompatible");
            } else {
                commonAssignmentCheck(
                        paramUpperBound,
                        typeArg,
                        typeargTrees.get(typeargs.indexOf(typeArg)),
                        "type.argument.type.incompatible");
            }

            if (!atypeFactory.getTypeHierarchy().isSubtype(bounds.getLowerBound(), typeArg)) {
                if (typeargTrees == null || typeargTrees.isEmpty()) {
                    // The type arguments were inferred and we mark the whole method.
                    checker.report(
                            Result.failure("type.argument.type.incompatible", typeArg, bounds),
                            toptree);
                } else {
                    checker.report(
                            Result.failure("type.argument.type.incompatible", typeArg, bounds),
                            typeargTrees.get(typeargs.indexOf(typeArg)));
                }
            }
        }
    }

    private boolean isIgnoredUninferredWildcard(AnnotatedTypeMirror type) {
        return atypeFactory.ignoreUninferredTypeArguments
                && type.getKind() == TypeKind.WILDCARD
                && ((AnnotatedWildcardType) type).isUninferredTypeArgument();
    }

    //TODO: REMOVE WHEN CAPTURE CONVERSION IS IMPLEMENTED
    //TODO: This may not occur only in places where capture conversion occurs but in those cases
    //TODO: The containment check provided by this method should be enough
    /**
     * Identifies cases that would not happen if capture conversion were implemented. These special
     * cases should be removed when capture conversion is implemented.
     */
    private boolean shouldBeCaptureConverted(
            final AnnotatedTypeMirror typeArg, final AnnotatedTypeParameterBounds bounds) {
        return typeArg.getKind() == TypeKind.WILDCARD
                && bounds.getUpperBound().getKind() == TypeKind.WILDCARD;
    }

    /* Updated version that performs more well-formedness checks.

    protected void checkTypeArguments(Tree toptree,
            List<? extends AnnotatedTypeVariable> typevars,
            List<? extends AnnotatedTypeMirror> typeargs,
            List<? extends Tree> typeargTrees) {

        // System.out.printf("BaseTypeVisitor.checkTypeArguments: %s, TVs: %s, TAs: %s, TATs: %s\n",
        //         toptree, typevars, typeargs, typeargTrees);

        // If there are no type variables, do nothing.
        if (typevars.isEmpty()) {
            return;
            }

        assert typevars.size() == typeargs.size() :
            "BaseTypeVisitor.checkTypeArguments: mismatch between type arguments: " +
            typeargs + " and type variables " + typevars;

        assert typeargTrees.isEmpty() ||
                    typeargTrees.size() == typeargs.size() :
            "BaseTypeVisitor.checkTypeArguments: mismatch between type arguments: " +
            typeargs + " and their trees " + typeargTrees;

        Iterator<? extends AnnotatedTypeVariable> varIter = typevars.iterator();
        Iterator<? extends AnnotatedTypeMirror> argIter = typeargs.iterator();
        Iterator<? extends Tree> argTreeIter = typeargTrees.iterator();

        while (varIter.hasNext()) {

            AnnotatedTypeVariable typeVar = varIter.next();
            AnnotatedTypeMirror typearg = argIter.next();

            Tree typeArgTree = null;
            if (argTreeIter.hasNext()) {
                typeArgTree = argTreeIter.next();
            }

            if (typeArgTree != null) {
                boolean valid = validateType(typeArgTree, typearg);
                if (!valid) {
                    // validateType already issued an error; check the next argument.
                    continue;
                }
            } else {
                if (!AnnotatedTypes.isValidType(atypeFactory.getQualifierHierarchy(), typearg)) {
                    continue;
                }
                typeArgTree = toptree;
            }

            if (typeVar.getUpperBound() != null) {
                if (!AnnotatedTypes.isValidType(atypeFactory.getQualifierHierarchy(), typeVar.getUpperBound())) {
                    continue;
                }

                commonAssignmentCheck(typeVar.getUpperBound(),
                        typearg, typeArgTree,
                        "type.argument.type.incompatible", false);
            }

            // Should we compare lower bounds instead of the annotations on the
            // type variables?
            if (!typeVar.getAnnotations().isEmpty()) {
                if (!typearg.getEffectiveAnnotations().equals(typeVar.getEffectiveAnnotations())) {
                    checker.report(Result.failure("type.argument.type.incompatible",
                            typearg, typeVar),
                            typeArgTree);
                }
            }

        }
    }
    */

    /**
     * Indicates whether to skip subtype checks on the receiver when checking method invocability. A
     * visitor may, for example, allow a method to be invoked even if the receivers are siblings in
     * a hierarchy, provided that some other condition (implemented by the visitor) is satisfied.
     *
     * @param node the method invocation node
     * @param methodDefinitionReceiver the ATM of the receiver of the method definition
     * @param methodCallReceiver the ATM of the receiver of the method call
     * @return whether to skip subtype checks on the receiver
     */
    protected boolean skipReceiverSubtypeCheck(
            MethodInvocationTree node,
            AnnotatedTypeMirror methodDefinitionReceiver,
            AnnotatedTypeMirror methodCallReceiver) {
        return false;
    }

    /**
     * Tests whether the method can be invoked using the receiver of the 'node' method invocation,
     * and issues a "method.invocation.invalid" if the invocation is invalid.
     *
     * <p>This implementation tests whether the receiver in the method invocation is a subtype of
     * the method receiver type. This behavior can be specialized by overriding
     * skipReceiverSubtypeCheck.
     *
     * @param method the type of the invoked method
     * @param node the method invocation node
     */
    protected void checkMethodInvocability(
            AnnotatedExecutableType method, MethodInvocationTree node) {
        if (method.getReceiverType() == null) {
            // Static methods don't have a receiver.
            return;
        }
        if (method.getElement().getKind() == ElementKind.CONSTRUCTOR) {
            // TODO: Explicit "this()" calls of constructors have an implicit passed
            // from the enclosing constructor. We must not use the self type, but
            // instead should find a way to determine the receiver of the enclosing constructor.
            // rcv = ((AnnotatedExecutableType)atypeFactory.getAnnotatedType(atypeFactory.getEnclosingMethod(node))).getReceiverType();
            return;
        }

        AnnotatedTypeMirror methodReceiver = method.getReceiverType().getErased();
        AnnotatedTypeMirror treeReceiver = methodReceiver.shallowCopy(false);
        AnnotatedTypeMirror rcv = atypeFactory.getReceiverType(node);

        treeReceiver.addAnnotations(rcv.getEffectiveAnnotations());

        if (!skipReceiverSubtypeCheck(node, methodReceiver, rcv)
                && !atypeFactory.getTypeHierarchy().isSubtype(treeReceiver, methodReceiver)) {
            checker.report(
                    Result.failure(
                            "method.invocation.invalid",
                            TreeUtils.elementFromUse(node),
                            treeReceiver.toString(),
                            methodReceiver.toString()),
                    node);
        }
    }

    protected boolean checkConstructorInvocation(
            AnnotatedDeclaredType invocation,
            AnnotatedExecutableType constructor,
            NewClassTree newClassTree) {
        AnnotatedDeclaredType returnType = (AnnotatedDeclaredType) constructor.getReturnType();
        // When an interface is used as the identifier in an anonymous class (e.g. new Comparable() {})
        // the constructor method will be Object.init() {} which has an Object return type
        // When TypeHierarchy attempts to convert it to the supertype (e.g. Comparable) it will return
        // null from asSuper and return false for the check.  Instead, copy the primary annotations
        // to the declared type and then do a subtyping check
        if (invocation.getUnderlyingType().asElement().getKind().isInterface()
                && TypesUtils.isObject(returnType.getUnderlyingType())) {
            final AnnotatedDeclaredType retAsDt = invocation.deepCopy();
            retAsDt.replaceAnnotations(returnType.getAnnotations());
            returnType = retAsDt;
        } else if (newClassTree.getClassBody() != null) {
            // An anonymous class invokes the constructor of its super class, so the underlying
            // types of invocation and returnType are not the same.  Call asSuper so they are the
            // same and the is subtype tests below work correctly
            invocation = AnnotatedTypes.asSuper(atypeFactory, invocation, returnType);
        }

        // The return type of the constructor (returnType) must be comparable to the type of the
        // constructor invocation (invocation).
        if (!(atypeFactory.getTypeHierarchy().isSubtype(invocation, returnType)
                || atypeFactory.getTypeHierarchy().isSubtype(returnType, invocation))) {
            checker.report(
                    Result.failure(
                            "constructor.invocation.invalid",
                            constructor.toString(),
                            invocation,
                            returnType),
                    newClassTree);
            return false;
        }
        return true;
        // TODO: what properties should hold for constructor receivers for
        // inner type instantiations?
    }

    /**
     * A helper method to check that each passed argument is a subtype of the corresponding required
     * argument, and issues "argument.invalid" error for each passed argument that not a subtype of
     * the required one.
     *
     * <p>Note this method requires the lists to have the same length, as it does not handle cases
     * like var args.
     *
     * @see #checkVarargs(AnnotatedExecutableType, Tree)
     * @param requiredArgs the required types
     * @param passedArgs the expressions passed to the corresponding types
     */
    protected void checkArguments(
            List<? extends AnnotatedTypeMirror> requiredArgs,
            List<? extends ExpressionTree> passedArgs) {
        assert requiredArgs.size() == passedArgs.size()
                : "mismatch between required args ("
                        + requiredArgs
                        + ") and passed args ("
                        + passedArgs
                        + ")";

        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = visitorState.getAssignmentContext();
        try {
            for (int i = 0; i < requiredArgs.size(); ++i) {
                visitorState.setAssignmentContext(
                        Pair.<Tree, AnnotatedTypeMirror>of(
                                (Tree) null, (AnnotatedTypeMirror) requiredArgs.get(i)));
                commonAssignmentCheck(
                        requiredArgs.get(i), passedArgs.get(i), "argument.type.incompatible");
                // Also descend into the argument within the correct assignment
                // context.
                scan(passedArgs.get(i), null);
            }
        } finally {
            visitorState.setAssignmentContext(preAssCtxt);
        }
    }

    /**
     * @return true if both types are type variables and outer contains inner Outer contains inner
     *     implies: {@literal inner.upperBound <: outer.upperBound outer.lowerBound <:
     *     inner.lowerBound }
     */
    protected boolean testTypevarContainment(
            final AnnotatedTypeMirror inner, final AnnotatedTypeMirror outer) {
        if (inner.getKind() == TypeKind.TYPEVAR && outer.getKind() == TypeKind.TYPEVAR) {

            final AnnotatedTypeVariable innerAtv = (AnnotatedTypeVariable) inner;
            final AnnotatedTypeVariable outerAtv = (AnnotatedTypeVariable) outer;

            if (AnnotatedTypes.areCorrespondingTypeVariables(elements, innerAtv, outerAtv)) {
                final TypeHierarchy typeHierarchy = atypeFactory.getTypeHierarchy();
                return typeHierarchy.isSubtype(innerAtv.getUpperBound(), outerAtv.getUpperBound())
                        && typeHierarchy.isSubtype(
                                outerAtv.getLowerBound(), innerAtv.getLowerBound());
            }
        }

        return false;
    }

    /**
     * Create an OverrideChecker.
     *
     * <p>This exists so that subclasses can subclass OverrideChecker and use their subclass instead
     * of using OverrideChecker itself.
     */
    protected OverrideChecker createOverrideChecker(
            Tree overriderTree,
            AnnotatedExecutableType overrider,
            AnnotatedTypeMirror overridingType,
            AnnotatedTypeMirror overridingReturnType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            AnnotatedTypeMirror overriddenReturnType) {
        return new OverrideChecker(
                overriderTree,
                overrider,
                overridingType,
                overridingReturnType,
                overridden,
                overriddenType,
                overriddenReturnType);
    }

    /**
     * Type checks that a method may override another method. Uses an OverrideChecker subclass as
     * created by createOverrideChecker().
     *
     * @param overriderTree declaration tree of overriding method
     * @param overridingType type of overriding class
     * @param overridden type of overridden method
     * @param overriddenType type of overridden class
     * @return true if the override is allowed
     */
    protected boolean checkOverride(
            MethodTree overriderTree,
            AnnotatedDeclaredType overridingType,
            AnnotatedExecutableType overridden,
            AnnotatedDeclaredType overriddenType,
            Void p) {

        // Get the type of the overriding method.
        AnnotatedExecutableType overrider = atypeFactory.getAnnotatedType(overriderTree);

        // This needs to be done before overrider.getReturnType() and overridden.getReturnType()
        if (overrider.getTypeVariables().isEmpty() && !overridden.getTypeVariables().isEmpty()) {
            overridden = overridden.getErased();
        }

        OverrideChecker overrideChecker =
                createOverrideChecker(
                        overriderTree,
                        overrider,
                        overridingType,
                        overrider.getReturnType(),
                        overridden,
                        overriddenType,
                        overridden.getReturnType());

        return overrideChecker.checkOverride();
    }

    // Only issue the methodref.inference.unimplemented message once
    private static boolean typeArgumentInferenceCheck = false;

    /**
     * Check that a method reference is allowed. Using the OverrideChecker class.
     *
     * @param memberReferenceTree the tree for the method reference
     * @return true if the method reference is allowed
     */
    protected boolean checkMethodReferenceAsOverride(
            MemberReferenceTree memberReferenceTree, Void p) {

        Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result =
                atypeFactory.getFnInterfaceFromTree(memberReferenceTree);
        AnnotatedDeclaredType overriddenType = result.first;
        AnnotatedExecutableType overriddenMethodType = result.second;

        // ========= Overriding Type =========
        // Get declared type from <expression>::method or <type use>::method
        // This doesn't get the correct type for a "MyOuter.super" based on the receiver of the enclosing method.
        // That is handled separately in method receiver check.
        // TODO: Class type argument inference
        AnnotatedTypeMirror overridingType =
                atypeFactory.getAnnotatedType(memberReferenceTree.getQualifierExpression());

        // ========= Overriding Executable =========
        // The ::method element
        ExecutableElement overridingElement =
                (ExecutableElement) InternalUtils.symbol(memberReferenceTree);
        AnnotatedExecutableType overridingMethodType =
                atypeFactory.methodFromUse(memberReferenceTree, overridingElement, overridingType)
                        .first;

        if (checkMethodReferenceInference(
                memberReferenceTree, overridingMethodType, overriddenMethodType, overridingType)) {
            // Type argument inference is required, skip check.
            // #checkMethodReferenceInference issued a warning.
            return true;
        }

        // This needs to be done before overridingMethodType.getReturnType() and overriddenMethodType.getReturnType()
        if (overridingMethodType.getTypeVariables().isEmpty()
                && !overriddenMethodType.getTypeVariables().isEmpty()) {
            overriddenMethodType = overriddenMethodType.getErased();
        }

        // Use the functional interface's parameters to resolve poly quals.
        QualifierPolymorphism poly =
                new QualifierPolymorphism(atypeFactory.getProcessingEnv(), atypeFactory);
        poly.annotate(overriddenMethodType, overridingMethodType);

        AnnotatedTypeMirror overridingReturnType;
        if (overridingElement.getKind() == ElementKind.CONSTRUCTOR) {
            if (overridingType.getKind() == TypeKind.ARRAY) {
                // Special casing for the return of array constructor
                overridingReturnType = overridingType;
            } else {
                overridingReturnType =
                        atypeFactory.getResultingTypeOfConstructorMemberReference(
                                memberReferenceTree, overridingMethodType);
            }
        } else {
            overridingReturnType = overridingMethodType.getReturnType();
        }

        AnnotatedTypeMirror overriddenReturnType = overriddenMethodType.getReturnType();
        if (overriddenReturnType.getKind() == TypeKind.VOID) {
            // If the functional interface return type is void, the overriding return
            // type doesn't matter.
            overriddenReturnType = overridingReturnType;
        }

        OverrideChecker overrideChecker =
                createOverrideChecker(
                        memberReferenceTree,
                        overridingMethodType,
                        overridingType,
                        overridingReturnType,
                        overriddenMethodType,
                        overriddenType,
                        overriddenReturnType);
        return overrideChecker.checkOverride();
    }

    /** Check if method reference type argument inference is required. Issue an error if it is. */
    private boolean checkMethodReferenceInference(
            MemberReferenceTree memberReferenceTree,
            AnnotatedExecutableType memberReferenceType,
            AnnotatedExecutableType overridden,
            AnnotatedTypeMirror overridingType) {
        // TODO: Issue #802
        // TODO: https://github.com/typetools/checker-framework/issues/802
        // TODO: Method type argument inference
        // TODO: Enable checks for method reference with inferred type arguments.
        // For now, error on mismatch of class or method type arguments.
        if (overridden.getTypeVariables().size() == 0) {
            boolean requiresInference = false;
            // The functional interface does not have any method type parameters
            if (memberReferenceType.getTypeVariables().size() > 0
                    && (memberReferenceTree.getTypeArguments() == null
                            || memberReferenceTree.getTypeArguments().size() == 0)) {
                // Method type args

                requiresInference = true;
            } else if (overridingType.getKind() == TypeKind.DECLARED
                    && ((AnnotatedDeclaredType) overridingType).getTypeArguments().size() > 0) {
                // Class type args

                if (memberReferenceTree.getQualifierExpression().getKind()
                        != Tree.Kind.PARAMETERIZED_TYPE) {
                    requiresInference = true;
                } else if (((AnnotatedDeclaredType) overridingType).getTypeArguments().size()
                        != ((ParameterizedTypeTree) memberReferenceTree.getQualifierExpression())
                                .getTypeArguments()
                                .size()) {
                    requiresInference = true;
                }
            }
            if (requiresInference) {
                if (!typeArgumentInferenceCheck) {
                    checker.report(
                            Result.warning("methodref.inference.unimplemented"),
                            memberReferenceTree);
                    typeArgumentInferenceCheck = true;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Class to perform method override and method reference checks.
     *
     * <p>Method references are checked similarly to method overrides, with the method reference
     * viewed as overriding the functional interface's method.
     *
     * <p>Checks that an overriding method's return type, parameter types, and receiver type are
     * correct with respect to the annotations on the overridden method's return type, parameter
     * types, and receiver type.
     *
     * <p>Furthermore, any contracts on the method must satisfy behavioral subtyping, that is,
     * postconditions must be at least as strong as the postcondition on the superclass, and
     * preconditions must be at most as strong as the condition on the superclass.
     *
     * <p>This method returns the result of the check, but also emits error messages as a side
     * effect.
     */
    public class OverrideChecker {
        // Strings for printing
        protected final String overriderMeth;
        protected final String overriderTyp;
        protected final String overriddenMeth;
        protected final String overriddenTyp;

        protected final Tree overriderTree;
        protected final Boolean methodReference;

        protected final AnnotatedExecutableType overrider;
        protected final AnnotatedTypeMirror overridingType;
        protected final AnnotatedExecutableType overridden;
        protected final AnnotatedDeclaredType overriddenType;
        protected final AnnotatedTypeMirror overriddenReturnType;
        protected final AnnotatedTypeMirror overridingReturnType;

        /**
         * Create an OverrideChecker.
         *
         * <p>Notice that the return types are passed in separately. This is to support some types
         * of method references where the overrider's return type is not the appropriate type to
         * check.
         *
         * @param overriderTree the AST node of the overriding method or method reference
         * @param overrider the type of the overriding method
         * @param overridingType the type enclosing the overrider method, usually an
         *     AnnotatedDeclaredType; for Method References may be something else
         * @param overridingReturnType the return type of the overriding method
         * @param overridden the type of the overridden method
         * @param overriddenType the declared type enclosing the overridden method
         * @param overriddenReturnType the return type of the overridden method
         */
        public OverrideChecker(
                Tree overriderTree,
                AnnotatedExecutableType overrider,
                AnnotatedTypeMirror overridingType,
                AnnotatedTypeMirror overridingReturnType,
                AnnotatedExecutableType overridden,
                AnnotatedDeclaredType overriddenType,
                AnnotatedTypeMirror overriddenReturnType) {

            this.overriderTree = overriderTree;
            this.overrider = overrider;
            this.overridingType = overridingType;
            this.overridden = overridden;
            this.overriddenType = overriddenType;
            this.overriddenReturnType = overriddenReturnType;
            this.overridingReturnType = overridingReturnType;

            overriderMeth = overrider.toString();
            if (overridingType.getKind() == TypeKind.DECLARED) {
                DeclaredType overriderTypeMirror =
                        ((AnnotatedDeclaredType) overridingType).getUnderlyingType();
                overriderTyp = overriderTypeMirror.asElement().toString();
            } else {
                overriderTyp = overridingType.toString();
            }
            overriddenMeth = overridden.toString();
            overriddenTyp = overriddenType.getUnderlyingType().asElement().toString();

            this.methodReference = overriderTree.getKind() == Tree.Kind.MEMBER_REFERENCE;
        }

        /**
         * Perform the check
         *
         * @return true if the override is allowed
         */
        public boolean checkOverride() {
            if (checker.shouldSkipUses(overriddenType.getUnderlyingType().asElement())) {
                return true;
            }

            boolean result = checkReturn();
            result &= checkParameters();
            if (methodReference) {
                result &= checkMemberReferenceReceivers();
            } else {
                result &= checkReceiverOverride();
            }
            checkPreAndPostConditions();
            checkPurity();

            return result;
        }

        private void checkPurity() {
            String msgKey =
                    methodReference ? "purity.invalid.methodref" : "purity.invalid.overriding";

            // check purity annotations
            Set<Pure.Kind> superPurity =
                    new HashSet<Pure.Kind>(
                            PurityUtils.getPurityKinds(atypeFactory, overridden.getElement()));
            Set<Pure.Kind> subPurity =
                    new HashSet<Pure.Kind>(
                            PurityUtils.getPurityKinds(atypeFactory, overrider.getElement()));
            if (!subPurity.containsAll(superPurity)) {
                checker.report(
                        Result.failure(
                                msgKey,
                                overriderMeth,
                                overriderTyp,
                                overriddenMeth,
                                overriddenTyp,
                                subPurity,
                                superPurity),
                        overriderTree);
            }
        }

        private void checkPreAndPostConditions() {
            String msgKey = methodReference ? "methodref" : "override";
            if (methodReference) {
                // TODO: Support post conditions and method references.
                // The parse context always expects instance methods, but method references can be static.
                return;
            }

            // Check postconditions
            ContractsUtils contracts = ContractsUtils.getInstance(atypeFactory);
            Set<Postcondition> superPost = contracts.getPostconditions(overridden.getElement());
            Set<Postcondition> subPost = contracts.getPostconditions(overrider.getElement());
            Set<Pair<Receiver, AnnotationMirror>> superPost2 =
                    resolveContracts(superPost, overridden);
            Set<Pair<Receiver, AnnotationMirror>> subPost2 = resolveContracts(subPost, overrider);
            @SuppressWarnings("CompilerMessages")
            /*@CompilerMessageKey*/ String postmsg =
                    "contracts.postcondition." + msgKey + ".invalid";
            checkContractsSubset(
                    overriderMeth,
                    overriderTyp,
                    overriddenMeth,
                    overriddenTyp,
                    superPost2,
                    subPost2,
                    postmsg);

            // Check preconditions
            Set<Precondition> superPre = contracts.getPreconditions(overridden.getElement());
            Set<Precondition> subPre = contracts.getPreconditions(overrider.getElement());
            Set<Pair<Receiver, AnnotationMirror>> superPre2 =
                    resolveContracts(superPre, overridden);
            Set<Pair<Receiver, AnnotationMirror>> subPre2 = resolveContracts(subPre, overrider);
            @SuppressWarnings("CompilerMessages")
            /*@CompilerMessageKey*/ String premsg = "contracts.precondition." + msgKey + ".invalid";
            checkContractsSubset(
                    overriderMeth,
                    overriderTyp,
                    overriddenMeth,
                    overriddenTyp,
                    subPre2,
                    superPre2,
                    premsg);

            // Check conditional postconditions
            Set<ConditionalPostcondition> superCPost =
                    contracts.getConditionalPostconditions(overridden.getElement());
            Set<ConditionalPostcondition> subCPost =
                    contracts.getConditionalPostconditions(overrider.getElement());
            // consider only 'true' postconditions
            Set<Postcondition> superCPostTrue = filterConditionalPostconditions(superCPost, true);
            Set<Postcondition> subCPostTrue = filterConditionalPostconditions(subCPost, true);
            Set<Pair<Receiver, AnnotationMirror>> superCPostTrue2 =
                    resolveContracts(superCPostTrue, overridden);
            Set<Pair<Receiver, AnnotationMirror>> subCPostTrue2 =
                    resolveContracts(subCPostTrue, overrider);
            @SuppressWarnings("CompilerMessages")
            /*@CompilerMessageKey*/ String posttruemsg =
                    "contracts.conditional.postcondition.true." + msgKey + ".invalid";
            checkContractsSubset(
                    overriderMeth,
                    overriderTyp,
                    overriddenMeth,
                    overriddenTyp,
                    superCPostTrue2,
                    subCPostTrue2,
                    posttruemsg);

            // consider only 'false' postconditions
            Set<Postcondition> superCPostFalse = filterConditionalPostconditions(superCPost, false);
            Set<Postcondition> subCPostFalse = filterConditionalPostconditions(subCPost, false);
            Set<Pair<Receiver, AnnotationMirror>> superCPostFalse2 =
                    resolveContracts(superCPostFalse, overridden);
            Set<Pair<Receiver, AnnotationMirror>> subCPostFalse2 =
                    resolveContracts(subCPostFalse, overrider);
            @SuppressWarnings("CompilerMessages")
            /*@CompilerMessageKey*/ String postfalsemsg =
                    "contracts.conditional.postcondition.false." + msgKey + ".invalid";
            checkContractsSubset(
                    overriderMeth,
                    overriderTyp,
                    overriddenMeth,
                    overriddenTyp,
                    superCPostFalse2,
                    subCPostFalse2,
                    postfalsemsg);
        }

        private boolean checkMemberReferenceReceivers() {
            JCTree.JCMemberReference memberTree = (JCTree.JCMemberReference) overriderTree;

            if (overridingType.getKind() == TypeKind.ARRAY) {
                // Assume the receiver for all method on arrays are @Top
                // This simplifies some logic because an AnnotatedExecutableType for an array method
                // (ie String[]::clone) has a receiver of "Array." The UNBOUND check would then
                // have to compare "Array" to "String[]".
                return true;
            }

            // These act like a traditional override
            if (memberTree.kind == JCTree.JCMemberReference.ReferenceKind.UNBOUND) {
                AnnotatedTypeMirror overriderReceiver = overrider.getReceiverType();
                AnnotatedTypeMirror overriddenReceiver = overridden.getParameterTypes().get(0);
                boolean success =
                        atypeFactory
                                .getTypeHierarchy()
                                .isSubtype(overriddenReceiver, overriderReceiver);
                if (!success) {
                    checker.report(
                            Result.failure(
                                    "methodref.receiver.invalid",
                                    overriderMeth,
                                    overriderTyp,
                                    overriddenMeth,
                                    overriddenTyp,
                                    overriderReceiver,
                                    overriddenReceiver),
                            overriderTree);
                }
                return success;
            }

            // The rest act like method invocations
            AnnotatedTypeMirror receiverDecl;
            AnnotatedTypeMirror receiverArg;
            switch (memberTree.kind) {
                case UNBOUND:
                    ErrorReporter.errorAbort("Case UNBOUND should already be handled.");
                    return true; // Dead code
                case SUPER:
                    receiverDecl = overrider.getReceiverType();
                    receiverArg =
                            atypeFactory.getAnnotatedType(memberTree.getQualifierExpression());

                    final AnnotatedTypeMirror selfType = atypeFactory.getSelfType(memberTree);
                    receiverArg.replaceAnnotations(selfType.getAnnotations());
                    break;
                case BOUND:
                    receiverDecl = overrider.getReceiverType();
                    receiverArg = overridingType;
                    break;
                case IMPLICIT_INNER:
                    receiverDecl = overrider.getReceiverType();
                    receiverArg = atypeFactory.getSelfType(memberTree);
                    break;
                case TOPLEVEL:
                case STATIC:
                case ARRAY_CTOR:
                default:
                    // Intentional fallthrough
                    // These don't have receivers
                    return true;
            }

            boolean success = atypeFactory.getTypeHierarchy().isSubtype(receiverArg, receiverDecl);
            if (!success) {
                checker.report(
                        Result.failure(
                                "methodref.receiver.bound.invalid",
                                receiverArg,
                                overriderMeth,
                                overriderTyp,
                                receiverArg,
                                receiverDecl),
                        overriderTree);
            }

            return success;
        }

        protected boolean checkReceiverOverride() {
            // Check the receiver type.
            // isSubtype() requires its arguments to be actual subtypes with
            // respect to JLS, but overrider receiver is not a subtype of the
            // overridden receiver.  Hence copying the annotations.
            // TODO: this will need to be improved for generic receivers.
            AnnotatedTypeMirror overriddenReceiver =
                    overrider.getReceiverType().getErased().shallowCopy(false);
            overriddenReceiver.addAnnotations(overridden.getReceiverType().getAnnotations());
            if (!atypeFactory
                    .getTypeHierarchy()
                    .isSubtype(overriddenReceiver, overrider.getReceiverType().getErased())) {
                checker.report(
                        Result.failure(
                                "override.receiver.invalid",
                                overriderMeth,
                                overriderTyp,
                                overriddenMeth,
                                overriddenTyp,
                                overrider.getReceiverType(),
                                overridden.getReceiverType()),
                        overriderTree);
                return false;
            }
            return true;
        }

        private boolean checkParameters() {
            List<AnnotatedTypeMirror> overriderParams = overrider.getParameterTypes();
            List<AnnotatedTypeMirror> overriddenParams = overridden.getParameterTypes();

            // Fix up method reference parameters.
            // See https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.13.1
            if (methodReference) {
                // The functional interface of an unbound member reference has an extra parameter (the receiver).
                if (((JCTree.JCMemberReference) overriderTree)
                        .hasKind(JCTree.JCMemberReference.ReferenceKind.UNBOUND)) {
                    overriddenParams = new ArrayList<>(overriddenParams);
                    overriddenParams.remove(0);
                }
                // Deal with varargs
                if (overrider.isVarArgs() && !overridden.isVarArgs()) {
                    overriderParams =
                            AnnotatedTypes.expandVarArgsFromTypes(overrider, overriddenParams);
                }
            }

            boolean result = true;
            for (int i = 0; i < overriderParams.size(); ++i) {
                boolean success =
                        atypeFactory
                                .getTypeHierarchy()
                                .isSubtype(overriddenParams.get(i), overriderParams.get(i));
                if (!success) {
                    success =
                            testTypevarContainment(overriddenParams.get(i), overriderParams.get(i));
                }

                checkParametersMsg(success, i, overriderParams, overriddenParams);
                result &= success;
            }
            return result;
        }

        private void checkParametersMsg(
                boolean success,
                int index,
                List<AnnotatedTypeMirror> overriderParams,
                List<AnnotatedTypeMirror> overriddenParams) {
            String msgKey = methodReference ? "methodref.param.invalid" : "override.param.invalid";
            long valuePos =
                    overriderTree instanceof MethodTree
                            ? positions.getStartPosition(
                                    root, ((MethodTree) overriderTree).getParameters().get(index))
                            : positions.getStartPosition(root, overriderTree);
            Tree posTree =
                    overriderTree instanceof MethodTree
                            ? ((MethodTree) overriderTree).getParameters().get(index)
                            : overriderTree;

            if (checker.hasOption("showchecks")) {
                System.out.printf(
                        " %s (line %3d):%n     overrider: %s %s (parameter %d type %s)%n   overridden: %s %s (parameter %d type %s)%n",
                        (success
                                ? "success: overridden parameter type is subtype of overriding"
                                : "FAILURE: overridden parameter type is not subtype of overriding"),
                        (root.getLineMap() != null
                                ? root.getLineMap().getLineNumber(valuePos)
                                : -1),
                        overriderMeth,
                        overriderTyp,
                        index,
                        overriderParams.get(index).toString(),
                        overriddenMeth,
                        overriddenTyp,
                        index,
                        overriddenParams.get(index).toString());
            }
            if (!success) {
                checker.report(
                        Result.failure(
                                msgKey,
                                overriderMeth,
                                overriderTyp,
                                overriddenMeth,
                                overriddenTyp,
                                overriderParams.get(index).toString(),
                                overriddenParams.get(index).toString()),
                        posTree);
            }
        }

        private boolean checkReturn() {
            boolean success = true;
            // Check the return value.
            if ((overridingReturnType.getKind() != TypeKind.VOID)) {
                final TypeHierarchy typeHierarchy = atypeFactory.getTypeHierarchy();
                success = typeHierarchy.isSubtype(overridingReturnType, overriddenReturnType);

                // If both the overridden method have type variables as return types and both types were
                // defined in their respective methods then, they can be covariant or invariant
                // use super/subtypes for the overrides locations
                if (!success) {
                    success = testTypevarContainment(overridingReturnType, overriddenReturnType);

                    // sometimes when using a Java 8 compiler (not JSR308) the overridden return type of a method reference
                    // becomes a captured type.  This leads to defaulting that often makes the overriding return type
                    // invalid.  We ignore these.  This happens in Issue403/Issue404 when running without JSR308 Langtools
                    if (!success && methodReference) {

                        boolean isCaptureConverted =
                                (overriddenReturnType.getKind() == TypeKind.TYPEVAR)
                                        && InternalUtils.isCaptured(
                                                (TypeVariable)
                                                        overriddenReturnType.getUnderlyingType());

                        if (methodReference && isCaptureConverted) {
                            ExecutableElement overridenMethod = overridden.getElement();
                            boolean isFunctionApply =
                                    overridenMethod.getSimpleName().toString().equals("apply")
                                            && overridenMethod
                                                    .getEnclosingElement()
                                                    .toString()
                                                    .equals("java.util.function.Function");

                            if (isFunctionApply) {
                                AnnotatedTypeMirror overridingUpperBound =
                                        ((AnnotatedTypeVariable) overriddenReturnType)
                                                .getUpperBound();
                                success =
                                        typeHierarchy.isSubtype(
                                                overridingReturnType, overridingUpperBound);
                            }
                        }
                    }
                }

                checkReturnMsg(success);
            }
            return success;
        }

        private void checkReturnMsg(boolean success) {
            String msgKey =
                    methodReference ? "methodref.return.invalid" : "override.return.invalid";
            long valuePos =
                    overriderTree instanceof MethodTree
                            ? positions.getStartPosition(
                                    root, ((MethodTree) overriderTree).getReturnType())
                            : positions.getStartPosition(root, overriderTree);
            Tree posTree =
                    overriderTree instanceof MethodTree
                            ? ((MethodTree) overriderTree).getReturnType()
                            : overriderTree;
            // The return type of a MethodTree is null for a constructor.
            if (posTree == null) {
                posTree = overriderTree;
            }

            if (checker.hasOption("showchecks")) {
                System.out.printf(
                        " %s (line %3d):%n     overrider: %s %s (return type %s)%n   overridden: %s %s (return type %s)%n",
                        (success
                                ? "success: overriding return type is subtype of overridden"
                                : "FAILURE: overriding return type is not subtype of overridden"),
                        (root.getLineMap() != null
                                ? root.getLineMap().getLineNumber(valuePos)
                                : -1),
                        overriderMeth,
                        overriderTyp,
                        overrider.getReturnType().toString(),
                        overriddenMeth,
                        overriddenTyp,
                        overridden.getReturnType().toString());
            }
            if (!success) {
                checker.report(
                        Result.failure(
                                msgKey,
                                overriderMeth,
                                overriderTyp,
                                overriddenMeth,
                                overriddenTyp,
                                overridingReturnType,
                                overriddenReturnType),
                        posTree);
            }
        }
    }

    /**
     * Filters the set of conditional postconditions to return only those whose annotation result
     * value matches the value of the given boolean {@code b}. For example, if {@code b == true},
     * then the following {@code @EnsuresNonNullIf} conditional postcondition would match:<br>
     * {@code @EnsuresNonNullIf(expression="#1", result=true)}<br>
     * {@code boolean equals(@Nullable Object o)}
     */
    private Set<Postcondition> filterConditionalPostconditions(
            Set<ConditionalPostcondition> conditionalPostconditions, boolean b) {
        Set<Postcondition> result = new LinkedHashSet<>();
        for (ConditionalPostcondition p : conditionalPostconditions) {
            if (p.annoResult == b) {
                result.add(new Postcondition(p.expression, p.annotation));
            }
        }
        return result;
    }

    /**
     * Checks that {@code mustSubset} is a subset of {@code set} in the following sense: For every
     * expression in {@code mustSubset} there must be the same expression in {@code set}, with the
     * same (or a stronger) annotation.
     */
    private void checkContractsSubset(
            String overriderMeth,
            String overriderTyp,
            String overriddenMeth,
            String overriddenTyp,
            Set<Pair<Receiver, AnnotationMirror>> mustSubset,
            Set<Pair<Receiver, AnnotationMirror>> set,
            /*@CompilerMessageKey*/ String messageKey) {
        for (Pair<Receiver, AnnotationMirror> a : mustSubset) {
            boolean found = false;

            for (Pair<Receiver, AnnotationMirror> b : set) {
                // are we looking at a contract of the same receiver?
                if (a.first.equals(b.first)) {
                    // check subtyping relationship of annotations
                    QualifierHierarchy qualifierHierarchy = atypeFactory.getQualifierHierarchy();
                    if (qualifierHierarchy.isSubtype(a.second, b.second)) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                MethodTree method = visitorState.getMethodTree();
                checker.report(
                        Result.failure(
                                messageKey,
                                overriderMeth,
                                overriderTyp,
                                overriddenMeth,
                                overriddenTyp,
                                a.second,
                                a.first),
                        method);
            }
        }
    }

    /**
     * Takes a set of contracts identified by their expression and annotation strings and resolves
     * them to the correct {@link Receiver} and {@link AnnotationMirror}.
     */
    private Set<Pair<Receiver, AnnotationMirror>> resolveContracts(
            Set<? extends Contract> contractSet, AnnotatedExecutableType method) {
        Set<Pair<Receiver, AnnotationMirror>> result = new HashSet<>();
        MethodTree methodTree = visitorState.getMethodTree();
        TreePath path = atypeFactory.getPath(methodTree);
        FlowExpressionContext flowExprContext = null;
        for (Contract p : contractSet) {
            String expression = p.expression;
            AnnotationMirror annotation = p.annotation;
            if (flowExprContext == null) {
                flowExprContext =
                        FlowExpressionContext.buildContextForMethodDeclaration(
                                methodTree,
                                method.getReceiverType().getUnderlyingType(),
                                checker.getContext());
            }

            try {
                // TODO: currently, these expressions are parsed many times.
                // this could
                // be optimized to store the result the first time.
                // (same for other annotations)
                FlowExpressions.Receiver expr =
                        FlowExpressionParseUtil.parse(expression, flowExprContext, path, false);
                result.add(Pair.of(expr, annotation));
            } catch (FlowExpressionParseException e) {
                // report errors here
                checker.report(e.getResult(), methodTree);
            }
        }
        return result;
    }

    /**
     * Tests, for a re-assignment, whether the variable is assignable or not. If not, it emits an
     * assignability.invalid error.
     *
     * @param varType the type of the variable being re-assigned
     * @param varTree the tree used to access the variable in the assignment
     */
    protected void checkAssignability(AnnotatedTypeMirror varType, Tree varTree) {
        if (TreeUtils.isExpressionTree(varTree)) {
            AnnotatedTypeMirror rcvType = atypeFactory.getReceiverType((ExpressionTree) varTree);
            if (!isAssignable(varType, rcvType, varTree)) {
                checker.report(
                        Result.failure(
                                "assignability.invalid", InternalUtils.symbol(varTree), rcvType),
                        varTree);
            }
        }
    }

    /**
     * Tests whether the variable accessed is an assignable variable or not, given the current scope
     *
     * <p>TODO: document which parameters are nullable; e.g. receiverType is null in many cases,
     * e.g. local variables.
     *
     * @param varType the annotated variable type
     * @param variable tree used to access the variable
     * @return true iff variable is assignable in the current scope
     */
    protected boolean isAssignable(
            AnnotatedTypeMirror varType, AnnotatedTypeMirror receiverType, Tree variable) {
        return true;
    }

    protected MemberSelectTree enclosingMemberSelect() {
        TreePath path = this.getCurrentPath();
        assert path.getLeaf().getKind() == Tree.Kind.IDENTIFIER
                : "expected identifier, found: " + path.getLeaf();
        if (path.getParentPath().getLeaf().getKind() == Tree.Kind.MEMBER_SELECT) {
            return (MemberSelectTree) path.getParentPath().getLeaf();
        } else {
            return null;
        }
    }

    protected Tree enclosingStatement(Tree tree) {
        TreePath path = this.getCurrentPath();
        while (path != null && path.getLeaf() != tree) {
            path = path.getParentPath();
        }

        if (path != null) {
            return path.getParentPath().getLeaf();
        } else {
            return null;
        }
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        checkAccess(node, p);
        return super.visitIdentifier(node, p);
    }

    protected void checkAccess(IdentifierTree node, Void p) {
        MemberSelectTree memberSel = enclosingMemberSelect();
        ExpressionTree tree;
        Element elem;

        if (memberSel == null) {
            tree = node;
            elem = TreeUtils.elementFromUse(node);
        } else {
            tree = memberSel;
            elem = TreeUtils.elementFromUse(memberSel);
        }

        if (elem == null || !elem.getKind().isField()) {
            return;
        }

        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(tree);

        if (!isAccessAllowed(elem, receiver, tree)) {
            checker.report(Result.failure("unallowed.access", elem, receiver), node);
        }
    }

    protected boolean isAccessAllowed(
            Element field, AnnotatedTypeMirror receiver, ExpressionTree accessTree) {
        AnnotationMirror unused = atypeFactory.getDeclAnnotation(field, Unused.class);
        if (unused == null) {
            return true;
        }

        Name when = AnnotationUtils.getElementValueClassName(unused, "when", false);
        if (receiver.getAnnotation(when) == null) {
            return true;
        }

        Tree tree = this.enclosingStatement(accessTree);

        // assigning unused to null is OK
        return (tree != null
                && tree.getKind() == Tree.Kind.ASSIGNMENT
                && ((AssignmentTree) tree).getVariable() == accessTree
                && ((AssignmentTree) tree).getExpression().getKind() == Tree.Kind.NULL_LITERAL);
    }

    /**
     * Tests that the qualifiers present on the useType are valid qualifiers, given the qualifiers
     * on the declaration of the type, declarationType.
     *
     * <p>The check is shallow, as it does not descend into generic or array types (i.e. only
     * performing the validity check on the raw type or outermost array dimension). {@link
     * BaseTypeVisitor#validateTypeOf(Tree)} would call this for each type argument or array
     * dimension separately.
     *
     * <p>In most cases, {@code useType} simply needs to be a subtype of {@code declarationType},
     * but there are exceptions.
     *
     * @param declarationType the type of the class (TypeElement)
     * @param useType the use of the class (instance type)
     * @param tree the tree where the type is used
     * @return true if the useType is a valid use of elemType
     */
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        return atypeFactory
                .getTypeHierarchy()
                .isSubtype(useType.getErased(), declarationType.getErased());
    }

    /**
     * Tests that the qualifiers present on the primitive type are valid.
     *
     * <p>The default implementation always returns true. Subclasses should override this method to
     * limit what annotations are allowed on primitive types.
     *
     * @param type the use of the primitive type
     * @param tree the tree where the type is used
     * @return true if the type is a valid use of the primitive type
     */
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        return true;
    }

    /**
     * Tests that the qualifiers present on the array type are valid. This method will be invoked
     * for each array level independently, i.e. this method only needs to check the top-level
     * qualifiers of an array.
     *
     * <p>The default implementation always returns true. Subclasses should override this method to
     * limit what annotations are allowed on array types.
     *
     * @param type the array type use
     * @param tree the tree where the type is used
     * @return true if the type is a valid array type
     */
    public boolean isValidUse(AnnotatedArrayType type, Tree tree) {
        return true;
    }

    /**
     * Tests whether the tree expressed by the passed type tree is a valid type, and emits an error
     * if that is not the case (e.g. '@Mutable String'). If the tree is a method or constructor,
     * check the return type.
     *
     * @param tree the AST type supplied by the user
     */
    public boolean validateTypeOf(Tree tree) {
        AnnotatedTypeMirror type;
        // It's quite annoying that there is no TypeTree.
        switch (tree.getKind()) {
            case PRIMITIVE_TYPE:
            case PARAMETERIZED_TYPE:
            case TYPE_PARAMETER:
            case ARRAY_TYPE:
            case UNBOUNDED_WILDCARD:
            case EXTENDS_WILDCARD:
            case SUPER_WILDCARD:
            case ANNOTATED_TYPE:
                type = atypeFactory.getAnnotatedTypeFromTypeTree(tree);
                break;
            case METHOD:
                type = atypeFactory.getMethodReturnType((MethodTree) tree);
                if (type == null || type.getKind() == TypeKind.VOID) {
                    // Nothing to do for void methods.
                    // Note that for a constructor the AnnotatedExecutableType does
                    // not use void as return type.
                    return true;
                }
                break;
            default:
                type = atypeFactory.getAnnotatedType(tree);
        }
        return validateType(tree, type);
    }

    /**
     * Tests whether the type and corresponding type tree is a valid type, and emits an error if
     * that is not the case (e.g. '@Mutable String'). If the tree is a method or constructor, check
     * the return type.
     *
     * @param tree the type tree supplied by the user
     * @param type the type corresponding to tree
     */
    public boolean validateType(Tree tree, AnnotatedTypeMirror type) {
        // basic consistency checks
        if (!AnnotatedTypes.isValidType(atypeFactory.getQualifierHierarchy(), type)) {
            checker.report(
                    Result.failure("type.invalid", type.getAnnotations(), type.toString()), tree);
            return false;
        }

        // more checks (also specific to checker, potentially)
        return typeValidator.isValid(type, tree);
    }

    // This is a test to ensure that all types are valid
    protected final TypeValidator typeValidator;

    protected TypeValidator createTypeValidator() {
        return new BaseTypeValidator(checker, this, atypeFactory);
    }

    // **********************************************************************
    // Random helper methods
    // **********************************************************************

    /**
     * Tests whether the expression should not be checked because of the tree referring to
     * unannotated classes, as specified in the {@code checker.skipUses} property.
     *
     * <p>It returns true if exprTree is a method invocation or a field access to a class whose
     * qualified name matches @{link checker.skipUses} expression.
     *
     * @param exprTree any expression tree
     * @return true if checker should not test exprTree
     */
    protected final boolean shouldSkipUses(ExpressionTree exprTree) {
        // System.out.printf("shouldSkipUses: %s: %s%n", exprTree.getClass(), exprTree);

        // This special case for ConditionalExpressionTree seems wrong, so
        // I commented it out.  It will skip expressions that should be
        // checked, just because they are lexically near expressions that
        // should be skipped.  Presumably it's because conditionals do some
        // type inference, but if so, this is the wrong way to fix the
        // problem. -MDE
        // if (exprTree instanceof ConditionalExpressionTree) {
        //     ConditionalExpressionTree condTree =
        //         (ConditionalExpressionTree)exprTree;
        //     return (shouldSkipUses(condTree.getTrueExpression()) ||
        //             shouldSkipUses(condTree.getFalseExpression()));
        // }

        // Don't use commonAssignmentCheck for lambdas or method references.
        if (exprTree instanceof MemberReferenceTree || exprTree instanceof LambdaExpressionTree) {
            return true;
        }
        Element elm = InternalUtils.symbol(exprTree);
        return checker.shouldSkipUses(elm);
    }

    // **********************************************************************
    // Overriding to avoid visit part of the tree
    // **********************************************************************

    /** Override Compilation Unit so we won't visit package names or imports */
    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
        Void r = scan(node.getPackageAnnotations(), p);
        // r = reduce(scan(node.getPackageName(), p), r);
        // r = reduce(scan(node.getImports(), p), r);
        r = reduce(scan(node.getTypeDecls(), p), r);
        return r;
    }

    // **********************************************************************
    // Check that the annotated JDK is being used.
    // **********************************************************************

    private static boolean checkedJDK = false;

    // Not all subclasses call this -- only those that have an annotated JDK.
    /** Warn if the annotated JDK is not being used. */
    protected void checkForAnnotatedJdk() {
        if (checkedJDK) {
            return;
        }
        checkedJDK = true;
        if (checker.hasOption("nocheckjdk")) {
            return;
        }
        TypeElement objectTE = elements.getTypeElement("java.lang.Object");
        List<? extends Element> members = elements.getAllMembers(objectTE);

        for (Element member : members) {
            if (member.toString().equals("equals(java.lang.Object)")) {
                ExecutableElement m = (ExecutableElement) member;
                // The Nullness JDK serves as a proxy for all annotated
                // JDKs.

                // Note that we cannot use the AnnotatedTypeMirrors from the
                // Checker Framework, because those only return the annotations
                // that are used by the current checker.
                // That is, if this code is executed by something other than the
                // Nullness Checker, we would not find the annotations.
                // Therefore, we go to the Element and get all annotations on
                // the parameter.

                // TODO: doing types.typeAnnotationOf(m.getParameters().get(0).asType(), Nullable.class)
                // or types.typeAnnotationsOf(m.asType())
                // does not work any more. It should.

                boolean foundNN = false;
                for (com.sun.tools.javac.code.Attribute.TypeCompound tc :
                        ((com.sun.tools.javac.code.Symbol) m).getRawTypeAttributes()) {
                    if (tc.position.type
                                    == com.sun.tools.javac.code.TargetType.METHOD_FORMAL_PARAMETER
                            && tc.position.parameter_index == 0
                            &&
                            // TODO: using .class would be nicer, but adds a circular dependency on
                            // the "checker" project
                            // tc.type.toString().equals(org.checkerframework.checker.nullness.qual.Nullable.class.getName()) ) {
                            tc.type
                                    .toString()
                                    .equals(
                                            "org.checkerframework.checker.nullness.qual.Nullable")) {
                        foundNN = true;
                    }
                }

                if (!foundNN) {
                    String jdkJarName = PluginUtil.getJdkJarName();

                    checker.message(
                            Kind.WARNING,
                            "You do not seem to be using the distributed annotated JDK.  To fix the"
                                    + System.getProperty("line.separator")
                                    + "problem, supply this argument (first, fill in the \"...\") when you run javac:"
                                    + System.getProperty("line.separator")
                                    + "  -Xbootclasspath/p:.../checker/dist/"
                                    + jdkJarName);
                }
            }
        }
    }
}
