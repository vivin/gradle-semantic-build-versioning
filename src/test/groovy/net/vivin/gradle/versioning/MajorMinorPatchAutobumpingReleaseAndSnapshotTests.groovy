package net.vivin.gradle.versioning

import static org.testng.Assert.assertEquals

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

/**
 * Created on 6/24/16 at 5:39 PM
 * @author vivin
 */
class MajorMinorPatchAutobumpingReleaseAndSnapshotTests extends TestNGRepositoryTestCase {

    @Test(expectedExceptions = BuildException)
    void testAutobumpingWithoutAnyPriorCommitsCausesBuildToFail() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpingWithoutMatchingPriorCommitMessageCausesBuildToFail() {
        testRepository.commit("This is a message\npatch")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test
    void testAutobumpedVersionWithoutPriorTagsIsDefaultStartingReleaseVersion() {
        testRepository.commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.0.1")
    }

    @Test
    void testAutobumpedVersionWithPriorTagAndCommittedChangesIsNextReleaseVersion() {
        testRepository
            .commitAndTag("0.0.1")
            .makeChanges()
            .commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.0.2")
    }

    @Test
    void testAutobumpingPatchVersionForRelease() {
        testRepository
            .commitAndTag("0.0.2")
            .makeChanges()
            .commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.0.3")
    }

    @Test
    void testAutobumpingMinorVersionForRelease() {
        testRepository
            .commitAndTag("0.1.3")
            .makeChanges()
            .commit("This is a message\n[minor]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.0")
    }

    @Test
    void testAutobumpingMajorVersionForRelease() {
        testRepository
            .commitAndTag("0.2.3")
            .makeChanges()
            .commit("This is a message\n[major]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test
    void testAutobumpingPatchVersionWithTagPatternForRelease() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.0.3")
    }

    @Test
    void testAutobumpingMinorVersionWithTagPatternForRelease() {
        testRepository
            .commitAndTag("foo-0.2.1")
            .commitAndTag("foo-0.2.2")
            .commitAndTag("bar-0.1.1")
            .commitAndTag("bar-0.1.2")
            .commit("This is a message\n[minor]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.0")
    }

    @Test
    void testAutobumpingMajorVersionWithTagPatternForRelease() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit("This is a message\n[major]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test(expectedExceptions = BuildException)
    void testTagPatternThatDoesNotMatchAnyTagCausesBuildFailureWithAutobump() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit("This is a message\n[major]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^baz-/
        release(version)
        autobump(version)

        version.toString()
    }

    @Test
    void testAutobumpingPatchVersionWithVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("1.1.1")
            .commitAndTag("1.1.2")
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 1
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.1.3")
    }

    @Test
    void testAutobumpingMinorVersionWithVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("1.2.1")
            .commitAndTag("1.2.2")
            .commitAndTag("1.1.1")
            .commitAndTag("1.1.2")
            .commit("This is a message\n[minor]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 1
            minor = 2
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.3.0")
    }

    @Test
    void testAutobumpingMajorVersionWithVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("0.1.1")
            .commitAndTag("0.1.2")
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commit("This is a message\n[major]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 0
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test(expectedExceptions = BuildException)
    void testVersionsMatchingThatDoesNotMatchAnyTagCausesBuildFailureWithAutobump() {
        testRepository
            .commitAndTag("0.1.1")
            .commitAndTag("0.1.2")
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 3
        }
        release(version)
        autobump(version)

        version.toString()
    }

    @Test
    void testAutobumpingPatchVersionWithTagPatternAndVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("foo-1.1.1")
            .commitAndTag("foo-1.1.2")
            .commitAndTag("foo-0.0.1")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^foo-/
        version.matching {
            major = 1
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.1.3")
    }

    @Test
    void testAutobumpingMinorVersionWithTagPatternAndVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("foo-1.2.1")
            .commitAndTag("bar-1.2.2")
            .commitAndTag("bar-1.1.1")
            .commitAndTag("bar-1.1.2")
            .commit("This is a message\n[minor]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^foo-/
        version.matching {
            major = 1
            minor = 2
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.3.0")
    }

    @Test
    void testAutobumpingMajorVersionWithTagPatternAndVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("bar-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit("This is a message\n[major]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.matching {
            major = 0
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test(expectedExceptions = BuildException)
    void testTagPatternAndVersionsMatchingThatDoesNotMatchAnyTagCausesBuildFailureWithAutobump() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit("This is a message\n[patch]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^foo-/
        version.matching {
            major = 0
            minor = 1
            patch = 3
        }
        release(version)
        autobump(version)

        version.toString()
    }
}
