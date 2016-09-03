package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

import static org.testng.Assert.assertEquals

class PreReleaseBumpingTests extends TestNGRepositoryTestCase {

    @Test(expectedExceptions = BuildException)
    void testBumpingPreReleaseVersionWithoutPreReleaseConfigurationCausesBuildToFail() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.bump = VersionComponent.PRERELEASE

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpedPreReleaseSnapshotVersionWithoutPriorPreReleaseVersionCausesBuildToFail() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        snapshot(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpedPreReleaseVersionWithoutPriorPreReleaseVersionCausesBuildToFail() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpedPreReleaseVersionWithPriorNonPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        snapshot(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpedPreReleaseSnapshotVersionWithPriorNonPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        project.version.toString()
    }

    @Test
    void testBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersionForSnapshot() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PRERELEASE
        snapshot(version)

        assertEquals(project.version.toString(), "0.2.1-alpha.1-SNAPSHOT")
    }

    @Test
    void testBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        assertEquals(project.version.toString(), "0.2.1-alpha.1")
    }

    @Test
    void testImplicitlyBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersionForSnapshot() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        snapshot(version)

        assertEquals(project.version.toString(), "0.2.1-alpha.1-SNAPSHOT")
    }

    @Test
    void testImplicitlyBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)

        assertEquals(project.version.toString(), "0.2.1-alpha.1")
    }

    @Test(expectedExceptions = BuildException)
    void testImplicitlyBumpingPreReleaseVersionWithoutPreReleaseConfigurationWithLatestPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.1-alpha.0")
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testBumpedPreReleaseVersionWithInvalidBumpedPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}.00^1"
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternBumpedPreReleaseSnapshotVersionWithoutPriorPreReleaseVersionCausesBuildToFail() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        snapshot(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternBumpedPreReleaseVersionWithoutPriorPreReleaseVersionCausesBuildToFail() {
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternBumpedPreReleaseSnapshotVersionWithPriorNonPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        snapshot(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternBumpedPreReleaseVersionWithPriorNonPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternBumpedPreReleaseSnapshotVersionWithNonMatchingPriorPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PRERELEASE
        snapshot(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternBumpedPreReleaseVersionWithNonMatchingPriorPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        project.version.toString()
    }

    @Test
    void testWithPatternBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersionForSnapshot() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PRERELEASE
        snapshot(version)

        assertEquals(project.version.toString(), "0.2.1-beta.1-SNAPSHOT")
    }

    @Test
    void testWithPatternBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        version.bump = VersionComponent.PRERELEASE
        release(version)

        assertEquals(project.version.toString(), "0.2.1-beta.1")
    }

    @Test
    void testWithPatternImplicitlyBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersionForSnapshot() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        snapshot(version)

        assertEquals(project.version.toString(), "0.2.1-beta.1-SNAPSHOT")
    }

    @Test
    void testWithPatternImplicitlyBumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)

        assertEquals(project.version.toString(), "0.2.1-beta.1")
    }

    @Test
    void testPromotingPreReleaseVersionWithSnapshot() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        snapshot(version)
        promote(version)

        assertEquals(project.version.toString(), "0.2.1-SNAPSHOT")
    }

    @Test
    void testPromotingPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()
            .commit()

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        promote(version)

        assertEquals(project.version.toString(), "0.2.1")
    }
}
