package com.sun.source.tree;
import org.checkerframework.checker.javari.qual.*;

public interface EnhancedForLoopTree extends StatementTree {
    VariableTree getVariable();
    ExpressionTree getExpression();
    StatementTree getStatement();
}
