package org.checkerframework.common.reflection;

import java.util.LinkedHashSet;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;
import org.checkerframework.common.reflection.qual.ClassValBottom;
import org.checkerframework.common.reflection.qual.UnknownClass;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

/**
* The ClassVal Checker provides a sound estimate of the fully qualified class name
* of Class objects.
*
* Note: A "fully qualified class name" is defined by the JLS 6.7. This checker
* uses that definition except for member classes or interfaces. The fully
* qualified name of a member class or interface is defined as follows: A member
* class or member interface M of another class or interface C fully qualified
* name is the fully qualified name of C, followed by "$", followed by the
* simple name of M.
*
* Annotation semantics:
* @ClassVal(String[] classnames): an expression with this type is a Class object
*   representing a class in the list of fully qualified class names. The strings
*   in this annotation must be a legal class name, but the class may not be on the class path.
* @ClassBound(String[] classnames): an expression with this type is a Class object
*   representing a class or a subclass of a class in the list of fully qualified class names. The strings
*   in this annotation must be a legal class name, but the class may not be on the class path.
* @UnknownClassVal: no estimate of possible names, default qualifier
* @ClassValBottom: qualifier given to the null literal.
*
* Subtyping rules:
* Top: @UnknownClassVal
* Bottom: @ClassValBottom
*
* @ClassVal(A) subtype of @ClassVal(B), if A is a subset of B
* @ClassBound(A) subtype of @ClassBound(B), if A is a subset of B
* @ClassVal(A) subtype of @ClassBound(B), if A is a subset of B
*
* Special typing rules:
* Class.class:           @ClassVal("package.Class")
* Class.forName(expression): @ClassVal(string value(s) of expression found by Value Checker)
* expression.getClass(): @ClassBound("fully qualified name of expression's type")
*
* (Uses the Value Checker)
*/

@TypeQualifiers({ UnknownClass.class, ClassVal.class, ClassBound.class,
        ClassValBottom.class })
public class ClassValChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new ClassValVisitor(this);
    }

    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        //Don't call super otherwise MethodVal will be added as a subChecker
        // which creates a circular dependency.
        LinkedHashSet<Class<? extends BaseTypeChecker>> subCheckers = new LinkedHashSet<>();
        subCheckers.add(ValueChecker.class);
        return subCheckers;
    }
    @Override
    public boolean shouldResolveReflection() {
        // Because this checker is a subchecker of MethodVal,
        // reflection can't be resolved.
        return false;
    }
}
