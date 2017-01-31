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

    @Unroll
    def 'tagging with uncommitted changes causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()

        when:
        def buildResult = gradleRunner.withArguments('tag').buildAndFail()

        then:
        buildResult.output.contains 'Cannot create a tag when there are uncommitted changes'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'tagging snapshot version causes build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commit()

        when:
        def buildResult = gradleRunner.withArguments(tagTask).buildAndFail()

        then:
        buildResult.output.contains 'Cannot create a tag for a snapshot version'

        where:
        [tagTask, annotated] << [['tag'], [false, true]].combinations()
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
            .commitAndTag(tagName, annotated)
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
        testNamePart                          | tagName        | tagPrefix | annotated || expectedTagName
        'tag is created'                      | '0.0.1'        | null      | false     || '0.0.2'
        'tag is created with prefix'          | 'prefix-0.0.1' | 'prefix-' | false     || 'prefix-0.0.2'
        'tag is created with dashless prefix' | 'v0.0.1'       | 'v'       | false     || 'v0.0.2'
        'tag is created'                      | '0.0.1'        | null      | true      || '0.0.2'
        'tag is created with prefix'          | 'prefix-0.0.1' | 'prefix-' | true      || 'prefix-0.0.2'
        'tag is created with dashless prefix' | 'v0.0.1'       | 'v'       | true      || 'v0.0.2'

        and:
        testName = "$testNamePart (annotated: $annotated)"
    }

    @Unroll
    def 'tags are not pushed (annotated: #annotated)'() {
        given:
        testRepository.origin = origin

        and:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commit()

        when:
        gradleRunner.withArguments('-P', 'release', 'tag').build()

        then:
        def originTags = origin.repository.tags.keySet()
        !originTags.contains('0.0.1')
        !originTags.contains('0.0.2')

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'created tag is pushed (annotated: #annotated)'() {
        given:
        testRepository.origin = origin

        and:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commit()

        when:
        gradleRunner.withArguments('-P', 'release', 'tag', 'pushTag').build()

        then:
        def originTags = origin.repository.tags.keySet()
        !originTags.contains('0.0.1')
        originTags.contains('0.0.2')

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'non version tags does not cause build to fail (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('3.1.2', annotated)
            .makeChanges()
            .commitAndTag('foo', annotated)
            .makeChanges()
            .commit()

        and:
        new File(gradleRunner.projectDir, 'semantic-build-versioning.gradle') << "tagPattern = ~/.*/"

        when:
        gradleRunner.withArguments('-P', 'release', 'tag').build()

        then:
        notThrown UnexpectedBuildFailure

        where:
        annotated << [false, true]
    }

    @Unroll("#testName")
    def 'created annotated tag has expected message'() {
        given:
        System.getenv()
        testRepository
            .makeChanges()
            .commitAndTag("1.0.0", true)
            .makeChanges()
            .commit()

        and:
        if(tagMessage) {
            new File(gradleRunner.projectDir, 'build.gradle') << "tag { tagMessage = $tagMessage }"
        }

        when:
        def arguments = ['-P', 'release', 'tag']

        arguments.addAll additionalArguments
        gradleRunner.withArguments(arguments).build()

        then:
        testRepository.headTagMessage == expectedTagMessage

        where:

        caseName                                 | tagMessage                         | additionalArguments     || expectedTagMessage
        "when using default tagMessage"          | null                               | []                      || "v1.0.1"
        "when using custom tagMessage"           | "{ \"version: awesome\" }"         | []                      || "version: awesome"
        "when using system property closure"     | "fromSystemProperty(\"message\")"  | ["-Dmessage=test sys"]  || "test sys"
        "when using project property closure"    | "fromProjectProperty(\"message\")" | ["-Pmessage=test proj"] || "test proj"
        "without tagMessage"                     | "null"                             | []                      || null
        "with closure returning null"            | "{ null }"                         | []                      || null
        "with closure returning empty string"    | "{ \"\" }"                         | []                      || null
        "with closure returning blank string"    | "{ \"    \" }"                     | []                      || null

        and:
        testName = "test that we get expected message $caseName"
    }
}
