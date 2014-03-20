package org.checkerframework.stubparser.ast;

import java.util.List;

import org.checkerframework.stubparser.ast.visitor.GenericVisitor;
import org.checkerframework.stubparser.ast.visitor.VoidVisitor;

public class IndexUnit extends Node {
    List<CompilationUnit> compilationUnits;

    public IndexUnit(List<CompilationUnit> compilationUnits) {
        this.compilationUnits = compilationUnits;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public List<CompilationUnit> getCompilationUnits() {
        return this.compilationUnits;
    }

    public void setCompilationUnit(List<CompilationUnit> compilationUnits) {
        this.compilationUnits = compilationUnits;
    }
}
