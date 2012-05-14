package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface ForLoopTree extends StatementTree {
    @PolyRead List<? extends StatementTree> getInitializer() @PolyRead;
    @PolyRead ExpressionTree getCondition() @PolyRead;
    @PolyRead List<? extends ExpressionStatementTree> getUpdate() @PolyRead;
    @PolyRead StatementTree getStatement() @PolyRead;
}
