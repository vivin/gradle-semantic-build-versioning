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
            autobump = true
        }
    }

    @Unroll
    def 'new pre-release autobumping with #testNamePart causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit """
                This is a message
                [new-pre-release]
                [$autobumpTag]
            """.stripIndent()

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == expectedExceptionMessage

        where:
        testNamePart         | autobumpTag   || expectedExceptionMessage
        'pre-release bump'   | 'pre-release' || 'Could not autobump because it is not possible to bump the pre-release version when also creating a new pre-release version'
        'promote to release' | 'promote'     || 'Could not autobump because it is not possible to promote to a release version when also creating a new pre-release version'
    }

    @Unroll('#testName')
    def 'test various new pre-release autobumping variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it
        }
        testRepository
            .makeChanges()
            .commit """
                This is a message
                [new-pre-release]
                [$autobumpTag]
            """.stripIndent()

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        tagNames  | autobumpTag || expectedVersion
        []        | false       || '0.1.0-pre.0'
        []        | 'patch'     || '0.1.0-pre.0'
        []        | 'minor'     || '0.2.0-pre.0'
        []        | 'major'     || '1.0.0-pre.0'
        ['0.2.0'] | false       || '0.2.1-pre.0'
        ['0.2.0'] | 'patch'     || '0.2.1-pre.0'
        ['0.2.0'] | 'minor'     || '0.3.0-pre.0'
        ['0.2.0'] | 'major'     || '1.0.0-pre.0'

        and:
        testName = "new pre-release autobumping ${tagNames ? 'with' : 'without'} prior versions " +
            (autobumpTag ? "with bump $autobumpTag" : 'and without explicit bump')
    }
}
