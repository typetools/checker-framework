package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface SwitchTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead SwitchTree this);
    @PolyRead List<? extends CaseTree> getCases(@PolyRead SwitchTree this);
}
