/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tools.sizereduction.analyzer.suggesters.libraries;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.BundleModuleName;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LibraryForFeatureSplitUsage;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LibraryForFeatureSplitUsage.LibraryEligibleForFeatureSplit;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LibraryForFeatureSplitUsage.LibraryEligibleForFeatureSplit.Library;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LibraryForFeatureSplitUsage.LibraryEligibleForFeatureSplit.LibraryUsage;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

/**
 * Suggester to find whether the App Bundle uses libraries that might be good candidates for
 * extraction to a feature module; currently only card-io is analyzed.
 */
public final class LibraryEligibleForFeatureSplitSuggester
    implements BundleSuggester, ProjectSuggester {

  @VisibleForTesting
  static final String LIBRARIES_IN_FEATURE_MODULES =
      "Install and uninstall some libraries on demand; install only when they're needed and"
          + " uninstall it after use so that it doesn't occupy space on the user's device when"
          + " it's not needed.";

  static final String CARD_IO_GROUP = "io.card";
  static final String CARD_IO_NAME = "android-sdk";

  private static final ImmutableSet<String> CARD_IO_LIBRARY_FILES =
      ImmutableSet.of(
          "libcardioDecider.so", "libcardioRecognizer.so", "libcardioRecognizer_tegra2.so");

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    ImmutableMap<Library, Function<BundleRepresentation, ImmutableList<String>>> libraryDetectors =
        getDetectorsForFeatureModuleCandidateLibraries();

    AppBundle appBundle = AppBundle.buildFromZip(bundleZip);
    BundleRepresentation representation = BundleRepresentation.create(bundleZip, appBundle);

    ImmutableMap<Library, ImmutableList<String>> libraryUsageToModules =
        ImmutableMap.copyOf(
            Maps.transformValues(libraryDetectors, detector -> detector.apply(representation)));

    return suggestionsFromLibraryUsages(libraryUsageToModules);
  }

  @Override
  public ImmutableList<Suggestion> processProject(GradleContext context, File projectDir) {
    // Bail out early if on demand. This means the library won't effect initial download size
    if (context.getOnDemand()) {
      return ImmutableList.of();
    }
    ImmutableMap<Library, Function<ProjectRepresentation, ImmutableList<String>>> libraryDetectors =
        getDetectorsForFeatureModuleCandidateLibrariesForProject();

    ProjectRepresentation representation = ProjectRepresentation.create(context, projectDir);

    ImmutableMap<Library, ImmutableList<String>> libraryUsageToModules =
        ImmutableMap.copyOf(
            Maps.transformValues(libraryDetectors, detector -> detector.apply(representation)));
    return suggestionsFromLibraryUsages(libraryUsageToModules);
  }

  /**
   * Returns a map of functions that for a specific library, given a bundle, return a list of
   * modules using the library in question.
   */
  @VisibleForTesting
  ImmutableMap<Library, Function<BundleRepresentation, ImmutableList<String>>>
      getDetectorsForFeatureModuleCandidateLibraries() {
    return ImmutableMap.of(Library.CARD_IO, LibraryEligibleForFeatureSplitSuggester::cardIoModules);
  }

  /**
   * Returns a map of functions that for a specific library, given a project, return a list of
   * modules that are either the base or an non-ondemand dynamic feature using the library in
   * question.
   */
  private ImmutableMap<Library, Function<ProjectRepresentation, ImmutableList<String>>>
      getDetectorsForFeatureModuleCandidateLibrariesForProject() {
    return ImmutableMap.of(
        Library.CARD_IO, LibraryEligibleForFeatureSplitSuggester::cardIoModulesForProjects);
  }

  /** Provides list of modules in a bundle that use the card-io library */
  private static ImmutableList<String> cardIoModules(BundleRepresentation bundle) {
    return bundle.appBundle().getModules().values().stream()
        .filter(LibraryEligibleForFeatureSplitSuggester::moduleHasCardIoFiles)
        .map(BundleModule::getName)
        .map(BundleModuleName::getName)
        .collect(toImmutableList());
  }

  /** Provides list of modules that are base or non-ondeman dynamic that use the card-io library */
  private static ImmutableList<String> cardIoModulesForProjects(ProjectRepresentation project) {
    if (containsCardIoLibraries(project)) {
      if (project.gradleContext().getPluginType() == GradleContext.PluginType.APPLICATION) {
        return ImmutableList.of(BundleModuleName.BASE_MODULE_NAME.getName());
      } else if (project.gradleContext().getPluginType()
          == GradleContext.PluginType.DYNAMIC_FEATURE) {
        return ImmutableList.of(project.projectDir().getName());
      }
    }

    return ImmutableList.of();
  }

  private static boolean containsCardIoLibraries(ProjectRepresentation project) {
    return project.gradleContext().getLibraryDependencies().stream()
        .map(com.android.bundle.AppDependenciesOuterClass.Library::getMavenLibrary)
        .anyMatch(LibraryEligibleForFeatureSplitSuggester::mavenLibraryIsCardIo);
  }

  private static boolean mavenLibraryIsCardIo(
      com.android.bundle.AppDependenciesOuterClass.MavenLibrary mavenLibrary) {
    return mavenLibrary.getGroupId().equals(CARD_IO_GROUP)
        && mavenLibrary.getArtifactId().equals(CARD_IO_NAME);
  }

  private static boolean moduleHasCardIoFiles(BundleModule module) {
    return module
        .findEntriesUnderPath(BundleModule.LIB_DIRECTORY)
        .map(ModuleEntry::getPath)
        .anyMatch(path -> CARD_IO_LIBRARY_FILES.contains(path.getFileName().toString()));
  }

  private static ImmutableList<Suggestion> suggestionsFromLibraryUsages(
      ImmutableMap<Library, ImmutableList<String>> libraryUsage) {
    ImmutableList<LibraryEligibleForFeatureSplit> libraryUsages =
        libraryUsage.entrySet().stream()
            .map(
                singleLibraryUsage ->
                    LibraryEligibleForFeatureSplit.newBuilder()
                        .setLibrary(singleLibraryUsage.getKey())
                        .setUsage(
                            getLibraryUsageStatusFromModuleList(singleLibraryUsage.getValue()))
                        .build())
            .collect(toImmutableList());

    if (libraryUsages.stream()
        .map(LibraryEligibleForFeatureSplit::getUsage)
        .allMatch(Predicate.isEqual(LibraryUsage.LIBRARY_NOT_USED))) {
      return ImmutableList.of();
    }

    return ImmutableList.of(
        Suggestion.create(
            IssueType.LIBRARY_SHOULD_BE_EXTRACTED_TO_FEATURE_SPLITS,
            Category.LIBRARIES,
            Payload.newBuilder()
                .setLibraryForFeatureSplitUsage(
                    LibraryForFeatureSplitUsage.newBuilder().addAllLibraryUsage(libraryUsages))
                .build(),
            LIBRARIES_IN_FEATURE_MODULES,
            /* estimatedBytesSaved= */ null,
            /* autoFix= */ null));
  }

  /**
   * Inspects if any of the modules that use a library is the base module, if any modules use the
   * library at all
   */
  private static LibraryUsage getLibraryUsageStatusFromModuleList(ImmutableList<String> modules) {
    if (modules.isEmpty()) {
      return LibraryUsage.LIBRARY_NOT_USED;
    }

    return modules.contains(BundleModuleName.BASE_MODULE_NAME.getName())
        ? LibraryUsage.LIBRARY_IN_BASE
        : LibraryUsage.LIBRARY_IN_FEATURE_MODULES_ONLY;
  }

  @AutoValue
  abstract static class BundleRepresentation {
    static BundleRepresentation create(ZipFile bundleZipFile, AppBundle appBundle) {
      return new AutoValue_LibraryEligibleForFeatureSplitSuggester_BundleRepresentation(
          bundleZipFile, appBundle);
    }

    abstract ZipFile bundleZipFile();

    abstract AppBundle appBundle();
  }

  @AutoValue
  abstract static class ProjectRepresentation {
    static ProjectRepresentation create(GradleContext context, File projectDir) {
      return new AutoValue_LibraryEligibleForFeatureSplitSuggester_ProjectRepresentation(
          context, projectDir);
    }

    abstract GradleContext gradleContext();

    abstract File projectDir();
  }
}
