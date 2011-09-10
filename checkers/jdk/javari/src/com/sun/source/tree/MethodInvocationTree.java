package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface MethodInvocationTree extends ExpressionTree {
    @PolyRead List<? extends Tree> getTypeArguments() @PolyRead;
    @PolyRead ExpressionTree getMethodSelect() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getArguments() @PolyRead;
}
