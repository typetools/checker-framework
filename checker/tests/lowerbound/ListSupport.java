import java.util.*;

public class ListSupport {
	
	void testGet(){
		List<Integer> list = new ArrayList<Integer>();
		int i = -1;
		int j = 0;
		
		// try and use a negative to get, should fail
		//:: error: (assignment.type.incompatible)
		list.get(i);
		
		//try and us a nonnegative, should work
		list.get(j)
	}
}
