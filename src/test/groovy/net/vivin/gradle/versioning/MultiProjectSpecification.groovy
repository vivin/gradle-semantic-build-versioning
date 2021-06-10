package net.vivin.gradle.versioning

import net.vivin.gradle.versioning.spock.extensions.gradle.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

import java.nio.file.Files

@Title('Multi Project Specification')
class MultiProjectSpecification extends Specification {
    private TestRepository testRepository
    private TestRepository testRepository2
    @ProjectDirProvider({ testRepository })
    private GradleRunner gradleRunner

    def 'plugin does not treat sub project if no marker file is found for it'() {
        given:
        new File(gradleRunner.projectDir, 'settings.gradle') << '''
            rootProject.name = 'root'
            include 'sub'
        '''.stripIndent()
        new File(gradleRunner.projectDir, 'build.gradle') << 'allprojects { println "$it.name version: $version" }'

        when:
        def buildResult = gradleRunner.build()

        then:
        buildResult.output.contains 'root version: 0.1.0-SNAPSHOT'
        buildResult.output.contains 'sub version: unspecified'
    }

    def 'plugin does not treat root project if no marker file is found for it'() {
        given:
        new File(gradleRunner.projectDir, 'settings.gradle') << '''
            rootProject.name = 'root'
            include 'sub'
        '''.stripIndent()
        new File(gradleRunner.projectDir, 'build.gradle') << 'allprojects { println "$it.name version: $version" }'
        def subDir = new File(gradleRunner.projectDir, 'sub')
        subDir.mkdir()
        Files.move(
            new File(gradleRunner.projectDir, 'semantic-build-versioning.gradle').toPath(),
            new File(subDir, 'semantic-build-versioning.gradle').toPath()
        )

        when:
        def buildResult = gradleRunner.build()

        then:
        buildResult.output.contains 'root version: unspecified'
        buildResult.output.contains 'sub version: 0.1.0-SNAPSHOT'
    }

    @Unroll
    def 'sub projects with independent git repositories get independent versions (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commit()

        and:
        new File(gradleRunner.projectDir, 'settings.gradle') << """
            rootProject.name = 'root'
            includeFlat 'sub'
            project(':sub').projectDir = new File(new URI('${testRepository2.repository.workTree.toURI()}'))
        """.stripIndent()
        new File(gradleRunner.projectDir, 'build.gradle') << 'allprojects { println "$it.name version: $version" }'
        new File(testRepository2.repository.workTree, 'semantic-build-versioning.gradle').createNewFile()

        when:
        def buildResult = gradleRunner.withArguments('-P', 'release', 'printVersion').build()

        then:
        buildResult.output.contains 'root version: 0.0.2'
        buildResult.output.contains 'sub version: 0.1.0'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'printVersion prints correct version (annotated: #annotated)'() {
        given:
        testRepository
            .makeChanges()
            .commitAndTag('0.0.1', annotated)
            .makeChanges()
            .commit()

        and:
        new File(gradleRunner.projectDir, 'settings.gradle') << """
            rootProject.name = 'root'
            includeFlat 'sub'
            project(':sub').projectDir = new File(new URI('${testRepository2.repository.workTree.toURI()}'))
        """.stripIndent()
        new File(testRepository2.repository.workTree, 'semantic-build-versioning.gradle').createNewFile()

        when:
        def buildResult = gradleRunner.withArguments(':printVersion').build()

        then:
        buildResult.output.contains ':printVersion'
        buildResult.output.contains '0.0.2-SNAPSHOT'
        !buildResult.output.contains(':sub:printVersion')
        !buildResult.output.contains('0.1.0-SNAPSHOT')

        when:
        buildResult = gradleRunner.withArguments(':sub:printVersion').build()

        then:
        buildResult.output.contains ':sub:printVersion'
        buildResult.output.contains '0.1.0-SNAPSHOT'
        !buildResult.output.contains('0.0.2-SNAPSHOT')

        when:
        buildResult = gradleRunner.withArguments('printVersion').build()

        then:
        buildResult.output.contains ':sub:printVersion'
        buildResult.output.contains '0.0.2-SNAPSHOT'
        buildResult.output.contains '0.1.0-SNAPSHOT'

        where:
        annotated << [false, true]
    }

    @Unroll
    def 'printVersion prints correct latest version (annotated: #annotated)'() {
        given:
        testRepository
                .makeChanges()
                .commitAndTag('0.0.1', annotated)
                .makeChanges()
                .commit()

        testRepository2
                .makeChanges()
                .commitAndTag('0.1.0', annotated)
                .makeChanges()
                .commit()

        and:
        new File(gradleRunner.projectDir, 'settings.gradle') << """
            rootProject.name = 'root'
            includeFlat 'sub'
            project(':sub').projectDir = new File(new URI('${testRepository2.repository.workTree.toURI()}'))
        """.stripIndent()
        new File(testRepository2.repository.workTree, 'semantic-build-versioning.gradle').createNewFile()

        when:
        def buildResult = gradleRunner.withArguments(':printVersion','-P', 'latest').build()

        then:
        buildResult.output.contains ':printVersion'
        buildResult.output.contains '0.0.1'
        !buildResult.output.contains(':sub:printVersion')
        !buildResult.output.contains('0.1.0')

        when:
        buildResult = gradleRunner.withArguments(':sub:printVersion', '-P', 'latest').build()

        then:
        buildResult.output.contains ':sub:printVersion'
        buildResult.output.contains '0.1.0'
        !buildResult.output.contains('0.0.1')

        when:
        buildResult = gradleRunner.withArguments('printVersion', '-P', 'latest').build()

        then:
        buildResult.output.contains ':sub:printVersion'
        buildResult.output.contains '0.0.1'
        buildResult.output.contains '0.1.0'

        where:
        annotated << [false, true]
    }
}
