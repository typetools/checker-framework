package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ImportTree extends Tree {
    boolean isStatic() @ReadOnly;
    @PolyRead Tree getQualifiedIdentifier() @PolyRead;
}
