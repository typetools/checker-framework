package checkers.flow.analysis.checkers;

import java.util.Collection;
import java.util.LinkedList;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import javacutils.InternalUtils;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.cfg.CFGBuilder;
import checkers.flow.cfg.ControlFlowGraph;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.quals.TerminatesExecution;
import checkers.types.AnnotatedTypeFactory;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * A control-flow graph builder (see {@link CFGBuilder}) that knows about the
 * {@link TerminatesExecution} annotation.
 *
 * @author Stefan Heule
 */
public class CFCFGBuilder extends CFGBuilder {

    /** The associated checker. */
    protected final BaseTypeChecker checker;

    public CFCFGBuilder(BaseTypeChecker checker) {
        super(checker.getProcessingEnvironment().getOptions()
                .containsKey("assumeAssertionsAreEnabled"), checker
                .getProcessingEnvironment().getOptions()
                .containsKey("assumeAssertionsAreDisabled"));
        if (assumeAssertionsEnabled && assumeAssertionsDisabled) {
            BaseTypeChecker
                    .errorAbort("Assertions cannot be assumed to be enabled and disabled at the same time.");
        }
        this.checker = checker;
    }

    /**
     * Build the control flow graph of some code.
     */
    @Override
    public ControlFlowGraph run(AnnotatedTypeFactory factory,
            CompilationUnitTree root, ProcessingEnvironment env,
            UnderlyingAST underlyingAST) {
        declaredClasses = new LinkedList<>();
        PhaseOneResult phase1result = new CFCFGTranslationPhaseOne().process(
                factory, root, env, underlyingAST, exceptionalExitLabel);
        ControlFlowGraph phase2result = new CFGTranslationPhaseTwo()
                .process(phase1result);
        ControlFlowGraph phase3result = CFGTranslationPhaseThree
                .process(phase2result);
        return phase3result;
    }

    public class CFCFGTranslationPhaseOne extends CFGTranslationPhaseOne {

        /**
         * Change behavior such that the {@link TerminatesExecution} annotation
         * is recognized.
         */
        @Override
        public MethodInvocationNode visitMethodInvocation(
                MethodInvocationTree tree, Void p) {
            MethodInvocationNode mi = super.visitMethodInvocation(tree, p);
            ExtendedNode extendedMethodNode = nodeList.get(nodeList.size() - 1);
            Element methodElement = InternalUtils.symbol(tree);
            boolean terminatesExecution = factory.getDeclAnnotation(
                    methodElement, TerminatesExecution.class) != null;
            if (terminatesExecution) {
                extendedMethodNode.setTerminatesExecution(true);
            }
            return mi;
        }

        @Override
        protected boolean assumeAssertionsEnabledFor(AssertTree tree) {
            ExpressionTree detail = tree.getDetail();
            if (detail != null) {
                String msg = detail.toString();
                Collection<String> warningKeys = checker
                        .getSuppressWarningsKey();
                for (String warningKey : warningKeys) {
                    String key = "@AssumeAssertion(" + warningKey + ")";
                    if (msg.contains(key)) {
                        return true;
                    }
                }
            }
            return super.assumeAssertionsEnabledFor(tree);
        }
    }
}
