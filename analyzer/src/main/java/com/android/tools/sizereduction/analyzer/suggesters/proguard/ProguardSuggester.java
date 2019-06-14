/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tools.sizereduction.analyzer.suggesters.proguard;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.ProguardData;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.model.ProguardConfig;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Analyzes Proguard usage of a ZIP artifact (should be used on App Bundles only). */
public final class ProguardSuggester implements BundleSuggester, ProjectSuggester {


  private static final String PROGUARD_MAP =
      "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map";

  @VisibleForTesting
  static final String NO_MAP_SUGGESTION_MESSAGE =
      "It seems that you are not using Proguard/R8, consider enabling it in your application.";

  @VisibleForTesting
  static final String EMPTY_MAP_SUGGESTION_MESSAGE =
      "Your application is not using Proguard or R8 obfuscation, consider enabling it to save "
          + "space.";

  @VisibleForTesting
  static final String NO_CODE_SHRINKING =
      "It seems that you are not using Proguard/R8, consider enabling it in your application.";

  @VisibleForTesting
  static final String NO_OBFUSCATION =
      "Your application is not using Proguard or R8 obfuscation, consider enabling it to save "
          + "space.";

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    // Some old bundles contain multidex code in a way not compatible with the new AppBundle
    // representation, so the extraction of ZIP entries results in null pointers. Hence, entries are
    // wrapped in optionals to precisely capture the nulls.
    ImmutableList<Optional<ZipEntry>> dexFileEntries =
        bundle.getModules().values().stream()
            .flatMap(
                module ->
                    module
                        .findEntriesUnderPath(BundleModule.DEX_DIRECTORY)
                        .map(ModuleEntry::getPath)
                        .map(ZipPath.create(module.getName().toString())::resolve))
            .map(ZipPath::toString)
            .map(bundleZip::getEntry)
            .map(Optional::ofNullable)
            .collect(toImmutableList());

    OptionalLong totalDex =
        dexFileEntries.stream().anyMatch(not(Optional::isPresent))
            ? OptionalLong.empty()
            : OptionalLong.of(
                dexFileEntries.stream().map(Optional::get).mapToLong(ZipEntry::getSize).sum());

    ZipEntry proguardEntry = bundleZip.getEntry(PROGUARD_MAP);

    if (proguardEntry == null) {
      return ImmutableList.of(
          Suggestion.create(
              IssueType.PROGUARD_NO_MAP,
              Category.PROGUARD,
              totalDexPayload(totalDex),
              NO_MAP_SUGGESTION_MESSAGE,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }

    // Deobfuscation map present.
    if (proguardEntry.getSize() == 0) {
      // Empty deobfuscation map.
      return ImmutableList.of(
          Suggestion.create(
              IssueType.PROGUARD_EMPTY_MAP,
              Category.PROGUARD,
              totalDexPayload(totalDex),
              EMPTY_MAP_SUGGESTION_MESSAGE,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }

    // Everything is fine with the map, no suggestions.
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<Suggestion> processProject(GradleContext context, File projectDir) {
    if (context.getPluginType() != GradleContext.PluginType.APPLICATION) {
      // dynamic-features and root build.gradle files do not contain the proguard configuration
      // being used.
      return ImmutableList.of();
    }
    ImmutableMap<String, ProguardConfig> proguardConfigs = context.getProguardConfigs();
    ProguardConfig proguardConfig =
        proguardConfigs.getOrDefault(
            "release", proguardConfigs.getOrDefault(ProguardConfig.DEFAULT_CONFIG_NAME, null));
    if (proguardConfig == null || !proguardConfig.getHasProguardRules()) {
      return ImmutableList.of(
          Suggestion.create(
              IssueType.PROGUARD_NO_SHRINKING,
              Category.PROGUARD,
              Payload.getDefaultInstance(),
              NO_CODE_SHRINKING,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null),
          Suggestion.create(
              IssueType.PROGUARD_NO_OBFUSCATION,
              Category.PROGUARD,
              Payload.getDefaultInstance(),
              NO_OBFUSCATION,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }
    ImmutableList.Builder<Suggestion> suggestions = ImmutableList.<Suggestion>builder();
    if (!proguardConfig.getMinifyEnabled()) {
      suggestions.add(
          Suggestion.create(
              IssueType.PROGUARD_NO_SHRINKING,
              Category.PROGUARD,
              Payload.getDefaultInstance(),
              NO_CODE_SHRINKING,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }
    if (!proguardConfig.getObfuscationEnabled()) {
      suggestions.add(
          Suggestion.create(
              IssueType.PROGUARD_NO_OBFUSCATION,
              Category.PROGUARD,
              Payload.getDefaultInstance(),
              NO_OBFUSCATION,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }
    return suggestions.build();
  }

  private static Payload totalDexPayload(OptionalLong totalDex) {
    if (!totalDex.isPresent()) {
      // Incorrect multidex usage, no payload can be provided.
      return Payload.getDefaultInstance();
    }

    return Payload.newBuilder()
        .setProguardData(ProguardData.newBuilder().setTotalDexSize(totalDex.getAsLong()))
        .build();
  }
}
