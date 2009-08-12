package com.sun.source.tree;
import checkers.javari.quals.*;

public interface ArrayTypeTree extends Tree {
    @PolyRead ModifiersTree getModifiers() @PolyRead;
    @PolyRead Tree getType() @PolyRead;
}
