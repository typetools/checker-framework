package org.checkerframework.framework.flow;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.Collection;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A control-flow graph builder (see {@link CFGBuilder}) that knows about the Checker Framework
 * annotations and their representation as {@link AnnotatedTypeMirror}s.
 *
 * @author Stefan Heule
 */
public class CFCFGBuilder extends CFGBuilder {

    /** The associated checker. */
    protected final BaseTypeChecker checker;

    /** Type factory to provide types used during CFG building. */
    protected final AnnotatedTypeFactory factory;

    public CFCFGBuilder(BaseTypeChecker checker, AnnotatedTypeFactory factory) {
        super(
                checker.hasOption("assumeAssertionsAreEnabled"),
                checker.hasOption("assumeAssertionsAreDisabled"));
        if (assumeAssertionsEnabled && assumeAssertionsDisabled) {
            ErrorReporter.errorAbort(
                    "Assertions cannot be assumed to be enabled and disabled at the same time.");
        }
        this.checker = checker;
        this.factory = factory;
    }

    /** Build the control flow graph of some code. */
    @Override
    public ControlFlowGraph run(
            CompilationUnitTree root, ProcessingEnvironment env, UnderlyingAST underlyingAST) {
        declaredClasses.clear();
        declaredLambdas.clear();

        CFTreeBuilder builder = new CFTreeBuilder(env);
        PhaseOneResult phase1result =
                new CFCFGTranslationPhaseOne()
                        .process(root, env, underlyingAST, exceptionalExitLabel, builder, factory);
        ControlFlowGraph phase2result = new CFGTranslationPhaseTwo().process(phase1result);
        ControlFlowGraph phase3result = CFGTranslationPhaseThree.process(phase2result);
        return phase3result;
    }

    /*
     * Given a SourceChecker and an AssertTree, returns whether the AssertTree
     * uses an @AssumeAssertion string that is relevant to the SourceChecker.
     */
    public static boolean assumeAssertionsActivatedForAssertTree(
            SourceChecker checker, AssertTree tree) {
        ExpressionTree detail = tree.getDetail();
        if (detail != null) {
            String msg = detail.toString();
            Collection<String> warningKeys = checker.getSuppressWarningsKeys();
            for (String warningKey : warningKeys) {
                String key = "@AssumeAssertion(" + warningKey + ")";
                if (msg.contains(key)) {
                    return true;
                }
            }
        }

        return false;
    }

    public class CFCFGTranslationPhaseOne extends CFGTranslationPhaseOne {

        @Override
        protected boolean assumeAssertionsEnabledFor(AssertTree tree) {
            if (assumeAssertionsActivatedForAssertTree(checker, tree)) {
                return true;
            }
            return super.assumeAssertionsEnabledFor(tree);
        }

        @Override
        public void handleArtificialTree(Tree tree) {
            // Record the method or class that encloses the newly created tree.
            MethodTree enclosingMethod = TreeUtils.enclosingMethod(getCurrentPath());
            if (enclosingMethod != null) {
                Element methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
                factory.setPathHack(tree, methodElement);
            } else {
                ClassTree enclosingClass = TreeUtils.enclosingClass(getCurrentPath());
                if (enclosingClass != null) {
                    Element classElement = TreeUtils.elementFromDeclaration(enclosingClass);
                    factory.setPathHack(tree, classElement);
                }
            }
        }

        @Override
        protected VariableTree createEnhancedForLoopIteratorVariable(
                MethodInvocationTree iteratorCall, VariableElement variableElement) {
            // We do not want to cache flow-insensitive types
            // retrieved during CFG building.
            boolean oldShouldCache = factory.shouldCache;
            factory.shouldCache = false;
            AnnotatedTypeMirror annotatedIteratorType = factory.getAnnotatedType(iteratorCall);
            factory.shouldCache = oldShouldCache;

            Tree annotatedIteratorTypeTree =
                    ((CFTreeBuilder) treeBuilder).buildAnnotatedType(annotatedIteratorType);
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
            // We do not want to cache flow-insensitive types
            // retrieved during CFG building.
            boolean oldShouldCache = factory.shouldCache;
            factory.shouldCache = false;
            AnnotatedTypeMirror annotatedArrayType = factory.getAnnotatedType(expression);
            factory.shouldCache = oldShouldCache;

            assert (annotatedArrayType instanceof AnnotatedTypeMirror.AnnotatedArrayType)
                    : "ArrayType must be represented by AnnotatedArrayType";

            Tree annotatedArrayTypeTree =
                    ((CFTreeBuilder) treeBuilder).buildAnnotatedType(annotatedArrayType);
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
