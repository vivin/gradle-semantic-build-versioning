package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

@Title('Major Minor Patch Autobumping Specification')
class MajorMinorPatchAutobumpingSpecification extends Specification {
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

    def 'autobumping without any prior commits causes build to fail'() {
        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == 'Could not autobump because there are no prior commits'
    }

    def 'autobumping without matching prior commit message causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit '''
                This is a message
                patch
            '''.stripIndent()

        when:
        semanticBuildVersion as String

        then:
        BuildException e = thrown()
        e.message == "Could not autobump because the last commit message did not match the major (/${semanticBuildVersion.config.autobump.majorPattern}/), minor (/${semanticBuildVersion.config.autobump.minorPattern}/), patch (/${semanticBuildVersion.config.autobump.patchPattern}/), pre-release (/${semanticBuildVersion.config.autobump.preReleasePattern}/), or release (/${semanticBuildVersion.config.autobump.promoteToReleasePattern}/) patterns specified in the autobump configuration"
    }

    @Unroll('#testName')
    def 'test various autobumping variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it
        }
        testRepository
            .makeChanges()
            .commit """
                This is a message
                [$autobumpTag]
            """.stripIndent()

        and:
        semanticBuildVersion.with {
            if(tagPattern) {
                config.tagPattern = tagPattern
            }
            if(matching) {
                config.matching = new VersionsMatching(matching)
            }
        }

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        tagPattern | matching             | tagNames                                                          | autobumpTag || expectedVersion
        false      | false                | []                                                                | 'patch'     || '0.1.0'
        false      | false                | ['0.0.1']                                                         | 'patch'     || '0.0.2'
        false      | false                | ['0.1.3']                                                         | 'minor'     || '0.2.0'
        false      | false                | ['0.2.3']                                                         | 'major'     || '1.0.0'
        ~/^bar-/   | false                | ['foo-0.1.1', 'foo-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'patch'     || '0.0.3'
        ~/^bar-/   | false                | ['foo-0.2.1', 'foo-0.2.2', 'bar-0.1.1', 'bar-0.1.2']              | 'minor'     || '0.2.0'
        ~/^bar-/   | false                | ['foo-0.1.1', 'foo-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'major'     || '1.0.0'
        false      | [major: 0]           | ['1.1.1', '1.1.2', '0.0.1', '0.0.2']                              | 'patch'     || '0.0.3'
        false      | [major: 1]           | ['1.1.1', '1.1.2', '0.0.1', '0.0.2']                              | 'patch'     || '1.1.3'
        false      | [major: 2]           | ['1.1.1', '1.1.2', '0.0.1', '0.0.2']                              | 'patch'     || '0.1.0'
        false      | [major: 0, minor: 1] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'minor'     || '0.1.0'
        false      | [major: 1, minor: 1] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'minor'     || '1.2.0'
        false      | [major: 1, minor: 2] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'minor'     || '1.3.0'
        false      | [major: 0]           | ['0.1.1', '0.1.2', '0.0.1', '0.0.2']                              | 'major'     || '1.0.0'
        false      | [major: 1]           | ['0.1.1', '0.1.2', '0.0.1', '0.0.2']                              | 'major'     || '1.0.0'
        ~/^foo-/   | [major: 0]           | ['foo-1.1.1', 'foo-1.1.2', 'foo-0.0.1', 'bar-0.0.1', 'bar-0.0.2'] | 'patch'     || '0.0.2'
        ~/^foo-/   | [major: 1]           | ['foo-1.1.1', 'foo-1.1.2', 'foo-0.0.1', 'bar-0.0.1', 'bar-0.0.2'] | 'patch'     || '1.1.3'
        ~/^foo-/   | [major: 1, minor: 2] | ['foo-1.2.1', 'bar-1.2.2', 'bar-1.1.1', 'bar-1.1.2']              | 'minor'     || '1.3.0'
        ~/^bar-/   | [major: 1]           | ['foo-0.1.1', 'bar-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'major'     || '1.0.0'

        and:
        testName = "autobumping $autobumpTag version" +
            (tagNames ? '' : ' without tags') +
            (tagPattern || matching ? ' with' : '') +
            (tagPattern ? " tag pattern '$tagPattern'" : '') +
            (tagPattern && matching ? ' and' : '') +
            (matching ? " versions matching $matching" : '') +
            ' for release'
    }
}
