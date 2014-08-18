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
 * Created on 04/11/2006
 */
package org.checkerframework.stubparser.ast.stmt;

import java.util.List;

import org.checkerframework.stubparser.ast.visitor.GenericVisitor;
import org.checkerframework.stubparser.ast.visitor.VoidVisitor;

/**
 * @author Julio Vilmar Gesser
 */
public final class BlockStmt extends Statement {

    private List<Statement> stmts;

    public BlockStmt() {
    }

    public BlockStmt(List<Statement> stmts) {
        this.stmts = stmts;
    }

    public BlockStmt(int beginLine, int beginColumn, int endLine, int endColumn, List<Statement> stmts) {
        super(beginLine, beginColumn, endLine, endColumn);
        this.stmts = stmts;
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    public List<Statement> getStmts() {
        return stmts;
    }

    public void setStmts(List<Statement> stmts) {
        this.stmts = stmts;
    }
}
