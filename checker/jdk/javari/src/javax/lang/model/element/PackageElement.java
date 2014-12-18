package javax.lang.model.element;

import org.checkerframework.checker.javari.qual.*;

public interface PackageElement extends Element, QualifiedNameable {
    @PolyRead Name getQualifiedName(@PolyRead PackageElement this);
    boolean isUnnamed(@ReadOnly PackageElement this);
}
