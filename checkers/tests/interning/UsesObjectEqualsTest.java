import checkers.interning.quals.Interned;
import checkers.interning.quals.UsesObjectEquals;


public class UsesObjectEqualsTest {
	
	@UsesObjectEquals 
	class A {}
	
	@UsesObjectEquals
	class B extends A {}
	
	//:: (superclass.marked)
	class C extends A {}
	
	class D {}
	
	//:: (superclass.unmarked)
	@UsesObjectEquals
	class E extends D {}
	
	//:: (overrides.equals)
	@UsesObjectEquals
	class TestEquals {
		
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
	
}