package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface ParameterizedTypeTree extends Tree {
    @PolyRead Tree getType() @PolyRead;
    @PolyRead List<? extends Tree> getTypeArguments() @PolyRead;
}
