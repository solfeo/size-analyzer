package com.android.tools.sizereduction.plugin;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LibraryForFeatureSplitUsage.LibraryEligibleForFeatureSplit;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Creates SuggestionData instances with appropriate descriptions from a Suggestion instance. */
public class SuggestionDataFactory {
  public static EnumMap<IssueType, String> issueTypeNodeNames = new EnumMap<>(IssueType.class);
  public static EnumMap<IssueType, String> issueTypeDescriptions = new EnumMap<>(IssueType.class);
  public static EnumMap<IssueType, String> issueTypeAutoFixTitles = new EnumMap<>(IssueType.class);
  public static EnumMap<IssueType, String> suggestionTitles = new EnumMap<>(IssueType.class);
  public static EnumMap<IssueType, String> suggestionDescriptions = new EnumMap<>(IssueType.class);
  public static EnumMap<IssueType, String> suggestionMoreInfos = new EnumMap<>(IssueType.class);
  public static EnumMap<IssueType, String> suggestionAutoFixTitles = new EnumMap<>(IssueType.class);

  static {
    // Set to null to prevent creation of an intermediate node under that issue type.
    issueTypeNodeNames.put(
        IssueType.WEBP,
        "Use lossless WebP where appropriate");
    issueTypeNodeNames.put(
        IssueType.MEDIA_STREAMING,
        "Stream media files from the Internet");
    issueTypeNodeNames.put(
        IssueType.LARGE_FILES_DYNAMIC_FEATURE,
        "Add large files to an on demand dynamic-feature");
    issueTypeNodeNames.put(IssueType.PROGUARD_NO_MAP, null);
    issueTypeNodeNames.put(IssueType.PROGUARD_EMPTY_MAP, null);
    issueTypeNodeNames.put(IssueType.PROGUARD_NO_SHRINKING, null);
    issueTypeNodeNames.put(IssueType.PROGUARD_NO_OBFUSCATION, null);
    issueTypeNodeNames.put(
        IssueType.QUESTIONABLE_FILE,
        "Remove inaccessible files");
    issueTypeNodeNames.put(IssueType.BUNDLES_OLD_GRADLE_PLUGIN, null);
    issueTypeNodeNames.put(IssueType.BUNDLES_NO_ABI_SPLITTING, null);
    issueTypeNodeNames.put(IssueType.BUNDLES_NO_DENSITY_SPLITTING, null);
    issueTypeNodeNames.put(IssueType.BUNDLES_NO_LANGUAGE_SPLITTING, null);
    issueTypeNodeNames.put(IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS, null);

    issueTypeDescriptions.put(
        IssueType.WEBP,
        "Converting PNG images to WebP will yield smaller files without any reduction in image"
            + " quality.");
    issueTypeDescriptions.put(
        IssueType.MEDIA_STREAMING,
        "Serving media files from the Internet will reduce your app's initial install size.");
    issueTypeDescriptions.put(
        IssueType.LARGE_FILES_DYNAMIC_FEATURE,
        "Placing large files in an on-demand dynamic feature module will allow you to avoid"
            + " bundling them in the app.");
    issueTypeDescriptions.put(IssueType.PROGUARD_NO_MAP, null);
    issueTypeDescriptions.put(IssueType.PROGUARD_EMPTY_MAP, null);
    issueTypeDescriptions.put(IssueType.PROGUARD_NO_SHRINKING, null);
    issueTypeDescriptions.put(IssueType.PROGUARD_NO_OBFUSCATION, null);
    issueTypeDescriptions.put(
        IssueType.QUESTIONABLE_FILE,
        "Removing inaccessible and unneeded files will reduce the initial download size of your"
            + " app.");
    issueTypeDescriptions.put(IssueType.BUNDLES_OLD_GRADLE_PLUGIN, null);
    issueTypeDescriptions.put(IssueType.BUNDLES_NO_ABI_SPLITTING, null);
    issueTypeDescriptions.put(IssueType.BUNDLES_NO_DENSITY_SPLITTING, null);
    issueTypeDescriptions.put(IssueType.BUNDLES_NO_LANGUAGE_SPLITTING, null);
    issueTypeDescriptions.put(IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS, null);

    issueTypeAutoFixTitles.put(
        IssueType.WEBP, "Convert image(s) to webp");
    issueTypeAutoFixTitles.put(IssueType.MEDIA_STREAMING, null);
    issueTypeAutoFixTitles.put(IssueType.LARGE_FILES_DYNAMIC_FEATURE, null);
    issueTypeAutoFixTitles.put(IssueType.PROGUARD_NO_MAP, null);
    issueTypeAutoFixTitles.put(IssueType.PROGUARD_EMPTY_MAP, null);
    issueTypeAutoFixTitles.put(IssueType.PROGUARD_NO_SHRINKING, null);
    issueTypeAutoFixTitles.put(IssueType.PROGUARD_NO_OBFUSCATION, null);
    issueTypeAutoFixTitles.put(IssueType.QUESTIONABLE_FILE, null);
    issueTypeAutoFixTitles.put(IssueType.BUNDLES_OLD_GRADLE_PLUGIN, null);
    issueTypeAutoFixTitles.put(IssueType.BUNDLES_NO_ABI_SPLITTING, null);
    issueTypeAutoFixTitles.put(IssueType.BUNDLES_NO_DENSITY_SPLITTING, null);
    issueTypeAutoFixTitles.put(IssueType.BUNDLES_NO_LANGUAGE_SPLITTING, null);
    issueTypeAutoFixTitles.put(IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS, null);
    issueTypeAutoFixTitles.put(IssueType.NONOPTIMAL_LIBRARY, null);
    issueTypeAutoFixTitles.put(IssueType.LIBRARY_SHOULD_BE_EXTRACTED_TO_FEATURE_SPLITS, null);

    suggestionTitles.put(IssueType.WEBP, null);
    suggestionTitles.put(IssueType.MEDIA_STREAMING, null);
    suggestionTitles.put(IssueType.LARGE_FILES_DYNAMIC_FEATURE, null);
    suggestionTitles.put(
        IssueType.PROGUARD_NO_MAP, "Your application does not include a Proguard/R8 map");
    suggestionTitles.put(
        IssueType.PROGUARD_EMPTY_MAP, "Your application's Proguard/R8 map is empty");
    suggestionTitles.put(IssueType.PROGUARD_NO_SHRINKING, "Enable Proguard/R8");
    suggestionTitles.put(IssueType.PROGUARD_NO_OBFUSCATION, "Enable Proguard/R8 obfuscation");
    suggestionTitles.put(IssueType.QUESTIONABLE_FILE, null);
    suggestionTitles.put(
        IssueType.BUNDLES_OLD_GRADLE_PLUGIN,
        "Upgrade to a newer version of the Android Gradle plugin");
    suggestionTitles.put(IssueType.BUNDLES_NO_ABI_SPLITTING, "Generate per-ABI APKs");
    suggestionTitles.put(IssueType.BUNDLES_NO_DENSITY_SPLITTING, "Generate per-density APKs");
    suggestionTitles.put(IssueType.BUNDLES_NO_LANGUAGE_SPLITTING, "Generate per-language APKs");
    suggestionTitles.put(
        IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS, "Use uncompressed native libs");
    suggestionTitles.put(IssueType.NONOPTIMAL_LIBRARY, null);
    suggestionTitles.put(
        IssueType.LIBRARY_SHOULD_BE_EXTRACTED_TO_FEATURE_SPLITS,
        "Move library to on-demand module");

    suggestionDescriptions.put(
        IssueType.WEBP,
        "Converting png images to webp format will reduce the storage that your images take up"
            + " without any reduction in image quality.");
    suggestionDescriptions.put(
        IssueType.MEDIA_STREAMING,
        "Serving large media files from the Internet will reduce the initial download size of"
            + " your app.");
    suggestionDescriptions.put(
        IssueType.LARGE_FILES_DYNAMIC_FEATURE,
        "Adding large files to an on demand dynamic-feature will allow you to still use the file"
            + " in your app without including it in your initial download size.");
    suggestionDescriptions.put(
        IssueType.PROGUARD_NO_MAP,
        "You do not appear to be using Proguard/R8. Using Proguard/R8 enables various features"
            + " that may help to reduce the size of your app.");
    suggestionDescriptions.put(
        IssueType.PROGUARD_EMPTY_MAP,
        "Your app does not appear to be using Proguard or R8 obfuscation. Consider enabling it to"
            + " reduce the size of your app.");
    suggestionDescriptions.put(
        IssueType.PROGUARD_NO_SHRINKING,
        "Enabling code shrinking will reduce the size of the built apk, removing code that is not"
            + " in use by the final artifact.");
    suggestionDescriptions.put(
        IssueType.PROGUARD_NO_OBFUSCATION,
        "Your app does not appear to be using Proguard or R8 obfuscation. Consider enabling it to"
            + " reduce the size of your app.");
    suggestionDescriptions.put(
        IssueType.QUESTIONABLE_FILE,
        "These files do not appear to be needed. Consider removing the files or placing them"
            + " in the Assets directory. For any files added through a library dependency, consider"
            + " using the packaging options to exclude the files.");
    suggestionDescriptions.put(
        IssueType.BUNDLES_OLD_GRADLE_PLUGIN,
        "Consider upgrading to the Android Gradle plugin version 3.2 or later to use Android App "
            + "Bundles. This will likely offer significant app size savings.");
    suggestionDescriptions.put(
        IssueType.BUNDLES_NO_ABI_SPLITTING,
        "Your App Bundle is not configured to split APKs by ABI. Consider enabling this option for"
            + " maximum app size reduction.");
    suggestionDescriptions.put(
        IssueType.BUNDLES_NO_DENSITY_SPLITTING,
        "Your App Bundle is not configured to split APKs by screen density. Consider enabling this"
            + " option for maximum app size reduction.");
    suggestionDescriptions.put(
        IssueType.BUNDLES_NO_LANGUAGE_SPLITTING,
        "Your App Bundle is not configured to split APKs by language. Consider enabling this"
            + " option for maximum app size reduction.");
    suggestionDescriptions.put(
        IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS,
        "Your App Bundle uses native libraries, but is not configured to utilize uncompressed"
            + " native libs. Consider enabling it for maximum app size reduction.");
    suggestionDescriptions.put(
        IssueType.LIBRARY_SHOULD_BE_EXTRACTED_TO_FEATURE_SPLITS,
        null);

    suggestionMoreInfos.put(
        IssueType.WEBP, "https://developer.android.com/studio/write/convert-webp");
    suggestionMoreInfos.put(IssueType.MEDIA_STREAMING, null);
    suggestionMoreInfos.put(
        IssueType.LARGE_FILES_DYNAMIC_FEATURE,
        "https://developer.android.com/studio/projects/dynamic-delivery");
    suggestionMoreInfos.put(IssueType.PROGUARD_NO_MAP, null);
    suggestionMoreInfos.put(IssueType.PROGUARD_EMPTY_MAP, null);
    suggestionMoreInfos.put(IssueType.PROGUARD_NO_SHRINKING, null);
    suggestionMoreInfos.put(IssueType.PROGUARD_NO_OBFUSCATION, null);
    suggestionMoreInfos.put(
        IssueType.QUESTIONABLE_FILE,
        "https://google.github.io/android-gradle-dsl/current/com.android.build.gradle.internal.dsl.PackagingOptions.html");
    suggestionMoreInfos.put(
        IssueType.BUNDLES_OLD_GRADLE_PLUGIN, "https://developer.android.com/guide/app-bundle/");
    suggestionMoreInfos.put(IssueType.BUNDLES_NO_ABI_SPLITTING, null);
    suggestionMoreInfos.put(IssueType.BUNDLES_NO_DENSITY_SPLITTING, null);
    suggestionMoreInfos.put(IssueType.BUNDLES_NO_LANGUAGE_SPLITTING, null);
    suggestionMoreInfos.put(IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS, null);
    suggestionMoreInfos.put(IssueType.NONOPTIMAL_LIBRARY, null);
    suggestionMoreInfos.put(IssueType.LIBRARY_SHOULD_BE_EXTRACTED_TO_FEATURE_SPLITS, null);

    suggestionAutoFixTitles.put(
        IssueType.WEBP, "Convert image to webp");
    suggestionAutoFixTitles.put(IssueType.MEDIA_STREAMING, null);
    suggestionAutoFixTitles.put(IssueType.LARGE_FILES_DYNAMIC_FEATURE, null);
    suggestionAutoFixTitles.put(IssueType.PROGUARD_NO_MAP, null);
    suggestionAutoFixTitles.put(IssueType.PROGUARD_EMPTY_MAP, null);
    suggestionAutoFixTitles.put(IssueType.PROGUARD_NO_SHRINKING, null);
    suggestionAutoFixTitles.put(IssueType.PROGUARD_NO_OBFUSCATION, null);
    suggestionAutoFixTitles.put(IssueType.QUESTIONABLE_FILE, null);
    suggestionAutoFixTitles.put(IssueType.BUNDLES_OLD_GRADLE_PLUGIN, null);
    suggestionAutoFixTitles.put(IssueType.BUNDLES_NO_ABI_SPLITTING, null);
    suggestionAutoFixTitles.put(IssueType.BUNDLES_NO_DENSITY_SPLITTING, null);
    suggestionAutoFixTitles.put(IssueType.BUNDLES_NO_LANGUAGE_SPLITTING, null);
    suggestionAutoFixTitles.put(IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS, null);
    suggestionAutoFixTitles.put(IssueType.NONOPTIMAL_LIBRARY, null);
    suggestionAutoFixTitles.put(IssueType.LIBRARY_SHOULD_BE_EXTRACTED_TO_FEATURE_SPLITS, null);
  }

