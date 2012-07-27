package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

import checkers.javari.quals.*;

public interface MethodTree extends Tree {
    @PolyRead ModifiersTree getModifiers(@PolyRead MethodTree this);
    @PolyRead Name getName(@PolyRead MethodTree this);
    @PolyRead Tree getReturnType(@PolyRead MethodTree this);
    @PolyRead List<? extends TypeParameterTree> getTypeParameters(@PolyRead MethodTree this);
    @PolyRead List<? extends VariableTree> getParameters(@PolyRead MethodTree this);
    @PolyRead List<? extends AnnotationTree> getReceiverAnnotations(@PolyRead MethodTree this);
    @PolyRead List<? extends ExpressionTree> getThrows(@PolyRead MethodTree this);
    @PolyRead BlockTree getBody(@PolyRead MethodTree this);
    @PolyRead Tree getDefaultValue(@PolyRead MethodTree this);
}
