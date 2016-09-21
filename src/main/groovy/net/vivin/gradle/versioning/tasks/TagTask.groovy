package net.vivin.gradle.versioning.tasks

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException

/**
 * Created on 6/23/16 at 10:26 AM
 * @author vivin
 */
class TagTask extends DefaultTask {
    def tagPrefix
    boolean push

    @TaskAction
    void tag() {
        if(project.hasUncommittedChanges()) {
            throw new BuildException("Cannot create a tag when there are uncommitted changes", null)
        } else if(project.version.snapshot) {
            throw new BuildException("Cannot create a tag for a snapshot version", null)
        }

        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(project.projectDir)
            .findGitDir(project.projectDir)
            .build()

        String tag = String.format("%s%s", tagPrefix, project.version as String)

        Git git = new Git(repository)
        def tagRef = git.tag().setAnnotated(false).setName(tag).call()
        if(push) {
            git.push().add(tagRef).call()
        }
    }
}
