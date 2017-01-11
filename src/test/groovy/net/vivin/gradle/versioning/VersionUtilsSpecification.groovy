package net.vivin.gradle.versioning

import mockit.Mock
import mockit.MockUp
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.StatusCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

import static groovy.io.FileType.FILES
import static net.vivin.gradle.versioning.VersionComponent.PATCH

@Title('Version Utils Specification')
class VersionUtilsSpecification extends Specification {
    private TestRepository testRepository
    private SemanticBuildVersion semanticBuildVersion

    @Subject
    private VersionUtils versionUtils

    def setup() {
        def project = Mock(Project) {
            getProjectDir() >> testRepository.repository.workTree
        }
        semanticBuildVersion = new SemanticBuildVersion(project)
        semanticBuildVersion.config = new SemanticBuildVersionConfiguration()
        versionUtils = new VersionUtils(semanticBuildVersion, testRepository.repository.workTree)
    }

    @Unroll('latest version is #testNamePart')
    def 'test various latest version variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it
        }

        expect:
        versionUtils.latestVersion == expectedVersion

        where:
        testNamePart                                                            | tagNames                                                                 || expectedVersion
        'correct'                                                               | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0', '3.0.0']          || '3.0.0'
        'non pre-release version'                                               | ['0.0.1', '0.0.1-alpha']                                                 || '0.0.1'
        'numerically higher pre-release version'                                | ['0.0.1-2', '0.0.1-3']                                                   || '0.0.1-3'
        'numerically higher pre-release version even with multiple identifiers' | ['0.0.1-alpha.2', '0.0.1-alpha.3']                                       || '0.0.1-alpha.3'
        'lexically higher pre-release version'                                  | ['0.0.1-x', '0.0.1-y']                                                   || '0.0.1-y'
        'lexically higher pre-release version even with multiple identifiers'   | ['0.0.1-alpha.x', '0.0.1-alpha.y']                                       || '0.0.1-alpha.y'
        'non numeric pre-release version'                                       | ['0.0.1-999', '0.0.1-alpha']                                             || '0.0.1-alpha'
        'non numeric pre-release version even with multiple identifiers'        | ['0.0.1-alpha.999', '0.0.1-alpha.beta']                                  || '0.0.1-alpha.beta'
        'version with largest set of pre-release fields'                        | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0',
                                                                                   '3.0.0', '3.0.1-alpha', '3.0.1-alpha.0', '3.0.1-alpha.1',
                                                                                   '3.0.1-alpha.beta.gamma', '3.0.1-alpha.beta.gamma.delta']               || '3.0.1-alpha.beta.gamma.delta'
        'not version with largest set of pre-release fields'                    | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0',
                                                                                   '3.0.0', '3.0.1-alpha', '3.0.1-alpha.0', '3.0.1-alpha.1',
                                                                                   '3.0.1-alpha.beta.gamma', '3.0.1-alpha.beta.gamma.delta', '3.0.1-beta'] || '3.0.1-beta'
        'null when there are no tags'                                           | []                                                                       || null
    }

    def 'latest version is null when there are no matching tags'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.0.1'

        and:
        semanticBuildVersion.config.tagPattern = ~/^bar/

        expect:
        versionUtils.latestVersion == null
    }

    def 'non sem ver tags are ignored automatically'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1')
            .makeChanges()
            .commitAndTag '0.1'

        and:
        semanticBuildVersion.config.tagPattern = ~/.*/

        when:
        versionUtils.determineVersion()

        then:
        notThrown BuildException

        and:
        semanticBuildVersion as String == '0.0.2-SNAPSHOT'
    }

    def 'tagged version is recognized as non snapshot'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '0.0.1'

        when:
        versionUtils.determineVersion()

        then:
        !semanticBuildVersion.snapshot
    }

    def 'invalid repository causes build to fail'() {
        given:
        testRepository.repository.config.configFile.text = '['

        when:
        new VersionUtils(semanticBuildVersion, testRepository.repository.workTree)

        then:
        BuildException e = thrown()
        e.message == 'Unable to find Git repository: Unknown repository format'
    }

    @Unroll
    def 'promoting pre-release version with snapshot \'#snapshot\', promoteToRelease \'#promoteToRelease\', newPreRelease \'#newPreRelease\' and bump \'#bump.toString().toLowerCase()\' when head is pointing to tag causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '0.2.0'

        and:
        semanticBuildVersion.with {
            config.preRelease = new PreRelease(startingVersion: 'alpha.0', bump: { it })
            it.bump = bump
            it.newPreRelease = newPreRelease
            it.promoteToRelease = promoteToRelease
            it.snapshot = snapshot
        }

        when:
        versionUtils.determineVersion()

        then:
        BuildException e = thrown()
        e.message == 'Cannot bump the version, create a new pre-release version, or promote a pre-release version because HEAD is currently pointing to a tag that identifies an existing version. To be able to create a new version, you must make changes'

        where:
        [newPreRelease, promoteToRelease, snapshot, bump] << ([[true, false]] * 3 << [PATCH, null]).combinations() - [[false, false, false, null], [false, false, true, null]]
    }

    def 'determining HEAD tag with corrupt objects directory causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '1.0.0'

        and:
        new File(testRepository.repository.directory, 'objects').eachFileRecurse(FILES) { it.delete() }

        when:
        versionUtils.determineVersion()

        then:
        BuildException e = thrown()
        e.message.startsWith 'Unexpected error while determining HEAD tag: Missing unknown '
    }

    def 'determining HEAD tag with corrupt packed-refs causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '1.0.0'

        and:
        versionUtils.refresh()
        new File(testRepository.repository.directory, 'packed-refs').text = '^'

        when:
        versionUtils.determineVersion()

        then:
        BuildException e = thrown()
        e.message == 'Unexpected error while determining HEAD tag: Peeled line before ref.'
    }

    def 'using git-status that throws a GitAPIException causes build to fail'() {
        given:
        // Mock this call, because the method declares a checked exception, but never throws it ever
        // So to test this code patch, the call has to be mocked with JMockit
        new MockUp<StatusCommand>() {
            @Mock
            Status call() {
                throw new GitAPIException('error during git-status') {
                }
            }
        }

        when:
        versionUtils.hasUncommittedChanges()

        then:
        BuildException e = thrown()
        e.message == 'Unexpected error while determining repository status: error during git-status'
    }
}
