package org.checkerframework.eclipse.javac;

import java.util.List;

public interface CheckersRunner {
    public void run();

    public List<JavacError> getErrors();
}
