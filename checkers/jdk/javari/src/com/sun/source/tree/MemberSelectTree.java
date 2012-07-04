package com.sun.source.tree;

import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface MemberSelectTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead MemberSelectTree this);
    @PolyRead Name getIdentifier(@PolyRead MemberSelectTree this);
}
