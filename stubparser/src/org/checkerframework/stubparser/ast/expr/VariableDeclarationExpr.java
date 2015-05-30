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
 * Created on 03/11/2006
 */
package org.checkerframework.stubparser.ast.expr;

import java.util.List;

import org.checkerframework.stubparser.ast.body.ModifierSet;
import org.checkerframework.stubparser.ast.body.VariableDeclarator;
import org.checkerframework.stubparser.ast.type.Type;
import org.checkerframework.stubparser.ast.visitor.GenericVisitor;
import org.checkerframework.stubparser.ast.visitor.VoidVisitor;

/**
 * @author Julio Vilmar Gesser
 */
public final class VariableDeclarationExpr extends Expression {

    private int modifiers;

    private List<AnnotationExpr> annotations;

    private Type type;

    private List<VariableDeclarator> vars;

    public VariableDeclarationExpr() {
    }

    public VariableDeclarationExpr(Type type, List<VariableDeclarator> vars) {
        this.type = type;
        this.vars = vars;
    }

    public VariableDeclarationExpr(int modifiers, Type type, List<VariableDeclarator> vars) {
        this.modifiers = modifiers;
        this.type = type;
        this.vars = vars;
    }

    public VariableDeclarationExpr(int beginLine, int beginColumn, int endLine, int endColumn, int modifiers, List<AnnotationExpr> annotations, Type type, List<VariableDeclarator> vars) {
        super(beginLine, beginColumn, endLine, endColumn);
        this.modifiers = modifiers;
        this.annotations = annotations;
        this.type = type;
        this.vars = vars;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public List<AnnotationExpr> getAnnotations() {
        return annotations;
    }

    /**
     * Return the modifiers of this variable declaration.
     * 
     * @see ModifierSet
     * @return modifiers
     */
    public int getModifiers() {
        return modifiers;
    }

    public Type getType() {
        return type;
    }

    public List<VariableDeclarator> getVars() {
        return vars;
    }

    public void setAnnotations(List<AnnotationExpr> annotations) {
        this.annotations = annotations;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setVars(List<VariableDeclarator> vars) {
        this.vars = vars;
    }
}
