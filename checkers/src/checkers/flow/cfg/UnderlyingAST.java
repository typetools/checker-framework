package checkers.flow.cfg;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
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
         * The underlying code is an arbitrary sequence of statements (of type
         * {@link StatementTree})
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
    abstract public StatementTree getCode();

    public Kind getKind() {
        return kind;
    }

    /**
     * If the underlying AST is a method.
     */
    public static class CFGMethod extends UnderlyingAST {

        /** The method declaration */
        protected final MethodTree method;

        public CFGMethod(MethodTree method) {
            super(Kind.METHOD);
            this.method = method;
        }

        @Override
        public StatementTree getCode() {
            return method.getBody();
        }

        public MethodTree getMethod() {
            return method;
        }

    }

    /**
     * If the underlying AST is a statement.
     */
    public static class CFGStatement extends UnderlyingAST {

        /** The sequence of statements */
        protected final StatementTree statement;

        public CFGStatement(StatementTree statement) {
            super(Kind.ARBITRARY_CODE);
            this.statement = statement;
        }

        @Override
        public StatementTree getCode() {
            return statement;
        }
    }
}
