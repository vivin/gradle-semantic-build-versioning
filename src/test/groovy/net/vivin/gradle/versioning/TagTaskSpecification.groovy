package net.vivin.gradle.versioning

import net.vivin.gradle.versioning.spock.extensions.git.Bare
import net.vivin.gradle.versioning.spock.extensions.gradle.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Requires
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
        def buildResult = gradleRunner.withArguments('tag').buildAndFail()

        then:
        buildResult.output.contains 'Cannot create a tag for a snapshot version'

        where:
        annotated << [false, true]
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
        gradleRunner.withArguments('-P', 'release', 'tag', '--push').build()

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

    @Unroll
    def 'test that we get expected message #caseName (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .makeChanges()
            .commit()

        and:
        if(tagMessage) {
            new File(gradleRunner.projectDir, 'build.gradle') << "tag { message $tagMessage }"
        }

        when:
        gradleRunner.withArguments('-P', 'release', 'tag', *additionalArguments).build()

        then:
        testRepository.headTag == '1.0.1'
        testRepository.headTagAnnotated == (expectedTagMessage != null)
        testRepository.headTagMessage == expectedTagMessage

        where:
        caseName                                                                                    | tagMessage                                                                                    | additionalArguments                               | annotated || expectedTagMessage
        'when using default tagMessage'                                                             | null                                                                                          | []                                                | false     || ''
        'when using custom tagMessage'                                                              | '{ "version: awesome" }'                                                                      | []                                                | false     || 'version: awesome'
        'when using custom dynamic tagMessage'                                                      | '{ "version: $version" }'                                                                     | []                                                | false     || 'version: 1.0.1'
        'when using environment variable closure and without variable'                              | 'fromEnvironmentVariable("noMessage")'                                                        | []                                                | false     || null
        'when using system property closure'                                                        | 'fromSystemProperty("message")'                                                               | ['-Dmessage=test sys']                            | false     || 'test sys'
        'when using mandatory system property closure'                                              | 'fromMandatorySystemProperty("message")'                                                      | ['-Dmessage=test sys']                            | false     || 'test sys'
        'when using system property closure and without value'                                      | 'fromSystemProperty("message")'                                                               | ['-Dmessage']                                     | false     || ''
        'when using mandatory system property closure and without value'                            | 'fromMandatorySystemProperty("message")'                                                      | ['-Dmessage']                                     | false     || ''
        'when using system property closure and without property'                                   | 'fromSystemProperty("message")'                                                               | []                                                | false     || null
        'when using project property closure'                                                       | 'fromProjectProperty("message")'                                                              | ['-P', 'message=test proj']                       | false     || 'test proj'
        'when using mandatory project property closure'                                             | 'fromMandatoryProjectProperty("message")'                                                     | ['-P', 'message=test proj']                       | false     || 'test proj'
        'when using project property closure and without value'                                     | 'fromProjectProperty("message")'                                                              | ['-P', 'message']                                 | false     || ''
        'when using mandatory project property closure and without value'                           | 'fromMandatoryProjectProperty("message")'                                                     | ['-P', 'message']                                 | false     || ''
        'when using project property closure and without property'                                  | 'fromProjectProperty("message")'                                                              | []                                                | false     || null
        'when using project and system property closure with both properties'                       | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-P', 'message=test proj', '-Dmessage=test sys'] | false     || 'test proj'
        'when using project and system property closure with both properties in other order'        | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-Dmessage=test sys', '-P', 'message=test proj'] | false     || 'test proj'
        'when using project and system property closure with empty project and set system property' | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-Dmessage=test sys', '-P', 'message']           | false     || ''
        'when using project and system property closure with system property'                       | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-Dmessage=test sys']                            | false     || 'test sys'
        'when using project and system property closure with project property'                      | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-P', 'message=test proj']                       | false     || 'test proj'
        'when using project and system property closure without property'                           | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | []                                                | false     || null
        'when using project and system property closure without property, fallback to annotated'    | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult("") { it() } }' | []                                                | false     || ''
        'without tagMessage'                                                                        | 'null'                                                                                        | []                                                | false     || null
        'with closure returning null'                                                               | '{ null }'                                                                                    | []                                                | false     || null
        'with closure returning empty string'                                                       | '{ "" }'                                                                                      | []                                                | false     || ''
        'with closure returning blank string'                                                       | '{ "    " }'                                                                                  | []                                                | false     || ''
        'when using default tagMessage'                                                             | null                                                                                          | []                                                | true      || ''
        'when using custom tagMessage'                                                              | '{ "version: awesome" }'                                                                      | []                                                | true      || 'version: awesome'
        'when using custom dynamic tagMessage'                                                      | '{ "version: $version" }'                                                                     | []                                                | true      || 'version: 1.0.1'
        'when using environment variable closure and without variable'                              | 'fromEnvironmentVariable("noMessage")'                                                        | []                                                | true      || null
        'when using system property closure'                                                        | 'fromSystemProperty("message")'                                                               | ['-Dmessage=test sys']                            | true      || 'test sys'
        'when using mandatory system property closure'                                              | 'fromMandatorySystemProperty("message")'                                                      | ['-Dmessage=test sys']                            | true      || 'test sys'
        'when using system property closure and without value'                                      | 'fromSystemProperty("message")'                                                               | ['-Dmessage']                                     | true      || ''
        'when using mandatory system property closure and without value'                            | 'fromMandatorySystemProperty("message")'                                                      | ['-Dmessage']                                     | true      || ''
        'when using system property closure and without property'                                   | 'fromSystemProperty("message")'                                                               | []                                                | true      || null
        'when using project property closure'                                                       | 'fromProjectProperty("message")'                                                              | ['-P', 'message=test proj']                       | true      || 'test proj'
        'when using mandatory project property closure'                                             | 'fromMandatoryProjectProperty("message")'                                                     | ['-P', 'message=test proj']                       | true      || 'test proj'
        'when using project property closure and without value'                                     | 'fromProjectProperty("message")'                                                              | ['-P', 'message']                                 | true      || ''
        'when using mandatory project property closure and without value'                           | 'fromMandatoryProjectProperty("message")'                                                     | ['-P', 'message']                                 | true      || ''
        'when using project property closure and without property'                                  | 'fromProjectProperty("message")'                                                              | []                                                | true      || null
        'when using project and system property closure with both properties'                       | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-P', 'message=test proj', '-Dmessage=test sys'] | true      || 'test proj'
        'when using project and system property closure with both properties in other order'        | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-Dmessage=test sys', '-P', 'message=test proj'] | true      || 'test proj'
        'when using project and system property closure with empty project and set system property' | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-Dmessage=test sys', '-P', 'message']           | true      || ''
        'when using project and system property closure with system property'                       | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-Dmessage=test sys']                            | true      || 'test sys'
        'when using project and system property closure with project property'                      | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | ['-P', 'message=test proj']                       | true      || 'test proj'
        'when using project and system property closure without property'                           | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult { it() } }'     | []                                                | true      || null
        'when using project and system property closure without property, fallback to annotated'    | '{ [fromProjectProperty("message"), fromSystemProperty("message")].findResult("") { it() } }' | []                                                | true      || ''
        'without tagMessage'                                                                        | 'null'                                                                                        | []                                                | true      || null
        'with closure returning null'                                                               | '{ null }'                                                                                    | []                                                | true      || null
        'with closure returning empty string'                                                       | '{ "" }'                                                                                      | []                                                | true      || ''
        'with closure returning blank string'                                                       | '{ "    " }'                                                                                  | []                                                | true      || ''
    }

    @Unroll
    def 'test that we get expected Exception #caseName (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .makeChanges()
            .commit()

        and:
        if(tagMessage) {
            new File(gradleRunner.projectDir, 'build.gradle') << "tag { message $tagMessage }"
        }

        when:
        def buildResult = gradleRunner.withArguments('-P', 'release', 'tag', *additionalArguments).buildAndFail()

        then:
        buildResult.output.contains expectedExceptionMessage

        where:
        caseName                                                                    | tagMessage                                                                                         | additionalArguments | annotated || expectedExceptionMessage
        'when using mandatory environment variable closure and without variable'    | 'fromMandatoryEnvironmentVariable("noMessage")'                                                    | []                  | false     || 'Mandatory environment variable \'noMessage\' was not found'
        'when using mandatory system property closure and without property'         | 'fromMandatorySystemProperty("message")'                                                           | []                  | false     || 'Mandatory system property \'message\' was not found'
        'when using mandatory project property closure and without property'        | 'fromMandatoryProjectProperty("message")'                                                          | []                  | false     || 'Mandatory project property \'message\' was not found'
        'when using project and mandatory system property closure without property' | '{ [fromProjectProperty("message"), fromMandatorySystemProperty("message")].findResult { it() } }' | []                  | false     || 'Mandatory system property \'message\' was not found'
        'when using mandatory environment variable closure and without variable'    | 'fromMandatoryEnvironmentVariable("noMessage")'                                                    | []                  | true      || 'Mandatory environment variable \'noMessage\' was not found'
        'when using mandatory system property closure and without property'         | 'fromMandatorySystemProperty("message")'                                                           | []                  | true      || 'Mandatory system property \'message\' was not found'
        'when using mandatory project property closure and without property'        | 'fromMandatoryProjectProperty("message")'                                                          | []                  | true      || 'Mandatory project property \'message\' was not found'
        'when using project and mandatory system property closure without property' | '{ [fromProjectProperty("message"), fromMandatorySystemProperty("message")].findResult { it() } }' | []                  | true      || 'Mandatory system property \'message\' was not found'
    }

    @Requires({ System.env.message })
    @Unroll
    def 'test that we get expected message when using environment variable closure (mandatory: #mandatory, annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .makeChanges()
            .commit()

        and:
        new File(gradleRunner.projectDir, 'build.gradle') << "tag { message from${mandatory ? 'Mandatory' : ''}EnvironmentVariable('message') }"

        when:
        gradleRunner.withArguments('-P', 'release', 'tag').build()

        then:
        testRepository.headTag == '1.0.1'
        testRepository.headTagAnnotated
        testRepository.headTagMessage == 'test env'

        where:
        [mandatory, annotated] << [[false, true], [false, true]].combinations()
    }

    @Requires({ System.env.containsKey('emptyMessage') })
    @Unroll
    def 'test that we get expected message when using environment variable closure and without value (mandatory: #mandatory, annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('1.0.0', annotated)
            .makeChanges()
            .commit()

        and:
        new File(gradleRunner.projectDir, 'build.gradle') << "tag { message from${mandatory ? 'Mandatory' : ''}EnvironmentVariable('emptyMessage') }"

        when:
        gradleRunner.withArguments('-P', 'release', 'tag').build()

        then:
        testRepository.headTag == '1.0.1'
        testRepository.headTagAnnotated
        testRepository.headTagMessage == ''

        where:
        [mandatory, annotated] << [[false, true], [false, true]].combinations()
    }
}
