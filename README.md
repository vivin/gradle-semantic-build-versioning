# Semantic build-versioning for Gradle

[![Circle CI](https://circleci.com/gh/vivin/gradle-semantic-build-versioning.svg?style=svg&circle-token=10ad77d88ed4766073cca457059f28a62a8dd41b)](https://circleci.com/gh/vivin/gradle-semantic-build-versioning) [![JaCoCo](http://circle-jacoco-badge.herokuapp.com/line?author=vivin&project=gradle-semantic-build-versioning&circle-token=3e948328990f7e4cc9e519915518ab325d299551)](http://circle-jacoco-badge.herokuapp.com/report?author=vivin&project=gradle-semantic-build-versioning&circle-token=3e948328990f7e4cc9e519915518ab325d299551) [![JaCoCo](http://circle-jacoco-badge.herokuapp.com/branch?author=vivin&project=gradle-semantic-build-versioning&circle-token=3e948328990f7e4cc9e519915518ab325d299551)](http://circle-jacoco-badge.herokuapp.com/report?author=vivin&project=gradle-semantic-build-versioning&circle-token=3e948328990f7e4cc9e519915518ab325d299551) [![JaCoCo](http://circle-jacoco-badge.herokuapp.com/complexity?author=vivin&project=gradle-semantic-build-versioning&circle-token=3e948328990f7e4cc9e519915518ab325d299551)](http://circle-jacoco-badge.herokuapp.com/report?author=vivin&project=gradle-semantic-build-versioning&circle-token=3e948328990f7e4cc9e519915518ab325d299551)

  * [Introduction](#introduction)
  * [Usage](#usage)
  * [Project properties](#project-properties)
    * [`bumpComponent`](#bumpcomponent)
    * [`forceBump`](#forcebump)
    * [`newPreRelease`](#newprerelease)
    * [`promoteToRelease`](#promotetorelease)
    * [`release`](#release)
  * [Tasks](#tasks)
    * [`tag`](#tag)
    * [`tagAndPush`](#tagandpush)
    * [`printVersion`](#printversion)
  * [Options and use-cases](#options-and-use-cases)
    * [General options](#general-options)
      * [`startingVersion`](#startingversion)
      * [`tagPrefix`](#tagprefix)
      * [`snapshotSuffix`](#snapshotsuffix)
    * [Filtering tags](#filtering-tags)
      * [`tagPattern`](#tagpattern)
      * [`matching`](#matching)
    * [Pre-releases](#pre-releases)
      * [`preRelease`](#prerelease)
    * [Automatic bumping based on commit messages](#automatic-bumping-based-on-commit-messages)
      * [`autobump`](#autobump)
    * [Checking out a tag](#checking-out-a-tag)


# Introduction

**NOTE: Version 3.0.0 is still being developed. It is has not been released.**

**NOTE: Plugin configuration and usage has changed significantly since version 2.x. If you are still using that version, the documentation can be found [here](https://github.com/vivin/gradle-semantic-build-versioning/tree/2.x).**

This is a Gradle plugin that provides support for [semantic versioning](http://semver.org) of builds. It is quite easy to use and extremely configurable. The plugin allows you to bump the major, minor, patch or pre-release version based on the latest version, which is identified from a git tag. It also allows you to bump pre-release versions based on a scheme that you define. The version can be bumped by using version-component-specific project properties or can be bumped automatically based on the contents of a commit message. If no manual bumping is done via commit message or project property, the plugin will increment the version-component with the lowest precedence; this is usually the patch version, but can be the pre-release version if the latest version is a pre-release one.

As this plugin is applied to `settings.gradle`, the version calculation is done right at the start of the build, before any projects are configured. This means, the version is already present in the affected projects and will not change during build, no matter what the build does. No tagging during the build or changing of project properties will influence the version calculated. The only way to influence the version number is via project properties that are available in `settings.gradle` as described below.

The version that is set to the affected projects actually is a `String` object and can be used like that, even though officially the `version` field is just an `Object` and you should use its `toString()` method when using it. The version object has an additional boolean `snapshot` property that can be used for different project configuration for release or snapshot versions without the need for an `endsWith()` check. Besides that, the version object also has the additional properties `major`, `minor`, `patch` and `preRelease` that bear the single version components for further usage in the build process.

# Usage

Using the plugin is quite simple:

**In settings.gradle**
```gradle
buildscript {
    repositories {
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }
    dependencies {
        classpath 'gradle.plugin.net.vivin:gradle-semantic-build-versioning:3.0.0'
    }
}

apply plugin: 'net.vivin.gradle-semantic-build-versioning'
```

Additionally you need at least an empty `semantic-build-versioning.gradle` file in each project directory of each project in the build that should be treated by this plugin. In this file you can also do further configuration of how the plugin should behave (see [Options and use-cases](#options-and-use-cases)). If you do not want to have different version numbers for different projects in the build (e. g. by having the projects in different repositories or with different tag prefixes), you can simply add the file only for the root project and in the root projects build file do something like: 
```gradle
subprojects {
    version = rootProject.version
}
```

This is usually enough to start using the plugin. Assuming that you already have tags that are (or contain) semantic versions, the plugin will search for all nearest ancestor tags, select the latest<sup>1</sup> of them as base version and increment the component with the least precedence. The nearest ancestor tags are the tags that have a path between them and the HEAD commit, where no other tag occurs inbetween. This is the default behavior of the plugin.

<sup>1</sup> Latest based on ordering-rules defined in the semantic-version specification, **not latest by date**

# Project properties

The plugin recognizes project properties that can be used to bump specific components of the version or modify the version in other ways like adding a pre-release identifier or promoting a pre-release version to a release version. Those properties do not need any value and actually the value is ignored, except for `bumpComponent`. It is sufficient to specify e. g. `-P newPreRelease`. Besides on the commandline as `-P` parameters, the properties can also be specified in any other way that is valid for giving project properties to `settings.gradle`. Writing them into a `gradle.properties` file usually makes no sense, but setting them through system properties or environment properties might be useful, depending on the situation. There are some restrictions when it comes to their usage:

1. It is not possible to use `promoteToRelease` when explicitly bumping a version-component.
1. With the exception of `bumpComponent=pre-release`, all other version-component bumping-values can be used in conjunction with `newPreRelease`; this has the effect of bumping a version-component and adding a pre-release identifier at the same time to create a new pre-release version.
1. It is not possible to modify the version in any manner if `HEAD` is already pointing to a tag that identifies a particular version and there are no uncommitted changes. This is because it would then be possible to push out an identical artifact with a different version-number and violate semantic-versioning rules. For example, assuming that the base version is `1.0.2`, it would be possible to check out tag `1.0.0`, bump the major version, and release it as `2.0.0`. For more information about tagging and checking out a tag, see [`tag`](#tag), [`tagAndPush`](#tagandpush), and [Checking out a tag](#checking-out-a-tag).

## `bumpComponent`

This property bumps the version. It can be set to the values `major`, `minor`, `patch` and `pre-release`.

Assuming that the base version is `x.y.z` and
- the value is `major`, the new version will be `(x + 1).0.0`; if the base version is a pre-release version, the pre-release version-component is discarded and the new version will still be `(x + 1).0.0`
- the value is `minor`, the new version will be `x.(y + 1).0`; if the base version is a pre-release version, the pre-release version-component is discarded and the new version will still be `x.(y + 1).0`
- the value is `patch`, the new version will be `x.y.(z + 1)`; if the base version is a pre-release version, the pre-release version-component is discarded and the new version will still be `x.y.(z + 1)`

If the value is `pre-release`, the pre-release version is bumped. Pre-release versions are denoted by appending a hyphen, and a series of dot-separated identifiers that can only consist of alphanumeric characters and hyphens; numeric identifiers cannot contain leading-zeroes. Since pre-release versions are arbitrary, using this property requires some additional configuration (see [Pre-releases](#pre-releases)). Assuming that the base version is `x.y.z-<identifier>`, the new version will be `x.y.z-<identifier++>` where the value of `<identifier++>` is determined based on a scheme defined by the pre-release configuration (see [`bump`](#prerelease.bump)).
 
**Notes:**
  - `pre-release` can only be used if the base version is already a pre-release version. If you want to create a new pre-release, use the `newPreRelease` property.
  - It is not possible to use `pre-release` with `promoteToRelease` or `newPreRelease`.
  - `pre-release` can fail if you have filtered tags in such a way (see [Filtering tags](#filtering-tags) and [`pattern`](#prerelease.pattern)) that the base version does not have a pre-release identifier.

## `forceBump`

If you use autobumping and manual bumping together, the following rules apply:
  - The autobump component and manual bump component are determined separately by taking the respective component with the highest precedence
  - If you manually bump a higher precedence component than via autobumping, the manual bump wins
  - If you manually bump a lesser precedence component than via autobumping and the property `forceBump` is set, the manual bump wins (you say "I know what I'm doing, my commit messages were wrong")
  - If you manually bump a lesser precedence component than via autobumping and the property `forceBump` is **not** set, the build fails

## `newPreRelease`

This property creates a new pre-release version by bumping the requested version-component and then adding the starting pre-release version from the pre-release configuration (see [`preRelease`](#prerelease)). It has the following behavior:
 - When used by itself it will bump the patch version and then append the starting pre-release version as specified in the pre-release configuration. Assuming that the base version is `x.y.z`, the new version will be `x.y.(z + 1)-<startingVersion>` (see [`startingVersion`](#prerelease.startingversion)).
 - When used with `bumpComponent=patch`, the behavior is the same as using `newPreRelease` by itself.
 - When used with `bumpComponent=minor`, it will bump the minor version and then append the starting pre-release version as specified in the pre-release configuration. Assuming that the base version is `x.y.z`, the new version will be `x.(y + 1).0-<startingVersion>` (see [`startingVersion`](#prerelease.startingversion)).
 - When used with `bumpComponent=major`, it will bump the major version and then append the starting pre-release version as specified in the pre-release configuration. Assuming that the base version is `x.y.z`, the new version will be `(x + 1).0.0-<startingVersion>` (see [`startingVersion`](#prerelease.startingversion)).
 
The behavior of this property is slightly different in the situation where the base version cannot be identified (usually when there are no ancestor tags): when using `newPreRelease` by itself or in conjunction with `bumpComponent=patch`, the starting pre-release identifier (see [`prerelease.startingVersion`](#prerelease.startingversion)) is appended to the starting version (see [`startingVersion`](#startingversion)). This is because the starting-version specifies the *next* point-version to use, which means that bumping the patch version will cause a point-version to be skipped. However, behavior remains the same when using `bumpComponent=minor` or `bumpComponent=major` with `newPreRelease` even when the base version cannot be identified.
 
**Note:** It is not possible to use `bumpComponent=pre-release` along with `newPreRelease`. 
 
## `promoteToRelease`

This property promotes a pre-release version to a release version. This is done by discarding the pre-release version-component. For example, assuming that the base version is `x.y.z-some.identifiers.here`, the new version will be `x.y.z`. **This property can only be used if the base version is a pre-release version**.

## `release`

This property specifies that the build is a release build, which means that a snapshot suffix is not attached to the version (see [`snapshotSuffix`](#snapshotsuffix)). **You cannot release a build if there are uncommitted changes**.

# Tasks

## `tag`

This task will create a tag corresponding to the new version (with an optional prefix; see [`tagPrefix`](#tagprefix)). It is recommended to use this task along with the `release` task when creating a release. **You cannot tag a snapshot release; use pre-release identifiers instead**. Also note that you can use `tag` or `tagAndPush`, but not both at the same time. If you try to execute both, this task will be skipped.

## `tagAndPush`

This task will create a tag corresponding to the new version (with an optional prefix; see [`tagPrefix`](#tagprefix)) *and* push the created tag. It is recommended to use this task along with the `release` task when creating a release. **You cannot tag a snapshot release; use pre-release identifiers instead**. Also note that you can use `tagAndPush` or `tag`, but not both at the same time. If you try to execute both, the `tag` task will be skipped.

## `printVersion`

Prints out the new version.

# Options and use-cases

The plugin has a few configuration options that you can use to fine-tune its behavior, or to provide additional options for certain tasks or properties. All these options are configured inside the `semantic-build-versioning.gradle` file that has to be present for each project in the build on which this plugin should operate.

## General options

These options control what your versions and tags look like. Using these options, you can set an optional starting-version (used when no tags are found), an optional tag-prefix, and the snapshot-suffix to use for snapshot versions.

### `startingVersion`

This sets the version to use when no ancestor tags could be found that were not excluded by filtering. By default it is set to `0.1.0`. This must be a valid semantic-version string **without** identifiers.

**Example:** First version should be `1.0.0`
```gradle
startingVersion = '1.0.0'
```

### `tagPrefix`

This option defines an optional prefix to use when tagging a release. By default it is blank, which means that the tag corresponds to the version number.

**Example:** Version tags should look like `v0.1.0`
```gradle
tagPrefix = 'v'
```

### `snapshotSuffix`

This is the suffix to use for snapshot versions. By default it is `SNAPSHOT`. This suffix is always attached to the version - separated by a dash - unless the [`release`](#release) property is set or a tag is checked out and no changes were made.

**Example:** Snapshot versions should look like `0.1.0-Candidate`
```gradle
snapshotSuffix = 'Candidate'
```

## Filtering tags

These options let you restrict the set of tags considered when determining the base version. 

**Note:** Be careful when filtering tags because it can affect plugin-behavior. The plugin works by determining the base version from tags, so behavior can vary depending on whether certain tags have been filtered out or not:
 - If your filtering options are set such that none of the existing ancestor tags match, the plugin will use the [`startingVersion`](#startingversion).
 - If your filtering options are set such that the base version is not a pre-release version and you are attempting to use [`bumpComponent=pre-release`](#bumpcomponent), the build will fail.

### `tagPattern`

This pattern tells the plugin to only consider those tags matching `tagPattern` when trying to determine the base version from the tags in your repository. The value for this option has to be a regular expression. Its default value is `~/\d++\.\d++\.\d++/`, which means that all tags that contain a semantic version part are considered and all others are ignored. This property can e. g. be used to tag different projects in the build individually in the same repository.

**Example:** Only tags that start with `foo` should be considered
```gradle
tagPattern = ~/^foo/
```

### `matching`

This option is similar in function to [`tagPattern`](#tagpattern), except that it allows you to restrict the set of tags considered, based on the explicitly-specified major, minor, or patch versions. When specifying a version component to match, preceding components (if any) must also be specified. While the effect of `matching` can also be accomplished by `tagPattern`, `matching` provides a more convenient way to restrict the set of considered tags based on versions alone.

**Example:** Only tags with major-version `2` should be considered:
```gradle
matching {
    major = 2
}
```

**Example:** Only tags with major and minor-version `1.2` should be considered:
```gradle
matching {
    major = 1
    minor = 2
}
```

**Example:** Only tags with major, minor, and patch-version `1.2.0` should be considered:
```gradle
matching {
    major = 1
    minor = 2
    patch = 0
}
```

**Note:** Filtering based on `matching` is performed **after** tags have been filtered based on `tagPattern`.

## Pre-releases

This is how you can define your pre-release versioning strategy. This is a special case because other than defining a basic syntax and ordering rules, the semantic-versioning specification has no other rules about pre-release identifiers. This means that some extra configuration is required if you want to generate pre-release versions.

### `preRelease`

This option allows you to specify how pre-release versions should be generated and bumped. It has the following sub-options:

 - <a id="prerelease.startingversion" />`startingVersion`: This is required and describes the starting pre-release version of a new pre-release. Its value will be used if [`newPreRelease`](#newprerelease) is invoked (either explicitly or via [Automatic bumping based on commit messages](#automatic-bumping-based-on-commit-messages)).

   **Example:** Starting version for a pre-release version should be `alpha.0`
   ```gradle
   preRelease {
       startingVersion = 'alpha.0'
   }
   ```
 - <a id="prerelease.pattern" />`pattern`: This is similar in function to [`tagPattern`](#tagpattern), except that it allows you to restrict the set of tags considered to those tags with pre-release versions matching `pattern`. The value for this has to be a regular expression as `String`. Its default value is `/.*+$/`. One thing to remember is that starting anchors (`^`) cannot be used, because the actual regular-expression that is used is `~/\d++\.\d++\.\d++-$pattern/`. Hence, if you are trying to filter based on pre-release versions starting with some string, it is simply enough to provide that string in the regular expression without prefixing it with `^`.

   **Example:** Only tags whose pre-release version starts with `alpha` should be considered
   ```gradle
   preRelease {
       pattern = /alpha/
   }
   ```

   **Note:** Filtering based on `pattern` is performed **after** tags have been filtered based on [`tagPattern`](#tagpattern) and [`matching`](#matching).

 - <a id="prerelease.bump" />`bump`: This allows you to specify how pre-release versions should be incremented or bumped. It has to be a closure that accepts a single argument which will be the base version, and it is expected to return an object which `String` representation is used as incremented version.

   **Example:** Defining how the pre-release version should be bumped
   ```gradle
   preRelease {
       // The bumping scheme is alpha.0 -> alpha.1 -> ... -> alpha.n
       bump = {
           "alpha.${((it - ~/^alpha\./) as int) + 1}"
       }
   }
```

## Automatic bumping based on commit messages

Sometimes you might want to automatically bump your version as part of your continuous-integration process. Without this option, you would have to explicitly configure your CI process to use the corresponding bumpComponent property value, if you want to bump the major or minor versions. This is because the default behavior is to bump the component with least precedence. Instead, you can configure the plugin to automatically bump the desired version based on the contents of your commit messages since the nearest tags, which essentially means all unreleased ancestor commits. If the commit messages contain multiple bumping matches, the one with the highest precedence wins. This way you can note in each commit message whether the change is major or minor directly and this plugin uses the information to build the next version number to be used.

If any of the patterns is set to `null`, the commit message is not even checked against it, so if you do not plan to use some of the patterns, you can disable them to gain some performance. If all patterns are set to `null`, autobumping is completely disabled and the commit message is not even retrieved. This improves the performance even more if you do not plan to use autobumping at all currently. You can re-enable autobumping at any time by using the default value for a pattern or setting a custom one.

### `autobump`

This option allows you to specify how the build version should be automatically bumped based on the last commit message. Each line of the commit message is checked to see if it matches a specified pattern. If so, the corresponding version is bumped. The option has the following sub-options:

 - `majorPattern`: If any line in the commit message matches `majorPattern`, the major version will be bumped. This has to be a regular expression, and its default value is `~/\[major\]/`.
 - `minorPattern`: If any line in the commit message matches `minorPattern`, the minor version will be bumped. This has to be a regular expression, and its default value is `~/\[minor\]/`.
 - `patchPattern`: If any line in the commit message matches `patchPattern`, the patch version will be bumped. This has to be a regular expression, and its default value is `~/\[patch\]/`.
 - `preReleasePattern`: If any line in the commit message matches `preReleasePattern`, the pre-release version will be bumped (see [`bump`](#prerelease.bump)). This has to be a regular expression, and its default value is `~/\[pre-release\]/`.
 - `newPreReleasePattern`: If any line in the commit message matches `newPreReleasePattern`, then a new pre-release version will be created. If no string matching `majorPattern`, `minorPattern`, or `patchPattern` can be found then the new pre-release version will be created after bumping the patch version. Otherwise, the new pre-release version is created after bumping the appropriate component based on the pattern that was matched. The same restrictions and rules that apply to the [`newPreRelease`](#newprerelease) property apply here as well. This has to be a regular expression, and its default value is `~/\[new-pre-release\]/`.
 - `promoteToReleasePattern`: If any line in the commit message matches `promoteToReleasePattern`, the version will be promoted to a release version. The same rules that apply to the [`promoteToRelease`](#promotetorelease) property apply here as well. This has to be a regular expression, and its default value is `~/\[promote\]/`.

**Example:** Defining custom patterns to be used by `autobump`
```gradle
autobump {
    majorPattern = ~/\[bump-major\]/
    minorPattern = ~/\[bump-minor\]/
    patchPattern = ~/\[bump-patch\]/
    preReleasePattern = ~/\[bump-pre-release\]/
    newPreReleasePattern = ~/\[make-new-pre-release\]/
    promoteToReleasePattern = ~/\[promote-to-release\]/
}
```

## Checking out a tag

It is useful to check out a tag when you want to create a build of an older version. If you do this, the plugin will detect that `HEAD` is pointing to a tag and will use the corresponding version as the version of the build. **It is not possible to bump or modify the version in any other manner if you have checked out a tag corresponding to that version and have not made additional changes. Also, for this to work as expected, the tag you are checking out must not be excluded by [`tagPattern`](#tagpattern), [`matching`](#matching), or [`preRelease.pattern`](#prerelease.pattern).**

