package checkers.flow.analysis.checkers;

import java.util.LinkedList;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import checkers.flow.cfg.CFGBuilder;
import checkers.flow.cfg.ControlFlowGraph;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.quals.TerminatesExecution;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.InternalUtils;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * A control-flow graph builder (see {@link CFGBuilder}) that knows about the
 * {@link TerminatesExecution} annotation.
 * 
 * @author Stefan Heule
 */
public class CFCFGBuilder extends CFGBuilder {

    protected AnnotatedTypeFactory factory;

    public CFCFGBuilder(AnnotatedTypeFactory factory) {
        this.factory = factory;
    }

    /**
     * Build the control flow graph of some code.
     */
    @Override
    public ControlFlowGraph run(CompilationUnitTree root,
            ProcessingEnvironment env, UnderlyingAST underlyingAST) {
        declaredClasses = new LinkedList<>();
        PhaseOneResult phase1result = new CFCFGTranslationPhaseOne().process(
                 root, env, underlyingAST, exceptionalExitLabel);
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
    }
}
