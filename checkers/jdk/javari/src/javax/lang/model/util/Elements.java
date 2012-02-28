package javax.lang.model.util;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

import checkers.javari.quals.*;

public interface Elements {
    @PolyRead PackageElement getPackageElement(@PolyRead CharSequence name);
    @PolyRead TypeElement getTypeElement(@PolyRead CharSequence name);
    @PolyRead Map<? extends ExecutableElement, ? extends AnnotationValue>
        getElementValuesWithDefaults(@PolyRead AnnotationMirror a);
    @PolyRead String getDocComment(@PolyRead Element e);
    boolean isDeprecated(@ReadOnly Element e);
    @PolyRead Name getBinaryName(@PolyRead TypeElement type);
    @PolyRead PackageElement getPackageOf(@PolyRead Element type);
    @PolyRead List<? extends Element> getAllMembers(@PolyRead TypeElement type);
    @PolyRead List<? extends AnnotationMirror> getAllAnnotationMirrors(@PolyRead Element e);
    boolean hides(@ReadOnly Element hider, @ReadOnly Element hidden);
    boolean overrides(@ReadOnly ExecutableElement overrider, @ReadOnly ExecutableElement overridden,
              @ReadOnly TypeElement type);
    @PolyRead String getConstantExpression(@PolyRead Object value);
    void printElements(java.io.Writer w, @ReadOnly Element... elements);
    @PolyRead Name getName(@PolyRead CharSequence cs);
}
