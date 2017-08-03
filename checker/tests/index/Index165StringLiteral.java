// Test case for Issue 165, where the argument is a string literal:
// https://github.com/kelloggm/checker-framework/issues/165

import org.checkerframework.checker.index.qual.IndexFor;

public class Index165StringLiteral {
	
    public void testMethodInvocation() {
    	requiresIndex("012345", 5);
    	//:: error: (argument.type.incompatible)
    	requiresIndex("012345", 6);
    }
    
    public void requiresIndex(String str, @IndexFor("#1") int index) {    }
}
