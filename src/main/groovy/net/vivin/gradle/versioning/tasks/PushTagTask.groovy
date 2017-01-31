package net.vivin.gradle.versioning.tasks

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created on 1/31/17 at 4:09 PM
 * @author vivin
 */
class PushTagTask extends DefaultTask {

    @TaskAction
    void pushTag() {
        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(project.projectDir)
            .findGitDir(project.projectDir)
            .build()

        TagTask tag = dependsOn.findAll {
            it instanceof TagTask
        }.find() as TagTask

        new Git(repository)
            .push()
            .add(tag.tagRef)
            .call()
    }
}
