package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class SecurityManager{
  public boolean getInCheck() { throw new RuntimeException("skeleton method"); }
  public SecurityManager() { throw new RuntimeException("skeleton method"); }
  public java.lang.Object getSecurityContext() { throw new RuntimeException("skeleton method"); }
  public void checkPermission(java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
  public void checkPermission(java.security.Permission a1, java.lang.Object a2) { throw new RuntimeException("skeleton method"); }
  public void checkCreateClassLoader() { throw new RuntimeException("skeleton method"); }
  public void checkAccess(java.lang.Thread a1) { throw new RuntimeException("skeleton method"); }
  public void checkAccess(java.lang.ThreadGroup a1) { throw new RuntimeException("skeleton method"); }
  public void checkExit(int a1) { throw new RuntimeException("skeleton method"); }
  public void checkExec(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void checkLink(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void checkRead(java.io.FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public void checkRead(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void checkRead(java.lang.String a1, java.lang.Object a2) { throw new RuntimeException("skeleton method"); }
  public void checkWrite(java.io.FileDescriptor a1) { throw new RuntimeException("skeleton method"); }
  public void checkWrite(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void checkDelete(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void checkConnect(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void checkConnect(java.lang.String a1, int a2, java.lang.Object a3) { throw new RuntimeException("skeleton method"); }
  public void checkListen(int a1) { throw new RuntimeException("skeleton method"); }
  public void checkAccept(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void checkMulticast(java.net.InetAddress a1) { throw new RuntimeException("skeleton method"); }
  public void checkMulticast(java.net.InetAddress a1, byte a2) { throw new RuntimeException("skeleton method"); }
  public void checkPropertiesAccess() { throw new RuntimeException("skeleton method"); }
  public void checkPropertyAccess(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public boolean checkTopLevelWindow(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void checkPrintJobAccess() { throw new RuntimeException("skeleton method"); }
  public void checkSystemClipboardAccess() { throw new RuntimeException("skeleton method"); }
  public void checkAwtEventQueueAccess() { throw new RuntimeException("skeleton method"); }
  public void checkPackageAccess(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void checkPackageDefinition(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void checkSetFactory() { throw new RuntimeException("skeleton method"); }
  public void checkMemberAccess(java.lang.Class<?> a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void checkSecurityAccess(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.ThreadGroup getThreadGroup() { throw new RuntimeException("skeleton method"); }
}
