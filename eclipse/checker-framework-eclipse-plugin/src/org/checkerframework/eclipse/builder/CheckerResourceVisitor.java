package org.checkerframework.eclipse.builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.checkerframework.eclipse.util.Util;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class CheckerResourceVisitor implements IResourceDeltaVisitor {
    HashSet<String> buildFiles;

    CheckerResourceVisitor() {
        buildFiles = new HashSet<String>();
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        // if the file has been removed, we don't need to visit
        // its children or process it any further
        if (delta.getKind() == IResourceDelta.REMOVED) {
            return false;
        } else if (Util.isJavaFile(delta.getResource())) {
            buildFiles.add(delta.getResource().getLocation().toOSString());
        }

        return true;
    }

    public List<String> getBuildFiles() {
        List<String> resultList = new ArrayList<String>();
        resultList.addAll(buildFiles);

        return resultList;
    }
}
