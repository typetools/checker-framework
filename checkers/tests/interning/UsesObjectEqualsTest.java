import checkers.interning.quals.Interned;
import checkers.interning.quals.UsesObjectEquals;

import java.util.LinkedList;
import java.util.prefs.*;

public class UsesObjectEqualsTest {
	
    public @UsesObjectEquals class A {
    		public A(){}
    }
	
    @UsesObjectEquals class B extends A {}
	
    class B2 extends A {}

    //changed to inherited, no (superclass.marked) warning
    class C extends A {}
	
    class D {}
	
    //:: error: (superclass.unmarked)
    @UsesObjectEquals class E extends D {}
	
    //:: error: (overrides.equals)
    @UsesObjectEquals class TestEquals {
		
		public boolean equals(Object o){
		    return true;
		}
    }
		
    class TestComparison {
		
	public void comp(@Interned Object o, A a1, A a2){
	    if (a1 == a2){
		System.out.println("one");
	    }
	    if (a1 == o){
		System.out.println("two");
	    }
	    if (o == a1){
		System.out.println("three");
	    }
	}
		
    }
    
    @UsesObjectEquals class ExtendsInner1 extends UsesObjectEqualsTest.A {}
    
    class ExtendsInner2 extends UsesObjectEqualsTest.A {}
	
    class MyList extends LinkedList {}

}