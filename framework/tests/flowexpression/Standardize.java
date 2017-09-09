package flowexpression;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import testlib.flowexpression.qual.FlowExp;

public class Standardize {
    Object field;

    @SuppressWarnings("assignment.type.incompatible")
    @FlowExp("field") Object fieldField = null;

    void variableDecls(@FlowExp("field") Standardize this, @FlowExp("field") Object paramField) {

        @FlowExp("field") Object localField = fieldField;

        @FlowExp("field") Object o1 = fieldField;
        @FlowExp("this.field") Object o2 = fieldField;

        @FlowExp("field") Object o3 = paramField;
        @FlowExp("this.field") Object o4 = paramField;

        @FlowExp("field") Object o5 = localField;
        @FlowExp("this.field") Object o6 = localField;

        try (@SuppressWarnings("assignment.type.incompatible")
                @FlowExp("field") FileInputStream in = new FileInputStream("")) {
            in.read();
            @FlowExp("field") Object o7 = in;
            @FlowExp("this.field") Object o8 = in;
        } catch (
                @SuppressWarnings("exception.parameter.invalid")
                @FlowExp("field") Exception ex) {
            @FlowExp("field") Object o9 = ex;
            @FlowExp("this.field") Object o10 = ex;
        }

        @FlowExp("field") Object o11 = this;
        @FlowExp("this.field") Object o12 = this;
    }

    class MyGen<T extends @FlowExp("field") Object> {}

    <X extends @FlowExp("field") Object, Y extends @FlowExp("this.field") Object>
            void typeVariables(X x) {
        MyGen<X> o1;
        MyGen<Y> o2;
        MyGen<@FlowExp("this.field") String> o3;
        MyGen<@FlowExp("field") String> o4;

        @FlowExp("field") Object o5 = x;
        @FlowExp("this.field") Object o6 = x;
    }

    <A> void typeVariable2(A a, @FlowExp("#1") A a2) {}

    void callTypeVariable2(@FlowExp("field") Object param) {
        typeVariable2(field, param);
        typeVariable2(this.field, param);
    }

    @FlowExp("field") Object testReturn(@FlowExp("this.field") Object param) {
        @FlowExp("this.field") Object o = testReturn(param);
        return param;
    }

    void testCasts() {
        @SuppressWarnings("cast.unsafe")
        @FlowExp("this.field") Object o = (@FlowExp("field") Object) new Object();
        @FlowExp("this.field") Object o2 = (@FlowExp("field") Object) o;
    }

    void testNewClassTree() {
        @FlowExp("this.field") Object o = new @FlowExp("field") Object();
    }

    void list(List<@FlowExp("field") Object> list) {
        Object field = new Object();
        // "field" is  local variable, but list.get(1) type is @FlowExp("this.field")
        @FlowExp("field")
        //:: error: (assignment.type.incompatible)
        Object o1 = list.get(1);
        @FlowExp("this.field") Object o2 = list.get(1);
    }

    Object dict = new Object();

    void typvar() {
        Map<@FlowExp("this.dict") String, String> that =
                new HashMap<@FlowExp("dict") String, String>();
    }

    void newArray() {
        @FlowExp("this.dict") String[] s = new @FlowExp("dict") String[1];
    }

    void clasLiteral(@FlowExp("java.lang.String.class") String param) {
        @FlowExp("String.class") String s = param;
    }
}
