package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface ImportTree extends Tree {
    boolean isStatic(@ReadOnly ImportTree this);
    @PolyRead Tree getQualifiedIdentifier(@PolyRead ImportTree this);
}
