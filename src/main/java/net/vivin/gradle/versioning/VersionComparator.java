package net.vivin.gradle.versioning;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {
    private static final Pattern VERSION_AT_START_PATTERN = Pattern.compile("^(\\d++\\.\\d++\\.\\d++)");
    private static final Pattern PRE_RELEASE_PART_PATTERN = Pattern.compile("(?<=^\\d+\\.\\d+\\.\\d+)-(?<preReleasePart>.*)$");
    private static final Pattern NUMERIC_COMPONENT_PATTERN = Pattern.compile("\\d++");

    @Override
    public int compare(String a, String b) {
        Matcher aMatcher = VERSION_AT_START_PATTERN.matcher(a);
        aMatcher.find();
        String[] aVersionComponents = aMatcher.group().split("\\.");
        Matcher bMatcher = VERSION_AT_START_PATTERN.matcher(b);
        bMatcher.find();
        String[] bVersionComponents = bMatcher.group().split("\\.");

        int aNumericValue = 0;
        int bNumericValue = 0;

        if(!aVersionComponents[VersionComponent.MAJOR.getIndex()].equals(bVersionComponents[VersionComponent.MAJOR.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.MAJOR.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.MAJOR.getIndex()]);

        } else if(!aVersionComponents[VersionComponent.MINOR.getIndex()].equals(bVersionComponents[VersionComponent.MINOR.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.MINOR.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.MINOR.getIndex()]);

        } else if(!aVersionComponents[VersionComponent.PATCH.getIndex()].equals(bVersionComponents[VersionComponent.PATCH.getIndex()])) {
            aNumericValue = Integer.parseInt(aVersionComponents[VersionComponent.PATCH.getIndex()]);
            bNumericValue = Integer.parseInt(bVersionComponents[VersionComponent.PATCH.getIndex()]);

        } else if(a.contains("-") && b.contains("-")) {
            aMatcher = PRE_RELEASE_PART_PATTERN.matcher(a);
            aMatcher.find();
            String aPreReleaseVersion = aMatcher.group("preReleasePart");
            bMatcher = PRE_RELEASE_PART_PATTERN.matcher(b);
            bMatcher.find();
            String bPreReleaseVersion = bMatcher.group("preReleasePart");

            String[] aPreReleaseComponents = aPreReleaseVersion.split("\\.");
            String[] bPreReleaseComponents = bPreReleaseVersion.split("\\.");

            int i = 0;
            boolean matching = true;
            for(; (i < aPreReleaseComponents.length) && (i < bPreReleaseComponents.length) && matching; i++) {
                matching = aPreReleaseComponents[i].equals(bPreReleaseComponents[i]);
            }

            if(matching) {
                aNumericValue = aPreReleaseComponents.length;
                bNumericValue = bPreReleaseComponents.length;
            } else {
                String aNonMatchingComponent = aPreReleaseComponents[i - 1];
                String bNonMatchingComponent = bPreReleaseComponents[i - 1];

                boolean aNumericComponent = NUMERIC_COMPONENT_PATTERN.matcher(aNonMatchingComponent).matches();
                boolean bNumericComponent = NUMERIC_COMPONENT_PATTERN.matcher(bNonMatchingComponent).matches();

                if(aNumericComponent && bNumericComponent) {
                    aNumericValue = Integer.parseInt(aNonMatchingComponent);
                    bNumericValue = Integer.parseInt(bNonMatchingComponent);
                } else if(!aNumericComponent && !bNumericComponent) {
                    aNumericValue = aNonMatchingComponent.compareTo(bNonMatchingComponent);
                    bNumericValue = -1 * aNumericValue;
                } else {
                    aNumericValue = aNumericComponent ? 0 : 1;
                    bNumericValue = bNumericComponent ? 0 : 1;
                }
            }
        } else if(a.contains("-") || b.contains("-")) {
            aNumericValue = a.contains("-") ? 0 : 1;
            bNumericValue = b.contains("-") ? 0 : 1;
        }

        return aNumericValue - bNumericValue;
    }
}
