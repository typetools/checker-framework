package com.sun.source.tree;

import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface LabeledStatementTree extends StatementTree {
    @PolyRead Name getLabel(@PolyRead LabeledStatementTree this);
    @PolyRead StatementTree getStatement(@PolyRead LabeledStatementTree this);
}
