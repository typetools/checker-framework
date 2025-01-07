import  org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * This class is for calling out to the command line. Use this, rather than writing a custom task class
 * when the call is simple and only used once in the builds files.
 * You can use it like so:
 * <pre>
 * tasks.register('myAdHocExecOperationsTask') {
 *   def injected = project.objects.newInstance(InjectedExecOps)
 *   doLast {
 *     injected.execOps.exec {
 *       commandLine 'ls', '-la'
 *     }
 *   }
 * }
 * </pre>
 *
 * This is copied from here:
 * https://docs.gradle.org/current/userguide/service_injection.html#execoperations
 */
interface InjectedExecOps {
  @Inject
  ExecOperations getExecOps()
}
