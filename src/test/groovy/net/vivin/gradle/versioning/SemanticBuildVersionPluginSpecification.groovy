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
        ['bumpMinor', 'bumpMajor']             || 'Cannot bump multiple version-components at the same time'
        ['bumpPatch', 'bumpMajor']             || 'Cannot bump multiple version-components at the same time'
        ['bumpPatch', 'bumpMinor']             || 'Cannot bump multiple version-components at the same time'
        ['bumpPreRelease', 'bumpMajor']        || 'Cannot bump multiple version-components at the same time'
        ['bumpPreRelease', 'bumpMinor']        || 'Cannot bump multiple version-components at the same time'
        ['bumpPreRelease', 'bumpPatch']        || 'Cannot bump multiple version-components at the same time'
        ['newPreRelease', 'bumpPreRelease']    || 'Bumping pre-release component while also creating a new pre-release is not supported'
        ['newPreRelease', 'promoteToRelease']  || 'Creating a new pre-release while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpMajor']      || 'Bumping any component while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpMinor']      || 'Bumping any component while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpPatch']      || 'Bumping any component while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpPreRelease'] || 'Bumping any component while also promoting a pre-release is not supported'

        and:
        gradleRunner = null
    }

    @Unroll
    def '\'#projectProperty\' with \'autobump\' does not cause build to fail but issues warning (annotated: #annotated)'() {
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
            .commitAndTag('0.1.0-pre.1', annotated)
            .makeChanges()
            .commit '[pre-release]'

        when:
        def buildResult = gradleRunner.withArguments('-P', projectProperty, '-P', 'autobump').build()

        then:
        noExceptionThrown()

        and:
        buildResult.output.contains 'The property "autobump" is deprecated and will be ignored'

        where:
        [projectProperty, annotated] << [[
            'bumpMajor',
            'bumpMinor',
            'bumpPatch',
            'bumpPreRelease',
            'autobump'
        ], [true, false]].combinations()
    }

    @Unroll
    def 'only \'#projectProperty\' does not fail (annotated: #annotated)'() {
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
            .commitAndTag('0.1.0-pre.1', annotated)
            .makeChanges()
            .commit '[patch]'

        expect:
        gradleRunner.withArguments('-P', projectProperty, '-P', 'forceBump').build()

        where:
        [projectProperty, annotated] << [[
            'release',
            'bumpPreRelease',
            'bumpPatch',
            'bumpMinor',
            'bumpMajor',
            'autobump',
            'newPreRelease'
        ], [true, false]].combinations()
    }

    @Unroll
    def 'accessing version during configuration phase works properly (annotated: #annotated)'() {
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
            .commitAndTag('1.0.0', annotated)
            .makeChanges()
            .commit 'Stuff-2'

        when:
        def buildResult = gradleRunner.build()

        then:
        buildResult.output.contains 'snapshot: true'
        buildResult.output.contains 'version: 1.0.1-SNAPSHOT'

        where:
        annotated << [false, true]
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
