package checkers.eclipse.actions;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;

import checkers.eclipse.javac.*;
import checkers.eclipse.util.*;

public class JSR308Worker{

    private static final String PATH_SEPARATOR = System.getProperty("path.separator");// This is : on unix
    private final IProgressMonitor pm;

    public JSR308Worker(IProgressMonitor monitor){
        pm = monitor;
    }

    public void work(IJavaProject project, Class<?> checkerClass) throws CoreException{
        pm.beginTask("Running checker " + checkerClass.getName() + " on " + project.getElementName(), 10);
        pm.setTaskName("Removing old markers");
        MarkerUtil.removeMarkers(project.getResource());
        pm.worked(1);
        List<String> javaFileNames = getSourceFilesOnClasspath(project);
        String processor = checkerClass.getCanonicalName();
        pm.setTaskName("Running checker");
        String cp = getClasspathForJavac(project);

        // XXX it is very annoying that we run commandline javac rather than directly. But otherwise there's a classpath hell.
        List<JavacError> callJavac = new CommandlineJavacRunner().callJavac(javaFileNames, processor, cp);
        pm.worked(6);
        pm.setTaskName("Updating problem list");
        for (JavacError javacError : callJavac){
            IResource file = getFile(project, javacError);
            if (file == null)
                continue;
            MarkerUtil.addMarker(javacError.message, project.getProject(), file, javacError.lineNumber);
        }
        pm.worked(3);
        pm.done();
    }

    // Returns the project's classpath in a format suitable for javac
    private String getClasspathForJavac(IJavaProject project) throws JavaModelException{
        StringBuilder classpath = new StringBuilder();
        for (IClasspathEntry cp : project.getRawClasspath()){
            int entryKind = cp.getEntryKind();
            switch (entryKind){
            case IClasspathEntry.CPE_SOURCE:
                classpath.append(outputLocation(cp, project) + PATH_SEPARATOR);
                continue;
            case IClasspathEntry.CPE_LIBRARY:
                classpath.append(getAbsolutePath(cp) + PATH_SEPARATOR);
                break;
            case IClasspathEntry.CPE_PROJECT:
                // TODO unimplemented!
                break;
            case IClasspathEntry.CPE_CONTAINER:
                IClasspathContainer c = JavaCore.getClasspathContainer(cp.getPath(), project);
                if (c.getKind() == IClasspathContainer.K_DEFAULT_SYSTEM || c.getKind() == IClasspathContainer.K_SYSTEM)
                    break;
                for (IClasspathEntry entry : c.getClasspathEntries()){
                    if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY){
                        classpath.append(entry.getPath().makeAbsolute().toFile().getAbsolutePath() + PATH_SEPARATOR);
                    }else{
                        // TODO unimplemented!
                    }
                }
                break;
            case IClasspathEntry.CPE_VARIABLE:
                // TODO unimplemented!
                break;
            }
        }
        classpath.append(PATH_SEPARATOR + CommandlineJavacRunner.checkersJARlocation());
        return classpath.toString();
    }

    private String outputLocation(IClasspathEntry cp, IJavaProject project){
        IPath out = cp.getOutputLocation();
        if (out != null)
            return out.toOSString();
        // location is null if the classpath entry outputs to the 'default' location, i.e. project
        IFile outDir = ResourcesPlugin.getWorkspace().getRoot().getFile(cp.getPath());
        return outDir.getLocation().toOSString();
    }

    private String getAbsolutePath(IClasspathEntry entry){
        IFile jarFile = ResourcesPlugin.getWorkspace().getRoot().getFile(entry.getPath());
        IPath location = jarFile.getLocation();
        if (location != null)
            return location.toOSString();
        else
            return jarFile.getFullPath().toOSString();// ??
    }

    private List<String> getSourceFilesOnClasspath(final IJavaProject project) throws CoreException{
        final List<String> fileNames = new ArrayList<String>();

        for (ICompilationUnit cu : Util.getAllCompilationUnits(project)){
            fileNames.add(cu.getResource().getLocation().toOSString());
        }
        return fileNames;
    }

    private IResource getFile(IJavaProject jProject, JavacError javacError){
        IPath fullPath = jProject.getProject().getLocation();
        return jProject.getProject().findMember(Path.fromOSString(javacError.file.getPath()).removeFirstSegments(fullPath.segmentCount()));
    }
}
