package javax.lang.model.element;

import java.util.List;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import checkers.javari.quals.*;

public interface TypeElement extends Element {
    NestingKind getNestingKind() @ReadOnly;
    @PolyRead Name getQualifiedName() @PolyRead;
    TypeMirror getSuperclass();
    @PolyRead List<? extends TypeMirror> getInterfaces() @PolyRead;
    @PolyRead List<? extends TypeParameterElement> getTypeParameters() @PolyRead;
}
