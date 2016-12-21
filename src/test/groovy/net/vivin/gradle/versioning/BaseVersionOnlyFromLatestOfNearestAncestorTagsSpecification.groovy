package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.internal.impldep.com.google.common.io.Files
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

import static net.vivin.gradle.versioning.VersionComponent.*

@Title('Base Version Only From Latest Of Nearest Ancestor Tags Specification')
class BaseVersionOnlyFromLatestOfNearestAncestorTagsSpecification extends Specification {
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

    def 'take the latest ancestor tag, ignoring other tags'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('2.0.0')
            .checkout('HEAD~')

        expect:
        semanticBuildVersion as String == '1.0.1'
    }

    def 'take the latest tag from the ancestor tags, ignoring other tags'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0')
            .branch('2.x')
            .checkoutBranch('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('2.0.0')
            .makeChanges()
            .commit()
            .makeChanges()
            .commit()
            .checkoutBranch('master')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('1.1.0')
            .merge('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('3.0.0')
            .checkout('master~')

        expect:
        semanticBuildVersion as String == '2.0.1'
    }

    def 'take the latest tag from the nearest ancestor tags, ignoring unrelated tags'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('3.0.0')
            .makeChanges()
            .commitAndTag('1.0.0')
            .branch('2.x')
            .checkoutBranch('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('2.0.0')
            .makeChanges()
            .commit()
            .makeChanges()
            .commit()
            .checkoutBranch('master')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('1.1.0')
            .merge('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('4.0.0')
            .checkout('master~')

        expect:
        semanticBuildVersion as String == '2.0.1'
    }

    def 'if the determined version exists already as tag, the build should fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0')
            .commit()
            .makeChanges()
            .commitAndTag('1.0.1')
            .checkout('HEAD~')

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == "Determined version '1.0.1' already exists in the repository at '$testRepository.repository.directory'.\nFix your bumping or manually create a tag with the intended version on the commit to be released."
    }

    def 'if the determined version exists already as tag, a snapshot build should not fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0')
            .commit()
            .makeChanges()
            .commitAndTag('1.0.1')
            .checkout('HEAD~')

        and:
        semanticBuildVersion.snapshot = true

        expect:
        semanticBuildVersion as String == '1.0.1-SNAPSHOT'
    }
}
