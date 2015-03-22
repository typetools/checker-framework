package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface AnnotationTree extends ExpressionTree {
    @PolyRead Tree getAnnotationType(@PolyRead AnnotationTree this);
    @PolyRead List<? extends ExpressionTree> getArguments(@PolyRead AnnotationTree this);
}
