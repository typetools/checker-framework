package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ImportTree extends Tree {
    boolean isStatic(@ReadOnly ImportTree this);
    @PolyRead Tree getQualifiedIdentifier(@PolyRead ImportTree this);
}
