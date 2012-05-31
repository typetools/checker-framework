package com.sun.source.tree;

import checkers.javari.quals.*;

public interface WildcardTree extends Tree {
    @PolyRead Tree getBound(@PolyRead WildcardTree this);
}
