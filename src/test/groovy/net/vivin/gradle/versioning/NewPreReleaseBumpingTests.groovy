package net.vivin.gradle.versioning

import static org.testng.Assert.assertEquals

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

class NewPreReleaseBumpingTests extends TestNGRepositoryTestCase {

    @Test(expectedExceptions = BuildException)
    void testNewPreReleaseVersionWithoutPreReleaseConfigurationCausesBuildToFail() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        newPreRelease(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testNewPreReleaseVersionWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        newPreRelease(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testNewPreReleaseSnapshotVersionWhenHeadIsPointingToTagCausesBuildToFail() {
        testRepository
            .commitAndTag("1.0.0")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        snapshot(version)
        newPreRelease(version)

        project.version.toString()
    }

    @Test
    void testNewPreReleaseVersionWithoutPriorVersions() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithoutPriorVersionsWithBumpPatch() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PATCH
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithoutMatchingTags() {
        testRepository.commitAndTag("foo-0.1.0");
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.tagPattern = ~/^bar/
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithoutPriorVersionsWithBumpMinor() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MINOR
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithoutPriorVersionsWithBumpMajor() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MAJOR
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorVersionWithBumpPatch() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PATCH
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorVersionWithBumpMinor() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MINOR
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.3.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorVersionWithBumpMajor() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MAJOR
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorPreReleaseVersionWithBumpPatch() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PATCH
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorPreReleaseVersionWithBumpMinor() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MINOR
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.3.0-pre.0")
    }

    @Test
    void testNewPreReleaseVersionWithPriorPreReleaseVersionWithBumpMajor() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MAJOR
        release(version)
        newPreRelease(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithoutMatchingTags() {
        testRepository.commitAndTag("foo-0.1.0");
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.tagPattern = ~/^bar/
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithoutPriorVersions() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithoutPriorVersionsWithBumpPatch() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PATCH
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithoutPriorVersionsWithBumpMinor() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MINOR
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithoutPriorVersionsWithBumpMajor() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MAJOR
        newPreRelease(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorVersionWithBumpPatch() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PATCH
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorVersionWithBumpMinor() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MINOR
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.3.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorVersionWithBumpMajor() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MAJOR
        newPreRelease(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorPreReleaseVersionWithBumpPatch() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PATCH
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorPreReleaseVersionWithBumpMinor() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MINOR
        newPreRelease(version)

        assertEquals(project.version.toString(), "0.3.0-pre.0-SNAPSHOT")
    }

    @Test
    void testSnapshotNewPreReleaseVersionWithPriorPreReleaseVersionWithBumpMajor() {
        testRepository
            .commitAndTag("0.2.0-pre.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.MAJOR
        newPreRelease(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0-SNAPSHOT")
    }
}
