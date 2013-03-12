package com.sun.source.tree;
import checkers.javari.quals.*;

public interface ArrayTypeTree extends Tree {
    @PolyRead Tree getType(@PolyRead ArrayTypeTree this);
}
