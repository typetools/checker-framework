/*
 * Copyright (C) 2007 Júlio Vilmar Gesser.
 * 
 * This file is part of Java 1.5 parser and Abstract Syntax Tree.
 *
 * Java 1.5 parser and Abstract Syntax Tree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java 1.5 parser and Abstract Syntax Tree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java 1.5 parser and Abstract Syntax Tree.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 05/10/2006
 */
package org.checkerframework.stubparser.ast.type;

import java.util.List;

import org.checkerframework.stubparser.ast.expr.AnnotationExpr;
import org.checkerframework.stubparser.ast.visitor.GenericVisitor;
import org.checkerframework.stubparser.ast.visitor.VoidVisitor;

/**
 * @author Julio Vilmar Gesser
 */
public final class ReferenceType extends Type {

    private Type type;

    private int arrayCount;

    private List<List<AnnotationExpr>> arrayAnnotations;

    public ReferenceType() {
    }

    public ReferenceType(Type type) {
        this.type = type;
    }

    public ReferenceType(Type type, int arrayCount) {
        this.type = type;
        this.arrayCount = arrayCount;
    }

    public ReferenceType(int beginLine, int beginColumn, int endLine, int endColumn, Type type, int arrayCount, List<List<AnnotationExpr>> arrayAnnotations) {
        super(beginLine, beginColumn, endLine, endColumn);
        this.type = type;
        this.arrayCount = arrayCount;
        this.arrayAnnotations = arrayAnnotations;
        assert arrayCount == arrayAnnotations.size();
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public int getArrayCount() {
        return arrayCount;
    }

    public List<List<AnnotationExpr>> getArrayAnnotations() {
        return arrayAnnotations;
    }

    public List<AnnotationExpr> getAnnotationsAtLevel(int level) {
        if (level == -1) {
            return type.getAnnotations();
        } else {
            return arrayAnnotations.get(level);
        }
    }

    public Type getType() {
        return type;
    }

    public void setArrayCount(int arrayCount) {
        this.arrayCount = arrayCount;
    }

    public void setArrayAnnotations(List<List<AnnotationExpr>> arrayAnnotations) {
        this.arrayAnnotations = arrayAnnotations;
    }

    public void setType(Type type) {
        this.type = type;
    }

}
