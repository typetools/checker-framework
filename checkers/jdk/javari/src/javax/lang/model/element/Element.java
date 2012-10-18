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
    TypeMirror asType(@ReadOnly Element this);
    ElementKind getKind(@ReadOnly Element this);
    List<? extends AnnotationMirror> getAnnotationMirrors(@ReadOnly Element this);
    <A extends Annotation> @PolyRead A getAnnotation(@PolyRead Element this, Class<A> annotationType);
    @PolyRead Set<Modifier> getModifiers(@PolyRead Element this);
    @PolyRead Name getSimpleName(@PolyRead Element this);
    @PolyRead Element getEnclosingElement(@PolyRead Element this);
    @PolyRead List<? extends Element> getEnclosedElements(@PolyRead Element this);
    boolean equals(@ReadOnly Element this, @ReadOnly Object obj);
    int hashCode(@ReadOnly Element this);
    <R, P> R accept(@ReadOnly Element this, ElementVisitor<R, P> v, P p);
}
