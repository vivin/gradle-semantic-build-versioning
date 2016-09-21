package net.vivin.gradle.versioning.spock.extensions.gradle

import org.gradle.testkit.runner.GradleRunner
import org.spockframework.runtime.extension.IMethodInvocation

import java.lang.reflect.Parameter

import static java.nio.file.Files.createTempDirectory

class InjectGradleRunnerIntoParametersInterceptor extends InjectGradleRunnerInterceptorBase {
    @Override
    public void intercept(IMethodInvocation invocation) {
        // create a map of all GradleRunner parameters with their parameter index
        Map<Parameter, Integer> parameters = [:]
        invocation.method.reflection.parameters.eachWithIndex { parameter, i -> parameters << [(parameter): i] }
        parameters = parameters.findAll { GradleRunner.equals it.key.type }

        // enlarge arguments array if necessary
        def lastGradleRunnerParameterIndex = parameters*.value.max()
        lastGradleRunnerParameterIndex = lastGradleRunnerParameterIndex == null ? 0 : lastGradleRunnerParameterIndex + 1
        if(invocation.arguments.length < lastGradleRunnerParameterIndex) {
            def newArguments = new Object[lastGradleRunnerParameterIndex]
            System.arraycopy invocation.arguments, 0, newArguments, 0, invocation.arguments.length
            invocation.arguments = newArguments
        }

        // find all parameters to fill
        def parametersToFill = parameters.findAll { !invocation.arguments[it.value] }

        if(!parametersToFill) {
            invocation.proceed()
            return
        }

        List<File> temporaryProjectDirs = []

        try {
            parametersToFill.each { parameter, i ->
                // determine the project dir to use
                def projectDirClosure = parameter.getAnnotation(ProjectDirProvider)?.value()

                File projectDir

                if(!projectDirClosure) {
                    projectDir = createTempDirectory('gradleRunner_').toFile()
                    temporaryProjectDirs << projectDir
                } else {
                    projectDir = determineProjectDir(projectDirClosure.newInstance(invocation.instance, invocation.instance)(), "parameter '$parameter.name'")
                }

                invocation.arguments[i] = prepareProjectDir(projectDir)
            }

            invocation.proceed()
        } finally {
            temporaryProjectDirs*.deleteDir()
        }
    }
}
