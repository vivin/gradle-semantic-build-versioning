package net.vivin.gradle.plugins.version.git;

import net.vivin.gradle.plugins.version.SemanticBuildVersion;
import net.vivin.gradle.plugins.version.SemanticVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.tooling.BuildException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created on 2/8/16 at 8:31 PM
 *
 * @author vivin
 */
public class TagVersion {

    private static final String STARTING_VERSION = "0.0.0";

    private String workingDirectory;

    public TagVersion(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getNextVersion(SemanticBuildVersion versionPlugin) throws IOException, GitAPIException {
        if(versionPlugin.getBump() == VersionComponent.PRERELEASE && versionPlugin.getPreReleaseConfiguration() == null) {
            throw new BuildException("Cannot bump pre-release identifier if a preRelease configuration is not specified", null);
        }

        String tagPattern = versionPlugin.getTagPattern().toString();

        String matchingMajor = versionPlugin.getVersionsMatching().getMajor() < 0 ? "<any>" : String.valueOf(versionPlugin.getVersionsMatching().getMajor());
        String matchingMinor = versionPlugin.getVersionsMatching().getMinor() < 0 ? "<any>" : String.valueOf(versionPlugin.getVersionsMatching().getMinor());
        String matchingPatch = versionPlugin.getVersionsMatching().getPatch() < 0 ? "<any>" : String.valueOf(versionPlugin.getVersionsMatching().getPatch());

        String highestVersion = getHighestVersion(versionPlugin);
        if(highestVersion == null) {
            if(versionPlugin.getBump() == VersionComponent.PRERELEASE) {
                String preReleaseVersionPattern = versionPlugin.getPreReleaseConfiguration().getPattern().toString();

                throw new BuildException(
                    String.format(
                        "Could not bump pre-release identifier because the highest version could not be found satisfying tag-pattern /%s/, matching major-version %s, matching minor-version %s, matching patch-version %s, and pre-release-version pattern /%s/", tagPattern, matchingMajor, matchingMinor, matchingPatch, preReleaseVersionPattern
                    ), null
                );
            } else if(versionPlugin.getBump() != null) {
                throw new BuildException(
                    String.format(
                        "Could not bump %s-version because the highest version could not be found satisfying tag-pattern /%s/, matching major-version %s, matching minor-version %s, and matching patch-version %s", tagPattern, versionPlugin.getBump().name().toLowerCase(), matchingMajor, matchingMinor, matchingPatch
                    ), null
                );
            } else {
                throw new BuildException(
                    String.format("Could not determine whether to bump patch-version or pre-release identifier because the highest version could not be found satisfying tag-pattern /%s/, matching major-version %s, matching minor-version %s, and matching patch-version %s", tagPattern, matchingMajor, matchingMinor, matchingPatch), null
                );
            }
        } else if(versionPlugin.getBump() == null) {
            if(versionPlugin.isPromoteToRelease()) {
                versionPlugin.setBump(VersionComponent.NONE);
            } else if(!highestVersion.contains("-")) {
                versionPlugin.setBump(VersionComponent.PATCH);
            } else {
                versionPlugin.setBump(VersionComponent.PRERELEASE);
            }
        }

        return incrementVersion(versionPlugin, highestVersion);
    }

    private String incrementVersion(SemanticBuildVersion version, String latestVersion) {
        String incrementedVersion;
        String[] components = latestVersion.split("[\\.-]");
        SemanticVersion latest = new SemanticVersion(
            Integer.parseInt(components[VersionComponent.MAJOR.getIndex()]),
            Integer.parseInt(components[VersionComponent.MINOR.getIndex()]),
            Integer.parseInt(components[VersionComponent.PATCH.getIndex()])
        );

        VersionComponent bump = version.getBump();
        if(bump == VersionComponent.MAJOR || bump == VersionComponent.MINOR || bump == VersionComponent.PATCH) {
            if(bump == VersionComponent.MAJOR) {
                latest.bumpMajor();
            } else if(bump == VersionComponent.MINOR) {
                latest.bumpMinor();
            } else {
                latest.bumpPatch();
            }

            incrementedVersion = latest.toString();
        } else if(bump == VersionComponent.PRERELEASE) {
            if(!latestVersion.contains("-")) {
                latest.bumpPatch();
                incrementedVersion = String.format("%s-%s", latest, version.getPreReleaseConfiguration().getStartingVersion());
            } else {
                String latestPreRelease = latestVersion.substring(latestVersion.indexOf("-") + 1);
                String nextPreRelease = version.getPreReleaseConfiguration().getBump().call(latestPreRelease).toString();

                List<String> preReleaseVersionComponents = Arrays.asList(nextPreRelease.split("\\."));
                preReleaseVersionComponents = preReleaseVersionComponents.stream().filter(component ->
                    !Pattern.compile("^[0-9A-Za-z-]+$").matcher(component).find() ||
                        Pattern.compile("^\\d+$").matcher(component).find() && Pattern.compile("^0\\d+$").matcher(component).find()
                ).collect(Collectors.toList());
                if(preReleaseVersionComponents.size() > 0) {
                    throw new BuildException(String.format("Bumped pre-release version %s is not a valid pre-release version. Identifiers must comprise only ASCII alphanumerics and hyphen, and numeric identifiers must not include leading zeroes", nextPreRelease), null);
                }

                incrementedVersion = String.format("%s-%s", latest, nextPreRelease);
            }
        } else {
            incrementedVersion = latest.toString();
        }

        if(version.isSnapshot()) {
            incrementedVersion = String.format("%s-%s", incrementedVersion, version.getSnapshotSuffix());
        }

        return incrementedVersion;
    }

    private String getHighestVersion(SemanticBuildVersion versionPlugin) throws IOException, GitAPIException {
        Pattern tagPattern = versionPlugin.getTagPattern();

        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(new File(workingDirectory))
            .findGitDir()
            .build();

        RevWalk walk = new RevWalk(repository);
        Git git = new Git(repository);

        //git.fetch().setTagOpt(TagOpt.AUTO_FOLLOW).call(); //Fetch all tags first
        List<Ref> tagRefs = git.tagList().call();
        if(!tagRefs.isEmpty()) {
            List<String> tagNames = tagRefs.stream()
                .map(tagRef -> parseTag(walk, tagRef.getObjectId()).getTagName())
                .filter(tagName -> tagPattern.matcher(tagName).find())
                .map(tagName -> tagName.replaceFirst("^.*?(\\d+\\.\\d+\\.\\d+-?)", "$1"))
                .filter(tagName -> versionPlugin.getVersionsMatching().toPattern().matcher(tagName).find())
                .filter(tagName -> (versionPlugin.getPreReleaseConfiguration() == null || versionPlugin.getBump() != VersionComponent.PRERELEASE || !tagName.contains("-")) || versionPlugin.getPreReleaseConfiguration().getPattern().matcher(tagName).find())
                .sorted(this::compareVersions)
                .collect(Collectors.toList());

            return tagNames.isEmpty() ? null : tagNames.get(0);
        } else {
            return STARTING_VERSION;
        }
    }

    private int compareVersions(String a, String b) {
        String[] aVersionComponents = a.replaceAll("^(\\d+\\.\\d+\\.\\d+).*$", "$1").split("\\.");
        String[] bVersionComponents = b.replaceAll("^(\\d+\\.\\d+\\.\\d+).*$", "$1").split("\\.");

        int aNumericValue = 0;
        int bNumericValue = 0;

        if (!aVersionComponents[VersionComponent.MAJOR.getIndex()].equals(bVersionComponents[VersionComponent.MAJOR.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.MAJOR.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.MAJOR.getIndex()]);

        } else if (!aVersionComponents[VersionComponent.MINOR.getIndex()].equals(bVersionComponents[VersionComponent.MINOR.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.MINOR.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.MINOR.getIndex()]);

        } else if (!aVersionComponents[VersionComponent.PATCH.getIndex()].equals(bVersionComponents[VersionComponent.PATCH.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.PATCH.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.PATCH.getIndex()]);

        } else if (a.contains("-") && b.contains("-")) {
            String aPreReleaseVersion = a.replaceFirst("\\d+\\.\\d+\\.\\d+", "").replaceAll("-(.*)$", "$1");
            String bPreReleaseVersion = b.replaceFirst("\\d+\\.\\d+\\.\\d+", "").replaceAll("-(.*)$", "$1");

            String[] aPreReleaseComponents = aPreReleaseVersion.split("\\.");
            String[] bPreReleaseComponents = bPreReleaseVersion.split("\\.");

            int i = 0;
            boolean bSmallerComponentSize = false;
            boolean matching = true;
            while (i < aPreReleaseComponents.length && !bSmallerComponentSize && matching) {
                bSmallerComponentSize = (i >= bPreReleaseComponents.length);
                if (!bSmallerComponentSize) {
                    matching = aPreReleaseComponents[i].equals(bPreReleaseComponents[i]);
                }

                i++;
            }

            if (matching) {
                aNumericValue = aPreReleaseComponents.length;
                bNumericValue = bPreReleaseComponents.length;
            } else {
                String aNonMatchingComponent = aPreReleaseComponents[i - 1];
                String bNonMatchingComponent = bPreReleaseComponents[i - 1];

                boolean aNumericComponent = Pattern.compile("^\\d+$").matcher(aNonMatchingComponent).matches();
                boolean bNumericComponent = Pattern.compile("^\\d+$").matcher(bNonMatchingComponent).matches();

                if (aNumericComponent && bNumericComponent) {
                    aNumericValue = Integer.parseInt(aNonMatchingComponent);
                    bNumericValue = Integer.parseInt(bNonMatchingComponent);
                } else if (!aNumericComponent && !bNumericComponent) {
                    aNumericValue = aNonMatchingComponent.compareTo(bNonMatchingComponent);
                    bNumericValue = -1 * aNumericValue;
                } else {
                    aNumericValue = aNumericComponent ? 0 : 1;
                    bNumericValue = bNumericComponent ? 0 : 1;
                }
            }
        } else if (a.contains("-") || b.contains("-")) {
            aNumericValue = a.contains("-") ? 0 : 1;
            bNumericValue = b.contains("-") ? 0 : 1;
        }

        return bNumericValue - aNumericValue;
    }

    private RevTag parseTag(RevWalk walk, ObjectId id) {
        try {
            return walk.parseTag(id);
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
