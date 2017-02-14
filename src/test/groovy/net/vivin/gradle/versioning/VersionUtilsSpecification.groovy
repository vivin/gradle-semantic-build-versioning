package net.vivin.gradle.versioning

import mockit.Mock
import mockit.MockUp
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.StatusCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
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

    @Unroll('latest version is #testNamePart (annotated: #annotated)')
    def 'test various latest version variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it, annotated
        }

        expect:
        versionUtils.latestVersion == expectedVersion

        where:
        testNamePart                                                            | tagNames                                                                 | annotated || expectedVersion
        'correct'                                                               | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0', '3.0.0']          | false     || '3.0.0'
        'non pre-release version'                                               | ['0.0.1-alpha', '0.0.1']                                                 | false     || '0.0.1'
        'numerically higher pre-release version'                                | ['0.0.1-2', '0.0.1-3']                                                   | false     || '0.0.1-3'
        'numerically higher pre-release version even with multiple identifiers' | ['0.0.1-alpha.2', '0.0.1-alpha.3']                                       | false     || '0.0.1-alpha.3'
        'lexically higher pre-release version'                                  | ['0.0.1-x', '0.0.1-y']                                                   | false     || '0.0.1-y'
        'lexically higher pre-release version even with multiple identifiers'   | ['0.0.1-alpha.x', '0.0.1-alpha.y']                                       | false     || '0.0.1-alpha.y'
        'non numeric pre-release version'                                       | ['0.0.1-999', '0.0.1-alpha']                                             | false     || '0.0.1-alpha'
        'non numeric pre-release version even with multiple identifiers'        | ['0.0.1-alpha.999', '0.0.1-alpha.beta']                                  | false     || '0.0.1-alpha.beta'
        'version with largest set of pre-release fields'                        | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0',
                                                                                   '3.0.0', '3.0.1-alpha', '3.0.1-alpha.0', '3.0.1-alpha.1',
                                                                                   '3.0.1-alpha.beta.gamma', '3.0.1-alpha.beta.gamma.delta']               | false     || '3.0.1-alpha.beta.gamma.delta'
        'not version with largest set of pre-release fields'                    | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0',
                                                                                   '3.0.0', '3.0.1-alpha', '3.0.1-alpha.0', '3.0.1-alpha.1',
                                                                                   '3.0.1-alpha.beta.gamma', '3.0.1-alpha.beta.gamma.delta', '3.0.1-beta'] | false     || '3.0.1-beta'
        'null when there are no tags'                                           | []                                                                       | false     || null
        'correct'                                                               | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0', '3.0.0']          | true      || '3.0.0'
        'non pre-release version'                                               | ['0.0.1-alpha', '0.0.1']                                                 | true      || '0.0.1'
        'numerically higher pre-release version'                                | ['0.0.1-2', '0.0.1-3']                                                   | true      || '0.0.1-3'
        'numerically higher pre-release version even with multiple identifiers' | ['0.0.1-alpha.2', '0.0.1-alpha.3']                                       | true      || '0.0.1-alpha.3'
        'lexically higher pre-release version'                                  | ['0.0.1-x', '0.0.1-y']                                                   | true      || '0.0.1-y'
        'lexically higher pre-release version even with multiple identifiers'   | ['0.0.1-alpha.x', '0.0.1-alpha.y']                                       | true      || '0.0.1-alpha.y'
        'non numeric pre-release version'                                       | ['0.0.1-999', '0.0.1-alpha']                                             | true      || '0.0.1-alpha'
        'non numeric pre-release version even with multiple identifiers'        | ['0.0.1-alpha.999', '0.0.1-alpha.beta']                                  | true      || '0.0.1-alpha.beta'
        'version with largest set of pre-release fields'                        | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0',
                                                                                   '3.0.0', '3.0.1-alpha', '3.0.1-alpha.0', '3.0.1-alpha.1',
                                                                                   '3.0.1-alpha.beta.gamma', '3.0.1-alpha.beta.gamma.delta']               | true      || '3.0.1-alpha.beta.gamma.delta'
        'not version with largest set of pre-release fields'                    | ['0.0.1', '0.0.2', '0.1.0', '0.1.1', '1.0.0', '2.0.0',
                                                                                   '3.0.0', '3.0.1-alpha', '3.0.1-alpha.0', '3.0.1-alpha.1',
                                                                                   '3.0.1-alpha.beta.gamma', '3.0.1-alpha.beta.gamma.delta', '3.0.1-beta'] | true      || '3.0.1-beta'
        'null when there are no tags'                                           | []                                                                       | true      || null
    }

    @Unroll
    def 'latest version is null when there are no matching tags (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag 'foo-0.0.1', annotated

        and:
        semanticBuildVersion.config.tagPattern = ~/^bar/

        expect:
        versionUtils.latestVersion == null

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'determining latest version with corrupt HEAD reference causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '0.0.1', annotated

        and:
        new File(testRepository.repository.directory, 'HEAD').text = '0000000000000000000000000000000000000000'

        when:
        versionUtils.latestVersion

        then:
        BuildException e = thrown()
        e.message == 'Unexpected error while parsing HEAD commit: Missing unknown 0000000000000000000000000000000000000000'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'latest version is found through merges (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commit()
            .branch('feature')
            .checkoutBranch('feature')
            .makeChanges()
            .commit()
            .checkoutBranch('master')
            .makeChanges()
            .commit()
            .merge('feature')

        expect:
        versionUtils.latestVersion == '0.0.1'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'non sem ver tags are ignored automatically (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commitAndTag '0.1', annotated

        and:
        semanticBuildVersion.config.tagPattern = ~/.*/

        when:
        versionUtils.determineVersion()

        then:
        notThrown BuildException

        and:
        semanticBuildVersion as String == '0.0.2-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'generating a tag that would be filtered out is not supported (annotated: #annotated)'() {
        given:
        semanticBuildVersion.with {
            config.tagPattern = ~/^foo-/
            snapshot = false
        }

        when:
        versionUtils.determineVersion()

        then:
        BuildException e = thrown()
        e.message == "Determined tag '0.1.0' is filtered out by your configuration; this is not supported.\nCheck your filtering and tag-prefix configuration. You may also be bumping the wrong component; if so, bump the component that will give you the intended version, or manually create a tag with the intended version on the commit to be released."

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'tagged version is recognized as non snapshot (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '0.0.1', annotated

        when:
        versionUtils.determineVersion()

        then:
        !semanticBuildVersion.snapshot

        where:
        annotated << [false, true]
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
    def 'promoting pre-release version with snapshot \'#snapshot\', promoteToRelease \'#promoteToRelease\', newPreRelease \'#newPreRelease\' and bump \'#bump.toString().toLowerCase()\' when head is pointing to tag causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '0.2.0', annotated

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
        [newPreRelease, promoteToRelease, snapshot, bump, annotated] << (([[true, false]] * 3 << [PATCH, null] << [false, true]).combinations() -
            // exclude valid non-failing cases
            [[false, false, false, null, false],
             [false, false, false, null, true],
             [false, false, true, null, false],
             [false, false, true, null, true]]).findAll {
            // exclude the 12 cases that fail due to another reason
            !(it[1] && (it[0] || it[3]))
        }
    }

    @Unroll
    def 'determining HEAD tag with corrupt objects directory causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '1.0.0', annotated

        and:
        new File(testRepository.repository.directory, 'objects').eachFileRecurse(FILES) { it.delete() }

        when:
        versionUtils.determineVersion()

        then:
        BuildException e = thrown()
        e.message.startsWith 'Unexpected error while parsing HEAD commit: Missing unknown '

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'determining HEAD tag with corrupt packed-refs causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag '1.0.0', annotated

        and:
        new File(testRepository.repository.directory, 'packed-refs').text = '^'

        when:
        versionUtils.getLatestTagOnReference(Constants.HEAD)

        then:
        BuildException e = thrown()
        e.message == 'Unexpected error while determining tag: Peeled line before ref.'

        where:
        annotated << [false, true]
    }

    def 'using git-status that throws a GitAPIException causes build to fail'() {
        given:
        // Mock this call, because the method declares a checked exception, but never throws it ever
        // So to test this code path, the call has to be mocked with JMockit
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

    def 'calling toString multiple times on SemanticBuildVersion only calculates the version once'() {
        given:
        semanticBuildVersion.versionUtils = Mock(VersionUtils)

        when:
        10.times { semanticBuildVersion.toString() }

        then:
        1 * semanticBuildVersion.versionUtils.determineVersion() >> '1.2.3'
    }
}
