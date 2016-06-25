package net.vivin.gradle.versioning

import org.eclipse.jgit.junit.RepositoryTestCase
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod

class TestNGRepositoryTestCase extends RepositoryTestCase {

    protected TestRepository testRepository;
    protected Project project

    static protected void release(SemanticBuildVersion version) {
        version.snapshot = false
    }

    static protected void snapshot(SemanticBuildVersion version) {
        version.snapshot = true
    }

    static protected void autobump(SemanticBuildVersion version) {
        version.autobump = true
    }

    @BeforeMethod
    void setUp() {
        super.setUp()
        testRepository = new TestRepository(db)

        project = ProjectBuilder.builder()
            .withProjectDir(testRepository.repository.getDirectory().getParentFile())
            .build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }
}
