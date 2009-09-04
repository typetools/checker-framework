package com.sun.source.tree;

import checkers.javari.quals.*;

public interface WildcardTree extends Tree {
    @PolyRead ModifiersTree getBoundModifiers() @PolyRead;
    @PolyRead Tree getBound() @PolyRead;
}
