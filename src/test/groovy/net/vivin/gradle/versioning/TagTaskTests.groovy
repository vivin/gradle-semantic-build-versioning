package net.vivin.gradle.versioning

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod

import static org.testng.Assert.assertEquals

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

class TagTaskTests extends TestNGRepositoryTestCase {

    @Override
    @BeforeMethod
    void setUp() {
        super.setUp()
        Repository bare = createBareRepository()
 		File clone = createTempDirectory("clone")
        Git git = Git.cloneRepository().setURI(bare.getDirectory().getAbsolutePath()).setDirectory(clone).call()
        db = (FileRepository) git.repository

        testRepository = new TestRepository(db)

        project = ProjectBuilder.builder()
            .withProjectDir(testRepository.repository.getDirectory().getParentFile())
            .build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = BuildException)
    void testTaggingWithUncommittedChangesCausesBuildToFail() {
        testRepository
            .commitAndTag("0.0.1")
            .makeChanges()

        project.tasks.tag.tag()
    }

    @Test(expectedExceptions = BuildException)
    void testTaggingSnapshotVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.0.1")
            .makeChanges()
            .commit()
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        snapshot(version)

        project.tasks.tag.tag()
    }

    @Test
    void testTagIsCreated() {
        testRepository
            .commitAndTag("0.0.1")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)

        project.tasks.tag.tag()

        version.versionUtils.refresh()
        assertEquals(testRepository.getHeadTag(), "0.0.2")
    }

    @Test
    void testTagIsCreatedWithPrefix() {
        testRepository
            .commitAndTag("prefix-0.0.1")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPrefix = "prefix-"
        release(version)

        project.tasks.tag.tag()

        version.versionUtils.refresh()
        assertEquals(testRepository.getHeadTag(), "prefix-0.0.2")
    }

    @Test
    void testTagIsCreatedWithDashlessPrefix() {
        testRepository
            .commitAndTag("v0.0.1")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPrefix = "v"
        release(version)

        project.tasks.tag.tag()

        version.versionUtils.refresh()
        assertEquals(testRepository.getHeadTag(), "v0.0.2")
    }
}
