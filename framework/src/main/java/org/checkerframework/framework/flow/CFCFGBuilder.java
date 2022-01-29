package org.checkerframework.framework.flow;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.dataflow.cfg.builder.CFGTranslationPhaseOne;
import org.checkerframework.dataflow.cfg.builder.CFGTranslationPhaseThree;
import org.checkerframework.dataflow.cfg.builder.CFGTranslationPhaseTwo;
import org.checkerframework.dataflow.cfg.builder.PhaseOneResult;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

import java.util.Collection;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A control-flow graph builder (see {@link CFGBuilder}) that knows about the Checker Framework
 * annotations and their representation as {@link AnnotatedTypeMirror}s.
 */
public class CFCFGBuilder extends CFGBuilder {
    /** This class should never be instantiated. Protected to still allow subclasses. */
    protected CFCFGBuilder() {}

    /** Build the control flow graph of some code. */
    public static ControlFlowGraph build(
            CompilationUnitTree root,
            UnderlyingAST underlyingAST,
            BaseTypeChecker checker,
            AnnotatedTypeFactory factory,
            ProcessingEnvironment env) {
        boolean assumeAssertionsEnabled = checker.hasOption("assumeAssertionsAreEnabled");
        boolean assumeAssertionsDisabled = checker.hasOption("assumeAssertionsAreDisabled");
        if (assumeAssertionsEnabled && assumeAssertionsDisabled) {
            throw new UserError(
                    "Assertions cannot be assumed to be enabled and disabled at the same time.");
        }

        // Subcheckers with dataflow share control-flow graph structure to
        // allow a super-checker to query the stores of a subchecker.
        if (factory instanceof GenericAnnotatedTypeFactory) {
            GenericAnnotatedTypeFactory<?, ?, ?, ?> asGATF =
                    (GenericAnnotatedTypeFactory<?, ?, ?, ?>) factory;
            if (asGATF.hasOrIsSubchecker) {
                ControlFlowGraph sharedCFG = asGATF.getSharedCFGForTree(underlyingAST.getCode());
                if (sharedCFG != null) {
                    return sharedCFG;
                }
            }
        }

        CFTreeBuilder builder = new CFTreeBuilder(env);
        PhaseOneResult phase1result =
                new CFCFGTranslationPhaseOne(
                                builder,
                                checker,
                                factory,
                                assumeAssertionsEnabled,
                                assumeAssertionsDisabled,
                                env)
                        .process(root, underlyingAST);
        ControlFlowGraph phase2result = CFGTranslationPhaseTwo.process(phase1result);
        ControlFlowGraph phase3result = CFGTranslationPhaseThree.process(phase2result);
        if (factory instanceof GenericAnnotatedTypeFactory) {
            GenericAnnotatedTypeFactory<?, ?, ?, ?> asGATF =
                    (GenericAnnotatedTypeFactory<?, ?, ?, ?>) factory;
            if (asGATF.hasOrIsSubchecker) {
                asGATF.addSharedCFGForTree(underlyingAST.getCode(), phase3result);
            }
        }
        return phase3result;
    }

