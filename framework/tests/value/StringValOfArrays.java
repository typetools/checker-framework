import org.checkerframework.common.value.qual.StringVal;

class Test{
    
    void bytes(){
        String s = "hello";
        byte @StringVal("hello") [] bytes = s.getBytes();
        @StringVal("hello") String s2 = new String(bytes);
    }
    
    void chars(){
        String s = "$-hello@";
        //Not Implemented.
//        char @StringVal("$-hello@") [] chars = s.toCharArray();
//        @StringVal("$-hello@") String s2 = new String(chars);
    }
}