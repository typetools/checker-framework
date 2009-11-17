/*
 * Copyright (C) 2008 Julio Vilmar Gesser.
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
 * Created on 11/06/2008
 */
package japa.parser.ast.test;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.test.classes.DumperTestClass;
import japa.parser.ast.visitor.GenericVisitorAdapter;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import junit.framework.TestCase;

/**
 * @author Julio Vilmar Gesser
 */
public class TestAdapters extends TestCase {

    public void testVoidVisitorAdapter() throws Exception {
        CompilationUnit cu = TestHelper.parserClass("./test", DumperTestClass.class);
        cu.accept(new VoidVisitorAdapter(), null);
    }

    public void testGenericVisitorAdapter() throws Exception {
        CompilationUnit cu = TestHelper.parserClass("./test", DumperTestClass.class);
        cu.accept(new GenericVisitorAdapter(), null);
    }

}
