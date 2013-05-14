package dataflow.cfg;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * Represents an abstract syntax tree of type {@link Tree} that underlies a
 * given control flow graph.
 * 
 * @author Stefan Heule
 * 
 */
public abstract class UnderlyingAST {
    public enum Kind {
        /** The underlying code is a whole method */
        METHOD,

        /**
         * The underlying code is an arbitrary Java statement or expression
         */
        ARBITRARY_CODE,
    }

    protected final Kind kind;

    public UnderlyingAST(Kind kind) {
        this.kind = kind;
    }

    /**
     * @return The code that corresponds to the CFG.
     */
    abstract public Tree getCode();

    public Kind getKind() {
        return kind;
    }

    /**
     * If the underlying AST is a method.
     */
    public static class CFGMethod extends UnderlyingAST {

        /** The method declaration */
        protected final MethodTree method;
        
        /** The class tree this method belongs to. */
        protected final ClassTree classTree;

        public CFGMethod(MethodTree method, ClassTree classTree) {
            super(Kind.METHOD);
            this.method = method;
            this.classTree = classTree;
        }

        @Override
        public Tree getCode() {
            return method.getBody();
        }

        public MethodTree getMethod() {
            return method;
        }
        
        public ClassTree getClassTree() {
            return classTree;
        }

    }

    /**
     * If the underlying AST is a statement or expression.
     */
    public static class CFGStatement extends UnderlyingAST {

        protected final Tree code;

        public CFGStatement(Tree code) {
            super(Kind.ARBITRARY_CODE);
            this.code = code;
        }

        @Override
        public Tree getCode() {
            return code;
        }
    }
}
