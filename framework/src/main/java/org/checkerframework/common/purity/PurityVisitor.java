package org.checkerframework.common.purity;

import static org.checkerframework.dataflow.qual.Pure.Kind.DETERMINISTIC;
import static org.checkerframework.dataflow.qual.Pure.Kind.SIDE_EFFECT_FREE;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A visitor that determines the purity (as defined by {@link
 * org.checkerframework.dataflow.qual.SideEffectFree}, {@link
 * org.checkerframework.dataflow.qual.Deterministic}, and {@link
 * org.checkerframework.dataflow.qual.Pure}) of a statement or expression.
 *
 * @see SideEffectFree
 * @see Deterministic
 * @see Pure
 */
public class PurityVisitor extends BaseTypeVisitor<PurityAnnotatedTypeFactory> {

    /** Whether -A suggestPureMethods was supplied. */
    private boolean suggestPureMethods;

    /** Whether -A checkPurityAnnotations was supplied. */
    private boolean checkPurityAnnotations;

    /** Whether -A assumeSideEffectFree was supplied. */
    private boolean assumeSideEffectFree;

    /**
     * Create a PurityVisitor associated with the given checker.
     *
     * @param checker the checker
     */
    public PurityVisitor(BaseTypeChecker checker) {
        super(checker);
        suggestPureMethods = checker.hasOption("suggestPureMethods");
        checkPurityAnnotations = checker.hasOption("checkPurityAnnotations");
        assumeSideEffectFree = checker.hasOption("assumeSideEffectFree");
    }

    /**
     * Compute whether the given statement is side-effect-free, deterministic, or both. Returns a
     * result that can be queried.
     */
    private PurityResult checkPurity(
            TreePath statement, AnnotationProvider annoProvider, boolean assumeSideEffectFree) {
        PurityVisitorHelper helper = new PurityVisitorHelper(annoProvider, assumeSideEffectFree);
        if (statement != null) {
            helper.scan(statement, null);
        }
        return helper.purityResult;
    }

    /**
     * Result of the {@link PurityVisitorHelper}. Can be queried regarding whether a given tree was
     * side-effect-free, deterministic, or both; also gives reasons if the answer is "no".
     */
    public static class PurityResult {

        /** Reasons why a method is not {@link SideEffectFree}. */
        protected final List<Pair<Tree, String>> notSEFreeReasons = new ArrayList<>(1);

        /** Reasons why a method is not {@link Deterministic}. */
        protected final List<Pair<Tree, String>> notDetReasons = new ArrayList<>(1);

        /** Reasons why a method is not {@link SideEffectFree} nor {@link Deterministic} */
        protected final List<Pair<Tree, String>> notBothReasons = new ArrayList<>(1);

        /**
         * Contains all the varieties of purity that the expression has. Starts out with all
         * varieties, and elements are removed from it as violations are found.
         */
        protected EnumSet<Pure.Kind> kinds = EnumSet.allOf(Pure.Kind.class);

        /** Accessor method for the kinds. */
        public EnumSet<Pure.Kind> getKinds() {
            return kinds;
        }

        /**
         * Is the method pure w.r.t. a given set of kinds?
         *
         * @param kinds the varieties of purity to check
         * @return true if the method is pure with respect to all the given kinds
         */
        public boolean isPure(EnumSet<Pure.Kind> kinds) {
            return kinds.containsAll(kinds);
        }

        /** Get the reasons why the method is not side-effect-free. */
        public List<Pair<Tree, String>> getNotSEFreeReasons() {
            return notSEFreeReasons;
        }

        /** Add a reason why the method is not side-effect-free. */
        public void addNotSEFreeReason(Tree t, String msgId) {
            notSEFreeReasons.add(Pair.of(t, msgId));
            kinds.remove(SIDE_EFFECT_FREE);
        }

        /** Get the reasons why the method is not deterministic. */
        public List<Pair<Tree, String>> getNotDetReasons() {
            return notDetReasons;
        }

        /** Add a reason why the method is not deterministic. */
        public void addNotDetReason(Tree t, String msgId) {
            notDetReasons.add(Pair.of(t, msgId));
            kinds.remove(DETERMINISTIC);
        }

