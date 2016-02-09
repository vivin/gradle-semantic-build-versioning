package net.vivin.gradle.plugins.version.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created on 2/8/16 at 8:31 PM
 *
 * @author vivin
 */
public class TagVersion {

    private String workingDirectory;

    public TagVersion(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getNextVersionNumber(VersioningRequest request) throws IOException, GitAPIException {
        String next;
        String latest = getLatestVersionNumber(request);

        String[] components = latest.split("[\\.-]");
        components[request.getBump().getIndex()] = String.valueOf(Integer.parseInt(components[request.getBump().getIndex()]) + 1);

        if(request.getBump() == VersioningRequest.Bump.MAJOR) {
            components[VersioningRequest.Bump.MINOR.getIndex()] = "0";
            components[VersioningRequest.Bump.PATCH.getIndex()] = "0";
        } else if(request.getBump() == VersioningRequest.Bump.MINOR) {
            components[VersioningRequest.Bump.PATCH.getIndex()] = "0";
        }

        next = String.format("%s.%s.%s", components[VersioningRequest.Bump.MAJOR.getIndex()], components[VersioningRequest.Bump.MINOR.getIndex()], components[VersioningRequest.Bump.PATCH.getIndex()]);
        if(!request.getIdentifiers().isEmpty()) {
            next = String.format("%s-%s", next, request.getIdentifiers());
        }

        if(request.isSnapshot()) {
            next = String.format("%s-%s", next, request.getSnapshotSuffix());
        } else if(!request.getReleaseSuffix().isEmpty()) {
            next = String.format("%s-%s", next, request.getReleaseSuffix());
        }

        return next;
    }

    private String getLatestVersionNumber(VersioningRequest request) throws IOException, GitAPIException {
        Pattern tagPattern = Pattern.compile(request.getPatternWithoutVersionSuffix());

        Repository repository = new FileRepositoryBuilder()
            .setWorkTree(new File(workingDirectory))
            .findGitDir()
            .build();

        RevWalk walk = new RevWalk(repository);
        List<Ref> tagRefs = new Git(repository).tagList().call();

        List<String> tagNames = tagRefs.stream()
            .map(tagRef -> parseTag(walk, tagRef.getObjectId()).getTagName())
            .filter(tagName -> tagPattern.matcher(tagName).matches())
            .map(tagName ->
                tagName.replaceAll(String.format("-%s", request.getIdentifiers()), "")
                    .replaceAll(String.format("-%s", request.getReleaseSuffix()), "")
            )
            .sorted((a, b) -> {
                int componentIndex = VersioningRequest.Bump.MAJOR.getIndex();

                String[] aComponents = a.split("\\.");
                String[] bComponents = b.split("\\.");

                if(aComponents[VersioningRequest.Bump.MAJOR.getIndex()].equals(bComponents[VersioningRequest.Bump.MAJOR.getIndex()])) {
                    componentIndex = VersioningRequest.Bump.MINOR.getIndex();
                    if(aComponents[VersioningRequest.Bump.MINOR.getIndex()].equals(bComponents[VersioningRequest.Bump.MINOR.getIndex()])) {
                        componentIndex = VersioningRequest.Bump.PATCH.getIndex();
                    }
                }

                return Integer.parseInt(bComponents[componentIndex]) - Integer.parseInt(aComponents[componentIndex]);
            })
            .collect(Collectors.toList());

        return tagNames.isEmpty() ? "0.0.0" : tagNames.get(0);
    }

    private RevTag parseTag(RevWalk walk, ObjectId id) {
        try {
            return walk.parseTag(id);
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
