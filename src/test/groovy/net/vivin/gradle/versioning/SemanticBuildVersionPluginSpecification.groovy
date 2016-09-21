package net.vivin.gradle.versioning

import net.vivin.gradle.versioning.spock.extensions.gradle.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Title('Semantic Build Version Plugin Specification')
class SemanticBuildVersionPluginSpecification extends Specification {
    private TestRepository testRepository
    @ProjectDirProvider({ testRepository })
    private GradleRunner gradleRunner

    @Unroll
    def '\'#projectProperties.first()\' with \'#projectProperties.last()\' causes build to fail'(projectProperties, expectedFailureMessage, GradleRunner gradleRunner) {
        when:
        def arguments = projectProperties.collectMany { ['-P', it] }
        def buildResult = gradleRunner.withArguments(arguments).buildAndFail()

        then:
        buildResult.output.contains expectedFailureMessage

        where:
        projectProperties                      || expectedFailureMessage
        ['bumpMajor', 'autobump']              || 'Cannot explicitly bump a version-component when also autobumping'
        ['bumpMinor', 'autobump']              || 'Cannot explicitly bump a version-component when also autobumping'
        ['bumpMinor', 'bumpMajor']             || 'Cannot bump multiple version-components at the same time'
        ['bumpPatch', 'autobump']              || 'Cannot explicitly bump a version-component when also autobumping'
        ['bumpPatch', 'bumpMajor']             || 'Cannot bump multiple version-components at the same time'
        ['bumpPatch', 'bumpMinor']             || 'Cannot bump multiple version-components at the same time'
        ['bumpPreRelease', 'autobump']         || 'Cannot explicitly bump a version-component when also autobumping'
        ['bumpPreRelease', 'bumpMajor']        || 'Cannot bump multiple version-components at the same time'
        ['bumpPreRelease', 'bumpMinor']        || 'Cannot bump multiple version-components at the same time'
        ['bumpPreRelease', 'bumpPatch']        || 'Cannot bump multiple version-components at the same time'
        ['newPreRelease', 'autobump']          || 'Cannot explicitly create a new pre-release version when also autobumping'
        ['newPreRelease', 'bumpPreRelease']    || 'Cannot bump pre-release version when also creating a new pre-release version'
        ['newPreRelease', 'promoteToRelease']  || 'Cannot promote to a release version when also creating a new pre-release version'
        ['promoteToRelease', 'autobump']       || 'Cannot explicitly promote to release when also autobumping'
        ['promoteToRelease', 'bumpMajor']      || 'Cannot bump major-version when also promoting to a release version'
        ['promoteToRelease', 'bumpMinor']      || 'Cannot bump minor-version when also promoting to a release version'
        ['promoteToRelease', 'bumpPatch']      || 'Cannot bump patch-version when also promoting to a release version'
        ['promoteToRelease', 'bumpPreRelease'] || 'Cannot bump pre-release version when also promoting to a release version'

        and:
        gradleRunner = null
    }

    @Unroll
    def 'only \'#projectProperty\' does not fail'() {
        given:
        new File(gradleRunner.projectDir, 'semantic-build-versioning.gradle') << '''
            preRelease {
                startingVersion = 'pre.1'
                bump = { it }
            }
        '''.stripIndent()

        and:
        testRepository
            .makeChanges()
            .commitAndTag('0.1.0-pre.1')
            .makeChanges()
            .commit '[patch]'

        expect:
        gradleRunner.withArguments('-P', projectProperty).build()

        where:
        projectProperty << [
            'release',
            'promoteToRelease',
            'bumpPreRelease',
            'bumpPatch',
            'bumpMinor',
            'bumpMajor',
            'autobump',
            'newPreRelease'
        ]
    }

    def 'accessing version during configuration phase works properly'() {
        given:
        new File(gradleRunner.projectDir, 'build.gradle') << '''
            task hello {
                println "snapshot: $version.snapshot"
                println "version: $project.version"
                ext.ver = project.version.toString()
            }
        '''.stripIndent()

        and:
        testRepository
            .add('build.gradle')
            .commit('Stuff-1')
            .makeChanges()
            .commitAndTag('1.0.0')
            .makeChanges()
            .commit 'Stuff-2'

        when:
        def buildResult = gradleRunner.build()

        then:
        buildResult.output.contains 'snapshot: true'
        buildResult.output.contains 'version: 1.0.1-SNAPSHOT'
    }

    def 'add version output if a task with name \'release\' is found'() {
        given:
        new File(gradleRunner.projectDir, 'settings.gradle') << 'rootProject.name = \'root\''
        new File(gradleRunner.projectDir, 'build.gradle') << 'task release'

        when:
        def buildResult = gradleRunner.withArguments('release').build()

        then:
        buildResult.output.contains 'Releasing \'root\' with version \'0.1.0-SNAPSHOT\''
    }
}
