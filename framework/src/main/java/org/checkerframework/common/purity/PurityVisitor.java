package org.checkerframework.common.purity;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.*;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.util.PurityChecker;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

public class PurityVisitor extends BaseTypeVisitor<PurityAnnotatedTypeFactory> {

    public PurityVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        boolean anyPurityAnnotation = PurityUtils.hasPurityAnnotation(atypeFactory, node);
        boolean checkPurityAlways = checker.hasOption("suggestPureMethods");
        boolean checkPurityAnnotations = checker.hasOption("checkPurityAnnotations");

        if (!checkPurityAnnotations || (!anyPurityAnnotation && !checkPurityAlways)) {
            return null;
        }

        // check "no" purity
        List<Pure.Kind> kinds = PurityUtils.getPurityKinds(atypeFactory, node);
        // @Deterministic makes no sense for a void method or constructor
        boolean isDeterministic = kinds.contains(Pure.Kind.DETERMINISTIC);
        if (isDeterministic) {
            if (TreeUtils.isConstructor(node)) {
                checker.report(Result.warning("purity.deterministic.constructor"), node);
            } else if (TreeUtils.typeOf(node.getReturnType()).getKind() == TypeKind.VOID) {
                checker.report(Result.warning("purity.deterministic.void.method"), node);
            }
        }

        // Report errors if necessary.
        PurityChecker.PurityResult r =
                PurityChecker.checkPurity(
                        atypeFactory.getPath(node.getBody()),
                        atypeFactory,
                        checker.hasOption("assumeSideEffectFree")
                                || checker.hasOption("assumePure"),
                        checker.hasOption("assumeDeterministic")
                                || checker.hasOption("assumePure"));
        if (!r.isPure(kinds)) {
            reportPurityErrors(r, kinds);
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
                    checker.report(Result.warning("purity.more.pure", node.getName()), node);
                } else if (additionalKinds.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
                    checker.report(
                            Result.warning("purity.more.sideeffectfree", node.getName()), node);
                } else if (additionalKinds.contains(Pure.Kind.DETERMINISTIC)) {
                    checker.report(
                            Result.warning("purity.more.deterministic", node.getName()), node);
                } else {
                    assert false : "BaseTypeVisitor reached undesirable state";
                }
            }
        }

        return super.visitMethod(node, p);
    }

    /** Reports errors found during purity checking. */
    protected void reportPurityErrors(
            PurityChecker.PurityResult result, Collection<Pure.Kind> expectedTypes) {
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
                reportPurityError(msgPrefix, r);
            }
            if (t.contains(Pure.Kind.SIDE_EFFECT_FREE)) {
                for (Pair<Tree, String> r : result.getNotSEFreeReasons()) {
                    reportPurityError("purity.not.sideeffectfree.", r);
                }
            }
            if (t.contains(Pure.Kind.DETERMINISTIC)) {
                for (Pair<Tree, String> r : result.getNotDetReasons()) {
                    reportPurityError("purity.not.deterministic.", r);
                }
            }
        }
    }

    /** Reports single purity error. */
    private void reportPurityError(String msgPrefix, Pair<Tree, String> r) {
        String reason = r.second;
        @SuppressWarnings("CompilerMessages")
        @CompilerMessageKey String msg = msgPrefix + reason;
        if (reason.equals("call")) {
            MethodInvocationTree mitree = (MethodInvocationTree) r.first;
            checker.report(Result.failure(msg, mitree.getMethodSelect()), r.first);
        } else {
            checker.report(Result.failure(msg), r.first);
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
            return true;
        }

        /**
         * Checks if the override is valid according to the Purity Checker and reports the errors if
         * there are any.
         */
        private void checkOverridePurity() {
            String msgKey =
                    methodReference ? "purity.invalid.methodref" : "purity.invalid.overriding";

            // check purity annotations
            Set<Pure.Kind> superPurity =
                    new HashSet<>(
                            PurityUtils.getPurityKinds(atypeFactory, overridden.getElement()));
            Set<Pure.Kind> subPurity =
                    new HashSet<>(PurityUtils.getPurityKinds(atypeFactory, overrider.getElement()));
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
