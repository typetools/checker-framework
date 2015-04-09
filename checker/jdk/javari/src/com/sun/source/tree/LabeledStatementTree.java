package com.sun.source.tree;

import javax.lang.model.element.Name;
import org.checkerframework.checker.javari.qual.*;

public interface LabeledStatementTree extends StatementTree {
    @PolyRead Name getLabel(@PolyRead LabeledStatementTree this);
    @PolyRead StatementTree getStatement(@PolyRead LabeledStatementTree this);
}
