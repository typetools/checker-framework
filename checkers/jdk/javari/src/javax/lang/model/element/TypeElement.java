package javax.lang.model.element;

import java.util.List;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import checkers.javari.quals.*;

public interface TypeElement extends Element, Parameterizable, QualifiedNameable {
    NestingKind getNestingKind(@ReadOnly TypeElement this);
    @PolyRead Name getQualifiedName(@PolyRead TypeElement this);
    TypeMirror getSuperclass();
    @PolyRead List<? extends TypeMirror> getInterfaces(@PolyRead TypeElement this);
    @PolyRead List<? extends TypeParameterElement> getTypeParameters(@PolyRead TypeElement this);
    @PolyRead List<? extends Element> getEnclosedElements(@PolyRead TypeElement this);
}
