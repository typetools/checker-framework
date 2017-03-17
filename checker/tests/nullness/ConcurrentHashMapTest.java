// Test case for issue 1142: https://github.com/typetools/checker-framework/issues/1142

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

class issue1142{

void foo(){
	
Map<Integer,@Nullable Integer> myMap=new ConcurrentHashMap<>();
myMap.put(1, 1);
myMap.put(3, 3);
myMap.put(3, null);

}
}


