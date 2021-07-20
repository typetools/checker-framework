import org.checkerframework.checker.nullness.qual.*;

import java.util.HashMap;
import java.util.Map;

// See issue 241: https://github.com/typetools/checker-framework/issues/241

// Cover all 8 combinations of:
// -(Non)static class
// -(Non)static field
// -(Non)static method

// Also test:
// -(Non)static field initialization

public class JavaExprContext {

    // Classes to perform tests on

    // The methods return booleans instead of void simply so they can
    // be tested as field initializers.

    public static class staticGraphClass {
        private Map<String, Integer> adjList = new HashMap<>();

        public boolean addEdge(@KeyFor("adjList") String source) {
            return true;
        }

        public static boolean addEdge2(
                @KeyFor("#2.adjList") String source, staticGraphClass theGraph) {
            return true;
        }

        public boolean addEdge3(@KeyFor("this.adjList") String source) {
            return true;
        }
    }

    public class nonstaticGraphClass {
        private Map<String, Integer> adjList = new HashMap<>();

        public boolean addEdge(@KeyFor("adjList") String source) {
            return true;
        }

        public boolean addEdge2(@KeyFor("this.adjList") String source) {
            return true;
        }
    }

    // Non-static field initialization

    staticGraphClass graphField1 = new staticGraphClass();
    nonstaticGraphClass graphField2 = new nonstaticGraphClass();

    @SuppressWarnings("assignment.type.incompatible")
    @KeyFor("graphField1.adjList") String key1 = "";

    @SuppressWarnings("assignment.type.incompatible")
    @KeyFor("graphField2.adjList") String key2 = "";

    boolean b1 = staticGraphClass.addEdge2(key1, graphField1);
    boolean b2 = graphField1.addEdge(key1);
    boolean b3 = graphField1.addEdge2(key1, graphField1);
    boolean b4 = graphField1.addEdge3(key1);

    boolean b5 = graphField2.addEdge(key2);
    boolean b6 = graphField2.addEdge2(key2);

    // Classes that perform tests

    public class nonstaticTestClass {

        staticGraphClass graphField1 = new staticGraphClass();
        nonstaticGraphClass graphField2 = new nonstaticGraphClass();

        public void buildGraph1(@KeyFor("graphField1.adjList") String hero) {
            staticGraphClass.addEdge2(hero, graphField1);
            graphField1.addEdge(hero);
            graphField1.addEdge2(
                    hero,
                    graphField1); // Calling a static method from an instance object. Ensuring this
            // doesn't confuse the JavaExpression parsing.
            graphField1.addEdge3(hero);
        }

        public void buildGraph2(@KeyFor("graphField2.adjList") String hero) {
            graphField2.addEdge(hero);
            graphField2.addEdge2(hero);
        }

        public void buildGraph3(staticGraphClass myGraph, @KeyFor("#1.adjList") String hero) {
            staticGraphClass.addEdge2(hero, myGraph);
            myGraph.addEdge(hero);
            myGraph.addEdge2(
                    hero,
                    myGraph); // Calling a static method from an instance object. Ensuring this
            // doesn't confuse the JavaExpression parsing.
            myGraph.addEdge3(hero);
        }

        public void buildGraph4(nonstaticGraphClass myGraph, @KeyFor("#1.adjList") String hero) {
            myGraph.addEdge(hero);
            myGraph.addEdge2(hero);
        }
    }

    public static class staticTestClass {

        staticGraphClass graphField1 = new staticGraphClass();
        static staticGraphClass graphField2 = new staticGraphClass();

        public void buildGraph1(@KeyFor("graphField1.adjList") String hero) {
            staticGraphClass.addEdge2(hero, graphField1);
            graphField1.addEdge(hero);
            graphField1.addEdge2(
                    hero,
                    graphField1); // Calling a static method from an instance object. Ensuring this
            // doesn't confuse the JavaExpression parsing.
            graphField1.addEdge3(hero);
        }

        public void buildGraph3(@KeyFor("graphField2.adjList") String hero) {
            staticGraphClass.addEdge2(hero, graphField2);
            graphField2.addEdge(hero);
            graphField2.addEdge2(
                    hero,
                    graphField2); // Calling a static method from an instance object. Ensuring this
            // doesn't confuse the JavaExpression parsing.
            graphField2.addEdge3(hero);
        }

        public void buildGraph5(staticGraphClass myGraph, @KeyFor("#1.adjList") String hero) {
            staticGraphClass.addEdge2(hero, myGraph);
            myGraph.addEdge(hero);
            myGraph.addEdge2(
                    hero,
                    myGraph); // Calling a static method from an instance object. Ensuring this
            // doesn't confuse the JavaExpression parsing.
            myGraph.addEdge3(hero);
        }

        public void buildGraph6(nonstaticGraphClass myGraph, @KeyFor("#1.adjList") String hero) {
            myGraph.addEdge(hero);
            myGraph.addEdge2(hero);
        }

        public static void buildGraph7(@KeyFor("graphField2.adjList") String hero) {
            staticGraphClass.addEdge2(hero, graphField2);
            graphField2.addEdge(hero);
            graphField2.addEdge2(
                    hero,
                    graphField2); // Calling a static method from an instance object. Ensuring this
            // doesn't confuse the JavaExpression parsing.
            graphField2.addEdge3(hero);
        }

        public static void buildGraph9(
                staticGraphClass myGraph, @KeyFor("#1.adjList") String hero) {
            staticGraphClass.addEdge2(hero, myGraph);
            myGraph.addEdge(hero);
            myGraph.addEdge2(
                    hero,
                    myGraph); // Calling a static method from an instance object. Ensuring this
            // doesn't confuse the JavaExpression parsing.
            myGraph.addEdge3(hero);
        }

        public static void buildGraph10(
                nonstaticGraphClass myGraph, @KeyFor("#1.adjList") String hero) {
            myGraph.addEdge(hero);
            myGraph.addEdge2(hero);
        }
    }
}
