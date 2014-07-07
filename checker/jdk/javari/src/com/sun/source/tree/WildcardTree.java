package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface WildcardTree extends Tree {
    @PolyRead Tree getBound(@PolyRead WildcardTree this);
}
