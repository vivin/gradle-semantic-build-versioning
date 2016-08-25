package net.vivin.gradle.versioning

import static org.testng.Assert.assertEquals

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

class NewPreReleaseAutobumpingTests extends TestNGRepositoryTestCase {

    @Test(expectedExceptions = BuildException)
    void testNewReleaseAutobumpingWithPreReleaseBumpCausesBuildToFail() {
        testRepository.commit("This is a message\n[new-pre-release]\n[pre-release]")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testNewReleaseAutobumpingWithPromoteToReleaseCausesBuildToFail() {
        testRepository.commit("This is a message\n[new-pre-release]\n[promote]")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        project.version.toString()
    }

    @Test
    void testNewReleaseAutobumpingWithoutPriorVersionsAndWithoutExplicitBump() {
        testRepository.commit("This is a message\n[new-pre-release]")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0")
    }

    @Test
    void testNewReleaseAutobumpingWithoutPriorVersionsWithBumpPatch() {
        testRepository.commit("This is a message\n[patch]\n[new-pre-release]")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.1.0-pre.0")
    }

    @Test
    void testNewReleaseAutobumpingWithoutPriorVersionsWithBumpMinor() {
        testRepository.commit("This is a message\n[minor]\n[new-pre-release]")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.0-pre.0")
    }

    @Test
    void testNewReleaseAutobumpingWithoutPriorVersionsWithBumpMajor() {
        testRepository.commit("This is a message\n[major]\n[new-pre-release]")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0")
    }

    @Test
    void testNewReleaseAutobumpingWithPriorVersionsAndWithoutExplicitBump() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[new-pre-release]")
        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0")
    }

    @Test
    void testNewReleaseAutobumpingWithPriorVersionWithBumpPatch() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[patch]\n[new-pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.2.1-pre.0")
    }

    @Test
    void testNewReleaseAutobumpingWithPriorVersionWithBumpMinor() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[minor]\n[new-pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "0.3.0-pre.0")
    }

    @Test
    void testNewReleaseAutobumpingWithPriorVersionWithBumpMajor() {
        testRepository
            .commitAndTag("0.2.0")
            .makeChanges()
            .commit("This is a message\n[major]\n[new-pre-release]")

        SemanticBuildVersion version = (SemanticBuildVersion) project.getVersion()
        version.preRelease {
            startingVersion = "pre.0"
            bump { String s ->
                String[] parts = s.split("\\.")
                return "${parts[0]}.${Integer.parseInt(parts[1]) + 1}"
            }
        }
        release(version)
        autobump(version)

        assertEquals(project.version.toString(), "1.0.0-pre.0")
    }
}
