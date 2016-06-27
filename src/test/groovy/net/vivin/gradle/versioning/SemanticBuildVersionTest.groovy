package net.vivin.gradle.versioning

import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.internal.impldep.com.google.common.io.Files
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.tooling.BuildException
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import static org.testng.Assert.*;

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * Created on 6/24/16 at 1:51 PM
 * @author vivin
 */
class SemanticBuildVersionTest {

    private GradleRunner runner

    @Test
    void testPluginAddsReleaseTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
        project.evaluate()

        project.gradle.taskGraph.whenReady { taskGraph ->
            assertTrue(taskGraph.hasTask('release'))
        }
    }

    @Test
    void testPluginDoesNotAddReleaseTaskToProjectIfItAlreadyExists() {
        Project project = ProjectBuilder.builder().build()
        project.task("release", group: "forTesting")
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
        project.evaluate()

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

    @Test(expectedExceptions = PluginApplicationException)
    void testPromoteToReleaseWithBumpPreReleaseCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['promoteToRelease', 'bumpPreRelease']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testPromoteToReleaseWithBumpPatchCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['promoteToRelease', 'bumpPatch']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testPromoteToReleaseWithBumpMinorCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['promoteToRelease', 'bumpMinor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testPromoteToReleaseWithBumpMajorCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['promoteToRelease', 'bumpMajor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testPromoteToReleaseWithAutobumpCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['promoteToRelease', 'autobump']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpPreReleaseAndBumpPatchCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPreRelease', 'bumpPatch']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpPreReleaseAndBumpMinorCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPreRelease', 'bumpMinor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpPreReleaseAndBumpMajorCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPreRelease', 'bumpMajor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpPreReleaseAndBumpAutobumpCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPreRelease', 'autobump']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpPatchAndBumpMinorCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPatch', 'bumpMinor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpPatchAndBumpMajorCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPatch', 'bumpMajor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpPatchAndAutobumpCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPatch', 'autobump']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpMinorAndBumpMajorCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpMinor', 'bumpMajor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpMinorAndAutobumpCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpMinor', 'autobump']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test(expectedExceptions = PluginApplicationException)
    void testBumpMajorAndAutobumpCausesBuildToFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpMajor', 'autobump']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'
    }

    @Test
    void testOnlyReleaseTaskDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['release']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        assertTrue(true);
    }

    @Test
    void testOnlyPromoteToReleaseTaskDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['promoteToRelease']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        assertTrue(true);
    }

    @Test
    void testOnlyBumpPreReleaseTaskDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPreRelease']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        assertTrue(true);
    }

    @Test
    void testOnlyBumpPatchTaskDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpPatch']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        assertTrue(true);
    }

    @Test
    void testOnlyBumpMinorTaskDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpMinor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        assertTrue(true);
    }

    @Test
    void testOnlyBumpMajorTaskDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['bumpMajor']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        assertTrue(true);
    }

    @Test
    void testOnlyAutobumpTaskDoesNotFail() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.taskNames = ['autobump']
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        assertTrue(true);
    }
}
