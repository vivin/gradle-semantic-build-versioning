package net.vivin.gradle.versioning.tasks

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException

import java.util.concurrent.Callable

/**
 * Created on 6/23/16 at 10:26 AM
 * @author vivin
 */
class TagTask extends DefaultTask {
    @Internal
    Callable<?> message = { '' }

    private Closure<String> fromEnvironmentVariableClosure = { System.env."$it" }

    private Closure<String> fromMandatoryEnvironmentVariableClosure = {
        def message = fromEnvironmentVariable(it)()
        if(message == null) {
            throw new BuildException("Mandatory environment variable '$it' was not found", null)
        }
        message
    }

    private Closure<String> fromSystemPropertyClosure = { System.properties."$it" }

    private Closure<String> fromMandatorySystemPropertyClosure = {
        def message = fromSystemProperty(it)()
        if(message == null) {
            throw new BuildException("Mandatory system property '$it' was not found", null)
        }
        message
    }

    private Closure<Object> fromProjectPropertyClosure = { project.hasProperty(it) ? project.property(it) : null }

    private Closure<Object> fromMandatoryProjectPropertyClosure = {
        def message = fromProjectProperty(it)()
        if(message == null) {
            throw new BuildException("Mandatory project property '$it' was not found", null)
        }
        message
    }

    @Internal
    def tagPrefix

    @Internal
    @Option(description = 'Automatically push the created tag')
    boolean push

    @TaskAction
    void tag() {
        if(project.hasUncommittedChanges()) {
            throw new BuildException('Cannot create a tag when there are uncommitted changes', null)
        } else if(project.version.snapshot) {
            throw new BuildException('Cannot create a tag for a snapshot version', null)
        }

        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(project.projectDir)
            .findGitDir(project.projectDir)
            .build()

        String tag = "$tagPrefix$project.version"

        Git git = new Git(repository)

        def tagMessage = (message?.call() as String)?.trim()

        def tagRef = git.tag()
            .setAnnotated(tagMessage != null)
            .setMessage(tagMessage)
            .setName(tag)
            .call()

        if(push) {
            git.push().add(tagRef).call()
        }
    }

    void message(Callable<?> messageProvider) {
        message = messageProvider
    }

    Closure<String> fromEnvironmentVariable(String variableName) {
        fromEnvironmentVariableClosure.curry variableName
    }

    Closure<String> fromMandatoryEnvironmentVariable(String variableName) {
        fromMandatoryEnvironmentVariableClosure.curry variableName
    }

    Closure<String> fromSystemProperty(String propertyName) {
        fromSystemPropertyClosure.curry propertyName
    }

    Closure<String> fromMandatorySystemProperty(String propertyName) {
        fromMandatorySystemPropertyClosure.curry propertyName
    }

    Closure<Object> fromProjectProperty(String propertyName) {
        fromProjectPropertyClosure.curry propertyName
    }

    Closure<Object> fromMandatoryProjectProperty(String propertyName) {
        fromMandatoryProjectPropertyClosure.curry propertyName
    }
}
