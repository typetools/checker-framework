
// Test case for issue 1142: https://github.com/typetools/checker-framework/issues/1142

class Test{

public static void main(String[] args){

Map<Integer,@Nullable Integer> myMap=new ConcurrentHashMap<>();

myMap.put(1, 1);
myMap.put(2, 2);
myMap.put(3, 3);

Iterator<Integer> it1 = myMap.keySet().iterator();
while(it1.hasNext()) {
Integer key = it1.next();
System.out.println(key+" Map Value:"+myMap.get(key));
if(key.equals(1)) {
myMap.put(3, null);
}
}
}

/*
Output without annotated ConcurrentHashMap.java: Nullness Checker passed!

Output at runtime: NullPointerException at line 16 

As per java docs ,
https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ConcurrentHashMap.html
The concurrent-hashmap raises NullpointerException if any of the key or value is null. 
So here the Nullness Checker fails in catching the NullPointerException generated at the runtime.
If bymistake any programmer annotes the value parameter to be @Nullable then the program will crash at the runtime.
*/