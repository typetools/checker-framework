import  org.gradle.process.ExecOperations
import javax.inject.Inject

interface InjectedExecOps {
  @Inject
  ExecOperations getExecOps()
}
