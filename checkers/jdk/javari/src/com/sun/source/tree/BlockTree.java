package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface BlockTree extends StatementTree {
    boolean isStatic() @ReadOnly;
    @PolyRead List<? extends StatementTree> getStatements() @PolyRead;
}
