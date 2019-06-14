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

package com.android.tools.sizereduction.analyzer.suggesters.bundles;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.android.bundle.Config.SplitDimension;
import com.android.bundle.Files.NativeLibraries;
import com.android.bundle.Targeting.Abi.AbiAlias;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.BundleSplittingData;
import com.android.tools.sizereduction.analyzer.model.AndroidPluginVersion;
import com.android.tools.sizereduction.analyzer.model.BundleConfig;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * Analyzes whether the Bundle splits over architecture, screen density and language, for maximum
 * size reduction
 */
public final class BundleSplitSuggester implements BundleSuggester, ProjectSuggester {


  @VisibleForTesting
  static final String NO_ABI_SPLITTING_MESSAGE =
      "Your App Bundle is not configured to split APKs by ABI, consider enabling it for maximum "
          + "app size reduction.";

  @VisibleForTesting
  static final String NO_DISPLAY_DENSITY_SPLITTING_MESSAGE =
      "Your App Bundle is not configured to split APKs by screen density, consider enabling it for "
          + "maximum app size reduction.";

  @VisibleForTesting
  static final String NO_LANGUAGE_SPLITTING_MESSAGE =
      "Your App Bundle is not configured to split APKs by languages, consider enabling it for "
          + "maximum app size reduction.";

  @VisibleForTesting
  static final String OLD_GRADLE_PLUGIN_MESSAGE =
      "Consider upgrading to the Android Gradle plugin version 3.2 or later to use Android App "
          + "Bundles. This will likely offer significant app size savings. To learn more, visit "
          + "https://developer.android.com/guide/app-bundle/.";

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    ImmutableSet<SplitDimension.Value> splitDimensionsNotUsed =
        bundle.getBundleConfig().getOptimizations().getSplitsConfig().getSplitDimensionList()
            .stream()
            .filter(SplitDimension::getNegate)
            .map(SplitDimension::getValue)
            .collect(toImmutableSet());
    ImmutableList.Builder<Suggestion> suggestions = ImmutableList.builder();

    if (splitDimensionsNotUsed.contains(SplitDimension.Value.ABI)) {
      // Set of ABIs that native libraries in the bundle target.
      ImmutableSet<AbiAlias> abis =
          bundle.getModules().values().stream()
              .map(BundleModule::getNativeConfig)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .flatMap(BundleSplitSuggester::getUsedAbis)
              .collect(toImmutableSet());

      if (!abis.isEmpty()) {
        suggestions.add(
            Suggestion.create(
                IssueType.BUNDLES_NO_ABI_SPLITTING,
                Category.BUNDLE_CONFIG,
                Payload.newBuilder()
                    .setBundleSplittingData(
                        BundleSplittingData.newBuilder()
                            .addAllAbis(
                                abis.stream().map(AbiAlias::toString).collect(toImmutableList())))
                    .build(),
                NO_ABI_SPLITTING_MESSAGE,
                /* estimatedBytesSaved= */ null,
                /* autoFix= */ null));
      }
    }

    if (splitDimensionsNotUsed.contains(SplitDimension.Value.SCREEN_DENSITY)) {
      suggestions.add(
          Suggestion.create(
              IssueType.BUNDLES_NO_DENSITY_SPLITTING,
              Category.BUNDLE_CONFIG,
              Payload.getDefaultInstance(),
              NO_DISPLAY_DENSITY_SPLITTING_MESSAGE,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }

    if (splitDimensionsNotUsed.contains(SplitDimension.Value.LANGUAGE)) {
      suggestions.add(
          Suggestion.create(
              IssueType.BUNDLES_NO_LANGUAGE_SPLITTING,
              Category.BUNDLE_CONFIG,
              Payload.getDefaultInstance(),
              NO_LANGUAGE_SPLITTING_MESSAGE,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }

    return suggestions.build();
  }

  @Override
  public ImmutableList<Suggestion> processProject(GradleContext context, File projectDir) {
    ImmutableList.Builder<Suggestion> suggestions = ImmutableList.builder();
    if (context.getPluginType() != GradleContext.PluginType.APPLICATION) {
      return suggestions.build();
    }

    AndroidPluginVersion pluginVersion = context.getAndroidPluginVersion();
    if (pluginVersion != null) {
      System.out.println(pluginVersion.getMajorVersion());
      System.out.println(pluginVersion.getMinorVersion());
    }
    System.out.println(pluginVersion);
    if (pluginVersion != null
        && pluginVersion.compareTo(AndroidPluginVersion.create("3.2.0")) < 0) {
      suggestions.add(
          Suggestion.create(
              IssueType.BUNDLES_OLD_GRADLE_PLUGIN,
              Category.BUNDLE_CONFIG,
              Payload.getDefaultInstance(),
              OLD_GRADLE_PLUGIN_MESSAGE,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
      return suggestions.build();
    }

    BundleConfig bundleConfig = context.getBundleConfig();
    if (!bundleConfig.getAbiSplitEnabled()) {
      suggestions.add(
          Suggestion.create(
              IssueType.BUNDLES_NO_ABI_SPLITTING,
              Category.BUNDLE_CONFIG,
              Payload.getDefaultInstance(),
              NO_ABI_SPLITTING_MESSAGE,
              /* estimatedBytesSaved= */ null,
              new BundleSplitAutoFix(
                  projectDir,
                  context.getBundleConfig().getBundleConfigLocation().getAbiSplitLineNumber())));
    }

    if (!bundleConfig.getDensitySplitEnabled()) {
      suggestions.add(
          Suggestion.create(
              IssueType.BUNDLES_NO_DENSITY_SPLITTING,
              Category.BUNDLE_CONFIG,
              Payload.getDefaultInstance(),
              NO_DISPLAY_DENSITY_SPLITTING_MESSAGE,
              /* estimatedBytesSaved= */ null,
              new BundleSplitAutoFix(
                  projectDir,
                  context
                      .getBundleConfig()
                      .getBundleConfigLocation()
                      .getDensitySplitLineNumber())));
    }

    if (!bundleConfig.getLanguageSplitEnabled()) {
      suggestions.add(
          Suggestion.create(
              IssueType.BUNDLES_NO_LANGUAGE_SPLITTING,
              Category.BUNDLE_CONFIG,
              Payload.getDefaultInstance(),
              NO_LANGUAGE_SPLITTING_MESSAGE,
              /* estimatedBytesSaved= */ null,
              new BundleSplitAutoFix(
                  projectDir,
                  context
                      .getBundleConfig()
                      .getBundleConfigLocation()
                      .getLanguageSplitLineNumber())));
    }

    return suggestions.build();
  }

  private static Stream<AbiAlias> getUsedAbis(NativeLibraries libs) {
    return libs.getDirectoryList().stream()
        .map(directory -> directory.getTargeting().getAbi().getAlias());
  }

}
