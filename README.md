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
      * [`message`](#message)
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

**NOTE: Plugin configuration and usage has changed significantly since version 2.x. If you are still using that version, the documentation can be found [here](https://github.com/vivin/gradle-semantic-build-versioning/tree/2.x).**

This is a Gradle plugin that provides support for [semantic versioning](http://semver.org) of builds. It is quite easy to use and extremely configurable. The plugin allows you to bump the major, minor, patch or pre-release version based on the latest version, which is identified from a git tag. It also allows you to bump pre-release versions based on a scheme that you define. The version can be bumped by using version-component-specific project properties or can be bumped automatically based on the contents of a commit message. If no manual bumping is done via commit message or project property, the plugin will increment the version-component with the lowest precedence; this is usually the patch version, but can be the pre-release version if the latest version is a pre-release one. The plugin does its best to ensure that you do not accidentally violate semver rules while generating your versions; in cases where this might happen the plugin forces you to be explicit about violating these rules.

As this is a settings plugin, it is applied to `settings.gradle` and  so version calculation is performed right at the start of the build, before any projects are configured. This means that the project version is immediately available (almost as if it was set explicitly -- which it effectively is), and will never change during the build (barring some other, external task that attempts to modify the version during the build). While the build is running, tagging or changing the project properties will not influence the version that was calculated at the start of the build.

**Note**: The gradle documentation specifies that the version property is an `Object` instance. So to be absolutely safe, and especially if you might change versioning-plugins later, you should use the `toString()` method on `project.version`. However, this plugin does set the value of `project.version` to a `String` instance and hence you can treat it as such. While the version property is a string, it does expose some additional properties. These are `snapshot`, `major`, `minor`, `patch` and `preRelease`. `snapshot` is a boolean and can be used for release vs. snapshot project-configuration, instead of having to do an `endsWith()` check. `major`, `minor`, `patch` and `preRelease` bear the single version components for further usage in the build process. `major`, `minor` and `patch` are of type `int` and are always set, `preRelease` is a `String` and can be `null` if the current version is not a pre-release version.

# Usage

The latest version of this plugin is **3.0.1**. Using the plugin is quite simple:

**In settings.gradle**
```gradle
buildscript {
    repositories {
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }
    dependencies {
        classpath 'gradle.plugin.net.vivin:gradle-semantic-build-versioning:3.0.1'
    }
}

apply plugin: 'net.vivin.gradle-semantic-build-versioning'
```

Additionally you need an (at least) empty `semantic-build-versioning.gradle` file in the corresponding project-directory of each project in the build that should be handled by this plugin. This file allows you to set options to configure the plugin's behavior (see [Options and use-cases](#options-and-use-cases)). If you do not want to version your sub-projects separately from the main project, and instead want to keep their versions in sync with the parent project, you can simply add `semantic-build-versioning.gradle` only under the root project and do something like the following in the root project's `build.gradle`:
```gradle
subprojects {
    version = rootProject.version
}
```

This is usually enough to start using the plugin. Assuming that you already have tags that are (or contain) semantic versions, the plugin will search for all nearest ancestor-tags, select the latest<sup>1</sup> of them as the base version, and increment the component with the least precedence. The nearest ancestor-tags are those tags with a path between them and the `HEAD` commit, without any intervening tags. This is the default behavior of the plugin.

<sup>1</sup> Latest based on ordering-rules defined in the semantic-version specification, **not latest by date**.

# Project properties

The plugin uses project properties to control how the version should be calculated. Some of these properties require values, whereas others don't. The ones that don't, essentially behave like switches and their presence is enough to "turn on" specific behavior. Aside from providing these properties via the commandline using `-P`, the properties can also be specified in any other valid way, similar to how one would provide properties to `settings.gradle`.  For example, you can set them through system properties or environment properties, depending on the situation. You can define them in `gradle.properties` as well if you wish, although that is usually not useful unless you want some sort of global versioning-configuration.

## `bumpComponent`

This property bumps the version. It can be set to the values `major`, `minor`, `patch` or `pre-release`.

Assuming that the base version is `x.y.z` and the value of `bumpComponent` is:
- `major`, the new version will be `(x + 1).0.0`; if the base version is a pre-release version, the pre-release version-component is discarded and the new version will still be `(x + 1).0.0`
- `minor`, the new version will be `x.(y + 1).0`; if the base version is a pre-release version, the pre-release version-component is discarded and the new version will still be `x.(y + 1).0`
- `patch`, the new version will be `x.y.(z + 1)`; if the base version is a pre-release version, the pre-release version-component is discarded and the new version will still be `x.y.(z + 1)`
- `pre-release`, the pre-release version is bumped. Pre-release versions are denoted by appending a hyphen, and a series of dot-separated identifiers that can only consist of alphanumeric characters and hyphens; numeric identifiers cannot contain leading-zeroes. Since pre-release versions are arbitrary, using this property requires some additional configuration (see [Pre-releases](#pre-releases)). Assuming that the base version is `x.y.z-<identifier>`, the new version will be `x.y.z-<identifier++>` where the value of `<identifier++>` is determined based on a scheme defined by the pre-release configuration (see [`bump`](#prerelease.bump)).

The behavior of this property is slightly different in the situation where the base version cannot be identified (usually when there are no ancestor tags). In this case, the base-version is set to the provided starting-version (or the default value of `0.1.0` if one is not provided; see [`startingVersion`](#startingversion)). The requested version-component is bumped only if doing so will not cause a version series to be skipped; i.e., the starting version **will not be bumped** when the value of `bumpComponent` is:
- `major` and the starting version has a non-zero major-version, a zero minor-version and a zero patch-version (i.e. `(x > 0).0.0`)
- `minor` and the starting version has:
  - a non-zero minor-version and a zero patch-version (i.e. `x.(y > 0).0`)
  - a non-zero major-version and a zero patch-version (i.e. `(x > 0).y.0`)
- `patch` and the starting version has:
  - a non-zero patch-version (i.e. `x.y.(z > 0)`)
  - a non-zero minor-version (i.e. `x.(y > 0).z`)
  - a non-zero major-version (i.e. `(x > 0).y.z`)

**Notes:**
  - `pre-release` can only be used if the base version is already a pre-release version. If you want to create a new pre-release, use the `newPreRelease` property.
  - It is not possible to use `pre-release` with `promoteToRelease` or `newPreRelease`.
  - `pre-release` can fail if you have filtered tags in such a way (see [Filtering tags](#filtering-tags) and [`pattern`](#prerelease.pattern)) that the base version does not have a pre-release identifier.

## `forceBump`

If you use autobumping (see [Automatic bumping based on commit messages](#automatic-bumping-based-on-commit-messages)) and manual bumping together, the following precedence-rules apply, after determining the autobump and manual-bump version-components separately:
  - If you are attempting to manually bump a component with higher-precedence than the one autobump is attempting to bump, the manual bump wins.
  - If you are attempting to manually bump a component with lesser-precedence than the one autobump is attempting to bump, and the `forceBump` property is **not** set, the build fails.
  - If you are attempting to manually bump a component with lesser-precedence than the one autobump is attempting to bump, and the `forceBump` property is set, the manual bump wins. Note that this means that you are **intentionally disregarding** your commit messages (i.e., "I know what I'm doing; my commit messages were wrong").

## `newPreRelease`

This property creates a new pre-release version by bumping the requested version-component and then adding the starting pre-release version from the pre-release configuration (see [`preRelease`](#prerelease)). It has the following behavior:
 - When used by itself it will bump the patch version and then append the starting pre-release version as specified in the pre-release configuration. Assuming that the base version is `x.y.z`, the new version will be `x.y.(z + 1)-<startingVersion>` (see [`startingVersion`](#prerelease.startingversion)).
 - When used with `bumpComponent=patch`, the behavior is the same as using `newPreRelease` by itself.
 - When used with `bumpComponent=minor`, it will bump the minor version and then append the starting pre-release version as specified in the pre-release configuration. Assuming that the base version is `x.y.z`, the new version will be `x.(y + 1).0-<startingVersion>` (see [`startingVersion`](#prerelease.startingversion)).
 - When used with `bumpComponent=major`, it will bump the major version and then append the starting pre-release version as specified in the pre-release configuration. Assuming that the base version is `x.y.z`, the new version will be `(x + 1).0.0-<startingVersion>` (see [`startingVersion`](#prerelease.startingversion)).

**Notes:**
  - It is not possible to use `bumpComponent=pre-release` along with `newPreRelease`.
  - If the base version cannot be identified and a starting version is used, note that the behavior of `bumpComponent` is still subject to the rules that prevent version series from being skipped when bumping.

## `promoteToRelease`

This property promotes a pre-release version to a release version. This is done by discarding the pre-release version-component. For example, assuming that the base version is `x.y.z-some.identifiers.here`, the new version will be `x.y.z`. **This property can only be used if the base version is a pre-release version**.

## `release`

This property specifies that the build is a release build, which means that a snapshot suffix is not attached to the version (see [`snapshotSuffix`](#snapshotsuffix)). **You cannot release a build if there are uncommitted changes**.

<hr />

**Note**: There are some restrictions to keep in mind when using the above properties:

1. It is not possible to use `promoteToRelease` when explicitly bumping a version-component.
1. With the exception of `bumpComponent=pre-release`, all other version-component bumping-values can be used in conjunction with `newPreRelease`; this has the effect of bumping a version-component and adding a pre-release identifier at the same time to create a new pre-release version.
1. It is not possible to modify the version in any manner if `HEAD` is at a commit that has been tagged to identify a particular version (and there are no uncommitted changes). This is because it would then be possible to push out an identical artifact with a different version-number and violate semantic-versioning rules. For example, assuming that the base version is `1.0.2`, it would be possible to check out tag `1.0.0`, bump the major version, and release it as `2.0.0`. For more information about tagging and checking out a tag, see [`tag`](#tag) and [Checking out a tag](#checking-out-a-tag).

# Tasks

## `tag`

This task will create an annotated or lightweight tag (see [`message`](#message)) corresponding to the new version (with an optional prefix; see [`tagPrefix`](#tagprefix)). It is recommended to use this task with a message (to force the creation of an annotated tag) along with the `release` task when creating a release. **You cannot tag a snapshot release; use pre-release identifiers if you want to deploy bleeding-edge  versions**.

If you specify the option `--push` to the task, the created tag will also get pushed automatically.

### `message`

The `message` property lets you control the annotated tag's message and whether the tag will be an annotated or lightweight one. The value of this property is expected to be a `Callable`, which typically is a closure that returns a string (technically, any type is acceptable and its string representation is what is used). If the return value is `null`, or if the whole property itself is `null`, the created tag will be a lightweight one. By default, the property is set to a closure that returns an empty string, which creates an annotated tag without a message.

If you want to change this behavior, you can define your own closure as follows:

**Example:** Defining a custom `message` closure:
```gradle
tag {
    message {
        "version: ${version}"
    }
}
```

The `tag` task exposes six predefined-closures that allow you to provide the tag message via environment or system variables, or project properties (each of which can be made optional or mandatory); you are also able to specify the name of the variable or property. In the optional variants, if the respective variable or property is set but has no value, an empty annotated-tag is created. If the variable or property is not set at all, a lightweight tag is created. If the mandatory variants are used, the absence of the variable or property is considered an error, but an empty value can still be used for an annotated tag without a message. This way you can not only make sure that the user does not forget to set a message, but can also mandate the creation of annotated tags.

**Example:** Retrieving the tag message from an environment variable:
```gradle
tag {
    // Now you can specify the message via the environment variable tagMessage
    message fromEnvironmentVariable('tagMessage')
}
```

**Example:** Retrieving the tag message from a mandatory environment variable:
```gradle
tag {
    // Now you can specify the message via the environment variable tagMessage
    message fromMandatoryEnvironmentVariable('tagMessage')
}
```

**Example:** Retrieving the tag message from a system property:
```gradle
tag {
    // Now you can specify the message via -DtagMessage="..."
    // e.g. ./gradlew tag -DtagMessage="..."
    message fromSystemProperty('tagMessage')
}
```

**Example:** Retrieving the tag message from a mandatory system property:
```gradle
tag {
    // Now you can specify the message via -DtagMessage="..."
    // e.g. ./gradlew tag -DtagMessage="..."
    message fromMandatorySystemProperty('tagMessage')
}
```

**Example:** Retrieving the tag message from a project property:
```gradle
tag {
    // Now you can specify the message via -P tagMessage="..."
    // e.g. ./gradlew tag --push -P tagMessage="..."
    message fromProjectProperty('tagMessage')
}
```

**Example:** Retrieving the tag message from a mandatory project property:
```gradle
tag {
    // Now you can specify the message via -P tagMessage="..."
    // e.g. ./gradlew tag --push -P tagMessage="..."
    message fromMandatoryProjectProperty('tagMessage')
}
```

**Example:** Retrieving the tag message from a project property
             or if not present from a system property
             or if not present create lightweight tag:
```gradle
tag {
    message {
        [
            fromProjectProperty('tagMessage'),
            fromSystemProperty('tagMessage')
        ].findResult { it() }
    }
}
```

**Example:** Retrieving the tag message from a project property
             or if not present from a system property
             or if not present throw an error:
```gradle
tag {
    message {
        [
            fromProjectProperty('tagMessage'),
            fromMandatorySystemProperty('tagMessage')
        ].findResult { it() }
    }
}
```

**Example:** Retrieving the tag message from a project property
             or if not present from a system property
             or if not present create empty annotated tag:
```gradle
tag {
    message {
        [
            fromProjectProperty('tagMessage'),
            fromSystemProperty('tagMessage')
        ].findResult('') { it() }
    }
}
```

## `printVersion`

Prints out the calculated version.

# Options and use-cases

The plugin has a few configuration options that you can use to fine-tune its behavior, or to provide additional options to modify the behavior of certain tasks or properties. All these options are configured inside a `semantic-build-versioning.gradle` file that must be present at the root of each project in the build that wants to use this plugin.

## General options

These options control what your versions and tags look like. Using these options, you can set an optional starting-version (used when no tags are found), an optional tag-prefix, and the snapshot-suffix to use for snapshot versions.

### `startingVersion`

This sets the version to use when no ancestor tags could be found that were not excluded by filtering. By default it is set to `0.1.0`. This must be a valid semantic-version string **without** pre-release identifiers.

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

This is the suffix to use for snapshot versions. By default it is `SNAPSHOT`. This suffix is always attached to the version - separated by a dash - unless the [`release`](#release) property is set or if a tag is checked out and no changes have been made.

**Example:** Snapshot versions should look like `0.1.0-Candidate`
```gradle
snapshotSuffix = 'Candidate'
```

## Filtering tags

These options let you restrict the set of tags considered when determining the base version.

**Note:** Be careful when filtering tags because it can affect plugin-behavior. The plugin works by determining the base version from tags, so behavior can vary depending on whether certain tags have been filtered out or not:
 - If your filtering options are set such that none of the existing ancestor-tags match, the plugin will use the [`startingVersion`](#startingversion).
 - If your filtering options are set such that the base version is not a pre-release version and you are attempting to use [`bumpComponent=pre-release`](#bumpcomponent), the build will fail.

### `tagPattern`

This pattern tells the plugin to only consider those tags matching `tagPattern` when trying to determine the base version from the tags in your repository. The value for this option has to be a regular expression. Its default value is `~/\d++\.\d++\.\d++/`, which means that all tags that contain a semantic-version portion are considered, while all others are ignored. This property can be used, for example, to tag and version different sub-projects under a root-project individually, while using the same repository.

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

This is how you can define your pre-release versioning-strategy. This is a special case because other than defining a basic syntax and ordering rules, the semantic-versioning specification has no other rules about pre-release identifiers. This means that some extra configuration is required if you want to generate pre-release versions.

### `preRelease`

This option allows you to specify how pre-release versions should be generated and bumped. It has the following sub-options:

 - <a id="prerelease.startingversion" />`startingVersion`: This option is required and describes the starting pre-release version of a new pre-release. This value will be used if [`newPreRelease`](#newprerelease) is invoked (either explicitly or via [Automatic bumping based on commit messages](#automatic-bumping-based-on-commit-messages)).

   **Example:** Starting version for a pre-release version should be `alpha.0`
   ```gradle
   preRelease {
       startingVersion = 'alpha.0'
   }
   ```
 - <a id="prerelease.pattern" />`pattern`: This option has a function similar to [`tagPattern`](#tagpattern), except that it allows you to restrict the set of tags considered to those tags with pre-release versions matching `pattern`. The value for this has to be a regular expression as a `String`. Its default value is `/.*+$/`. One thing to remember is that starting anchors (`^`) cannot be used, because the actual regular-expression that is used is `~/\d++\.\d++\.\d++-$pattern/`. Hence, if you are trying to filter tags based on pre-release versions starting with some string, it is enough to provide that string in the regular expression without prefixing it with `^`.

   **Example:** Only tags whose pre-release versions start with `alpha` should be considered
   ```gradle
   preRelease {
       pattern = /alpha/
   }
   ```

   **Note:** Filtering based on `pattern` is performed **after** tags have been filtered based on [`tagPattern`](#tagpattern) and [`matching`](#matching).

 - <a id="prerelease.bump" />`bump`: This property allows you to specify how pre-release versions should be incremented or bumped. This is expected to be a closure that accepts a single argument (the base pre-release version), and is expected to return an object, whose `toString()` value is used as the incremented version.

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

Sometimes you might want to automatically bump your version as part of your continuous-integration process. Without this option, you would have to explicitly configure your CI process to use the corresponding `bumpComponent` property value, depending on the version component you want to bump. This is because the default behavior of the plugin is to bump the component with least precedence. Instead, you can configure the plugin to automatically bump the desired version-component based on the contents of all your commit messages since the nearest ancestor-tags; this essentially means messages from all unreleased ancestor-commits. If multiple commit-messages apply, then the component with the highest precedence wins. This way you can note in each commit message whether the change is major or minor directly, and this plugin uses that information to calculate the next version-number to be used.

### `autobump`

This option allows you to specify how the build version should be automatically bumped based on the contents of commit messages. The full message of each applicable commit-message is checked to see if a match for any of specified pattern can be found. Note that in the case of multiple matches, the component with the highest precedence wins. This option has the following sub-options:

 - `majorPattern`: If any relevant commit message contains a match for `majorPattern`, the major version will be bumped. This has to be a regular expression, and its default value is `~/\[major\]/`, which means `[major]` anywhere in the commit message.
 - `minorPattern`: If any relevant commit message contains a match for `minorPattern`, the minor version will be bumped. This has to be a regular expression, and its default value is `~/\[minor\]/`, which means `[minor]` anywhere in the commit message.
 - `patchPattern`: If any relevant commit message contains a match for `patchPattern`, the patch version will be bumped. This has to be a regular expression, and its default value is `~/\[patch\]/`, which means `[patch]` anywhere in the commit message.
 - `newPreReleasePattern`: If any relevant commit message contains a match for `newPreReleasePattern`, then a new pre-release version will be created. If no major or minor-version bumping is specified via autobumping or manually, the new pre-release version will be created after bumping the patch version. Otherwise, the new pre-release version is created after bumping the appropriate component. The same restrictions and rules that apply to the [`newPreRelease`](#newprerelease) property apply here as well. This has to be a regular expression, and its default value is `~/\[new-pre-release\]/`, which means `[new-pre-release]` anywhere in the message.
 - `promoteToReleasePattern`: If any relevant commit message contains a match for `promoteToReleasePattern`, the version will be promoted to a release version. The same rules that apply to the [`promoteToRelease`](#promotetorelease) property apply here as well. This has to be a regular expression, and its default value is `~/\[promote\]/`, which means `[promote]` anywhere in any line.

**Example:** Defining custom patterns to be used by `autobump`
```gradle
autobump {
    majorPattern = ~/(?m)^\[bump-major\]$/                    # match "[bump-major]" on its own line without leading or trailing characters
    minorPattern = ~/(?m)^\[bump-minor\]$/                    # match "[bump-minor]" on its own line without leading or trailing characters
    patchPattern = ~/(?m)^\[bump-patch\]$/                    # match "[bump-patch]" on its own line without leading or trailing characters
    newPreReleasePattern = ~/(?m)^\[make-new-pre-release\]$/  # match "[make-new-pre-release]" on its own line without leading or trailing characters
    promoteToReleasePattern = ~/(?m)^\[promote-to-release\]$/ # match "[promote-to-release]" on its own line without leading or trailing characters
}
```

**Example:** Defining a multi-line pattern to be used by `autobump`
```gradle
autobump {
    minorPattern = ~/(?mx)                   # enable multi-line and comment mode
                     ^\|++\n                 # a line full of pipes
                     \|{2}                   # two pipes
                         \s++                # spaces
                         (?-x:bump the)      # "bump the"
                         \s++                # spaces
                         \|{2}               # two pipes
                         \n                  # EOL
                     \|{2}                   # two pipes
                         \s++                # spaces
                         (?-x:minor version) # "minor version"
                         \s++                # spaces
                         \|{2}               # two pipes
                         \n                  # EOL
                     \|++$                   # a line full of pipes
                    /

    # matches the following roughly, tolerating some typos,
    # like wrong amount of pipes in the header or footer line
    # or wrong amount of spaces between pipes and text:
    #
    # |||||||||||||||||||
    # ||   bump the    ||
    # || minor version ||
    # |||||||||||||||||||
}
```
**Notes**:

 1. If none of the commit messages match the patterns in `autobump`, the plugin assumes its default behavior and will bump the component with least-precedence.
 1. Commit messages will not be checked against any pattern that is set to `null`. So if you are not planning on looking for patterns corresponding to certain types of version bumps or calculations, you can disable them by setting them to `null` (which also boosts performance slightly). It is also useful to do this in cases where you might want to prevent certain types of bumps from happening (e.g., prevent any accidental major-version bumps until it is time to release). If all patterns are set to `null`, autobumping is completely disabled, and commit messages are not retrieved; this can further improve performance if you do not plan on using autobumping at all. You can re-enable autobumping at any time by using the default value for a pattern or by setting a custom value.

## Checking out a tag

It is useful to check out a tag when you want to create a build of an older version. If you do this, the plugin will detect that `HEAD` is pointing to a tag and will use the corresponding version as the version of the build. **It is not possible to bump or modify the version in any other manner if you have checked out a tag corresponding to that version and have not made additional changes. Also, for this to work as expected, the tag you are checking out must not be excluded by [`tagPattern`](#tagpattern), [`matching`](#matching), or [`preRelease.pattern`](#prerelease.pattern).**

