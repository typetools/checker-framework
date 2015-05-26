package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface ForLoopTree extends StatementTree {
    @PolyRead List<? extends StatementTree> getInitializer(@PolyRead ForLoopTree this);
    @PolyRead ExpressionTree getCondition(@PolyRead ForLoopTree this);
    @PolyRead List<? extends ExpressionStatementTree> getUpdate(@PolyRead ForLoopTree this);
    @PolyRead StatementTree getStatement(@PolyRead ForLoopTree this);
}
