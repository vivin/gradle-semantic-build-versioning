package net.vivin.gradle.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.URIish
import org.gradle.internal.impldep.org.apache.commons.lang.RandomStringUtils

class TestRepository {
    Repository repository

    TestRepository(Repository repository) {
        this.repository = repository
    }

    TestRepository commitAndTag(String tag, boolean annotated = false) {
        Git git = new Git(repository)
        git.commit()
            .setAuthor("Batman", "batman@waynemanor.com")
            .setMessage("blah")
            .call()

        git.tag()
            .setAnnotated(annotated)
            .setName(tag)
            .call()

        return this
    }

    TestRepository tag(String tag, boolean annotated = false) {
        Git git = new Git(repository)
        git.tag()
            .setAnnotated(annotated)
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

        String fileName = String.format("file-%s", RandomStringUtils.randomAlphanumeric(5))
        File file = new File(repository.directory.parentFile.absolutePath, fileName)
        file.createNewFile()
        file.write(RandomStringUtils.randomAlphanumeric(20))

        git.add().addFilepattern(fileName).call()

        return this
    }

    TestRepository add(String filePattern) {
        Git git = new Git(repository)
        git.add().addFilepattern(filePattern).call()

        return this
    }

    TestRepository checkoutBranch(String branch) {
        Git git = new Git(repository)
        git.checkout()
            .setName(branch)
            .call()

        return this
    }

    TestRepository checkout(String revString) {
        Git git = new Git(repository)
        git.checkout()
            .setName(repository.resolve(revString).name)
            .call()

        return this
    }

    TestRepository branch(String name) {
        Git git = new Git(repository)
        git.branchCreate()
            .setName(name)
            .call()

        return this
    }

    TestRepository merge(String target) {
        Git git = new Git(repository)
        git.merge()
            .include(repository.resolve(target))
            .call()

        return this
    }

    TestRepository setOrigin(TestRepository origin) {
        Git git = new Git(repository)
        def remoteAddCommand = git.remoteAdd()
        remoteAddCommand.name = 'origin'
        remoteAddCommand.uri = new URIish(origin.repository.directory.toURI().toURL())
        remoteAddCommand.call()

        return this
    }

    String getHeadTag() {
        return new Git(repository)
            .describe()
            .setTarget(repository.resolve(Constants.HEAD))
            .call()
    }
}
