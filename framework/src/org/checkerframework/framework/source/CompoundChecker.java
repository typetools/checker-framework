package org.checkerframework.framework.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;

/**
 * 
 * Like an AggregateChecker, a CompoundChecker is shorthand to invoke
 * a sequence of checkers. In an AggregateChecker, there is no communication
 * between checkers. In a CompoundChecker, a checker later in the sequence
 * may inspect the analysis resuls of a checker earlier in the sequence via
 * {@link SourceChecker#getPreviousCheckers()}
 *
 * @See AggregateChecker
 */
public abstract class CompoundChecker extends AggregateChecker {

    public CompoundChecker() {
        super();
    }

    @Override
    protected void initializeCheckers() {
        ArrayList<SourceChecker> visited = new ArrayList<SourceChecker>();
        for (SourceChecker checker : checkers) {
            // provide references to all previous checkers (for the first checker, the list is empty)
            checker.initChecker(visited);
            visited.add(checker);
        }
    }
}
