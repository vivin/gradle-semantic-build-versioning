package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException
import org.testng.annotations.Test

import static org.testng.Assert.*;

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class SemanticBuildVersionTest {

    @Test
    void testPluginAddsReleaseTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        project.afterEvaluate {
            assertNotNull(project.tasks.release, "There must be a release task")
        }
    }

    @Test
    void testPluginDoesNotAddReleaseTaskToProjectIfItAlreadyExists() {
        Project project = ProjectBuilder.builder().build()
        project.task("release", group: "forTesting")
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        // Will throw an exception if it attempts to add release when it already exists

        assertTrue(true)
    }

    @Test(expectedExceptions = BuildException)
    void testInvalidStartingVersionFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            startingVersion = "bad"
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testMatchingWithJustPatchFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
                patch = 2
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testMatchingWithJustMinorFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
                minor = 2
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testMatchingWithMinorAndPatchFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
                minor = 2
                patch = 2
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testMatchingWithMajorAndPatchFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
                major = 2
                patch = 2
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testMatchingWithoutAnyVersionsFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
            }
        }

        version.toString()
    }

    @Test
    void testMatchingWithJustMajorReturnsProperPattern() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
                major = 1
            }
        }

        assertEquals(version.versionsMatching.toPattern().toString(), "1\\.")
    }

    @Test
    void testMatchingWithMajorAndMinorReturnsProperPattern() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
                major = 1
                minor = 1
            }
        }

        assertEquals(version.versionsMatching.toPattern().toString(), "1\\.1\\.")
    }

    @Test
    void testMatchingWithMajorMinorAndPatchReturnsProperPattern() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            matching {
                major = 1
                minor = 1
                patch = 1
            }
        }

        assertEquals(version.versionsMatching.toPattern().toString(), "1\\.1\\.1")
    }

    @Test(expectedExceptions = BuildException)
    void testPreReleaseWithoutStartingVersionFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            preRelease {
            }
        }

        version.toString()
    }


    @Test(expectedExceptions = BuildException)
    void testPreReleaseWithoutPatternFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            preRelease {
                pattern = null
                startingVersion = "alpha.0"
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testPreReleaseWithInvalidStartingVersionFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            preRelease {
                startingVersion = "!bad!"
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException, expectedExceptionsMessageRegExp = ".*leading zeroes\$")
    void testPreReleaseWithLeadingZeroesInStartingVersionIdentifierFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            preRelease {
                startingVersion = "alpha.012"
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testPreReleaseWithoutBumpFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            preRelease {
                startingVersion = "alpha.0"
            }
        }

        version.toString()
    }

    @Test
    void testPreReleaseWithValidAttributesDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            preRelease {
                startingVersion = "alpha.0"
                pattern = ~/alpha.*/
                bump { String s ->
                    s
                }
            }
        }

        assertEquals(version.preReleaseConfiguration.pattern.toString(), "\\d+\\.\\d+\\.\\d+-alpha.*")
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpWithoutMajorPatternFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            autobump {
                majorPattern = null
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpWithoutMinorPatternFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            autobump {
                minorPattern = null
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpWithoutPathPatternFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            autobump {
                patchPattern = null
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpWithoutPreReleasePatternFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            autobump {
                preReleasePattern = null
            }
        }

        version.toString()
    }

    @Test(expectedExceptions = BuildException)
    void testAutobumpWithoutPromoteToReleasePatternFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            autobump {
                promoteToReleasePattern = null
            }
        }

        version.toString()
    }

    @Test
    void testAutobumpWithValidPatternsDoesNotFailBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            autobump {
                majorPattern =~ /.*/
                minorPattern =~ /.*/
                patchPattern =~ /.*/
                preReleasePattern =~ /.*/
                promoteToReleasePattern =~ /.*/
            }
        }

        assertNotNull(version.autobumpConfiguration.majorPattern)
        assertNotNull(version.autobumpConfiguration.minorPattern)
        assertNotNull(version.autobumpConfiguration.patchPattern)
        assertNotNull(version.autobumpConfiguration.preReleasePattern)
        assertNotNull(version.autobumpConfiguration.promoteToReleasePattern)

        version.toString()
    }
}
