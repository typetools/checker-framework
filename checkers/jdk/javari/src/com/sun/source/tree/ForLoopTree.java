package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface ForLoopTree extends StatementTree {
    @PolyRead List<? extends StatementTree> getInitializer(@PolyRead ForLoopTree this);
    @PolyRead ExpressionTree getCondition(@PolyRead ForLoopTree this);
    @PolyRead List<? extends ExpressionStatementTree> getUpdate(@PolyRead ForLoopTree this);
    @PolyRead StatementTree getStatement(@PolyRead ForLoopTree this);
}