    /**
     * Given a SourceChecker and an AssertTree, returns whether the AssertTree uses
     * an @AssumeAssertion string that is relevant to the SourceChecker.
     *
     * @param checker the checker
     * @param tree an assert tree
     * @return true if the assert tree contains an @AssumeAssertion(checker) message string for any
     *     subchecker of the given checker's ultimate parent checker
     */
    public static boolean assumeAssertionsActivatedForAssertTree(
            BaseTypeChecker checker, AssertTree tree) {
        ExpressionTree detail = tree.getDetail();
        if (detail != null) {
            String msg = detail.toString();
            BaseTypeChecker ultimateParent = checker.getUltimateParentChecker();
            Collection<String> prefixes = ultimateParent.getSuppressWarningsPrefixesOfSubcheckers();
            for (String prefix : prefixes) {
                String assumeAssert = "@AssumeAssertion(" + prefix + ")";
                if (msg.contains(assumeAssert)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * A specialized phase-one CFG builder, with a few modifications that make use of the type
     * factory. It is responsible for: 1) translating foreach loops so that the declarations of
     * their iteration variables have the right annotations, 2) registering the containing elements
     * of artificial trees with the relevant type factories, and 3) generating appropriate assertion
     * CFG structure in the presence of @AssumeAssertion assertion strings which mention the checker
     * or its supercheckers.
     */
    protected static class CFCFGTranslationPhaseOne extends CFGTranslationPhaseOne {
        /** The associated checker. */
        protected final BaseTypeChecker checker;

        /** Type factory to provide types used during CFG building. */
        protected final AnnotatedTypeFactory factory;

        public CFCFGTranslationPhaseOne(
                CFTreeBuilder builder,
                BaseTypeChecker checker,
                AnnotatedTypeFactory factory,
                boolean assumeAssertionsEnabled,
                boolean assumeAssertionsDisabled,
                ProcessingEnvironment env) {
            super(builder, factory, assumeAssertionsEnabled, assumeAssertionsDisabled, env);
            this.checker = checker;
            this.factory = factory;
        }

        @Override
        protected boolean assumeAssertionsEnabledFor(AssertTree tree) {
            if (assumeAssertionsActivatedForAssertTree(checker, tree)) {
                return true;
            }
            return super.assumeAssertionsEnabledFor(tree);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Assigns a path to the artificial tree.
         *
         * @param tree the newly created Tree
         */
        @Override
        public void handleArtificialTree(Tree tree) {
            // Create a new child of the current path and assign to the artificial tree.
            // Although intuitively, using the sibling of the current path as the artificial tree
            // path makes more sense, it has the risk of improperly changing the defaulting scope
            // of the artificial tree.
            TreePath artificialPath = new TreePath(getCurrentPath(), tree);
            factory.setPathForArtificialTree(tree, artificialPath);
        }

        @Override
        protected VariableTree createEnhancedForLoopIteratorVariable(
                MethodInvocationTree iteratorCall, VariableElement variableElement) {
            Tree annotatedIteratorTypeTree =
                    ((CFTreeBuilder) treeBuilder)
                            .buildAnnotatedType(TreeUtils.typeOf(iteratorCall));
            handleArtificialTree(annotatedIteratorTypeTree);

            // Declare and initialize a new, unique iterator variable
            VariableTree iteratorVariable =
                    treeBuilder.buildVariableDecl(
                            annotatedIteratorTypeTree,
                            uniqueName("iter"),
                            variableElement.getEnclosingElement(),
                            iteratorCall);
            return iteratorVariable;
        }

        @Override
        protected VariableTree createEnhancedForLoopArrayVariable(
                ExpressionTree expression, VariableElement variableElement) {

            TypeMirror type = null;
            if (TreeUtils.isLocalVariable(expression)) {
                // It is necessary to get the elt because just getting the type of expression
                // directly (via TreeUtils.typeOf) doesn't include annotations on the declarations
                // of local variables, for some reason.
                Element elt = TreeUtils.elementFromTree(expression);
                if (elt != null) {
                    type = ElementUtils.getType(elt);
                }
            }

            // In all other cases cases, instead get the type of the expression. This case is
            // also triggered when the type from the element is not an array, which can occur
            // if the declaration of the local is a generic, such as in
            // framework/tests/all-systems/java8inference/Issue1775.java.
            // Getting the type from the expression itself guarantees the result will be an array.
            if (type == null || type.getKind() != TypeKind.ARRAY) {
                TypeMirror expressionType = TreeUtils.typeOf(expression);
                type = expressionType;
            }

            assert (type instanceof ArrayType) : "array types must be represented by ArrayType";

            Tree annotatedArrayTypeTree = ((CFTreeBuilder) treeBuilder).buildAnnotatedType(type);
            handleArtificialTree(annotatedArrayTypeTree);

            // Declare and initialize a temporary array variable
            VariableTree arrayVariable =
                    treeBuilder.buildVariableDecl(
                            annotatedArrayTypeTree,
                            uniqueName("array"),
                            variableElement.getEnclosingElement(),
                            expression);
            return arrayVariable;
        }
    }
}
