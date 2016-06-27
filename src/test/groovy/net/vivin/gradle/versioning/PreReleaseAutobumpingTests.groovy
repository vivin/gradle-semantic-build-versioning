package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

import static org.testng.Assert.assertEquals

class PreReleaseAutobumpingTests extends TestNGRepositoryTestCase {

    @Test(expectedExceptions = BuildException)
    void testBumpingPreReleaseVersionWithoutPreReleaseConfigurationCausesBuildToFail() {
        testRepository
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        release(version)
        autobump(version)

        version.toString()
    }

    @Test
    void testAutobumpedPreReleaseVersionWithoutPriorPreReleaseVersionIsStartingPreReleaseVersion() {
        testRepository
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.0.1-alpha.0")
    }

    @Test
    void testAutobumpedPreReleaseVersionWithPriorNonPreReleaseVersionIsPatchBumpedStartingPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-alpha.0")
    }

    @Test
    void testAutobumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-alpha.1")
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpedPreReleaseVersionWithInvalidBumpedPreReleaseVersionCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}.00^1"
            }
        }
        release(version)
        autobump(version)

        version.toString()
    }

    @Test
    void testWithPatternAutobumpedPreReleaseVersionWithoutPriorPreReleaseVersionIsStartingPreReleaseVersion() {
        testRepository
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.0.1-beta.0")
    }

    @Test
    void testWithPatternAutobumpedPreReleaseVersionWithPriorNonPreReleaseVersionIsPatchBumpedStartingPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "beta.0"
            pattern = ~/beta/
            bump { String s ->
                s
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-beta.0")
    }

    @Test
    void testWithPatternAutobumpedPreReleaseVersionWithPriorPreReleaseVersionIsNewPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

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
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-beta.0")
    }

    @Test
    void testWithPatternAutobumpedPreReleaseVersionWithPriorPreReleaseVersionIsBumpedPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

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
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-beta.1")
    }

    @Test(expectedExceptions = BuildException)
    void testWithPatternAutobumpedPreReleaseVersionWithoutMatchingTagsCausesBuildToFail() {
        testRepository
            .commitAndTag("0.2.0-alpha.0")
            .makeChanges()
            .commit("This is a message\n[pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "alpha.0"
            pattern = ~/beta/
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}.00^1"
            }
        }
        release(version)
        autobump(version)

        version.toString()
    }

    @Test
    void testPromotingPreReleaseVersion() {
        testRepository
            .commitAndTag("0.2.0")
            .commitAndTag("0.2.1-alpha.0")
            .commitAndTag("0.2.1-beta.0")
            .makeChanges()
            .commit("This is a message\n[promote]")

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
        autobump(version)
        promote(version)

        assertEquals(project.version.toString(), "0.2.1")
    }
}
