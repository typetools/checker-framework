package javax.lang.model.element;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.IncompleteAnnotationException;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import checkers.javari.quals.*;

public interface Element {
    TypeMirror asType() @ReadOnly;
    ElementKind getKind() @ReadOnly;
    List<? extends AnnotationMirror> getAnnotationMirrors() @ReadOnly;
    <A extends Annotation> @PolyRead A getAnnotation(Class<A> annotationType) @PolyRead;
    @PolyRead Set<Modifier> getModifiers() @PolyRead;
    @PolyRead Name getSimpleName() @PolyRead;
    @PolyRead Element getEnclosingElement() @PolyRead;
    @PolyRead List<? extends Element> getEnclosedElements() @PolyRead;
    boolean equals(@ReadOnly Object obj) @ReadOnly;
    int hashCode() @ReadOnly;
    <R, P> R accept(ElementVisitor<R, P> v, P p) @ReadOnly;
}
