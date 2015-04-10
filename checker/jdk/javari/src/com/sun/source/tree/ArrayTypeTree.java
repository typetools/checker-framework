package com.sun.source.tree;
import org.checkerframework.checker.javari.qual.*;

public interface ArrayTypeTree extends Tree {
    @PolyRead Tree getType(@PolyRead ArrayTypeTree this);
}