        /** Get the reasons why the method is not both side-effect-free and deterministic. */
        public List<Pair<Tree, String>> getNotBothReasons() {
            return notBothReasons;
        }

        /** Add a reason why the method is not both side-effect-free and deterministic. */
        public void addNotBothReason(Tree t, String msgId) {
            notBothReasons.add(Pair.of(t, msgId));
            kinds.remove(DETERMINISTIC);
            kinds.remove(SIDE_EFFECT_FREE);
        }
    }

    // TODO: It would be possible to improve efficiency by visiting fewer nodes.  This would require
    // overriding more visit* methods.  I'm not sure whether such an optimization would be worth it.

    /** Helper class. */
    protected static class PurityVisitorHelper extends TreePathScanner<Void, Void> {

        /** The result of the analysis. */
        PurityVisitor.PurityResult purityResult = new PurityVisitor.PurityResult();

        /** Helps performing the analysis. */
        protected final AnnotationProvider annoProvider;

        /**
         * True if all methods should be assumed to be @SideEffectFree, for the purposes of
         * org.checkerframework.dataflow analysis.
         */
        private final boolean assumeSideEffectFree;

        /** Constructor for the {@link PurityVisitorHelper} inner class. */
        public PurityVisitorHelper(AnnotationProvider annoProvider, boolean assumeSideEffectFree) {
            this.annoProvider = annoProvider;
            this.assumeSideEffectFree = assumeSideEffectFree;
        }

        @Override
        public Void visitCatch(CatchTree node, Void ignore) {
            purityResult.addNotDetReason(node, "catch");
            return super.visitCatch(node, ignore);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void ignore) {
            assert TreeUtils.isUseOfElement(node) : "@AssumeAssertion(nullness): tree kind";
            Element elt = TreeUtils.elementFromUse(node);
            if (!PurityUtils.hasPurityAnnotation(annoProvider, elt)) {
                purityResult.addNotBothReason(node, "call.method");
            } else {
                boolean det = PurityUtils.isDeterministic(annoProvider, elt);
                boolean seFree =
                        (assumeSideEffectFree || PurityUtils.isSideEffectFree(annoProvider, elt));
                if (!det && !seFree) {
                    purityResult.addNotBothReason(node, "call.method");
                } else if (!det) {
                    purityResult.addNotDetReason(node, "call.method");
                } else if (!seFree) {
                    purityResult.addNotSEFreeReason(node, "call.method");
                }
            }
            return super.visitMethodInvocation(node, ignore);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void ignore) {
            // Ordinarily, "new MyClass()" is forbidden.  It is permitted, however, when it is the
            // expression in "throw EXPR;".  (In the future, more expressions could be permitted.)
            //
            // The expression in "throw EXPR;" is allowed to be non-@Deterministic, so long as it is
            // not within a catch block that could catch an exception that the statement throws.
            // For example, EXPR can be object creation (a "new" expression) or can call a
            // non-deterministic method.
            //
            // Coarse rule (currently implemented):
            //  * permit only "throw new SomeExpression(args)", where the constructor is
            //    @SideEffectFree and the args are pure, and forbid all enclosing try statements
            //    that have a catch clause.
            // More precise rule:
            //  * permit other non-deterministic expresssions within throw (at which time move this
            //    logic to visitThrow()).
            //  * the only bad try statements are those with a catch block that is:
            //     * unchecked exceptions
            //        * checked = Exception or lower, but excluding RuntimeException and its
            //          subclasses
            //     * super- or sub-classes of the type of _expr_
            //        * if _expr_ is exactly "new SomeException", this can be changed to just
            //          "superclasses of SomeException".
            //     * super- or sub-classes of exceptions declared to be thrown by any component of
            //       _expr_.
            //     * need to check every containing try statement, not just the nearest enclosing
            //       one.

            // Object creation is usually prohibited, but permit "throw new SomeException();"
            // if it is not contained within any try statement that has a catch clause.
            // (There is no need to check the latter condition, because the Purity Checker
            // forbids all catch statements.)
            Tree parent = getCurrentPath().getParentPath().getLeaf();
            boolean okThrowDeterministic = parent.getKind() == Tree.Kind.THROW;

            assert TreeUtils.isUseOfElement(node) : "@AssumeAssertion(nullness): tree kind";
            Element ctorElement = TreeUtils.elementFromUse(node);
            boolean deterministic = okThrowDeterministic;
            boolean sideEffectFree =
                    (assumeSideEffectFree
                            || PurityUtils.isSideEffectFree(annoProvider, ctorElement));
            // This does not use "addNotBothReason" because the reasons are different:  one is
            // because the constructor is called at all, and the other is because the constuctor
            // is not side-effect-free.
            if (!deterministic) {
                purityResult.addNotDetReason(node, "object.creation");
            }
            if (!sideEffectFree) {
                purityResult.addNotSEFreeReason(node, "call.constructor");
            }

            // TODO: if okThrowDeterministic, permit arguments to the newClass to be
            // non-deterministic (don't add those to purityResult), but still don't permit them to
            // have side effects.  This should probably wait until a rewrite of the Purity Checker.
            return super.visitNewClass(node, ignore);
        }

