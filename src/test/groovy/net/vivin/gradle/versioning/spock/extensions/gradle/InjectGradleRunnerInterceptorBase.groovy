package net.vivin.gradle.versioning.spock.extensions.gradle

import org.gradle.testkit.runner.GradleRunner
import org.spockframework.runtime.extension.IMethodInterceptor

abstract class InjectGradleRunnerInterceptorBase implements IMethodInterceptor {
    private static pluginClasspath = getClass()
        .getResource('/plugin-classpath.txt')
        .readLines()
        .collect { new File(it) }
        .collect { it.toURI().toURL() }
        .collect { "'$it'" }
        .join(", ")

    private static supportedProjectDirProviderTypes = ProjectDirProviderCategory.methods
        .findAll { it.name == 'get' }
        .findAll { it.parameterCount == 1 }
        .collect { it.parameterTypes.first() }

    File determineProjectDir(projectDirProvider, fieldOrParameterName) {
        if(!projectDirProvider) {
            throw new RuntimeException("The project dir provider closure for the GradleRunner $fieldOrParameterName returned '$projectDirProvider'")
        }

        if(!supportedProjectDirProviderTypes.any { it.isAssignableFrom projectDirProvider.getClass() }) {
            throw new RuntimeException("The project dir provider closure for the GradleRunner $fieldOrParameterName " +
                "returned an object of the unsupported type '${projectDirProvider.getClass().typeName}'\n" +
                "\tsupported types:\n\t\t${supportedProjectDirProviderTypes.typeName.sort().join('\n\t\t')}")
        }

        def projectDir
        use(ProjectDirProviderCategory) {
            projectDir = projectDirProvider.get()
        }

        if(!projectDir) {
            throw new RuntimeException("The extracted directory from the project dir provider closure result for the GradleRunner '$fieldOrParameter.name' is '$projectDir'")
        }

        projectDir
    }

    GradleRunner prepareProjectDir(File projectDir) {
        // prepare the project dir with a basic Gradle project with this plugin applied
        new File(projectDir, 'settings.gradle').text = """
            buildscript {
                dependencies {
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: 'net.vivin.gradle-semantic-build-versioning'
        """.stripIndent()

        def jacocoAgentClasspath = getClass()
            .getResource('/jacoco-agent-classpath.txt')
            .readLines()
            .collect { it.replace '\\', '\\\\' }

        new File(projectDir, 'gradle.properties').text = """
            org.gradle.jvmargs = -javaagent:${jacocoAgentClasspath[0]}=destfile=${jacocoAgentClasspath[1]},includes=net.vivin.gradle.versioning.*
        """.stripIndent()

        new File(projectDir, 'semantic-build-versioning.gradle').createNewFile()

        // create, configure and return the gradle runner instance
        GradleRunner
            .create()
            .forwardStdOutput(System.out.newWriter())
            .forwardStdError(System.err.newWriter())
            .withProjectDir(projectDir)
    }
}
