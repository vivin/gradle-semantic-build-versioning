package net.vivin.gradle.versioning

import org.gradle.tooling.BuildException
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

@Title('Semantic Build Version Configuration Specification')
class SemanticBuildVersionConfigurationSpecification extends Specification {
    @Unroll
    def 'invalid starting version \'#startingVersion\' fails build'() {
        given:
        def config = new SemanticBuildVersionConfiguration(startingVersion: startingVersion)

        when:
        config.validate()

        then:
        BuildException e = thrown()
        e.message == 'Starting version must be a valid semantic version without identifiers'

        where:
        startingVersion << ['bad', '0.0.1-pre.0']
    }

    @Unroll
    def 'invalid versions matching configuration \'#versionsMatching\' fails build'() {
        given:
        def matching = new VersionsMatching(versionsMatching)

        when:
        matching.validate()

        then:
        BuildException e = thrown()
        e.message == expectedExceptionMessage

        where:
        versionsMatching     || expectedExceptionMessage
        [patch: 2]           || 'When specifying a matching versioning-component, all preceding components (if any) must also be specified'
        [minor: 2]           || 'When specifying a matching versioning-component, all preceding components (if any) must also be specified'
        [minor: 2, patch: 2] || 'When specifying a matching versioning-component, all preceding components (if any) must also be specified'
        [major: 2, patch: 2] || 'When specifying a matching versioning-component, all preceding components (if any) must also be specified'
        [:]                  || 'Matching versions must be specified'
    }

    @Unroll
    def 'versions matching \'#versionsMatching\' returns proper pattern \'#expectedPattern\''() {
        given:
        def matching = new VersionsMatching(versionsMatching)

        when:
        matching.validate()

        then:
        notThrown BuildException

        and:
        matching.toPattern() as String == expectedPattern

        where:
        versionsMatching               || expectedPattern
        [major: 1]                     || /1\./
        [major: 1, minor: 2]           || /1\.2\./
        [major: 1, minor: 2, patch: 3] || /1\.2\.3/
    }

    @Unroll
    def 'invalid pre-release configuration \'#preReleaseConfig\' fails build'() {
        given:
        def preRelease = new PreRelease(preReleaseConfig)

        when:
        preRelease.validate()

        then:
        BuildException e = thrown()
        e.message == expectedExceptionMessage

        where:
        preReleaseConfig                             || expectedExceptionMessage
        [:]                                          || 'A startingVersion must be specified in preRelease'
        [pattern: null, startingVersion: 'alpha.0']  || 'A valid pattern must be specified in preRelease'
        [startingVersion: 'alpha.0']                 || 'Bumping scheme for preRelease versions must be specified'
        [startingVersion: '!bad!', bump: { it }]     || 'Identifiers must comprise only ASCII alphanumerics and hyphen'
        [startingVersion: 'alpha.012', bump: { it }] || 'Numeric identifiers must not include leading zeroes'
    }

    def 'pre-release with valid attributes does not fail'() {
        given:
        def preRelease = new PreRelease(
            startingVersion: 'alpha.0',
            pattern: ~/alpha.*/,
            bump: { it }
        )

        when:
        preRelease.validate()

        then:
        notThrown BuildException

        and:
        preRelease.pattern.toString() == /\d++\.\d++\.\d++-alpha.*/
    }

    @Unroll
    def 'invalid autobump configuration \'#autobumpConfig\' fails build'() {
        given:
        def autobump = new Autobump(autobumpConfig)

        when:
        autobump.validate()

        then:
        BuildException e = thrown()
        e.message == expectedExceptionMessage

        where:
        autobumpConfig                  || expectedExceptionMessage
        [majorPattern: null]            || 'Valid majorPattern must be specified in autobump'
        [minorPattern: null]            || 'Valid minorPattern must be specified in autobump'
        [patchPattern: null]            || 'Valid patchPattern must be specified in autobump'
        [preReleasePattern: null]       || 'Valid preReleasePattern must be specified in autobump'
        [newPreReleasePattern: null]    || 'Valid newPreReleasePattern must be specified in autobump'
        [promoteToReleasePattern: null] || 'Valid promoteToReleasePattern must be specified in autobump'
    }

    def 'autobump with valid patterns does not fail build'() {
        given:
        def autobump = new Autobump(
            majorPattern: ~/.*/,
            minorPattern: ~/.*/,
            patchPattern: ~/.*/,
            preReleasePattern: ~/.*/,
            newPreReleasePattern: ~/.*/,
            promoteToReleasePattern: ~/.*/
        )

        when:
        autobump.validate()

        then:
        notThrown BuildException

        and:
        autobump.majorPattern
        autobump.minorPattern
        autobump.patchPattern
        autobump.preReleasePattern
        autobump.newPreReleasePattern
        autobump.promoteToReleasePattern
    }
}
