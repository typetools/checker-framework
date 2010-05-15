package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface AnnotationTree extends ExpressionTree {
    @PolyRead Tree getAnnotationType() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getArguments() @PolyRead;
}
