package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

import static org.testng.Assert.assertEquals

class MajorMinorPatchBumpingTests extends TestNGRepositoryTestCase {

    @Test
    void testVersionWithoutPriorTagsIsDefaultStartingSnapshotVersion() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        snapshot(version)

        assertEquals(project.version.toString(), "0.1.0-SNAPSHOT")
    }

    @Test
    void testVersionWithoutPriorTagsIsDefaultStartingReleaseVersion() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)

        assertEquals(project.version.toString(), "0.1.0")
    }

    @Test(expectedExceptions = BuildException)
    void testCreatingReleaseVersionWithUncommittedChangesCausesBuildToFail() {
        testRepository.makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)

        version.toString()
    }

    @Test
    void testVersionWithPriorTagAndUncommittedChangesIsNextSnapshotVersion() {
        testRepository
            .commitAndTag("0.0.1")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        snapshot(version)

        assertEquals(project.version.toString(), "0.0.2-SNAPSHOT")
    }

    @Test
    void testVersionWithPriorTagAndCommittedChangesIsNextReleaseVersion() {
        testRepository
            .commitAndTag("0.0.1")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)

        assertEquals(project.version.toString(), "0.0.2")
    }

    @Test
    void testVersionWithCustomSnapshotSuffix() {
        testRepository.makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.snapshotSuffix = "CURRENT"
        snapshot(version)

        assertEquals(project.version.toString(), "0.1.0-CURRENT")
    }

    @Test
    void testCheckingOutTagProducesSameVersionAsTag() {
        testRepository
            .commitAndTag("3.1.2")
            .commitAndTag("3.1.3")
            .commitAndTag("3.1.4")
            .checkout("3.1.2")

        assertEquals(project.version.toString(), "3.1.2")
    }

    @Test
    void testCheckingOutTagProducesSameVersionAsTagEvenIfOtherTagsArePresent() {
        testRepository
            .commitAndTag("3.1.2")
            .commitAndTag("3.1.3")
            .commitAndTag("3.1.4")
            .checkout("3.1.2")
            .tag("foo")

        assertEquals(project.version.toString(), "3.1.2")
    }

    @Test
    void testBumpingPatchVersionForSnapshot() {
        testRepository
            .commitAndTag("0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.PATCH
        snapshot(version)

        assertEquals(project.version.toString(), "0.0.3-SNAPSHOT")
    }

    @Test
    void testBumpingPatchVersionForRelease() {
        testRepository
            .commitAndTag("0.0.2")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.PATCH
        release(version)

        assertEquals(project.version.toString(), "0.0.3")
    }

    @Test
    void testBumpingMinorVersionForSnapshot() {
        testRepository
            .commitAndTag("0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MINOR
        snapshot(version)

        assertEquals(project.version.toString(), "0.1.0-SNAPSHOT")
    }

    @Test
    void testBumpingMinorVersionForRelease() {
        testRepository
            .commitAndTag("0.1.3")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MINOR
        release(version)

        assertEquals(project.version.toString(), "0.2.0")
    }

    @Test
    void testBumpingMajorVersionForSnapshot() {
        testRepository
            .commitAndTag("0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MAJOR
        snapshot(version)

        assertEquals(project.version.toString(), "1.0.0-SNAPSHOT")
    }

    @Test
    void testBumpingMajorVersionForRelease() {
        testRepository
            .commitAndTag("0.2.3")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MAJOR
        release(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test
    void testBumpingPatchVersionWithTagPatternForSnapshot() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.bump = VersionComponent.PATCH
        snapshot(version)

        assertEquals(project.version.toString(), "0.0.3-SNAPSHOT")
    }

    @Test
    void testBumpingPatchVersionWithTagPatternForRelease() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        release(version)

        assertEquals(project.version.toString(), "0.0.3")
    }

    @Test
    void testBumpingMinorVersionWithTagPatternForSnapshot() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.bump = VersionComponent.MINOR
        snapshot(version)

        assertEquals(project.version.toString(), "0.1.0-SNAPSHOT")
    }

    @Test
    void testBumpingMinorVersionWithTagPatternForRelease() {
        testRepository
            .commitAndTag("foo-0.2.1")
            .commitAndTag("foo-0.2.2")
            .commitAndTag("bar-0.1.1")
            .commitAndTag("bar-0.1.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.bump = VersionComponent.MINOR
        release(version)

        assertEquals(project.version.toString(), "0.2.0")
    }

    @Test
    void testBumpingMajorVersionWithTagPatternForSnapshot() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.bump = VersionComponent.MAJOR
        snapshot(version)

        assertEquals(project.version.toString(), "1.0.0-SNAPSHOT")
    }

    @Test
    void testBumpingMajorVersionWithTagPatternForRelease() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.bump = VersionComponent.MAJOR
        release(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test
    void testBumpingPatchVersionWithVersionsMatchingForSnapshot() {
        testRepository
            .commitAndTag("0.1.1")
            .commitAndTag("0.1.2")
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 0
            minor = 1
        }
        version.bump = VersionComponent.PATCH
        snapshot(version)

        assertEquals(project.version.toString(), "0.1.3-SNAPSHOT")
    }

    @Test
    void testBumpingPatchVersionWithVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("1.1.1")
            .commitAndTag("1.1.2")
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 1
        }
        release(version)

        assertEquals(project.version.toString(), "1.1.3")
    }

    @Test
    void testBumpingMinorVersionWithVersionsMatchingForSnapshot() {
        testRepository
            .commitAndTag("1.1.1")
            .commitAndTag("1.1.2")
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 1
            minor = 1
            patch = 2
        }
        version.bump = VersionComponent.MINOR
        snapshot(version)

        assertEquals(project.version.toString(), "1.2.0-SNAPSHOT")
    }

    @Test
    void testBumpingMinorVersionWithVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("1.2.1")
            .commitAndTag("1.2.2")
            .commitAndTag("1.1.1")
            .commitAndTag("1.1.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 1
            minor = 2
        }
        version.bump = VersionComponent.MINOR
        release(version)

        assertEquals(project.version.toString(), "1.3.0")
    }

    @Test
    void testBumpingMajorVersionWithVersionsMatchingForSnapshot() {
        testRepository
            .commitAndTag("3.1.1")
            .commitAndTag("3.1.2")
            .commitAndTag("2.0.1")
            .commitAndTag("2.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 3
        }
        version.bump = VersionComponent.MAJOR
        snapshot(version)

        assertEquals(project.version.toString(), "4.0.0-SNAPSHOT")
    }

    @Test
    void testBumpingMajorVersionWithVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("0.1.1")
            .commitAndTag("0.1.2")
            .commitAndTag("0.0.1")
            .commitAndTag("0.0.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.matching {
            major = 0
        }
        version.bump = VersionComponent.MAJOR
        release(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test
    void testBumpingPatchVersionWithTagPatternAndVersionsMatchingForSnapshot() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("foo-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^foo-/
        version.matching {
            major = 0
            minor = 1
        }
        version.bump = VersionComponent.PATCH
        snapshot(version)

        assertEquals(project.version.toString(), "0.1.3-SNAPSHOT")
    }

    @Test
    void testBumpingPatchVersionWithTagPatternAndVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("foo-1.1.1")
            .commitAndTag("foo-1.1.2")
            .commitAndTag("foo-0.0.1")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^foo-/
        version.matching {
            major = 1
        }
        release(version)

        assertEquals(project.version.toString(), "1.1.3")
    }

    @Test
    void testBumpingMinorVersionWithTagPatternAndVersionsMatchingForSnapshot() {
        testRepository
            .commitAndTag("foo-1.1.1")
            .commitAndTag("foo-1.1.2")
            .commitAndTag("foo-0.0.1")
            .commitAndTag("foo-0.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^foo/
        version.matching {
            major = 1
            minor = 1
            patch = 2
        }
        version.bump = VersionComponent.MINOR
        snapshot(version)

        assertEquals(project.version.toString(), "1.2.0-SNAPSHOT")
    }

    @Test
    void testBumpingMinorVersionWithTagPatternAndVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("foo-1.2.1")
            .commitAndTag("bar-1.2.2")
            .commitAndTag("bar-1.1.1")
            .commitAndTag("bar-1.1.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^foo-/
        version.matching {
            major = 1
            minor = 2
        }
        version.bump = VersionComponent.MINOR
        release(version)

        assertEquals(project.version.toString(), "1.3.0")
    }

    @Test
    void testBumpingMajorVersionWithTagPatternAndVersionsMatchingForSnapshot() {
        testRepository
            .commitAndTag("foo-3.1.1")
            .commitAndTag("bar-3.1.2")
            .commitAndTag("bar-2.0.1")
            .commitAndTag("bar-2.0.2")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.matching {
            major = 3
        }
        version.bump = VersionComponent.MAJOR
        snapshot(version)

        assertEquals(project.version.toString(), "4.0.0-SNAPSHOT")
    }

    @Test
    void testBumpingMajorVersionWithTagPatternAndVersionsMatchingForRelease() {
        testRepository
            .commitAndTag("foo-0.1.1")
            .commitAndTag("bar-0.1.2")
            .commitAndTag("bar-0.0.1")
            .commitAndTag("bar-0.0.2")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.tagPattern = ~/^bar-/
        version.matching {
            major = 0
        }
        version.bump = VersionComponent.MAJOR
        release(version)

        assertEquals(project.version.toString(), "1.0.0")
    }

    @Test(expectedExceptions = BuildException)
    void testBumpingPatchVersionWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")
            .commitAndTag("1.0.1")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.PATCH
        release(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpingMinorVersionWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")
            .commitAndTag("1.0.1")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MINOR
        release(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpingMajorVersionWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")
            .commitAndTag("1.0.1")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MAJOR
        release(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpingPatchVersionForSnapshotWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")
            .commitAndTag("1.0.1")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.PATCH
        snapshot(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpingMinorVersionForSnapshotWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")
            .commitAndTag("1.0.1")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MINOR
        snapshot(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpingMajorVersionForSnapshotWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")
            .commitAndTag("1.0.1")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.MAJOR
        snapshot(version)

        project.version.toString()
    }
}
