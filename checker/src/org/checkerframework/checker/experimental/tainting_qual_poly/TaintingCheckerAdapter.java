package org.checkerframework.checker.experimental.tainting_qual_poly;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.qualframework.base.CheckerAdapter;
import org.checkerframework.qualframework.base.TypecheckVisitorAdapter;
import org.checkerframework.qualframework.poly.QualParams;

import com.sun.source.tree.CatchTree;

public class TaintingCheckerAdapter extends CheckerAdapter<QualParams<Tainting>> {
    public TaintingCheckerAdapter() {
        super(new TaintingChecker());
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new TypecheckVisitorAdapter<QualParams<Tainting>>(this){
               @Override
               protected void checkExceptionParameter(CatchTree node) {
                   // TODO: The standard check fails with this error:
                   /*
javacheck -processor org.checkerframework.checker.experimental.tainting_qual_poly.TaintingCheckerAdapter SimpleLog.java
SimpleLog.java:8: error: [exception.parameter.invalid] invalid type in catch argument.
    } catch (Exception e) {
                       ^
  found   : QualParams(primary=TAINTED,{})
  required: QualParams(primary=null,{__TOP__=null})
1 error
                    */
                   //Need to override getRequiredExceptionParameterQualifier and return the correct
                   //Top qualifier.  Or fix it so that QualParams.getTop() returns the top with respect to a given qualifier.
                   //See issue: 387
                   //https://code.google.com/p/checker-framework/issues/detail?id=387
               }

        };
    }
}