  public SuggestionData fromSuggestion(Suggestion suggestion) {
    Pattern filePath;
    Matcher filePathMatcher;
    switch (suggestion.getIssueType()) {
      case QUESTIONABLE_FILE:
        filePath = Pattern.compile(".*?(File .* does not appear to be a needed file.).*");
        filePathMatcher = filePath.matcher(suggestion.getMessage());
        return new SuggestionData(
            suggestion,
            filePathMatcher.find() ? filePathMatcher.group(1) : null,
            suggestionDescriptions.getOrDefault(suggestion.getIssueType(), null),
            suggestionMoreInfos.getOrDefault(suggestion.getIssueType(), null),
            suggestionAutoFixTitles.getOrDefault(suggestion.getIssueType(), null));
      case NONOPTIMAL_LIBRARY:
        filePath = Pattern.compile(".*?(Library Dependency: .* can be replaced).*");
        filePathMatcher = filePath.matcher(suggestion.getMessage());
        return new SuggestionData(
            suggestion,
            filePathMatcher.find() ? filePathMatcher.group(1) : null,
            suggestion.getMessage(),
            suggestionMoreInfos.getOrDefault(suggestion.getIssueType(), null),
            suggestionAutoFixTitles.getOrDefault(suggestion.getIssueType(), null));
      case LIBRARY_SHOULD_BE_EXTRACTED_TO_FEATURE_SPLITS:
        List<LibraryEligibleForFeatureSplit> libraryUsages =
            suggestion.payload().getLibraryForFeatureSplitUsage().getLibraryUsageList();
        String libraryUsagesString = "";
        for (LibraryEligibleForFeatureSplit library : libraryUsages) {
          switch (library.getLibrary()) {
            case CARD_IO:
              libraryUsagesString += " io.card:android-sdk";
              break;
            default:
              libraryUsagesString += " " + library.getLibrary();
          }
        }
        return new SuggestionData(
            suggestion,
            "Library Dependencies may be better used on demand",
            "You may want to consider installing and uninstalling these libraries on demand:"
                + libraryUsagesString
                + ". Install them only when they're needed and uninstall them after use so that"
                + " they don't occupy space on the user's device when they're not needed.",
            suggestionMoreInfos.getOrDefault(suggestion.getIssueType(), null),
            suggestionAutoFixTitles.getOrDefault(suggestion.getIssueType(), null));
      default:
        return new SuggestionData(
            suggestion,
            suggestionTitles.getOrDefault(suggestion.getIssueType(), null),
            suggestionDescriptions.getOrDefault(suggestion.getIssueType(), null),
            suggestionMoreInfos.getOrDefault(suggestion.getIssueType(), null),
            suggestionAutoFixTitles.getOrDefault(suggestion.getIssueType(), null));
    }
  }
}
