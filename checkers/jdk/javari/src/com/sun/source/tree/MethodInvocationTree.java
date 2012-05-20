package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface MethodInvocationTree extends ExpressionTree {
    @PolyRead List<? extends Tree> getTypeArguments(@PolyRead MethodInvocationTree this);
    @PolyRead ExpressionTree getMethodSelect(@PolyRead MethodInvocationTree this);
    @PolyRead List<? extends ExpressionTree> getArguments(@PolyRead MethodInvocationTree this);
}
