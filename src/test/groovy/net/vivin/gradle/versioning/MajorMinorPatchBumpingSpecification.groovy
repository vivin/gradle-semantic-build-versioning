package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

import static net.vivin.gradle.versioning.VersionComponent.*

@Title('Major Minor Patch Bumping Specification')
class MajorMinorPatchBumpingSpecification extends Specification {
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

    def 'version without prior tags is default starting snapshot version'() {
        expect:
        semanticBuildVersion as String == '0.1.0-SNAPSHOT'
    }

    def 'version without prior tags is default starting release version'() {
        given:
        semanticBuildVersion.snapshot = false

        expect:
        semanticBuildVersion as String == '0.1.0'
    }

    def 'version without prior tags and bumping pre-release is not possible'() {
        given:
        semanticBuildVersion.config.preRelease = new PreRelease(startingVersion: 'pre.1', bump: { it })
        semanticBuildVersion.snapshot = false
        semanticBuildVersion.bump = PRE_RELEASE

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump pre-release because the latest version is not a pre-release version. To create a new pre-release version, use newPreRelease instead'
    }

    @Unroll
    def 'without prior tags and with startingVersion "#startingVersion" and bump "#bump" resulting version should be "#expectedVersion"'() {
        given:
        semanticBuildVersion.config.startingVersion = startingVersion
        semanticBuildVersion.snapshot = false
        semanticBuildVersion.bump = bump

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        startingVersion | bump  || expectedVersion
        '0.0.0'         | null  || '0.0.1'
        '0.0.0'         | PATCH || '0.0.1'
        '0.0.0'         | MINOR || '0.1.0'
        '0.0.0'         | MAJOR || '1.0.0'
        '0.0.1'         | null  || '0.0.1'
        '0.0.1'         | PATCH || '0.0.1'
        '0.0.1'         | MINOR || '0.1.0'
        '0.0.1'         | MAJOR || '1.0.0'
        '0.1.0'         | null  || '0.1.0'
        '0.1.0'         | PATCH || '0.1.0'
        '0.1.0'         | MINOR || '0.1.0'
        '0.1.0'         | MAJOR || '1.0.0'
        '0.1.1'         | null  || '0.1.1'
        '0.1.1'         | PATCH || '0.1.1'
        '0.1.1'         | MINOR || '0.2.0'
        '0.1.1'         | MAJOR || '1.0.0'
        '1.0.0'         | null  || '1.0.0'
        '1.0.0'         | PATCH || '1.0.0'
        '1.0.0'         | MINOR || '1.0.0'
        '1.0.0'         | MAJOR || '1.0.0'
        '1.0.1'         | null  || '1.0.1'
        '1.0.1'         | PATCH || '1.0.1'
        '1.0.1'         | MINOR || '1.1.0'
        '1.0.1'         | MAJOR || '2.0.0'
        '1.1.0'         | null  || '1.1.0'
        '1.1.0'         | PATCH || '1.1.0'
        '1.1.0'         | MINOR || '1.1.0'
        '1.1.0'         | MAJOR || '2.0.0'
        '1.1.1'         | null  || '1.1.1'
        '1.1.1'         | PATCH || '1.1.1'
        '1.1.1'         | MINOR || '1.2.0'
        '1.1.1'         | MAJOR || '2.0.0'
    }

    @Unroll
    def 'version without matching tags is default starting snapshot version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.1.0', annotated

        and:
        semanticBuildVersion.config.tagPattern = ~/^bar-/
        semanticBuildVersion.config.tagPrefix = 'bar-'

