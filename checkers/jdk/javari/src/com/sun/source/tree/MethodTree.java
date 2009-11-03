package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

import checkers.javari.quals.*;

public interface MethodTree extends Tree {
    @PolyRead ModifiersTree getModifiers() @PolyRead;
    @PolyRead Name getName() @PolyRead;
    @PolyRead Tree getReturnType() @PolyRead;
    @PolyRead List<? extends TypeParameterTree> getTypeParameters() @PolyRead;
    @PolyRead List<? extends VariableTree> getParameters() @PolyRead;
    @PolyRead List<? extends AnnotationTree> getReceiverAnnotations() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getThrows() @PolyRead;
    @PolyRead BlockTree getBody() @PolyRead;
    @PolyRead Tree getDefaultValue() @PolyRead;
}
