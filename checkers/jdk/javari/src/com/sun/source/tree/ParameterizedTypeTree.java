package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface ParameterizedTypeTree extends Tree {
    @PolyRead Tree getType(@PolyRead ParameterizedTypeTree this);
    @PolyRead List<? extends Tree> getTypeArguments(@PolyRead ParameterizedTypeTree this);
}
