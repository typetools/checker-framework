package org.checkerframework.dataflow.cfg;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import org.plumelib.util.UniqueId;
import org.plumelib.util.UtilPlume;

/**
 * Represents an abstract syntax tree of type {@link Tree} that underlies a given control flow
 * graph.
 */
public abstract class UnderlyingAST implements UniqueId {
    public enum Kind {
        /** The underlying code is a whole method. */
        METHOD,
        /** The underlying code is a lambda expression. */
        LAMBDA,

        /** The underlying code is an arbitrary Java statement or expression. */
        ARBITRARY_CODE,
    }

    protected final Kind kind;

    /** The unique ID of this object. */
    final transient long uid = UniqueId.nextUid.getAndIncrement();

    @Override
    public long getUid() {
        return uid;
    }

    protected UnderlyingAST(Kind kind) {
        this.kind = kind;
    }

    /**
     * Returns the code that corresponds to the CFG.
     *
     * @return the code that corresponds to the CFG
     */
    public abstract Tree getCode();

    public Kind getKind() {
        return kind;
    }

    /** If the underlying AST is a method. */
    public static class CFGMethod extends UnderlyingAST {

        /** The method declaration. */
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

        /**
         * Returns the simple name of the enclosing class.
         *
         * @return the simple name of the enclosing class
         */
        public String getSimpleClassName() {
            return classTree.getSimpleName().toString();
        }

        @Override
        public String toString() {
            return UtilPlume.joinLines("CFGMethod(", method, ")");
        }
    }

    /** If the underlying AST is a lambda. */
    public static class CFGLambda extends UnderlyingAST {

        /** The lambda expression. */
        private final LambdaExpressionTree lambda;

        /** The enclosing class of the lambda. */
        private final ClassTree classTree;

        /** The enclosing method of the lambda. */
        private final MethodTree method;

        /**
         * Create a new CFGLambda.
         *
         * @param lambda the lambda expression
         * @param classTree the enclosing class of the lambda
         * @param method the enclosing method of the lambda
         */
        public CFGLambda(LambdaExpressionTree lambda, ClassTree classTree, MethodTree method) {
            super(Kind.LAMBDA);
            this.lambda = lambda;
            this.method = method;
            this.classTree = classTree;
        }

        @Override
        public Tree getCode() {
            return lambda.getBody();
        }

        /**
         * Returns the lambda expression tree.
         *
         * @return the lambda expression tree
         */
        public LambdaExpressionTree getLambdaTree() {
            return lambda;
        }

        /**
         * Returns the enclosing class of the lambda.
         *
         * @return the enclosing class of the lambda
         */
        public ClassTree getClassTree() {
            return classTree;
        }

        /**
         * Returns the simple name of the enclosing class.
         *
         * @return the simple name of the enclosing class
         */
        public String getSimpleClassName() {
            return classTree.getSimpleName().toString();
        }

        /**
         * Returns the enclosing method of the lambda.
         *
         * @return the enclosing method of the lambda
         */
        public MethodTree getMethod() {
            return method;
        }

        @Override
        public String toString() {
            return UtilPlume.joinLines("CFGLambda(", lambda, ")");
        }
    }

    /** If the underlying AST is a statement or expression. */
    public static class CFGStatement extends UnderlyingAST {

        protected final Tree code;

        /** The class tree this method belongs to. */
        protected final ClassTree classTree;

        public CFGStatement(Tree code, ClassTree classTree) {
            super(Kind.ARBITRARY_CODE);
            this.code = code;
            this.classTree = classTree;
        }

        @Override
        public Tree getCode() {
            return code;
        }

        public ClassTree getClassTree() {
            return classTree;
        }

        /**
         * Returns the simple name of the enclosing class.
         *
         * @return the simple name of the enclosing class
         */
        public String getSimpleClassName() {
            return classTree.getSimpleName().toString();
        }

        @Override
        public String toString() {
            return UtilPlume.joinLines("CFGStatement(", code, ")");
        }
    }
}