        expect:
        semanticBuildVersion as String == '0.1.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'version without matching tags is default starting release version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.1.0', annotated

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
            }
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.1.0'

        where:
        annotated << [false, true]
    }

    def 'creating release version with uncommitted changes causes build to fail'() {
        given:
        testRepository.makeChanges()

        and:
        semanticBuildVersion.snapshot = false

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Cannot create a release version when there are uncommitted changes'
    }

    @Unroll
    def 'version with prior tag and uncommitted changes is next snapshot version (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.0.1', annotated)
            .makeChanges()

        expect:
        semanticBuildVersion as String == '0.0.2-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'version with prior tag and committed changes is next release version (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.snapshot = false

        expect:
        semanticBuildVersion as String == '0.0.2'

        where:
        annotated << [false, true]
    }

    def 'version with custom snapshot suffix'() {
        given:
        testRepository.makeChanges()

        and:
        semanticBuildVersion.config.snapshotSuffix = 'CURRENT'

        expect:
        semanticBuildVersion as String == '0.1.0-CURRENT'
    }

    @Unroll
    def 'checking out tag produces same version as tag (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('3.1.2', annotated)
            .commitAndTag('3.1.3', annotated)
            .commitAndTag('3.1.4', annotated)
            .checkout '3.1.2'

        expect:
        semanticBuildVersion as String == '3.1.2'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'checking out tag produces same version as tag even if other tags are present (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('3.1.2', annotated)
            .commitAndTag('3.1.3', annotated)
            .commitAndTag('3.1.4', annotated)
            .checkout('3.1.2')
            .tag 'foo', annotated

        expect:
        semanticBuildVersion as String == '3.1.2'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.bump = PATCH

        expect:
        semanticBuildVersion as String == '0.0.3-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.0.2', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            bump = PATCH
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.0.3'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.bump = MINOR

        expect:
        semanticBuildVersion as String == '0.1.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.1.3', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.0'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.bump = MAJOR

        expect:
        semanticBuildVersion as String == '1.0.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    void 'bumping major version for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.2.3', annotated)
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            bump = MAJOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.0.0'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version with tag pattern for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('foo-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
            }
            bump = PATCH
        }

        expect:
        semanticBuildVersion as String == '0.0.3-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version with tag pattern for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('foo-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
            }
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.0.3'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version with tag pattern for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('foo-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
            }
            bump = MINOR
        }

        expect:
        semanticBuildVersion as String == '0.1.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version with tag pattern for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.2.1', annotated)
            .commitAndTag('foo-0.2.2', annotated)
            .commitAndTag('bar-0.1.1', annotated)
            .commitAndTag('bar-0.1.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
            }
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.0'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version with tag pattern for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('foo-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
            }
            bump = MAJOR
        }

        expect:
        semanticBuildVersion as String == '1.0.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version with tag pattern for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('foo-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
            }
            bump = MAJOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.0.0'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version with versions matching for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.1.1', annotated)
            .commitAndTag('0.1.2', annotated)
            .commitAndTag('0.0.1', annotated)
            .commitAndTag('0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 0, minor: 1)
            bump = PATCH
        }

        expect:
        semanticBuildVersion as String == '0.1.3-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version with versions matching for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('1.1.1', annotated)
            .commitAndTag('1.1.2', annotated)
            .commitAndTag('0.0.1', annotated)
            .commitAndTag('0.0.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 1)
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.1.3'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version with versions matching for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('1.1.1', annotated)
            .commitAndTag('1.1.2', annotated)
            .commitAndTag('0.0.1', annotated)
            .commitAndTag('0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 1, minor: 1, patch: 2)
            bump = MINOR
        }

        expect:
        semanticBuildVersion as String == '1.2.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version with versions matching for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('1.2.1', annotated)
            .commitAndTag('1.2.2', annotated)
            .commitAndTag('2.2.1', annotated)
            .commitAndTag('2.2.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 1)
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.3.0'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version with versions matching for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('3.1.1', annotated)
            .commitAndTag('3.1.2', annotated)
            .commitAndTag('2.0.1', annotated)
            .commitAndTag('2.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 3)
            bump = MAJOR
        }

        expect:
        semanticBuildVersion as String == '4.0.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version with versions matching for release should not work (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('0.1.1', annotated)
            .commitAndTag('0.1.2', annotated)
            .commitAndTag('0.0.1', annotated)
            .commitAndTag('0.0.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 0)
            bump = MAJOR
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == "Determined tag '1.0.0' is filtered out by your configuration; this is not supported.\nCheck your filtering and tag-prefix configuration. You may also be bumping the wrong component; if so, bump the component that will give you the intended version, or manually create a tag with the intended version on the commit to be released."

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version with tag pattern and versions matching for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('foo-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^foo-/
                tagPrefix = 'foo-'
                matching = new VersionsMatching(major: 0, minor: 1)
            }
            bump = PATCH
        }

        expect:
        semanticBuildVersion as String == '0.1.3-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping patch version with tag pattern and versions matching for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-1.1.1', annotated)
            .commitAndTag('foo-1.1.2', annotated)
            .commitAndTag('foo-0.0.1', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^foo-/
                tagPrefix = 'foo-'
                matching = new VersionsMatching(major: 1)
            }
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.1.3'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version with tag pattern and versions matching for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-1.1.1', annotated)
            .commitAndTag('foo-1.1.2', annotated)
            .commitAndTag('foo-0.0.1', annotated)
            .commitAndTag('foo-0.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^foo-/
                tagPrefix = 'foo-'
                matching = new VersionsMatching(major: 1, minor: 1, patch: 2)
            }
            bump = MINOR
        }

        expect:
        semanticBuildVersion as String == '1.2.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping minor version with tag pattern and versions matching for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-1.2.1', annotated)
            .commitAndTag('bar-1.2.2', annotated)
            .commitAndTag('bar-2.1.1', annotated)
            .commitAndTag('bar-2.1.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^foo-/
                tagPrefix = 'foo-'
                matching = new VersionsMatching(major: 1)
            }
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.3.0'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version with tag pattern and versions matching for snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-3.1.1', annotated)
            .commitAndTag('bar-3.1.2', annotated)
            .commitAndTag('bar-2.0.1', annotated)
            .commitAndTag('bar-2.0.2', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
                matching = new VersionsMatching(major: 3)
            }
            bump = MAJOR
        }

        expect:
        semanticBuildVersion as String == '4.0.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version with tag pattern and versions matching for release should not work (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('bar-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.with {
                tagPattern = ~/^bar-/
                tagPrefix = 'bar-'
                matching = new VersionsMatching(major: 0)
            }
            bump = MAJOR
            snapshot = false
        }

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == "Determined tag 'bar-1.0.0' is filtered out by your configuration; this is not supported.\nCheck your filtering and tag-prefix configuration. You may also be bumping the wrong component; if so, bump the component that will give you the intended version, or manually create a tag with the intended version on the commit to be released."

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'matching major version should not match on minor version component (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('5.4.2', annotated)
            .commitAndTag('6.5.3', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 5)
        }

        expect:
        semanticBuildVersion as String == '5.4.3-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'matching major version should not match on parts of major version component (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('5.4.2', annotated)
            .commitAndTag('15.14.12', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 5)
        }

        expect:
        semanticBuildVersion as String == '5.4.3-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'matching patch version should not match on parts of patch version component (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('5.4.3-pre.1', annotated)
            .commitAndTag('5.4.32', annotated)
            .makeChanges()

        and:
        semanticBuildVersion.with {
            semanticBuildVersion.config.preRelease = new PreRelease(
                startingVersion: 'alpha.0',
                bump: {
                    def parts = it.split(/\./)
                    "${parts[0]}.${++(parts[1] as int)}"
                }
            )
            config.with {
                matching = new VersionsMatching(major: 5, minor: 4, patch: 3)
                preRelease = new PreRelease(
                    startingVersion: 'pre.1',
                    bump: { "pre.${((it - ~/^pre\./) as int) + 1}" }
                )
            }
        }

        expect:
        semanticBuildVersion as String == '5.4.3-pre.2-SNAPSHOT'

        where:
        annotated << [false, true]
    }
}
