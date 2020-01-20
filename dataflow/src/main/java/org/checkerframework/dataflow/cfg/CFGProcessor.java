package org.checkerframework.dataflow.cfg;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BasicTypeProcessor;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This processor runs during compilation to generate the control flow graph of a given method in a
 * given class. See {@link CFGVisualizeLauncher} for the usage.
 */
@SupportedAnnotationTypes("*")
public class CFGProcessor extends BasicTypeProcessor {

    /** Name of a specified class which includes a specified method to generate the CFG for. */
    private final String className;
    /** Name of a specified method to generate the CFG for. */
    private final String methodName;

    /** AST for source file. */
    private @Nullable CompilationUnitTree rootTree;
    /** Tree node for the specified class. */
    private @Nullable ClassTree classTree;
    /** Tree node for the specified method. */
    private @Nullable MethodTree methodTree;

    /** Result of CFG process. */
    private @Nullable CFGProcessResult result;

    /**
     * Create a CFG processor.
     *
     * @param className the name of class which includes the specified method to generate the CFG
     *     for
     * @param methodName the name of method to generate the CFG for
     */
    protected CFGProcessor(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
        this.result = null;
    }

    /**
     * Get the CFG process result.
     *
     * @return result of cfg process
     */
    public final @Nullable CFGProcessResult getCFGProcessResult() {
        return this.result;
    }

    @Override
    public void typeProcessingOver() {
        if (rootTree == null) {
            this.result = new CFGProcessResult("Root tree is null.");
        } else if (classTree == null) {
            this.result = new CFGProcessResult("Method tree is null.");
        } else if (methodTree == null) {
            this.result = new CFGProcessResult("Class tree is null.");
        } else {
            ControlFlowGraph cfg = CFGBuilder.build(rootTree, methodTree, classTree, processingEnv);
            this.result = new CFGProcessResult(cfg);
        }
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
                    // stop execution by throwing an exception. this
                    // makes sure that compilation does not proceed, and
                    // thus the AST is not modified by further phases of
                    // the compilation (and we save the work to do the
                    // compilation).
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

    /** The result of CFG process, contains the control flow graph when it is succeed. */
    public static class CFGProcessResult {
        /** Control flow graph. */
        private final @Nullable ControlFlowGraph controlFlowGraph;
        /** Is the CFG process succeed or not. */
        private final boolean isSuccess;
        /** Error message (When result is failed). */
        private final @Nullable String errMsg;

        /**
         * Create the result of CFG process. Only called if CFG is built successfully.
         *
         * @param cfg control flow graph
         */
        CFGProcessResult(final ControlFlowGraph cfg) {
            this(cfg, true, null);
        }

        /**
         * Create the result of CFG process. Only called if CFG is built unsuccessfully.
         *
         * @param errMsg the error message to show
         */
        CFGProcessResult(final String errMsg) {
            this(null, false, errMsg);
        }

        /**
         * Create the result of CFG process.
         *
         * @param cfg Control flow graph
         * @param isSuccess Is a success or not
         * @param errMsg Error message (When result is failed)
         */
        CFGProcessResult(
                @Nullable ControlFlowGraph cfg, boolean isSuccess, @Nullable String errMsg) {
            this.controlFlowGraph = cfg;
            this.isSuccess = isSuccess;
            this.errMsg = errMsg;
        }

        /** Check if the CFG process result is succeed. */
        @EnsuresNonNullIf(expression = "getCFG()", result = true)
        @SuppressWarnings("nullness:contracts.conditional.postcondition.not.satisfied")
        public boolean isSuccess() {
            return isSuccess;
        }

        /** Get the generated control flow graph. */
        public @Nullable ControlFlowGraph getCFG() {
            return controlFlowGraph;
        }

        /** Get the error message. */
        public @Nullable String getErrMsg() {
            return errMsg;
        }
    }
}
