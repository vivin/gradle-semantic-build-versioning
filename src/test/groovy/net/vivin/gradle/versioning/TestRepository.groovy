package net.vivin.gradle.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository

class TestRepository {

    Repository repository

    TestRepository(Repository repository) {
        this.repository = repository
    }

    TestRepository commitAndTag(String tag) {
        Git git = new Git(repository)
        git.commit()
            .setAuthor("Batman", "batman@waynemanor.com")
            .setMessage("blah")
            .call()

        git.tag()
            .setAnnotated(true)
            .setName(tag)
            .call()

        return this
    }

    TestRepository commit() {
        Git git = new Git(repository)
        git.commit()
            .setAuthor("Batman", "batman@waynemanor.com")
            .setMessage("blah")
            .call()

        return this
    }

    TestRepository commit(String message) {
        Git git = new Git(repository)
        git.commit()
            .setAuthor("Batman", "batman@waynemanor.com")
            .setMessage(message)
            .call()

        return this
    }

    TestRepository makeChanges() {
        Git git = new Git(repository)
        File file = new File(repository.directory.parentFile.absolutePath, "file")
        file.createNewFile()
        git.add().addFilepattern("file").call()

        return this
    }

    TestRepository checkout(String tag) {
        Git git = new Git(repository)
        git.checkout()
            .setName(tag)
            .call()

        return this
    }

    String getHeadTag() {
        return new Git(repository)
            .describe()
            .setTarget(repository.resolve(Constants.HEAD))
            .call()
    }
}