        @Override
        public Void visitAssignment(AssignmentTree node, Void ignore) {
            ExpressionTree variable = node.getVariable();
            assignmentCheck(variable);
            return super.visitAssignment(node, ignore);
        }

        /** Performs an assignment check and updates the result if necessary. */
        protected void assignmentCheck(ExpressionTree variable) {
            if (TreeUtils.isFieldAccess(variable)) {
                // lhs is a field access
                purityResult.addNotBothReason(variable, "assign.field");
            } else if (variable instanceof ArrayAccessTree) {
                // lhs is array access
                purityResult.addNotBothReason(variable, "assign.array");
            } else {
                // lhs is a local variable
                assert isLocalVariable(variable);
            }
        }

        /** Checks if the argument passed is a local variable. */
        protected boolean isLocalVariable(ExpressionTree variable) {
            return variable instanceof IdentifierTree && !TreeUtils.isFieldAccess(variable);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, Void ignore) {
            ExpressionTree variable = node.getVariable();
            assignmentCheck(variable);
            return super.visitCompoundAssignment(node, ignore);
        }
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        boolean anyPurityAnnotation = PurityUtils.hasPurityAnnotation(atypeFactory, node);

        if (checkPurityAnnotations && (anyPurityAnnotation || suggestPureMethods)) {
            // check "no" purity
            EnumSet<Pure.Kind> kinds = PurityUtils.getPurityKinds(atypeFactory, node);
            // @Deterministic makes no sense for a void method or constructor
            boolean isDeterministic = kinds.contains(DETERMINISTIC);
            if (isDeterministic) {
                if (TreeUtils.isConstructor(node)) {
                    checker.report(Result.warning("purity.deterministic.constructor"), node);
                } else if (TreeUtils.typeOf(node.getReturnType()).getKind() == TypeKind.VOID) {
                    checker.report(Result.warning("purity.deterministic.void.method"), node);
                }
            }

            // Report errors if necessary.
            PurityResult r =
                    checkPurity(
                            atypeFactory.getPath(node.getBody()),
                            atypeFactory,
                            assumeSideEffectFree);
            if (!r.isPure(kinds)) {
                reportPurityErrors(r, kinds);
            }

            if (suggestPureMethods) {
                // Issue a warning if the method is pure, but not annotated as such.
                EnumSet<Pure.Kind> additionalKinds = r.getKinds().clone();
                additionalKinds.removeAll(kinds);
                if (TreeUtils.isConstructor(node)) {
                    additionalKinds.remove(DETERMINISTIC);
                }
                if (!additionalKinds.isEmpty()) {
                    if (additionalKinds.size() == 2) {
                        checker.report(Result.warning("purity.more.pure", node.getName()), node);
                    } else if (additionalKinds.contains(SIDE_EFFECT_FREE)) {
                        checker.report(
                                Result.warning("purity.more.sideeffectfree", node.getName()), node);
                    } else if (additionalKinds.contains(DETERMINISTIC)) {
                        checker.report(
                                Result.warning("purity.more.deterministic", node.getName()), node);
                    } else {
                        assert false : "BaseTypeVisitor reached undesirable state";
                    }
                }
            }
        }
        return super.visitMethod(node, p);
    }

