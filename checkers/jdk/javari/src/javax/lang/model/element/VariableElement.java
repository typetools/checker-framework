package javax.lang.model.element;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import checkers.javari.quals.*;

public interface VariableElement extends Element {
    @PolyRead Object getConstantValue(@PolyRead VariableElement this);
}
