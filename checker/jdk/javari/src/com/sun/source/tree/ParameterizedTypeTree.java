package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface ParameterizedTypeTree extends Tree {
    @PolyRead Tree getType(@PolyRead ParameterizedTypeTree this);
    @PolyRead List<? extends Tree> getTypeArguments(@PolyRead ParameterizedTypeTree this);
}
