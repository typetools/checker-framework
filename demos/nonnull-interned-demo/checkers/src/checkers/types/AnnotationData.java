package checkers.types;

import checkers.quals.*;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

/**
 * A general representation for annotations used as type qualifiers.
 * Annotations in this representation will most typically come from annotations
 * in Java source code or compiled Java class files. Others may be created for
 * data in which an annotation has not been written but is implicitly present
 * (e.g., a {@code @NonNull} annotation on a {@link String} literal).
 *
 * @see AnnotationFactory
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public interface AnnotationData {

    /**
     * @return the type of the annotation
     */
    TypeMirror getType();

    /**
     * @return the location of the annotation
     */
    AnnotationLocation getLocation();

    /**
     * @return the values of the annotation's arguments
     */
    Map<? extends ExecutableElement, ? extends AnnotationValue>
        getValues();
}
