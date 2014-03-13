package tests.reflection;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;

/**
 * Checker for a simple type system to test reflection resolution.
 * 
 * @author rjust
 */
public class ReflectionTestChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ReflectionTestVisitor(this);
    }

}
