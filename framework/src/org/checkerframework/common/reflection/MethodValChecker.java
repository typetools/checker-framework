package org.checkerframework.common.reflection;

import java.util.LinkedHashSet;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.reflection.qual.MethodValBottom;
import org.checkerframework.common.reflection.qual.UnknownMethod;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
/**
 *
 * Annotation semantics:
 * @MethodVal(String[] classnames, String[] methodnames, int[] params): Estimate of method or constructor signature
 *   that a Method or Constructor object represents.  (See MethodVal declaration for more information.)
 * @UnknownMethodVal: no estimate of method signatures, default qualifier
 * @MethodValBottom: qualifier given to the null literal.
 *
 * Subtyping rules:
 * Top: @UnknownMethodVal
 * Bottom: @MethodValBottom
 *
 * @MethodVal(classname=CA, methodname=MA, params=PA) <:@MethodVal(classname=CB, methodname=MB, params=PB)
 * (CA, MA, and PA are ordered lists of equal size and CB, MB, and PB are ordered lists of equal size)
 * for all indexes i to CA, there exists an index, j to CB, where CA[i] = CB[j], MA[i] = MA[j], and PA[i] = PB[j]
 *
 * Special typing rules:
 * exp.getMethod(methodname, parameterClasses): @MethodVal(classname= classname of exp (found using ClassValChecker)
 *                                                         methodname= string value of methodname  (found using ValueChecker)
 *                                                         params= arrray length of paramterClasses (found using ValueChecker))
 *    If more than one value is found for class name, method name, or number of paramters, the @MethodVal annotation
 *    contains all possible method signatures. (Cartesian product of all values.)
 *
 * exp.getConstructor(parameterClasses): @MethodVal(classname= exact classname of exp (found using ClassValChecker)
 *                                                         methodname= "<init>"
 *                                                         params= arrray length of paramterClasses (found using ValueChecker))
 *    If more than one value is found for class name, method name, or number of paramters, the @MethodVal annotation
 *    contains all possible method signatures. (Cartesian product of all values.)
 *
 * (Uses the Value Checker and ClassVal Checker)
 */
@TypeQualifiers({MethodVal.class, MethodValBottom.class, UnknownMethod.class})
public class MethodValChecker extends BaseTypeChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new MethodValVisitor(this);
    }
    @Override
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        //Don't call super otherwise MethodVal will be added as a subChecker
        // which creates a circular dependency.
        LinkedHashSet<Class<? extends BaseTypeChecker>> subCheckers = new LinkedHashSet<>();
        subCheckers.add(ValueChecker.class);
        subCheckers.add(ClassValChecker.class);
        return subCheckers;
    }
}
