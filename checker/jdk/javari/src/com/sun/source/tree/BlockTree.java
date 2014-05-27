package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface BlockTree extends StatementTree {
    boolean isStatic(@ReadOnly BlockTree this);
    @PolyRead List<? extends StatementTree> getStatements(@PolyRead BlockTree this);
}
