package javax.lang.model.element;

import checkers.javari.quals.*;

public interface PackageElement extends Element, QualifiedNameable {
    @PolyRead Name getQualifiedName(@PolyRead PackageElement this);
    boolean isUnnamed(@ReadOnly PackageElement this);
}
