package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

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
            autobump = true
        }
    }

    def 'bumping pre-release version without pre release configuration causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release identifier if a preRelease configuration is not specified'
    }

    def 'autobumped pre-release version without prior pre release version causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(startingVersion: 'alpha.0', bump: { it })

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    def 'autobumped pre-release version with prior non pre release version causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0')
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(startingVersion: 'alpha.0', bump: { it })

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    def 'autobumped pre-release version with prior pre release version is bumped pre release version'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0')
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0')
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(
            startingVersion: 'alpha.0',
            bump: {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}"
            }
        )

        expect:
        semanticBuildVersion as String == '0.2.1-alpha.1'
    }

    def 'autobumped pre-release version with invalid bumped pre release version causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0')
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0')
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(
            startingVersion: 'alpha.0',
            bump: {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}.00^1"
            }
        )

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Bumped pre-release version \'alpha.1.00^1\' is not a valid pre-release version. Identifiers must comprise only ASCII alphanumerics and hyphen, and numeric identifiers must not include leading zeroes'
    }

    def 'with pattern autobumped pre-release version without prior pre release version causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(
            startingVersion: 'beta.0',
            pattern: ~/beta/,
            bump: { it }
        )

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    def 'with pattern autobumped pre-release version with prior non pre release version causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0')
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(
            startingVersion: 'beta.0',
            pattern: ~/beta/,
            bump: { it }
        )

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    def 'with pattern autobumped pre-release version with non matching prior pre release version causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0')
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0')
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(
            startingVersion: 'beta.0',
            pattern: ~/beta/,
            bump: {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}"
            }
        )

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    def 'with pattern autobumped pre-release version with prior pre release version is bumped pre release version'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0')
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0')
            .makeChanges()
            .commitAndTag('0.2.1-beta.0')
            .makeChanges()
            .commit '''
                This is a message
                [pre-release]
            '''.stripIndent()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(
            startingVersion: 'beta.0',
            pattern: ~/beta/,
            bump: {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}"
            }
        )

        expect:
        semanticBuildVersion as String == '0.2.1-beta.1'
    }

    def 'promoting pre-release version'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0')
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0')
            .makeChanges()
            .commitAndTag('0.2.1-beta.0')
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
    }
}