    /** Reports errors found during purity checking. */
    protected void reportPurityErrors(PurityResult result, EnumSet<Pure.Kind> expectedKinds) {
        assert !result.isPure(expectedKinds);
        EnumSet<Pure.Kind> violations = EnumSet.copyOf(expectedKinds);
        violations.removeAll(result.getKinds());
        if (violations.contains(DETERMINISTIC) || violations.contains(SIDE_EFFECT_FREE)) {
            String msgKeyPrefix;
            if (!violations.contains(SIDE_EFFECT_FREE)) {
                msgKeyPrefix = "purity.not.deterministic.";
            } else if (!violations.contains(DETERMINISTIC)) {
                msgKeyPrefix = "purity.not.sideeffectfree.";
            } else {
                msgKeyPrefix = "purity.not.deterministic.not.sideeffectfree.";
            }
            for (Pair<Tree, String> r : result.getNotBothReasons()) {
                reportPurityError(msgKeyPrefix, r);
            }
            if (violations.contains(SIDE_EFFECT_FREE)) {
                for (Pair<Tree, String> r : result.getNotSEFreeReasons()) {
                    reportPurityError("purity.not.sideeffectfree.", r);
                }
            }
            if (violations.contains(DETERMINISTIC)) {
                for (Pair<Tree, String> r : result.getNotDetReasons()) {
                    reportPurityError("purity.not.deterministic.", r);
                }
            }
        }
    }

    /** Reports a single purity error. */
    private void reportPurityError(String msgKeyPrefix, Pair<Tree, String> r) {
        String reason = r.second;
        @SuppressWarnings("CompilerMessages")
        @CompilerMessageKey String msgKey = msgKeyPrefix + reason;
        if (reason.equals("call")) {
            MethodInvocationTree mitree = (MethodInvocationTree) r.first;
            checker.report(Result.failure(msgKey, mitree.getMethodSelect()), r.first);
        } else {
            checker.report(Result.failure(msgKey), r.first);
        }
    }

    @Override
    protected OverrideChecker createOverrideChecker(
            Tree overriderTree,
            AnnotatedTypeMirror.AnnotatedExecutableType overrider,
            AnnotatedTypeMirror overridingType,
            AnnotatedTypeMirror overridingReturnType,
            AnnotatedTypeMirror.AnnotatedExecutableType overridden,
            AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType,
            AnnotatedTypeMirror overriddenReturnType) {
        return new PurityOverrideChecker(
                overriderTree,
                overrider,
                overridingType,
                overridingReturnType,
                overridden,
                overriddenType,
                overriddenReturnType);
    }

    /** This class adds a purity check to the OverrideChecker. */
    protected class PurityOverrideChecker extends OverrideChecker {

        public PurityOverrideChecker(
                Tree overriderTree,
                AnnotatedTypeMirror.AnnotatedExecutableType overrider,
                AnnotatedTypeMirror overridingType,
                AnnotatedTypeMirror overridingReturnType,
                AnnotatedTypeMirror.AnnotatedExecutableType overridden,
                AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType,
                AnnotatedTypeMirror overriddenReturnType) {
            super(
                    overriderTree,
                    overrider,
                    overridingType,
                    overridingReturnType,
                    overridden,
                    overriddenType,
                    overriddenReturnType);
        }

        @Override
        public boolean checkOverride() {
            if (checker.shouldSkipUses(overriddenType.getUnderlyingType().asElement())) {
                return true;
            }

            checkOverridePurity();
            return super.checkOverride();
        }

        /**
         * Checks if the override is valid according to the Purity Checker and reports the errors if
         * there are any.
         */
        private void checkOverridePurity() {
            String msgKey =
                    methodReference ? "purity.invalid.methodref" : "purity.invalid.overriding";

            // check purity annotations
            EnumSet<Pure.Kind> superPurity =
                    PurityUtils.getPurityKinds(atypeFactory, overridden.getElement());
            EnumSet<Pure.Kind> subPurity =
                    PurityUtils.getPurityKinds(atypeFactory, overrider.getElement());
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
    }
}
