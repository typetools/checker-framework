package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface BlockTree extends StatementTree {
    boolean isStatic(@ReadOnly BlockTree this);
    @PolyRead List<? extends StatementTree> getStatements(@PolyRead BlockTree this);
}
