package javax.lang.model.element;

import java.util.List;
import javax.lang.model.util.Types;
import javax.lang.model.type.*;

import checkers.javari.quals.*;

public interface ExecutableElement extends Element, Parameterizable {
    @PolyRead List<? extends TypeParameterElement> getTypeParameters() @PolyRead;
    TypeMirror getReturnType() @ReadOnly;
    @PolyRead List<? extends VariableElement> getParameters() @PolyRead;
    boolean isVarArgs() @ReadOnly;
    @PolyRead List<? extends TypeMirror> getThrownTypes() @PolyRead;
    @PolyRead AnnotationValue getDefaultValue() @PolyRead;
}
