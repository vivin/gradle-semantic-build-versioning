package net.vivin.gradle.versioning

import org.gradle.api.Project
import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Title
import spock.lang.Unroll

import java.util.regex.Pattern

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
        }
    }

    def 'autobumping without any prior commits does not cause build to fail but returns starting version'() {
        expect:
        semanticBuildVersion as String == '0.1.0'
    }

    def 'autobumping without matching prior commit message causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commit '''
                This is a message
                patch
            '''.stripIndent()

        expect:
        semanticBuildVersion as String == '0.1.0'
    }

    @Unroll('#testName')
    def 'test various autobumping variants'() {
        given:
        tagNames.each {
            testRepository
                .makeChanges()
                .commitAndTag it, annotated
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
            if(tagPattern) {
                config.tagPrefix = tagPrefix
            }
            if(matching) {
                config.matching = new VersionsMatching(matching)
            }
        }

        expect:
        semanticBuildVersion as String == expectedVersion

        where:
        tagPattern | tagPrefix  | matching             | tagNames                                                          | autobumpTag | annotated || expectedVersion
        false      | false      | false                | []                                                                | 'patch'     | false     || '0.1.0'
        false      | false      | false                | ['0.0.1']                                                         | 'patch'     | false     || '0.0.2'
        false      | false      | false                | ['0.1.3']                                                         | 'minor'     | false     || '0.2.0'
        false      | false      | false                | ['0.2.3']                                                         | 'major'     | false     || '1.0.0'
        ~/^bar-/   | 'bar-'     | false                | ['foo-0.1.1', 'foo-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'patch'     | false     || '0.0.3'
        ~/^bar-/   | 'bar-'     | false                | ['foo-0.2.1', 'foo-0.2.2', 'bar-0.1.1', 'bar-0.1.2']              | 'minor'     | false     || '0.2.0'
        ~/^bar-/   | 'bar-'     | false                | ['foo-0.1.1', 'foo-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'major'     | false     || '1.0.0'
        false      | false      | [major: 0]           | ['1.1.1', '1.1.2', '0.0.1', '0.0.2']                              | 'patch'     | false     || '0.0.3'
        false      | false      | [major: 1]           | ['1.1.1', '1.1.2', '0.0.1', '0.0.2']                              | 'patch'     | false     || '1.1.3'
        false      | false      | [major: 0]           | ['1.1.1', '1.1.2', '2.1.1', '2.1.2']                              | 'patch'     | false     || '0.1.0'
        false      | false      | [major: 0, minor: 1] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'minor'     | false     || '0.1.0'
        false      | false      | [major: 1, minor: 1] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'patch'     | false     || '1.1.3'
        false      | false      | [major: 1, minor: 2] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'patch'     | false     || '1.2.3'
        false      | false      | [major: 1]           | ['0.1.1', '0.1.2', '0.0.1', '0.0.2']                              | 'major'     | false     || '1.0.0'
        ~/^foo-/   | 'foo-'     | [major: 0]           | ['foo-1.1.1', 'foo-1.1.2', 'foo-0.0.1', 'bar-0.0.1', 'bar-0.0.2'] | 'patch'     | false     || '0.0.2'
        ~/^foo-/   | 'foo-'     | [major: 1]           | ['foo-1.1.1', 'foo-1.1.2', 'foo-0.0.1', 'bar-0.0.1', 'bar-0.0.2'] | 'patch'     | false     || '1.1.3'
        ~/^foo-/   | 'foo-'     | [major: 1, minor: 2] | ['foo-1.2.1', 'bar-1.2.2', 'bar-1.1.1', 'bar-1.1.2']              | 'patch'     | false     || '1.2.2'
        ~/^bar-/   | 'bar-'     | [major: 1]           | ['foo-0.1.1', 'bar-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'major'     | false     || '1.0.0'
        false      | false      | false                | []                                                                | 'patch'     | true      || '0.1.0'
        false      | false      | false                | ['0.0.1']                                                         | 'patch'     | true      || '0.0.2'
        false      | false      | false                | ['0.1.3']                                                         | 'minor'     | true      || '0.2.0'
        false      | false      | false                | ['0.2.3']                                                         | 'major'     | true      || '1.0.0'
        ~/^bar-/   | 'bar-'     | false                | ['foo-0.1.1', 'foo-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'patch'     | true      || '0.0.3'
        ~/^bar-/   | 'bar-'     | false                | ['foo-0.2.1', 'foo-0.2.2', 'bar-0.1.1', 'bar-0.1.2']              | 'minor'     | true      || '0.2.0'
        ~/^bar-/   | 'bar-'     | false                | ['foo-0.1.1', 'foo-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'major'     | true      || '1.0.0'
        false      | false      | [major: 0]           | ['1.1.1', '1.1.2', '0.0.1', '0.0.2']                              | 'patch'     | true      || '0.0.3'
        false      | false      | [major: 1]           | ['1.1.1', '1.1.2', '0.0.1', '0.0.2']                              | 'patch'     | true      || '1.1.3'
        false      | false      | [major: 0]           | ['1.1.1', '1.1.2', '2.1.1', '2.1.2']                              | 'patch'     | true      || '0.1.0'
        false      | false      | [major: 0, minor: 1] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'minor'     | true      || '0.1.0'
        false      | false      | [major: 1, minor: 1] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'patch'     | true      || '1.1.3'
        false      | false      | [major: 1, minor: 2] | ['1.2.1', '1.2.2', '1.1.1', '1.1.2']                              | 'patch'     | true      || '1.2.3'
        false      | false      | [major: 1]           | ['0.1.1', '0.1.2', '0.0.1', '0.0.2']                              | 'major'     | true      || '1.0.0'
        ~/^foo-/   | 'foo-'     | [major: 0]           | ['foo-1.1.1', 'foo-1.1.2', 'foo-0.0.1', 'bar-0.0.1', 'bar-0.0.2'] | 'patch'     | true      || '0.0.2'
        ~/^foo-/   | 'foo-'     | [major: 1]           | ['foo-1.1.1', 'foo-1.1.2', 'foo-0.0.1', 'bar-0.0.1', 'bar-0.0.2'] | 'patch'     | true      || '1.1.3'
        ~/^foo-/   | 'foo-'     | [major: 1, minor: 2] | ['foo-1.2.1', 'bar-1.2.2', 'bar-1.1.1', 'bar-1.1.2']              | 'patch'     | true      || '1.2.2'
        ~/^bar-/   | 'bar-'     | [major: 1]           | ['foo-0.1.1', 'bar-0.1.2', 'bar-0.0.1', 'bar-0.0.2']              | 'major'     | true      || '1.0.0'

        and:
        testName = "autobumping $autobumpTag version" +
            (tagNames ? '' : ' without tags') +
            (tagPattern || matching ? ' with' : '') +
            (tagPattern ? " tag pattern '$tagPattern'" : '') +
            (tagPattern && matching ? ' and' : '') +
            (matching ? " versions matching $matching" : '') +
            " for release (annotated: $annotated)"
    }

    @Unroll
    def 'with missing [major: #major, minor: #minor, patch: #patch, preRelease: #preRelease, newPreRelease: #newPreRelease, promoteToRelease: #promoteToRelease] configuration respective autobump tag is ignored (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('2.0.0-pre.1', annotated)
            .makeChanges()
            .commit """
                This is a message
                [${major ? 'major' : ''}]
                [${minor ? 'minor' : ''}]
                [${patch ? 'patch' : ''}]
                [${preRelease ? 'pre-release' : ''}]
                [${newPreRelease ? 'new-pre-release' : ''}]
                [${promoteToRelease ? 'promote' : ''}]
            """.stripIndent()

        and:
        semanticBuildVersion.config.with {
            if(major) {
                autobump.majorPattern = null
            }
            if(minor) {
                autobump.minorPattern = null
            }
            if(patch) {
                autobump.patchPattern = null
            }
            if(preRelease) {
                autobump.preReleasePattern = null
            }
            if(newPreRelease) {
                autobump.newPreReleasePattern = null
            }
            if(promoteToRelease) {
                autobump.promoteToReleasePattern = null
            }
            it.preRelease = new PreRelease()
            it.preRelease.bump = {
                def parts = it.split(/\./)
                "${parts[0]}.${++(parts[1] as int)}"
            }
        }

        expect:
        semanticBuildVersion as String == '2.0.0-pre.2'

        where:
        [major, minor, patch, preRelease, newPreRelease, promoteToRelease, annotated] << ([[true, false]] * 7).combinations()
    }
}
