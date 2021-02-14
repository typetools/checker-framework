package org.checkerframework.dataflow.cfg.builder;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlockImpl;
import org.checkerframework.dataflow.cfg.block.ExceptionBlockImpl;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlockImpl;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BasicAnnotationProvider;
import org.checkerframework.javacutil.trees.TreeBuilder;

/**
 * Builds the control flow graph of some Java code (either a method, or an arbitrary statement).
 *
 * <p>The translation of the AST to the CFG is split into three phases:
 *
 * <ol>
 *   <li><em>Phase one.</em> In the first phase, the AST is translated into a sequence of {@link
 *       org.checkerframework.dataflow.cfg.builder.ExtendedNode}s. An extended node can either be a
 *       {@link Node}, or one of several meta elements such as a conditional or unconditional jump
 *       or a node with additional information about exceptions. Some of the extended nodes contain
 *       labels (e.g., for the jump target), and phase one additionally creates a mapping from
 *       labels to extended nodes. Finally, the list of leaders is computed: A leader is an extended
 *       node which will give rise to a basic block in phase two.
 *   <li><em>Phase two.</em> In this phase, the sequence of extended nodes is translated to a graph
 *       of control flow blocks that contain nodes. The meta elements from phase one are translated
 *       into the correct edges.
 *   <li><em>Phase three.</em> The control flow graph generated in phase two can contain degenerate
 *       basic blocks such as empty regular basic blocks or conditional basic blocks that have the
 *       same block as both 'then' and 'else' successor. This phase removes these cases while
 *       preserving the control flow structure.
 * </ol>
 */
public abstract class CFGBuilder {

    /**
     * Build the control flow graph of some code.
     *
     * @param root the compilation unit
     * @param underlyingAST the AST that underlies the control frow graph
     * @param assumeAssertionsDisabled can assertions be assumed to be disabled?
     * @param assumeAssertionsEnabled can assertions be assumed to be enabled?
     * @param env annotation processing environment containing type utilities
     * @return a control flow graph
     */
    public static ControlFlowGraph build(
            CompilationUnitTree root,
            UnderlyingAST underlyingAST,
            boolean assumeAssertionsEnabled,
            boolean assumeAssertionsDisabled,
            ProcessingEnvironment env) {
        TreeBuilder builder = new TreeBuilder(env);
        AnnotationProvider annotationProvider = new BasicAnnotationProvider();
        PhaseOneResult phase1result =
                new CFGTranslationPhaseOne(
                                builder,
                                annotationProvider,
                                assumeAssertionsEnabled,
                                assumeAssertionsDisabled,
                                env)
                        .process(root, underlyingAST);
        ControlFlowGraph phase2result = CFGTranslationPhaseTwo.process(phase1result);
        ControlFlowGraph phase3result = CFGTranslationPhaseThree.process(phase2result);
        return phase3result;
    }

    /**
     * Build the control flow graph of some code (method, initializer block, ...). bodyPath is the
     * TreePath to the body of that code.
     */
    public static ControlFlowGraph build(
            TreePath bodyPath,
            UnderlyingAST underlyingAST,
            boolean assumeAssertionsEnabled,
            boolean assumeAssertionsDisabled,
            ProcessingEnvironment env) {
        TreeBuilder builder = new TreeBuilder(env);
        AnnotationProvider annotationProvider = new BasicAnnotationProvider();
        PhaseOneResult phase1result =
                new CFGTranslationPhaseOne(
                                builder,
                                annotationProvider,
                                assumeAssertionsEnabled,
                                assumeAssertionsDisabled,
                                env)
                        .process(bodyPath, underlyingAST);
        ControlFlowGraph phase2result = CFGTranslationPhaseTwo.process(phase1result);
        ControlFlowGraph phase3result = CFGTranslationPhaseThree.process(phase2result);
        return phase3result;
    }

    /** Build the control flow graph of some code. */
    public static ControlFlowGraph build(
            CompilationUnitTree root, UnderlyingAST underlyingAST, ProcessingEnvironment env) {
        return build(root, underlyingAST, false, false, env);
    }

    /** Build the control flow graph of a method. */
    public static ControlFlowGraph build(
            CompilationUnitTree root,
            MethodTree tree,
            ClassTree classTree,
            ProcessingEnvironment env) {
        UnderlyingAST underlyingAST = new CFGMethod(tree, classTree);
        return build(root, underlyingAST, false, false, env);
    }

    /**
     * Return a printed representation of a collection of extended nodes.
     *
     * @param nodes a collection of extended nodes to format
     * @return a printed representation of the given collection
     */
    public static String extendedNodeCollectionToStringDebug(
            Collection<? extends ExtendedNode> nodes) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (ExtendedNode n : nodes) {
            result.add(n.toStringDebug());
        }
        return result.toString();
    }

    static <A> A firstNonNull(A first, A second) {
        if (first != null) {
            return first;
        } else if (second != null) {
            return second;
        } else {
            throw new NullPointerException();
        }
    }

    /* --------------------------------------------------------- */
    /* Utility routines for debugging CFG building */
    /* --------------------------------------------------------- */

    /**
     * Print a set of {@link Block}s and the edges between them. This is useful for examining the
     * results of phase two.
     *
     * @param blocks the blocks to print
     */
    protected static void printBlocks(Set<Block> blocks) {
        for (Block b : blocks) {
            System.out.print(b.getUid() + ": " + b);
            switch (b.getType()) {
                case REGULAR_BLOCK:
                case SPECIAL_BLOCK:
                    {
                        Block succ = ((SingleSuccessorBlockImpl) b).getSuccessor();
                        System.out.println(" -> " + (succ != null ? succ.getUid() : "||"));
                        break;
                    }
                case EXCEPTION_BLOCK:
                    {
                        Block succ = ((SingleSuccessorBlockImpl) b).getSuccessor();
                        System.out.print(" -> " + (succ != null ? succ.getUid() : "||") + " {");
                        for (Map.Entry<TypeMirror, Set<Block>> entry :
                                ((ExceptionBlockImpl) b).getExceptionalSuccessors().entrySet()) {
                            System.out.print(entry.getKey() + " : " + entry.getValue() + ", ");
                        }
                        System.out.println("}");
                        break;
                    }
                case CONDITIONAL_BLOCK:
                    {
                        Block tSucc = ((ConditionalBlockImpl) b).getThenSuccessor();
                        Block eSucc = ((ConditionalBlockImpl) b).getElseSuccessor();
                        System.out.println(
                                " -> T "
                                        + (tSucc != null ? tSucc.getUid() : "||")
                                        + " F "
                                        + (eSucc != null ? eSucc.getUid() : "||"));
                        break;
                    }
            }
        }
    }
}
