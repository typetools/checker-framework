package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface CaseTree extends Tree {
    @PolyRead ExpressionTree getExpression(@PolyRead CaseTree this);
    @PolyRead List<? extends StatementTree> getStatements(@PolyRead CaseTree this);
}
