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

    @Unroll
    def 'take the latest ancestor tag, ignoring other tags (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('2.0.0', annotated)
            .checkout('HEAD~')

        expect:
        semanticBuildVersion as String == '1.0.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'take the latest tag from the ancestor tags, ignoring other tags (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .branch('2.x')
            .checkoutBranch('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('2.0.0', annotated)
            .makeChanges()
            .commit()
            .makeChanges()
            .commit()
            .checkoutBranch('master')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('1.1.0', annotated)
            .merge('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('3.0.0', annotated)
            .checkout('master~')

        expect:
        semanticBuildVersion as String == '2.0.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'take the latest tag from the nearest ancestor tags, ignoring unrelated tags (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('3.0.0', annotated)
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .branch('2.x')
            .checkoutBranch('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('2.0.0', annotated)
            .makeChanges()
            .commit()
            .makeChanges()
            .commit()
            .checkoutBranch('master')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('1.1.0', annotated)
            .merge('2.x')
            .makeChanges()
            .commit()
            .makeChanges()
            .commitAndTag('4.0.0', annotated)
            .checkout('master~')

        expect:
        semanticBuildVersion as String == '2.0.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'if the determined version exists already as tag, the build should fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .commit()
            .makeChanges()
            .commitAndTag('1.0.1', annotated)
            .checkout('HEAD~')

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == "Determined version '1.0.1' already exists on another commit in the repository at '$testRepository.repository.directory'.\nCheck your configuration to ensure that you haven't forgotten to filter out certain tags or versions. You may also be bumping the wrong component; if so, bump the component that will give you the intended version, or manually create a tag with the intended version on the commit to be released."

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'if the determined version exists already as tag, a snapshot build should not fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .commit()
            .makeChanges()
            .commitAndTag('1.0.1', annotated)
            .checkout('HEAD~')

        and:
        semanticBuildVersion.snapshot = true

        expect:
        semanticBuildVersion as String == '1.0.1-SNAPSHOT'

        where:
        annotated << [false, true]
    }
}
