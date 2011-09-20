package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface CaseTree extends Tree {
    @PolyRead ExpressionTree getExpression(@PolyRead CaseTree this);
    @PolyRead List<? extends StatementTree> getStatements(@PolyRead CaseTree this);
}
