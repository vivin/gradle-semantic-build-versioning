package net.vivin.gradle.versioning

import net.vivin.gradle.versioning.spock.extensions.git.Bare
import net.vivin.gradle.versioning.spock.extensions.gradle.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Title('Tag Task Specification')
class TagTaskSpecification extends Specification {
    @Bare
    private TestRepository origin
    private TestRepository testRepository
    @ProjectDirProvider({ testRepository })
    private GradleRunner gradleRunner

    def 'tagging with uncommitted changes causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1')
            .makeChanges()

        when:
        def buildResult = gradleRunner.withArguments('tag').buildAndFail()

        then:
        buildResult.output.contains 'Cannot create a tag when there are uncommitted changes'
    }

    @Unroll
    def 'tagging snapshot version with task \'#tagTask\' causes build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1')
            .makeChanges()
            .commit()

        when:
        def buildResult = gradleRunner.withArguments(tagTask).buildAndFail()

        then:
        buildResult.output.contains 'Cannot create a tag for a snapshot version'

        where:
        tagTask << ['tag', 'tagAndPush', 'tAP']
    }

    def 'tag without Git repository causes build to fail'(GradleRunner gradleRunner) {
        when:
        def buildResult = gradleRunner.withArguments('-P', 'release', 'tag').buildAndFail()

        then:
        buildResult.output.contains 'Tag on repository without HEAD currently not supported'
    }

    @Unroll('#testName')
    def 'correct tag is created'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag(tagName)
            .makeChanges()
            .commit()

        and:
        if(tagPrefix) {
            new File(gradleRunner.projectDir, 'semantic-build-versioning.gradle') << "tagPrefix = '$tagPrefix'"
        }

        when:
        gradleRunner.withArguments('-P', 'release', 'tag').build()

        then:
        testRepository.headTag == expectedTagName

        where:
        testName                              | tagName        | tagPrefix || expectedTagName
        'tag is created'                      | '0.0.1'        | null      || '0.0.2'
        'tag is created with prefix'          | 'prefix-0.0.1' | 'prefix-' || 'prefix-0.0.2'
        'tag is created with dashless prefix' | 'v0.0.1'       | 'v'       || 'v0.0.2'
    }

    def 'tags are not pushed'() {
        given:
        testRepository.origin = origin

        and:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1')
            .makeChanges()
            .commit()

        when:
        gradleRunner.withArguments('-P', 'release', 'tag').build()

        then:
        def originTags = origin.repository.tags.keySet()
        !originTags.contains('0.0.1')
        !originTags.contains('0.0.2')
    }

    def 'created tag is pushed'() {
        given:
        testRepository.origin = origin

        and:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1')
            .makeChanges()
            .commit()

        when:
        gradleRunner.withArguments('-P', 'release', 'tagAndPush').build()

        then:
        def originTags = origin.repository.tags.keySet()
        !originTags.contains('0.0.1')
        originTags.contains('0.0.2')
    }

    def 'non version tags does not cause build to fail'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('3.1.2')
            .makeChanges()
            .commitAndTag('foo')
            .makeChanges()
            .commit()

        and:
        new File(gradleRunner.projectDir, 'semantic-build-versioning.gradle') << "tagPattern = ~/.*/"

        when:
        gradleRunner.withArguments('-P', 'release', 'tag').build()

        then:
        notThrown UnexpectedBuildFailure
    }

    def 'tag is skipped if tagAndPush is executed'() {
        given:
        testRepository.origin = origin

        and:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1')
            .makeChanges()
            .commit()

        when:
        def buildResult = gradleRunner.withArguments('-P', 'release', 'tag', tagAndPushName).build()

        then:
        buildResult.output.contains ':tag SKIPPED'
        testRepository.headTag == '0.0.2'
        origin.repository.tags.containsKey('0.0.2')

        where:
        tagAndPushName << ['tagAndPush', 'tAP']
    }
}
