package net.vivin.gradle.versioning.spock.extensions.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.BuildException
import org.spockframework.runtime.extension.IMethodInterceptor

abstract class InjectGradleRunnerInterceptorBase implements IMethodInterceptor {
    private static pluginClasspath
    private static jacocoAgentClasspath

    private static supportedProjectDirProviderTypes = ProjectDirProviderCategory.methods
        .findAll { it.name == 'get' }
        .findAll { it.parameterTypes.size() == 1 }
        .collect { it.parameterTypes.first() }

    static {
        def pluginClasspathFile = getClass().getResource('/plugin-classpath.txt')
        if (!pluginClasspathFile) {
            throw new BuildException('plugin-classpath.txt is missing, please execute "createPluginClasspathFile" Gradle task', null)
        }
        pluginClasspath = pluginClasspathFile
            .readLines()
            .collect { new File(it) }
            .collect { it.toURI().toURL() }
            .collect { "'$it'" }
            .join(", ")

        def jacocoAgentClasspathFile = getClass().getResource('/jacoco-agent-classpath.txt')
        if (!jacocoAgentClasspathFile) {
            throw new BuildException('jacoco-agent-classpath.txt is missing, please execute "createJacocoAgentClasspathFile" Gradle task', null)
        }
        jacocoAgentClasspath = jacocoAgentClasspathFile.readLines().collect { it.replace '\\', '\\\\' }
    }

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
