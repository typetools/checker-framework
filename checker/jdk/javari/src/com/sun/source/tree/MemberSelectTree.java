package com.sun.source.tree;

import javax.lang.model.element.Name;
import org.checkerframework.checker.javari.qual.*;

public interface MemberSelectTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead MemberSelectTree this);
    @PolyRead Name getIdentifier(@PolyRead MemberSelectTree this);
}
