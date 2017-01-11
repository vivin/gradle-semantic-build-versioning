package net.vivin.gradle.versioning.spock.extensions.gradle

import org.gradle.testkit.runner.GradleRunner
import org.spockframework.runtime.extension.IMethodInvocation
import spock.lang.Specification

import static java.nio.file.Files.createTempDirectory

class InjectGradleRunnerIntoFieldsInterceptor extends InjectGradleRunnerInterceptorBase {
    private shared

    InjectGradleRunnerIntoFieldsInterceptor(shared = true) {
        this.shared = shared
    }

    @Override
    public void intercept(IMethodInvocation invocation) {
        def instance = (shared ? invocation.sharedInstance : invocation.instance) as Specification

        def fieldsToFill = instance
            .specificationContext
            .currentSpec
            .fields
            .findAll { GradleRunner.equals it.type }
            .findAll { it.shared == shared }
            .findAll { !it.readValue(instance) }

        if(!fieldsToFill) {
            invocation.proceed()
            return
        }

        List<File> temporaryProjectDirs = []

        try {
            fieldsToFill.each {
                // determine the project dir to use
                def projectDirClosure = it.getAnnotation(ProjectDirProvider)?.value()

                File projectDir

                if(!projectDirClosure) {
                    projectDir = createTempDirectory('gradleRunner_').toFile()
                    temporaryProjectDirs << projectDir
                } else {
                    projectDir = determineProjectDir(projectDirClosure.newInstance(instance, instance)(), "field '$it.name'")
                }

                it.writeValue instance, prepareProjectDir(projectDir)
            }

            invocation.proceed()
        } finally {
            temporaryProjectDirs*.deleteDir()
        }
    }
}
