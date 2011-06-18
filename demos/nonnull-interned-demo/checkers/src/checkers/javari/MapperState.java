package checkers.javari;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ClassTree;

/**
 * Wrapper for pair of elements, representing parent class and parent method.
 */
class MapperState {

    MethodTree methodTree;
    ClassTree classTree;

    MapperState(ClassTree classTree) {
        this.classTree = classTree;
    }

}
