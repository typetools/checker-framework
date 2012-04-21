package javax.lang.model.element;

import java.util.List;
import javax.lang.model.util.Types;
import javax.lang.model.type.*;

import checkers.javari.quals.*;

public interface ExecutableElement extends Element, Parameterizable {
    @PolyRead List<? extends TypeParameterElement> getTypeParameters(@PolyRead ExecutableElement this);
    TypeMirror getReturnType(@ReadOnly ExecutableElement this);
    @PolyRead List<? extends VariableElement> getParameters(@PolyRead ExecutableElement this);
    boolean isVarArgs(@ReadOnly ExecutableElement this);
    @PolyRead List<? extends TypeMirror> getThrownTypes(@PolyRead ExecutableElement this);
    @PolyRead AnnotationValue getDefaultValue(@PolyRead ExecutableElement this);
}
