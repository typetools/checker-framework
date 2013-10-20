package polyall;

import checkers.basetype.BaseAnnotatedTypeFactory;
import checkers.basetype.BaseTypeChecker;
import checkers.types.QualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

public class PolyAllAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public PolyAllAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new MultiGraphQualifierHierarchy(factory);
    }
}