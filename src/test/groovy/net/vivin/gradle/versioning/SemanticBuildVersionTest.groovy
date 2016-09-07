package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.BuildException
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path

import static org.testng.Assert.*

class SemanticBuildVersionTest extends TestNGRepositoryTestCase {
    Path projectDir
    GradleRunner gradleRunner

    @BeforeClass
    void setUpOnce() {
        super.setUp();
        testRepository = new TestRepository(db)

        projectDir = testRepository.repository.getDirectory().getParentFile().toPath()
        new File(projectDir.toFile(), 'build.gradle').text = '''
            plugins {
                id 'net.vivin.gradle-semantic-build-versioning'
            }

            task hello {
                println version.snapshot
                println project.version
                ext.ver = project.version.toString()
                println project.version.bump
                println version.snapshot
            }

            '''.stripIndent()

        testRepository
            .add("build.gradle")
            .commit("Stuff-1")
            .makeChanges()
            .commitAndTag("1.0.0")
            .makeChanges()
            .commit("Stuff-2")
    }

    @BeforeMethod
    void setUp() {
        gradleRunner = GradleRunner
                .create()
                .forwardStdOutput(System.out.newWriter())
                .forwardStdError(System.err.newWriter())
                .withPluginClasspath()
                .withProjectDir(projectDir.toFile())
    }

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
    void testStartingVersionWithIdentifierFailsBuild() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'net.vivin.gradle-semantic-build-versioning'

        SemanticBuildVersion version = new SemanticBuildVersion(project)
        version.with {
            startingVersion = "0.0.1-pre.0"
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

    @Test
    void testNewPreReleaseWithPromoteToReleaseCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('newPreRelease', 'promoteToRelease').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot promote to a release version when also creating a new pre-release version'), 'Build output contains correct error message')
    }

    @Test
    void testNewPreReleaseWithAutobumpCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('newPreRelease', 'autobump').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot explicitly create a new pre-release version when also autobumping'), 'Build output contains correct error message')
    }

    @Test
    void testNewPreReleaseWithBumpPreReleaseCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('newPreRelease', 'bumpPreRelease').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump pre-release version when also creating a new pre-release version'), 'Build output contains correct error message')
    }

    @Test
    void testPromoteToReleaseWithBumpPreReleaseCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('promoteToRelease', 'bumpPreRelease').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump pre-release version when also promoting to a release version'), 'Build output contains correct error message')
    }

    @Test
    void testPromoteToReleaseWithBumpPatchCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('promoteToRelease', 'bumpPatch').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump patch-version when also promoting to a release version'), 'Build output contains correct error message')
    }

    @Test
    void testPromoteToReleaseWithBumpMinorCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('promoteToRelease', 'bumpMinor').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump minor-version when also promoting to a release version'), 'Build output contains correct error message')
    }

    @Test
    void testPromoteToReleaseWithBumpMajorCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('promoteToRelease', 'bumpMajor').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump major-version when also promoting to a release version'), 'Build output contains correct error message')
    }

    @Test
    void testPromoteToReleaseWithAutobumpCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('promoteToRelease', 'autobump').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot explicitly promote to release when also autobumping'), 'Build output contains correct error message')
    }

    @Test
    void testBumpPreReleaseAndBumpPatchCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpPreRelease', 'bumpPatch').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump multiple version-components at the same time'), 'Build output contains correct error message')
    }

    @Test
    void testBumpPreReleaseAndBumpMinorCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpPreRelease', 'bumpMinor').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump multiple version-components at the same time'), 'Build output contains correct error message')
    }

    @Test
    void testBumpPreReleaseAndBumpMajorCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpPreRelease', 'bumpMajor').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump multiple version-components at the same time'), 'Build output contains correct error message')
    }

    @Test
    void testBumpPreReleaseAndBumpAutobumpCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpPreRelease', 'autobump').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot explicitly bump a version-component when also autobumping'), 'Build output contains correct error message')
    }

    @Test
    void testBumpPatchAndBumpMinorCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpPatch', 'bumpMinor').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump multiple version-components at the same time'), 'Build output contains correct error message')
    }

    @Test
    void testBumpPatchAndBumpMajorCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpPatch', 'bumpMajor').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump multiple version-components at the same time'), 'Build output contains correct error message')
    }

    @Test
    void testBumpPatchAndAutobumpCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpPatch', 'autobump').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot explicitly bump a version-component when also autobumping'), 'Build output contains correct error message')
    }

    @Test
    void testBumpMinorAndBumpMajorCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpMinor', 'bumpMajor').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot bump multiple version-components at the same time'), 'Build output contains correct error message')
    }

    @Test
    void testBumpMinorAndAutobumpCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpMinor', 'autobump').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot explicitly bump a version-component when also autobumping'), 'Build output contains correct error message')
    }

    @Test
    void testBumpMajorAndAutobumpCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('bumpMajor', 'autobump').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot explicitly bump a version-component when also autobumping'), 'Build output contains correct error message')
    }

    @Test
    void testOnlyReleaseTaskDoesNotFail() {
        gradleRunner.withArguments('release').build()
    }

    @Test
    void testOnlyPromoteToReleaseTaskDoesNotFail() {
        gradleRunner.withArguments('promoteToRelease').build()
    }

    @Test
    void testOnlyBumpPreReleaseTaskDoesNotFail() {
        gradleRunner.withArguments('bumpPreRelease').build()
    }

    @Test
    void testOnlyBumpPatchTaskDoesNotFail() {
        gradleRunner.withArguments('bumpPatch').build()
    }

    @Test
    void testOnlyBumpMinorTaskDoesNotFail() {
        gradleRunner.withArguments('bumpMinor').build()
    }

    @Test
    void testOnlyBumpMajorTaskDoesNotFail() {
        gradleRunner.withArguments('bumpMajor').build()
    }

    @Test
    void testOnlyAutobumpTaskDoesNotFail() {
        gradleRunner.withArguments('autobump').build()
    }

    @Test
    void testOnlyNewPreReleaseDoesNotFail() {
        gradleRunner.withArguments('newPreRelease').build()
    }

    @Test
    void testTagWithoutReleaseCausesBuildToFail() {
        def buildResult = gradleRunner.withArguments('tag').buildAndFail()
        assertTrue(buildResult.output.contains('Cannot create a tag for a snapshot version'), 'Build output contains correct error message')
    }

    @Test
    void testTagWithReleaseCausesBuildToFailWithTheCorrectError() {
        // This test needs a repository without a HEAD so we're setting one up
        def projectDir = Files.createTempDirectory 'net.vivin.gradle-semantic-build-versioning'
        new File(projectDir.toFile(), 'build.gradle').text = '''
            plugins {
                id 'net.vivin.gradle-semantic-build-versioning'
            }

            '''.stripIndent()

        def gradleRunner = GradleRunner
                .create()
                .forwardStdOutput(System.out.newWriter())
                .forwardStdError(System.err.newWriter())
                .withPluginClasspath()
                .withProjectDir(projectDir.toFile())

        def buildResult = gradleRunner.withArguments('rel', 'tag').buildAndFail()

        projectDir.deleteDir()

        assertFalse(buildResult.output.contains('Cannot create a tag for a snapshot version'), 'Build output does not contain wrong error message')
        assertTrue(buildResult.output.contains('Tag on repository without HEAD currently not supported'), 'Build output contains correct error message')
    }
}
