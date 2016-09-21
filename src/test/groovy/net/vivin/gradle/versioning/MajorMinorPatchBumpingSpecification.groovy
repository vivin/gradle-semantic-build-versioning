package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title

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

    def 'version without matching tags is default starting snapshot version'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.1.0'

        and:
        semanticBuildVersion.config.tagPattern = ~/^bar-/

        expect:
        semanticBuildVersion as String == '0.1.0-SNAPSHOT'
    }

    def 'version without matching tags is default starting release version'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.1.0'

        and:
        semanticBuildVersion.config.tagPattern = ~/^bar-/
        semanticBuildVersion.snapshot = false

        expect:
        semanticBuildVersion as String == '0.1.0'
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

    def 'version with prior tag and uncommitted changes is next snapshot version'() {
        given:
        testRepository
            .commitAndTag('0.0.1')
            .makeChanges()

        expect:
        semanticBuildVersion as String == '0.0.2-SNAPSHOT'
    }

    def 'version with prior tag and committed changes is next release version'() {
        given:
        testRepository
            .commitAndTag('0.0.1')
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.snapshot = false

        expect:
        semanticBuildVersion as String == '0.0.2'
    }

    def 'version with custom snapshot suffix'() {
        given:
        testRepository.makeChanges()

        and:
        semanticBuildVersion.config.snapshotSuffix = 'CURRENT'

        expect:
        semanticBuildVersion as String == '0.1.0-CURRENT'
    }

    def 'checking out tag produces same version as tag'() {
        given:
        testRepository
            .commitAndTag('3.1.2')
            .commitAndTag('3.1.3')
            .commitAndTag('3.1.4')
            .checkout '3.1.2'

        expect:
        semanticBuildVersion as String == '3.1.2'
    }

    def 'checking out tag produces same version as tag even if other tags are present'() {
        given:
        testRepository
            .commitAndTag('3.1.2')
            .commitAndTag('3.1.3')
            .commitAndTag('3.1.4')
            .checkout('3.1.2')
            .tag 'foo'

        expect:
        semanticBuildVersion as String == '3.1.2'
    }

    def 'bumping patch version for snapshot'() {
        given:
        testRepository
            .commitAndTag('0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.bump = PATCH

        expect:
        semanticBuildVersion as String == '0.0.3-SNAPSHOT'
    }

    def 'bumping patch version for release'() {
        given:
        testRepository
            .commitAndTag('0.0.2')
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            bump = PATCH
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.0.3'
    }

    def 'bumping minor version for snapshot'() {
        given:
        testRepository
            .commitAndTag('0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.bump = MINOR

        expect:
        semanticBuildVersion as String == '0.1.0-SNAPSHOT'
    }

    def 'bumping minor version for release'() {
        given:
        testRepository
            .commitAndTag('0.1.3')
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.0'
    }

    def 'bumping major version for snapshot'() {
        given:
        testRepository
            .commitAndTag('0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.bump = MAJOR

        expect:
        semanticBuildVersion as String == '1.0.0-SNAPSHOT'
    }

    void 'bumping major version for release'() {
        given:
        testRepository
            .commitAndTag('0.2.3')
            .makeChanges()
            .commit()

        and:
        semanticBuildVersion.with {
            bump = MAJOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.0.0'
    }

    def 'bumping patch version with tag pattern for snapshot'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1')
            .commitAndTag('foo-0.1.2')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            bump = PATCH
        }

        expect:
        semanticBuildVersion as String == '0.0.3-SNAPSHOT'
    }

    def 'bumping patch version with tag pattern for release'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1')
            .commitAndTag('foo-0.1.2')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.0.3'
    }

    def 'bumping minor version with tag pattern for snapshot'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1')
            .commitAndTag('foo-0.1.2')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            bump = MINOR
        }

        expect:
        semanticBuildVersion as String == '0.1.0-SNAPSHOT'
    }

    def 'bumping minor version with tag pattern for release'() {
        given:
        testRepository
            .commitAndTag('foo-0.2.1')
            .commitAndTag('foo-0.2.2')
            .commitAndTag('bar-0.1.1')
            .commitAndTag('bar-0.1.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '0.2.0'
    }

    def 'bumping major version with tag pattern for snapshot'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1')
            .commitAndTag('foo-0.1.2')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            bump = MAJOR
        }

        expect:
        semanticBuildVersion as String == '1.0.0-SNAPSHOT'
    }

    def 'bumping major version with tag pattern for release'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1')
            .commitAndTag('foo-0.1.2')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            bump = MAJOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.0.0'
    }

    def 'bumping patch version with versions matching for snapshot'() {
        given:
        testRepository
            .commitAndTag('0.1.1')
            .commitAndTag('0.1.2')
            .commitAndTag('0.0.1')
            .commitAndTag('0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 0, minor: 1)
            bump = PATCH
        }

        expect:
        semanticBuildVersion as String == '0.1.3-SNAPSHOT'
    }

    def 'bumping patch version with versions matching for release'() {
        given:
        testRepository
            .commitAndTag('1.1.1')
            .commitAndTag('1.1.2')
            .commitAndTag('0.0.1')
            .commitAndTag('0.0.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 1)
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.1.3'
    }

    def 'bumping minor version with versions matching for snapshot'() {
        given:
        testRepository
            .commitAndTag('1.1.1')
            .commitAndTag('1.1.2')
            .commitAndTag('0.0.1')
            .commitAndTag('0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 1, minor: 1, patch: 2)
            bump = MINOR
        }

        expect:
        semanticBuildVersion as String == '1.2.0-SNAPSHOT'
    }

    def 'bumping minor version with versions matching for release'() {
        given:
        testRepository
            .commitAndTag('1.2.1')
            .commitAndTag('1.2.2')
            .commitAndTag('1.1.1')
            .commitAndTag('1.1.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 1, minor: 2)
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.3.0'
    }

    def 'bumping major version with versions matching for snapshot'() {
        given:
        testRepository
            .commitAndTag('3.1.1')
            .commitAndTag('3.1.2')
            .commitAndTag('2.0.1')
            .commitAndTag('2.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 3)
            bump = MAJOR
        }

        expect:
        semanticBuildVersion as String == '4.0.0-SNAPSHOT'
    }

    def 'bumping major version with versions matching for release'() {
        given:
        testRepository
            .commitAndTag('0.1.1')
            .commitAndTag('0.1.2')
            .commitAndTag('0.0.1')
            .commitAndTag('0.0.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 0)
            bump = MAJOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.0.0'
    }

    def 'bumping patch version with tag pattern and versions matching for snapshot'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1')
            .commitAndTag('foo-0.1.2')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^foo-/
            config.matching = new VersionsMatching(major: 0, minor: 1)
            bump = PATCH
        }

        expect:
        semanticBuildVersion as String == '0.1.3-SNAPSHOT'
    }

    def 'bumping patch version with tag pattern and versions matching for release'() {
        given:
        testRepository
            .commitAndTag('foo-1.1.1')
            .commitAndTag('foo-1.1.2')
            .commitAndTag('foo-0.0.1')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^foo-/
            config.matching = new VersionsMatching(major: 1)
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.1.3'
    }

    def 'bumping minor version with tag pattern and versions matching for snapshot'() {
        given:
        testRepository
            .commitAndTag('foo-1.1.1')
            .commitAndTag('foo-1.1.2')
            .commitAndTag('foo-0.0.1')
            .commitAndTag('foo-0.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^foo/
            config.matching = new VersionsMatching(major: 1, minor: 1, patch: 2)
            bump = MINOR
        }

        expect:
        semanticBuildVersion as String == '1.2.0-SNAPSHOT'
    }

    def 'bumping minor version with tag pattern and versions matching for release'() {
        given:
        testRepository
            .commitAndTag('foo-1.2.1')
            .commitAndTag('bar-1.2.2')
            .commitAndTag('bar-1.1.1')
            .commitAndTag('bar-1.1.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^foo-/
            config.matching = new VersionsMatching(major: 1, minor: 2)
            bump = MINOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.3.0'
    }

    def 'bumping major version with tag pattern and versions matching for snapshot'() {
        given:
        testRepository
            .commitAndTag('foo-3.1.1')
            .commitAndTag('bar-3.1.2')
            .commitAndTag('bar-2.0.1')
            .commitAndTag('bar-2.0.2')
            .makeChanges()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            config.matching = new VersionsMatching(major: 3)
            bump = MAJOR
        }

        expect:
        semanticBuildVersion as String == '4.0.0-SNAPSHOT'
    }

    def 'bumping major version with tag pattern and versions matching for release'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1')
            .commitAndTag('bar-0.1.2')
            .commitAndTag('bar-0.0.1')
            .commitAndTag('bar-0.0.2')
            .commit()

        and:
        semanticBuildVersion.with {
            config.tagPattern = ~/^bar-/
            config.matching = new VersionsMatching(major: 0)
            bump = MAJOR
            snapshot = false
        }

        expect:
        semanticBuildVersion as String == '1.0.0'
    }
}
