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
package org.checkerframework.stubparser.ast;

import java.util.List;

import org.checkerframework.stubparser.ast.expr.AnnotationExpr;
import org.checkerframework.stubparser.ast.type.ClassOrInterfaceType;
import org.checkerframework.stubparser.ast.visitor.GenericVisitor;
import org.checkerframework.stubparser.ast.visitor.VoidVisitor;

/**
 * <p>
 * This class represents the declaration of a genetics argument.
 * </p>
 * The TypeParameter is constructed following the syntax:<br>
 * <table summary="TypeParameter syntax">
 * <tr valign=baseline>
 *   <td align=right>TypeParameter</td>
 *   <td align=center>::=</td>
 *   <td align=left>
 *       &lt;IDENTIFIER&gt; ( "extends" {@link ClassOrInterfaceType} ( "&amp;" {@link ClassOrInterfaceType} )* )?
 *   </td>
 * </tr>
 * </table> 
 * 
 * @author Julio Vilmar Gesser
 */
public final class TypeParameter extends Node {

    private String name;

    private List<AnnotationExpr> annotations;

    private List<ClassOrInterfaceType> typeBound;

    public TypeParameter() {
    }

    public TypeParameter(String name, List<AnnotationExpr> annotations, List<ClassOrInterfaceType> typeBound) {
        this.name = name;
        this.annotations = annotations;
        this.typeBound = typeBound;
    }

    public TypeParameter(int beginLine, int beginColumn, int endLine, int endColumn, String name, List<AnnotationExpr> annotations, List<ClassOrInterfaceType> typeBound) {
        super(beginLine, beginColumn, endLine, endColumn);
        this.name = name;
        this.annotations = annotations;
        this.typeBound = typeBound;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    /**
     * Return the name of the paramenter.
     * 
     * @return the name of the paramenter
     */
    public String getName() {
        return name;
    }

    /**
     * Return the list of {@link ClassOrInterfaceType} that this parameter
     * extends. Return <code>null</code> null if there are no type.
     * 
     * @return list of types that this paramente extends or <code>null</code>
     */
    public List<ClassOrInterfaceType> getTypeBound() {
        return typeBound;
    }

    /**
     * Sets the name of this type parameter.
     * 
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the list o types.
     * 
     * @param typeBound
     *            the typeBound to set
     */
    public void setTypeBound(List<ClassOrInterfaceType> typeBound) {
        this.typeBound = typeBound;
    }

    public List<AnnotationExpr> getAnnotations() {
        return annotations;
    }

}
