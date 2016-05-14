import org.checkerframework.common.value.qual.StringVal;

class Test {
    void stringConcat() {
        @StringVal("helloa11.01.020truenull2626")  String everything = "hello"+'a'+1+1.0+1.0f+20L+true+null+0x1a+0b11010;

       @StringVal("true") String bool = ""+true;
       @StringVal("null") String nullV= ""+null;
       @StringVal("1") String intL = ""+1;
       @StringVal("$") String charL = ""+'$';
       @StringVal("1.0") String doubleDefault = ""+1.0;
       @StringVal("1.0") String doubleL = ""+1.0d;
       @StringVal("26") String hexVal= ""+0x1a;
       @StringVal("26") String binaryVal = ""+0b11010;
       @StringVal("12.3") String floatVal = ""+12.3f;
       @StringVal("123.0") String science = ""+1.23e2;

    }

    void compoundStringAssignement() {
        String s = "";
        s+="hello";
        s+='a';
        s+=1;
        s+=1.0;
        s+=1.0f;
        s+=20L;
        s+=true;
        s+=null;
        s+=0x1a;
        s+=0b11010;
        //TODO: this should pass
        // compound assignments have not been implemented.
        //:: error: (assignment.type.incompatible)
        @StringVal("helloa11.01.020truenull2626") String all = s;

    }
}
