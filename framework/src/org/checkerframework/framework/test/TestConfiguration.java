package org.checkerframework.framework.test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by jburke on 6/24/15.
 */
public interface TestConfiguration {
    List<File> getTestSourceFiles();
    List<File> getDiagnosticFiles();
    List<String> getProcessors();
    Map<String, String> getOptions();
    List<String> getFlatOptions();
    boolean shouldEmitDebugInfo();
}
