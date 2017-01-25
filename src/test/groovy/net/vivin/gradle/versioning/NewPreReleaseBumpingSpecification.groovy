package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

import static net.vivin.gradle.versioning.VersionComponent.*

@Title('New Pre-Release Bumping Specification')
class NewPreReleaseBumpingSpecification extends Specification {
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
            newPreRelease = true
        }
    }

    def 'new pre-release version without pre-release configuration causes build to fail'() {
        given:
        semanticBuildVersion.config.preRelease = null
        semanticBuildVersion.snapshot = false

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot create a new pre-release version if a preRelease configuration is not specified'
    }

    @Unroll('#testName')
    def 'test various new pre-release bumping variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it, annotated
        }
        testRepository
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            if(bumpComponent) {
                bump = bumpComponent
            }
            it.snapshot = snapshot
        }

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        tagNames        | bumpComponent | snapshot | annotated || expectedVersion
        []              | false         | false    | false     || '0.1.0-pre.0'
        []              | PATCH         | false    | false     || '0.1.0-pre.0'
        []              | MINOR         | false    | false     || '0.2.0-pre.0'
        []              | MAJOR         | false    | false     || '1.0.0-pre.0'
        ['0.2.0']       | false         | false    | false     || '0.2.1-pre.0'
        ['0.2.0']       | PATCH         | false    | false     || '0.2.1-pre.0'
        ['0.2.0']       | MINOR         | false    | false     || '0.3.0-pre.0'
        ['0.2.0']       | MAJOR         | false    | false     || '1.0.0-pre.0'
        ['0.2.0-pre.0'] | false         | false    | false     || '0.2.1-pre.0'
        ['0.2.0-pre.0'] | PATCH         | false    | false     || '0.2.1-pre.0'
        ['0.2.0-pre.0'] | MINOR         | false    | false     || '0.3.0-pre.0'
        ['0.2.0-pre.0'] | MAJOR         | false    | false     || '1.0.0-pre.0'
        []              | false         | true     | false     || '0.1.0-pre.0-SNAPSHOT'
        []              | PATCH         | true     | false     || '0.1.0-pre.0-SNAPSHOT'
        []              | MINOR         | true     | false     || '0.2.0-pre.0-SNAPSHOT'
        []              | MAJOR         | true     | false     || '1.0.0-pre.0-SNAPSHOT'
        ['0.2.0']       | false         | true     | false     || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0']       | PATCH         | true     | false     || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0']       | MINOR         | true     | false     || '0.3.0-pre.0-SNAPSHOT'
        ['0.2.0']       | MAJOR         | true     | false     || '1.0.0-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | false         | true     | false     || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | PATCH         | true     | false     || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | MINOR         | true     | false     || '0.3.0-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | MAJOR         | true     | false     || '1.0.0-pre.0-SNAPSHOT'
        []              | false         | false    | true      || '0.1.0-pre.0'
        []              | PATCH         | false    | true      || '0.1.0-pre.0'
        []              | MINOR         | false    | true      || '0.2.0-pre.0'
        []              | MAJOR         | false    | true      || '1.0.0-pre.0'
        ['0.2.0']       | false         | false    | true      || '0.2.1-pre.0'
        ['0.2.0']       | PATCH         | false    | true      || '0.2.1-pre.0'
        ['0.2.0']       | MINOR         | false    | true      || '0.3.0-pre.0'
        ['0.2.0']       | MAJOR         | false    | true      || '1.0.0-pre.0'
        ['0.2.0-pre.0'] | false         | false    | true      || '0.2.1-pre.0'
        ['0.2.0-pre.0'] | PATCH         | false    | true      || '0.2.1-pre.0'
        ['0.2.0-pre.0'] | MINOR         | false    | true      || '0.3.0-pre.0'
        ['0.2.0-pre.0'] | MAJOR         | false    | true      || '1.0.0-pre.0'
        []              | false         | true     | true      || '0.1.0-pre.0-SNAPSHOT'
        []              | PATCH         | true     | true      || '0.1.0-pre.0-SNAPSHOT'
        []              | MINOR         | true     | true      || '0.2.0-pre.0-SNAPSHOT'
        []              | MAJOR         | true     | true      || '1.0.0-pre.0-SNAPSHOT'
        ['0.2.0']       | false         | true     | true      || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0']       | PATCH         | true     | true      || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0']       | MINOR         | true     | true      || '0.3.0-pre.0-SNAPSHOT'
        ['0.2.0']       | MAJOR         | true     | true      || '1.0.0-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | false         | true     | true      || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | PATCH         | true     | true      || '0.2.1-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | MINOR         | true     | true      || '0.3.0-pre.0-SNAPSHOT'
        ['0.2.0-pre.0'] | MAJOR         | true     | true      || '1.0.0-pre.0-SNAPSHOT'

        and:
        testName = (snapshot ? 'snapshot ' : '') +
            "new pre-release version ${tagNames ? 'with' : 'without'} prior " +
            "${tagNames.any { it.endsWith '-pre.0' } ? 'pre-release ' : ''}" +
            "version" +
            (bumpComponent ? " with bump ${(bumpComponent as String).toLowerCase()}" : '') +
            " (annotated: $annotated)"
    }

    @Unroll
    def 'new pre-release version with snapshot \'#snapshot\' without matching tags (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.1.0', annotated

        and:
        semanticBuildVersion.config.tagPattern = ~/^bar-/
        semanticBuildVersion.config.tagPrefix = 'bar-'
        semanticBuildVersion.snapshot = snapshot

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        snapshot | annotated || expectedVersion
        false    | false     || '0.1.0-pre.0'
        true     | false     || '0.1.0-pre.0-SNAPSHOT'
        false    | true      || '0.1.0-pre.0'
        true     | true      || '0.1.0-pre.0-SNAPSHOT'
    }
}
