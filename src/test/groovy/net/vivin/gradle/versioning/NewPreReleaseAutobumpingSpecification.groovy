package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

@Title('New Pre-Release Autobumping Specification')
class NewPreReleaseAutobumpingSpecification extends Specification {
    private TestRepository testRepository

    @Subject
    private SemanticBuildVersion semanticBuildVersion

    def setup() {
        def project = Mock(Project) {
            getProjectDir() >> testRepository.repository.workTree
        }
        semanticBuildVersion = new SemanticBuildVersion(project)
        semanticBuildVersion.with {
            config = new SemanticBuildVersionConfiguration()
            config.preRelease = new PreRelease(startingVersion: 'pre.0')
            config.preRelease.bump = {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}"
            }
            snapshot = false
        }
    }

    def 'new pre-release autobumping with promote to release causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit '''
                This is a message
                [new-pre-release]
                [promote]
            '''.stripIndent()

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Creating a new pre-release while also promoting a pre-release is not supported'
    }

    @Unroll('#testName')
    def 'test various new pre-release autobumping variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it, annotated
        }
        testRepository
            .makeChanges()
            .commit """
                This is a message
                [new-pre-release]
                [$autobumpTag]
            """.stripIndent()

        and:
        semanticBuildVersion.config.startingVersion = startingVersion

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        startingVersion | tagNames  | autobumpTag | annotated || expectedVersion
        '0.0.0'         | []        | false       | false     || '0.0.1-pre.0'
        '0.0.0'         | []        | 'patch'     | false     || '0.0.1-pre.0'
        '0.0.0'         | []        | 'minor'     | false     || '0.1.0-pre.0'
        '0.0.0'         | []        | 'major'     | false     || '1.0.0-pre.0'
        '0.0.0'         | ['0.2.0'] | false       | false     || '0.2.1-pre.0'
        '0.0.0'         | ['0.2.0'] | 'patch'     | false     || '0.2.1-pre.0'
        '0.0.0'         | ['0.2.0'] | 'minor'     | false     || '0.3.0-pre.0'
        '0.0.0'         | ['0.2.0'] | 'major'     | false     || '1.0.0-pre.0'
        '0.0.0'         | []        | false       | true      || '0.0.1-pre.0'
        '0.0.0'         | []        | 'patch'     | true      || '0.0.1-pre.0'
        '0.0.0'         | []        | 'minor'     | true      || '0.1.0-pre.0'
        '0.0.0'         | []        | 'major'     | true      || '1.0.0-pre.0'
        '0.0.0'         | ['0.2.0'] | false       | true      || '0.2.1-pre.0'
        '0.0.0'         | ['0.2.0'] | 'patch'     | true      || '0.2.1-pre.0'
        '0.0.0'         | ['0.2.0'] | 'minor'     | true      || '0.3.0-pre.0'
        '0.0.0'         | ['0.2.0'] | 'major'     | true      || '1.0.0-pre.0'
        '0.0.1'         | []        | false       | false     || '0.0.1-pre.0'
        '0.0.1'         | []        | 'patch'     | false     || '0.0.1-pre.0'
        '0.0.1'         | []        | 'minor'     | false     || '0.1.0-pre.0'
        '0.0.1'         | []        | 'major'     | false     || '1.0.0-pre.0'
        '0.0.1'         | ['0.2.0'] | false       | false     || '0.2.1-pre.0'
        '0.0.1'         | ['0.2.0'] | 'patch'     | false     || '0.2.1-pre.0'
        '0.0.1'         | ['0.2.0'] | 'minor'     | false     || '0.3.0-pre.0'
        '0.0.1'         | ['0.2.0'] | 'major'     | false     || '1.0.0-pre.0'
        '0.0.1'         | []        | false       | true      || '0.0.1-pre.0'
        '0.0.1'         | []        | 'patch'     | true      || '0.0.1-pre.0'
        '0.0.1'         | []        | 'minor'     | true      || '0.1.0-pre.0'
        '0.0.1'         | []        | 'major'     | true      || '1.0.0-pre.0'
        '0.0.1'         | ['0.2.0'] | false       | true      || '0.2.1-pre.0'
        '0.0.1'         | ['0.2.0'] | 'patch'     | true      || '0.2.1-pre.0'
        '0.0.1'         | ['0.2.0'] | 'minor'     | true      || '0.3.0-pre.0'
        '0.0.1'         | ['0.2.0'] | 'major'     | true      || '1.0.0-pre.0'
        '0.1.0'         | []        | false       | false     || '0.1.0-pre.0'
        '0.1.0'         | []        | 'patch'     | false     || '0.1.0-pre.0'
        '0.1.0'         | []        | 'minor'     | false     || '0.1.0-pre.0'
        '0.1.0'         | []        | 'major'     | false     || '1.0.0-pre.0'
        '0.1.0'         | []        | false       | true      || '0.1.0-pre.0'
        '0.1.0'         | []        | 'patch'     | true      || '0.1.0-pre.0'
        '0.1.0'         | []        | 'minor'     | true      || '0.1.0-pre.0'
        '0.1.0'         | []        | 'major'     | true      || '1.0.0-pre.0'
        '0.1.1'         | []        | false       | false     || '0.1.1-pre.0'
        '0.1.1'         | []        | 'patch'     | false     || '0.1.1-pre.0'
        '0.1.1'         | []        | 'minor'     | false     || '0.2.0-pre.0'
        '0.1.1'         | []        | 'major'     | false     || '1.0.0-pre.0'
        '0.1.1'         | []        | false       | true      || '0.1.1-pre.0'
        '0.1.1'         | []        | 'patch'     | true      || '0.1.1-pre.0'
        '0.1.1'         | []        | 'minor'     | true      || '0.2.0-pre.0'
        '0.1.1'         | []        | 'major'     | true      || '1.0.0-pre.0'
        '1.0.0'         | []        | false       | false     || '1.0.0-pre.0'
        '1.0.0'         | []        | 'patch'     | false     || '1.0.0-pre.0'
        '1.0.0'         | []        | 'minor'     | false     || '1.0.0-pre.0'
        '1.0.0'         | []        | 'major'     | false     || '1.0.0-pre.0'
        '1.0.0'         | []        | false       | true      || '1.0.0-pre.0'
        '1.0.0'         | []        | 'patch'     | true      || '1.0.0-pre.0'
        '1.0.0'         | []        | 'minor'     | true      || '1.0.0-pre.0'
        '1.0.0'         | []        | 'major'     | true      || '1.0.0-pre.0'
        '1.0.1'         | []        | false       | false     || '1.0.1-pre.0'
        '1.0.1'         | []        | 'patch'     | false     || '1.0.1-pre.0'
        '1.0.1'         | []        | 'minor'     | false     || '1.1.0-pre.0'
        '1.0.1'         | []        | 'major'     | false     || '2.0.0-pre.0'
        '1.0.1'         | []        | false       | true      || '1.0.1-pre.0'
        '1.0.1'         | []        | 'patch'     | true      || '1.0.1-pre.0'
        '1.0.1'         | []        | 'minor'     | true      || '1.1.0-pre.0'
        '1.0.1'         | []        | 'major'     | true      || '2.0.0-pre.0'
        '1.1.0'         | []        | false       | false     || '1.1.0-pre.0'
        '1.1.0'         | []        | 'patch'     | false     || '1.1.0-pre.0'
        '1.1.0'         | []        | 'minor'     | false     || '1.1.0-pre.0'
        '1.1.0'         | []        | 'major'     | false     || '2.0.0-pre.0'
        '1.1.0'         | []        | false       | true      || '1.1.0-pre.0'
        '1.1.0'         | []        | 'patch'     | true      || '1.1.0-pre.0'
        '1.1.0'         | []        | 'minor'     | true      || '1.1.0-pre.0'
        '1.1.0'         | []        | 'major'     | true      || '2.0.0-pre.0'
        '1.1.1'         | []        | false       | false     || '1.1.1-pre.0'
        '1.1.1'         | []        | 'patch'     | false     || '1.1.1-pre.0'
        '1.1.1'         | []        | 'minor'     | false     || '1.2.0-pre.0'
        '1.1.1'         | []        | 'major'     | false     || '2.0.0-pre.0'
        '1.1.1'         | []        | false       | true      || '1.1.1-pre.0'
        '1.1.1'         | []        | 'patch'     | true      || '1.1.1-pre.0'
        '1.1.1'         | []        | 'minor'     | true      || '1.2.0-pre.0'
        '1.1.1'         | []        | 'major'     | true      || '2.0.0-pre.0'

        and:
        testName = "new pre-release autobumping ${tagNames ? 'with' : 'without'} prior versions " +
            (autobumpTag ? "with bump $autobumpTag" : 'and without explicit bump') + " (annotated: $annotated)"
    }
}
