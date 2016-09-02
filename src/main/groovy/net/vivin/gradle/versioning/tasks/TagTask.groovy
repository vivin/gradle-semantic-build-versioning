package net.vivin.gradle.versioning.tasks

import net.vivin.gradle.versioning.SemanticBuildVersion
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
    boolean push
    SemanticBuildVersion semanticBuildVersion

    @TaskAction
    void tag() {
        if(semanticBuildVersion.versionUtils.hasUncommittedChanges()) {
            throw new BuildException("Cannot create a tag when there are uncommitted changes", null)
        } else if(semanticBuildVersion.snapshot) {
            throw new BuildException("Cannot create a tag for a snapshot version", null)
        }

        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(new File(project.getRootProject().projectDir.absolutePath))
            .findGitDir(new File(project.getRootProject().projectDir.absolutePath))
            .build()

        String tag = semanticBuildVersion.toString()
        if(!semanticBuildVersion.tagPrefix.isEmpty()) {
            tag = String.format("%s-%s", semanticBuildVersion.tagPrefix, tag)
        }

        Git git = new Git(repository)
        def tagRef = git.tag().setName(tag).call()
        if (push) {
            git.push().add(tagRef).call()
        }
    }
}
