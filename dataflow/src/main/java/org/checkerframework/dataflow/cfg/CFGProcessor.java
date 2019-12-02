package org.checkerframework.dataflow.cfg;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.checkerframework.javacutil.BasicTypeProcessor;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The CFG processor running during compilation to generate the control flow graph of a given method
 * in a given class. See {@link CFGVisualizeLauncher} for the usage.
 */
@SupportedAnnotationTypes("*")
public class CFGProcessor extends BasicTypeProcessor {

    /** Class name. */
    private final String className;
    /** Method name to generate the CFG for. */
    private final String methodName;

    /** AST for source file. */
    private CompilationUnitTree rootTree;
    /** Class Tree. */
    private ClassTree classTree;
    /** Method Tree. */
    private MethodTree methodTree;

    /** Result of CFG process */
    private CFGProcessResult result;

    /**
     * Class constructor.
     *
     * @param className Class name
     * @param methodName Method name
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
    public final CFGProcessResult getCFGProcessResult() {
        return this.result;
    }

    @Override
    public void typeProcessingOver() {
        if (rootTree == null) {
            this.result = new CFGProcessResult(null, false, "root tree is null.");
            return;
        }

        if (classTree == null) {
            this.result = new CFGProcessResult(null, false, "method tree is null.");
            return;
        }

        if (methodTree == null) {
            this.result = new CFGProcessResult(null, false, "class tree is null.");
            return;
        }

        ControlFlowGraph cfg = CFGBuilder.build(rootTree, methodTree, classTree, processingEnv);
        this.result = new CFGProcessResult(cfg);
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
        private final ControlFlowGraph controlFlowGraph;
        /** Is the CFG process succeed or not. */
        private final boolean isSuccess;
        /** errMsg Error message (When result is failed). */
        private final String errMsg;

        /**
         * Class constructor. Only called if CFG is built successfully.
         *
         * @param cfg Control flow graph
         */
        CFGProcessResult(final ControlFlowGraph cfg) {
            this(cfg, true, null);
            assert cfg != null : "this constructor should called if cfg is built successfully.";
        }

        /**
         * Class constructor.
         *
         * @param cfg Control flow graph
         * @param isSuccess Is a success or not
         * @param errMsg Error message (When result is failed)
         */
        CFGProcessResult(ControlFlowGraph cfg, boolean isSuccess, String errMsg) {
            this.controlFlowGraph = cfg;
            this.isSuccess = isSuccess;
            this.errMsg = errMsg;
        }

        /** Check if the CFG process result is succeed. */
        public boolean isSuccess() {
            return isSuccess;
        }

        /** Get the generated control flow graph. */
        public ControlFlowGraph getCFG() {
            return controlFlowGraph;
        }

        /** Get the error message. */
        public String getErrMsg() {
            return errMsg;
        }
    }
}
