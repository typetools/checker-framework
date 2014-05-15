package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface SwitchTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead SwitchTree this);
    @PolyRead List<? extends CaseTree> getCases(@PolyRead SwitchTree this);
}
