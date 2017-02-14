package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

import static net.vivin.gradle.versioning.VersionComponent.PRE_RELEASE

@Title('Pre-Release Bumping Specification')
class PreReleaseBumpingSpecification extends Specification {
    private TestRepository testRepository

    @Subject
    private SemanticBuildVersion semanticBuildVersion

    def setup() {
        def project = Mock(Project) {
            getProjectDir() >> testRepository.repository.workTree
        }
        semanticBuildVersion = new SemanticBuildVersion(project)
        semanticBuildVersion.config = new SemanticBuildVersionConfiguration()
    }

    def 'bumping pre-release version without pre release configuration causes build to fail'() {
        given:
        semanticBuildVersion.bump = PRE_RELEASE

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release identifier if a preRelease configuration is not specified'
    }

    def 'bumped pre-release snapshot version without prior pre release version causes build to fail'() {
        given:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(startingVersion: 'alpha.0', bump: { it })
            bump = PRE_RELEASE
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    def 'bumped pre-release version without prior pre release version causes build to fail'() {
        given:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(startingVersion: 'alpha.0', bump: { it })
            bump = PRE_RELEASE
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    @Unroll
    def 'bumped pre-release version with prior non pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(startingVersion: 'alpha.0', bump: { it })
            bump = PRE_RELEASE
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumped pre-release snapshot version with prior non pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(startingVersion: 'alpha.0', bump: { it })
            bump = PRE_RELEASE
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumped pre-release version with prior pre release version is bumped pre release version for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'alpha.0',
                bump: {
                    def parts = it.split(/\./)
                    "${parts[0]}.${++(parts[1] as int)}"
                }
            )
            bump = PRE_RELEASE
        }

        expect:
        semanticBuildVersion as String == '0.2.1-alpha.1-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumped pre-release version with prior pre release version is bumped pre release version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'alpha.0',
                bump: {
                    def parts = it.split(/\./)
                    "${parts[0]}.${++(parts[1] as int)}"
                }
            )
            bump = PRE_RELEASE
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.1-alpha.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'implicitly bumped pre-release version with prior pre release version is bumped pre release version for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.config.preRelease = new PreRelease(
            startingVersion: 'alpha.0',
            bump: {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}"
            }
        )

        expect:
        semanticBuildVersion as String == '0.2.1-alpha.1-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'implicitly bumped pre-release version with prior pre release version is bumped pre release version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'alpha.0',
                bump: {
                    def parts = it.split(/\./)
                    "${parts[0]}.${++(parts[1] as int)}"
                }
            )
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.1-alpha.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'implicitly bumping pre-release version without pre release configuration with latest pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .commit()

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump version because the latest version is \'0.2.1-alpha.0\', which contains pre-release identifiers. However, no preRelease configuration has been specified'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumped pre-release version with invalid bumped pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'alpha.0',
                bump: {
                    def parts = it.split(/\./)
                    "${parts[0]}.${++(parts[1] as int)}.00^1"
                }
            )
            bump = PRE_RELEASE
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Bumped pre-release version \'alpha.1.00^1\' is not a valid pre-release version. Identifiers must comprise only ASCII alphanumerics and hyphen, and numeric identifiers must not include leading zeroes'

        where:
        annotated << [false, true]
    }

    def 'with pattern bumped pre-release snapshot version without prior pre release version causes build to fail'() {
        given:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'beta.0',
                pattern: ~/beta/,
                bump: { it }
            )
            bump = PRE_RELEASE
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    def 'with pattern bumped pre-release version without prior pre release version causes build to fail'() {
        given:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'beta.0',
                pattern: ~/beta/,
                bump: { it }
            )
            bump = PRE_RELEASE
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    @Unroll
    def 'with pattern bumped pre-release snapshot version with prior non pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'beta.0',
                pattern: ~/beta/,
                bump: { it }
            )
            bump = PRE_RELEASE
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'with pattern bumped pre-release version with prior non pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(
                startingVersion: 'beta.0',
                pattern: ~/beta/,
                bump: { it }
            )
            bump = PRE_RELEASE
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'with pattern bumped pre-release snapshot version with non matching prior pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()

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
            bump = PRE_RELEASE
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'with pattern bumped pre-release version with non matching prior pre release version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commit()

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
            bump = PRE_RELEASE
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'with pattern bumped pre-release version with prior pre release version is bumped pre release version for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-beta.0', annotated)
            .makeChanges()

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
            bump = PRE_RELEASE
        }

        expect:
        semanticBuildVersion as String == '0.2.1-beta.1-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'with pattern bumped pre-release version with prior pre release version is bumped pre release version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-beta.0', annotated)
            .makeChanges()
            .commit()

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
            bump = PRE_RELEASE
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.1-beta.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'with pattern implicitly bumped pre-release version with prior pre release version is bumped pre release version for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-beta.0', annotated)
            .makeChanges()

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
        semanticBuildVersion as String == '0.2.1-beta.1-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'with pattern implicitly bumped pre-release version with prior pre release version is bumped pre release version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-beta.0', annotated)
            .makeChanges()
            .commit()

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
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.1-beta.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'promoting pre-release version with snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.2.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-alpha.0', annotated)
            .makeChanges()
            .commitAndTag('0.2.1-beta.0', annotated)
            .makeChanges()

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
        semanticBuildVersion as String == '0.2.1-SNAPSHOT'

        where:
        annotated << [false, true]
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
            .commit()

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
            snapshot = false
            promoteToRelease = true
        }

        expect:
        semanticBuildVersion as String == '0.2.1'

        where:
        annotated << [false, true]
    }
}
