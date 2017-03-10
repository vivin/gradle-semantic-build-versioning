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
    def '\'#projectProperties.first()\' with \'#projectProperties.last()\' causes build to fail'(projectProperties, expectedFailureMessage) {
        when:
        def arguments = projectProperties.collectMany { ['-P', it] }
        def buildResult = gradleRunner.withArguments(arguments).buildAndFail()

        then:
        buildResult.output.contains expectedFailureMessage

        where:
        projectProperties                                 || expectedFailureMessage
        ['newPreRelease', 'bumpComponent=pre-release']    || 'Bumping pre-release component while also creating a new pre-release is not supported'
        ['newPreRelease', 'promoteToRelease']             || 'Creating a new pre-release while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpComponent=major']       || 'Bumping any component while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpComponent=minor']       || 'Bumping any component while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpComponent=patch']       || 'Bumping any component while also promoting a pre-release is not supported'
        ['promoteToRelease', 'bumpComponent=pre-release'] || 'Bumping any component while also promoting a pre-release is not supported'
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

        when:
        def buildResult = gradleRunner.withArguments('-P', projectProperty, '-P', 'autobump').build()

        then:
        noExceptionThrown()

        and:
        buildResult.output.contains 'The property "autobump" is deprecated and will be ignored'

        where:
        [projectProperty, annotated] << [
            [
                'bumpComponent=major',
                'bumpComponent=minor',
                'bumpComponent=patch',
                'bumpComponent=pre-release',
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
                                             'bumpComponent=pre-release',
                                             'bumpComponent=patch',
                                             'bumpComponent=minor',
                                             'bumpComponent=major',
                                             'autobump',
                                             'newPreRelease'
                                         ], [true, false]].combinations()
    }

    def 'invalid bumpComponent fails the build'() {
        when:
        def buildResult = gradleRunner.withArguments('-P', 'bumpComponent=prerelease', '-P', 'forceBump').buildAndFail()

        then:
        buildResult.output.contains 'No such property: PRERELEASE for class: net.vivin.gradle.versioning.VersionComponent'
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

    @Unroll
    def 'the project version is an instance of String (annotated: #annotated)'() {
        given:
        new File(gradleRunner.projectDir, 'build.gradle') << '''
            task hello {
                println "project version is instance of String: ${version instanceof String}"
            }
        '''.stripIndent()

        when:
        def buildResult = gradleRunner.build()

        then:
        buildResult.output.contains 'project version is instance of String: true'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'accessing version components on project version works properly, snapshot: #snapshot (annotated: #annotated)'() {
        given:
        new File(gradleRunner.projectDir, 'semantic-build-versioning.gradle') << '''
            preRelease {
                startingVersion = 'pre.1'
                bump = {
                   "pre.${((it - ~/^pre\\./) as int) + 1}"
               }
            }
        '''.stripIndent()

        and:
        new File(gradleRunner.projectDir, 'build.gradle') << '''
            task hello {
                println "major: $version.major"
                println "minor: $version.minor"
                println "patch: $version.patch"
                println "preRelease: $version.preRelease"
            }
        '''.stripIndent()

        and:
        testRepository
            .makeChanges()
            .commitAndTag('1.2.3-pre.3', annotated)
            .makeChanges()
            .commit()

        when:
        def buildResult = gradleRunner.withArguments(snapshot ? [] : ['-P', 'release']).build()

        then:
        (buildResult.output =~ /^major: 1\r?\n/).find()
        (buildResult.output =~ /\r?\nminor: 2\r?\n/).find()
        (buildResult.output =~ /\r?\npatch: 3\r?\n/).find()
        (buildResult.output =~ /\r?\npreRelease: pre\.4\r?\n/).find()

        where:
        [snapshot, annotated] << [[false, true], [false, true]].combinations()
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
