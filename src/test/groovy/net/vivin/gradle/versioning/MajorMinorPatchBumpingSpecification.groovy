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

    @Unroll
    def 'version without matching tags is default starting snapshot version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.1.0', annotated

        and:
        semanticBuildVersion.config.tagPattern = ~/^bar-/

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
        semanticBuildVersion.config.tagPattern = ~/^bar-/
        semanticBuildVersion.snapshot = false

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
            config.tagPattern = ~/^bar-/
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
            config.tagPattern = ~/^bar-/
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
            config.tagPattern = ~/^bar-/
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
            config.tagPattern = ~/^bar-/
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
            config.tagPattern = ~/^bar-/
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
            config.tagPattern = ~/^bar-/
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
            .commitAndTag('1.1.1', annotated)
            .commitAndTag('1.1.2', annotated)
            .commit()

        and:
        semanticBuildVersion.with {
            config.matching = new VersionsMatching(major: 1, minor: 2)
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
    def 'bumping major version with versions matching for release (annotated: #annotated)'() {
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

        expect:
        semanticBuildVersion as String == '1.0.0'

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
            config.tagPattern = ~/^foo-/
            config.matching = new VersionsMatching(major: 0, minor: 1)
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
            config.tagPattern = ~/^foo-/
            config.matching = new VersionsMatching(major: 1)
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
            config.tagPattern = ~/^foo/
            config.matching = new VersionsMatching(major: 1, minor: 1, patch: 2)
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
            .commitAndTag('bar-1.1.1', annotated)
            .commitAndTag('bar-1.1.2', annotated)
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
            config.tagPattern = ~/^bar-/
            config.matching = new VersionsMatching(major: 3)
            bump = MAJOR
        }

        expect:
        semanticBuildVersion as String == '4.0.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'bumping major version with tag pattern and versions matching for release (annotated: #annotated)'() {
        given:
        testRepository
            .commitAndTag('foo-0.1.1', annotated)
            .commitAndTag('bar-0.1.2', annotated)
            .commitAndTag('bar-0.0.1', annotated)
            .commitAndTag('bar-0.0.2', annotated)
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

        where:
        annotated << [false, true]
    }
}
