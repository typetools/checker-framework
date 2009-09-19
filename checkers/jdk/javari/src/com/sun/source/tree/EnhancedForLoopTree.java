package com.sun.source.tree;
import checkers.javari.quals.*;

public interface EnhancedForLoopTree extends StatementTree {
    VariableTree getVariable();
    ExpressionTree getExpression();
    StatementTree getStatement();
}
