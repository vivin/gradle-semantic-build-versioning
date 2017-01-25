package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

@Title('Pre-Release Autobumping Specification')
class PreReleaseAutobumpingSpecification extends Specification {
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
            snapshot = false
        }
    }

    @Unroll
    def 'promoting pre-release version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-beta.0', annotated)
            .makeChanges()
            .commit '''
                This is a message
                [promote]
            '''.stripIndent()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'beta.0',
                pattern: ~/beta/,
                bump: {
                    def parts = it.split(/\./)
                    "${parts[0]}.${++(parts[1] as int)}"
                }
            )
            promoteToRelease = true
        }

        expect:
        semanticBuildVersion as String == '0.2.1'

        where:
        annotated << [false, true]
    }
}
