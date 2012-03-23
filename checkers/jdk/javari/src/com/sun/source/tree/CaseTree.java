package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface CaseTree extends Tree {
    @PolyRead ExpressionTree getExpression() @PolyRead;
    @PolyRead List<? extends StatementTree> getStatements() @PolyRead;
}
