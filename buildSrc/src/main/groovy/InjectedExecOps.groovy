import javax.inject.Inject
import  org.gradle.process.ExecOperations

interface InjectedExecOps {
    @Inject
    ExecOperations getExecOps()
}
