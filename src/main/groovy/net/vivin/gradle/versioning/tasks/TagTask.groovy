package net.vivin.gradle.versioning.tasks

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException

/**
 * Created on 6/23/16 at 10:26 AM
 * @author vivin
 */
class TagTask extends DefaultTask {

    Closure<String> tagMessage = {
        "v${project.version}"
    }

    private Closure<String> fromSystemProperyClosure = { String p ->
        System.properties[p]
    } as Closure<String>

    private Closure<String> fromProjectPropertyClosure = { String p ->
        project.getProperties().get(p)
    } as Closure<String>

    @Internal
    def tagPrefix

    @Internal
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

        def tagRef
        if(!tagMessage) {
            tagRef = git.tag().setAnnotated(false).setName(tag).call()
        } else {
            tagRef = git.tag().setMessage(tagMessage.call()).setName(tag).call()
        }

        if(push) {
            git.push().add(tagRef).call()
        }
    }

    Closure<String> fromSystemProperty(String propertyName) {
        fromSystemProperyClosure.curry(propertyName)
    }

    Closure<String> fromProjectProperty(String propertyName) {
        fromProjectPropertyClosure.curry(propertyName)
    }
}
