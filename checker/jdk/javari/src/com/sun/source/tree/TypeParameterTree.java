package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

import org.checkerframework.checker.javari.qual.*;

public interface TypeParameterTree extends Tree {
    @PolyRead Name getName(@PolyRead TypeParameterTree this);
    @PolyRead List<? extends AnnotationTree> getAnnotations(@PolyRead TypeParameterTree this);
    @PolyRead List<? extends Tree> getBounds(@PolyRead TypeParameterTree this);
}
