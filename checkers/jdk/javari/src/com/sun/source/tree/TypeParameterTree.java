package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

import checkers.javari.quals.*;

public interface TypeParameterTree extends Tree {
    @PolyRead Name getName() @PolyRead;
    @PolyRead List<? extends ModifiersTree> getBoundsModifiers() @PolyRead;
    @PolyRead List<? extends Tree> getBounds() @PolyRead;
}
