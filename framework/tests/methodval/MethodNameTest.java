import org.checkerframework.common.reflection.qual.MethodVal;


public class MethodNameTest {
    @MethodVal(className="", methodName="_MethodName", params=0) Object o;
    @MethodVal(className="", methodName="$methname", params=0) Object o3;
    @MethodVal(className="", methodName="Method_Name", params=0) Object o5;
    @MethodVal(className="", methodName="<init>", params=0) Object o6;


    //:: error: (illegal.methodname)
    @MethodVal(className="", methodName="[]MethodName", params=0) Object o1;
    //:: error: (illegal.methodname)
    @MethodVal(className="", methodName="Meht.name", params=0) Object o2;
    //:: error: (illegal.methodname)
    @MethodVal(className="", methodName=".emethos", params=0) Object o4;
}
