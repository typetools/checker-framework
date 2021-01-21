// Test case for issue #4142: https://tinyurl.com/cfissue/4142

// @skip-test until the issue is fixed

import java.util.HashMap;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

public class SelfDependentType {

    public void copy1(
            HashMap<String, List<@KeyFor("#1") String>> a,
            HashMap<String, List<@KeyFor("#2") String>> b) {
        a = b;
    }

    public void copy2() {
        HashMap<String, List<@KeyFor("a") String>> a = null;
        HashMap<String, List<@KeyFor("b") String>> b = null;
        a = b;
    }

    class SdtGraph1<T> {

        HashMap<T, List<@KeyFor("childMap") T>> childMap;

        // :: error: (expression.parameter.name)
        public SdtGraph1(HashMap<T, List<@KeyFor("childMap") T>> childMap) {
            this.childMap = childMap;
        }
    }

    class SdtGraph2<T> {

        HashMap<T, List<@KeyFor("this.childMap") T>> childMap;

        // :: error: (expression.parameter.name)
        public SdtGraph2(HashMap<T, List<@KeyFor("childMap") T>> childMap) {
            this.childMap = childMap;
        }
    }

    class SdtGraph3<T> {

        HashMap<T, List<@KeyFor("childMap") T>> childMap;

        public SdtGraph3(HashMap<T, List<@KeyFor("#1") T>> childMap) {
            this.childMap = childMap;
        }
    }

    class SdtGraph4<T> {

        HashMap<T, List<@KeyFor("this.childMap") T>> childMap;

        public SdtGraph4(HashMap<T, List<@KeyFor("#1") T>> childMap) {
            this.childMap = childMap;
        }
    }

    class SdtGraph5<T> {

        HashMap<T, List<@KeyFor("childMap") T>> childMap;

        public SdtGraph5(HashMap<T, List<@KeyFor("this.childMap") T>> childMap) {
            this.childMap = childMap;
        }
    }

    class SdtGraph6<T> {

        HashMap<T, List<@KeyFor("this.childMap") T>> childMap;

        public SdtGraph6(HashMap<T, List<@KeyFor("this.childMap") T>> childMap) {
            this.childMap = childMap;
        }
    }

    class SdtGraph11<T> {

        HashMap<T, List<@KeyFor("childMapField") T>> childMapField;

        // :: error: (expression.parameter.name)
        public SdtGraph11(HashMap<T, List<@KeyFor("childMap") T>> childMap) {
            this.childMapField = childMap;
        }
    }

    class SdtGraph12<T> {

        HashMap<T, List<@KeyFor("this.childMapField") T>> childMapField;

        // :: error: (expression.parameter.name)
        public SdtGraph12(HashMap<T, List<@KeyFor("childMap") T>> childMap) {
            this.childMapField = childMap;
        }
    }

    class SdtGraph13<T> {

        HashMap<T, List<@KeyFor("childMapField") T>> childMapField;

        public SdtGraph13(HashMap<T, List<@KeyFor("#1") T>> childMap) {
            this.childMapField = childMap;
        }
    }

    class SdtGraph14<T> {

        HashMap<T, List<@KeyFor("this.childMapField") T>> childMapField;

        public SdtGraph14(HashMap<T, List<@KeyFor("#1") T>> childMap) {
            this.childMapField = childMap;
        }
    }

    class SdtGraph15<T> {

        HashMap<T, List<@KeyFor("childMapField") T>> childMapField;

        public SdtGraph15(HashMap<T, List<@KeyFor("childMapField") T>> childMap) {
            this.childMapField = childMap;
        }
    }

    class SdtGraph16<T> {

        HashMap<T, List<@KeyFor("this.childMapField") T>> childMapField;

        public SdtGraph16(HashMap<T, List<@KeyFor("this.childMapField") T>> childMap) {
            this.childMapField = childMap;
        }
    }

    class SdtGraph17<T> {

        HashMap<T, List<@KeyFor("childMapField") T>> childMapField;

        public SdtGraph17(HashMap<T, List<@KeyFor("this.childMapField") T>> childMap) {
            this.childMapField = childMap;
        }
    }

    class SdtGraph18<T> {

        HashMap<T, List<@KeyFor("this.childMapField") T>> childMapField;

        public SdtGraph18(HashMap<T, List<@KeyFor("childMapField") T>> childMap) {
            this.childMapField = childMap;
        }
    }
}
