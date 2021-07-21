package org.checkerframework.dataflow.cfg;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.util.Log;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.javacutil.BasicTypeProcessor;
import org.checkerframework.javacutil.TreeUtils;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Generate the control flow graph of a given method in a given class. See {@link
 * org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher} for example usage.
 */
@SupportedAnnotationTypes("*")
public class CFGProcessor extends BasicTypeProcessor {

    /**
     * Qualified name of a specified class which includes a specified method to generate the CFG
     * for.
     */
    private final String className;
    /** Name of a specified method to generate the CFG for. */
    private final String methodName;

    /** AST for source file. */
    private @Nullable CompilationUnitTree rootTree;
    /** Tree node for the specified class. */
    private @Nullable ClassTree classTree;
    /** Tree node for the specified method. */
    private @Nullable MethodTree methodTree;

    /** Result of CFG process; is set by {@link #typeProcessingOver}. */
    private @MonotonicNonNull CFGProcessResult result = null;

    /**
     * Create a CFG processor.
     *
     * @param className the qualified name of class which includes the specified method to generate
     *     the CFG for
     * @param methodName the name of the method to generate the CFG for
     */
    public CFGProcessor(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    /**
     * Get the CFG process result.
     *
     * @return result of cfg process
     */
    public final @Nullable CFGProcessResult getCFGProcessResult() {
        return result;
    }

    @Override
    public void typeProcessingOver() {
        if (rootTree == null) {
            result = new CFGProcessResult("Root tree is null.");
        } else if (classTree == null) {
            result = new CFGProcessResult("Method tree is null.");
        } else if (methodTree == null) {
            result = new CFGProcessResult("Class tree is null.");
        } else {
            Log log = getCompilerLog();
            if (log.nerrors > 0) {
                result = new CFGProcessResult("Compilation issued an error.");
            } else {
                ControlFlowGraph cfg =
                        CFGBuilder.build(rootTree, methodTree, classTree, processingEnv);
                result = new CFGProcessResult(cfg);
            }
        }
        super.typeProcessingOver();
    }

    @Override
    protected TreePathScanner<?, ?> createTreePathScanner(CompilationUnitTree root) {
        rootTree = root;
        return new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void p) {
                TypeElement el = TreeUtils.elementFromDeclaration(node);
                if (el.getSimpleName().contentEquals(className)) {
                    classTree = node;
                }
                return super.visitClass(node, p);
            }

            @Override
            public Void visitMethod(MethodTree node, Void p) {
                ExecutableElement el = TreeUtils.elementFromDeclaration(node);
                if (el.getSimpleName().contentEquals(methodName)) {
                    methodTree = node;
                    // Stop execution by throwing an exception. This makes sure that compilation
                    // does not proceed, and thus the AST is not modified by further phases of the
                    // compilation (and we save the work to do the compilation).
                    throw new RuntimeException();
                }
                return null;
            }
        };
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /** The result of the CFG process, contains the control flow graph when successful. */
    public static class CFGProcessResult {
        /** Control flow graph. */
        private final @Nullable ControlFlowGraph controlFlowGraph;
        /** Did the CFG process succeed? */
        private final boolean isSuccess;
        /** Error message (when the CFG process failed). */
        private final @Nullable String errMsg;

        /**
         * Create the result of the CFG process. Only called if the CFG was built successfully.
         *
         * @param cfg control flow graph
         */
        CFGProcessResult(final ControlFlowGraph cfg) {
            this(cfg, true, null);
        }

        /**
         * Create the result of the CFG process. Only called if the CFG was not built successfully.
         *
         * @param errMsg the error message
         */
        CFGProcessResult(final String errMsg) {
            this(null, false, errMsg);
        }

        /**
         * Create the result of CFG process.
         *
         * @param cfg the control flow graph
         * @param isSuccess did the CFG process succeed?
         * @param errMsg error message (when the CFG process failed)
         */
        private CFGProcessResult(
                @Nullable ControlFlowGraph cfg, boolean isSuccess, @Nullable String errMsg) {
            this.controlFlowGraph = cfg;
            this.isSuccess = isSuccess;
            this.errMsg = errMsg;
        }

        /** Check if the CFG process succeeded. */
        @Pure
        @EnsuresNonNullIf(expression = "getCFG()", result = true)
        // TODO: add once #1307 is fixed
        // @EnsuresNonNullIf(expression = "getErrMsg()", result = false)
        @SuppressWarnings("nullness:contracts.conditional.postcondition.not.satisfied")
        public boolean isSuccess() {
            return isSuccess;
        }

        /**
         * Returns the generated control flow graph.
         *
         * @return the generated control flow graph
         */
        @Pure
        public @Nullable ControlFlowGraph getCFG() {
            return controlFlowGraph;
        }

        /**
         * Returns the error message.
         *
         * @return the error message
         */
        @Pure
        public @Nullable String getErrMsg() {
            return errMsg;
        }
    }
}
