package org.checkerframework.framework.test.junit;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import org.checkerframework.javacutil.trees.TreeParser;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.processing.ProcessingEnvironment;

public class TreeParserTest {
    private final ProcessingEnvironment env;
    private final TreeParser parser;

    public TreeParserTest() {
        env = JavacProcessingEnvironment.instance(new Context());
        parser = new TreeParser(env);
    }

    @Test
    public void parsesIdentifiers() {
        String value = "id";
        ExpressionTree parsed = parser.parseTree(value);

        Assert.assertTrue(parsed instanceof IdentifierTree);
    }

    @Test
    public void parsesNumbers() {
        String value = "23";
        ExpressionTree parsed = parser.parseTree(value);

        Assert.assertTrue(parsed instanceof LiteralTree);
    }

    @Test
    public void parsesMethodInvocations() {
        String value = "test()";
        ExpressionTree parsed = parser.parseTree(value);

        Assert.assertTrue(parsed instanceof MethodInvocationTree);
        MethodInvocationTree invocation = (MethodInvocationTree) parsed;
        Assert.assertTrue(invocation.getMethodSelect() instanceof IdentifierTree);
        Assert.assertEquals(
                "test", ((IdentifierTree) invocation.getMethodSelect()).getName().toString());
    }

    @Test
    public void parsesMethodInvocationsWithSelect() {
        String value = "Class.test()";
        ExpressionTree parsed = parser.parseTree(value);

        Assert.assertTrue(parsed instanceof MethodInvocationTree);
        MethodInvocationTree invocation = (MethodInvocationTree) parsed;
        Assert.assertTrue(invocation.getMethodSelect() instanceof MemberSelectTree);
        MemberSelectTree select = (MemberSelectTree) invocation.getMethodSelect();
        Assert.assertEquals("test", select.getIdentifier().toString());
        Assert.assertEquals("Class", select.getExpression().toString());
    }

    @Test
    public void parsesIndex() {
        String value = "array[2]";
        ExpressionTree parsed = parser.parseTree(value);

        Assert.assertTrue(parsed instanceof ArrayAccessTree);
        ArrayAccessTree access = (ArrayAccessTree) parsed;

        Assert.assertEquals(2, ((LiteralTree) access.getIndex()).getValue());
        Assert.assertEquals(
                "array", ((IdentifierTree) access.getExpression()).getName().toString());
    }

    @Test
    public void randomParses() {
        ExpressionTree parsed = parser.parseTree("Class.method()[4].field[3]");

        Assert.assertTrue(parsed instanceof ArrayAccessTree);
        MemberSelectTree array = (MemberSelectTree) ((ArrayAccessTree) parsed).getExpression();
        Assert.assertEquals("field", array.getIdentifier().toString());
        Assert.assertTrue(array.getExpression() instanceof ArrayAccessTree);
    }

    @Test
    public void parsesMethodArguments() {
        parser.parseTree("method()");
        parser.parseTree("method(1)");
        parser.parseTree("method(1,2)");
    }
}
