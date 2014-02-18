package checkers.value;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;

/**
 * @author plvines
 * 
 */
public class ValueChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ValueVisitor(this);
    }

}
