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
  public ImmutableList<Suggestion> processBundle(BundleContext context, ZipFile bundle) {
    ZipEntry proguardEntry = bundle.getEntry(PROGUARD_MAP);

    if (proguardEntry == null) {
      return ImmutableList.of(
          Suggestion.create(
              IssueType.PROGUARD_NO_MAP,
              Category.PROGUARD,
              NO_MAP_SUGGESTION_MESSAGE,
              /* estimatedBytesSaved= */ null));
    }

    // Deobfuscation map present.
    if (proguardEntry.getSize() == 0) {
      // Empty deobfuscation map.
      return ImmutableList.of(
          Suggestion.create(
              IssueType.PROGUARD_EMPTY_MAP,
              Category.PROGUARD,
              EMPTY_MAP_SUGGESTION_MESSAGE,
              /* estimatedBytesSaved= */ null));
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
              NO_CODE_SHRINKING,
              /* estimatedBytesSaved= */ null),
          Suggestion.create(
              IssueType.PROGUARD_NO_OBFUSCATION,
              Category.PROGUARD,
              NO_OBFUSCATION,
              /* estimatedBytesSaved= */ null));
    }
    ImmutableList.Builder<Suggestion> suggestions = ImmutableList.<Suggestion>builder();
    if (!proguardConfig.getMinifyEnabled()) {
      suggestions.add(
          Suggestion.create(
              IssueType.PROGUARD_NO_SHRINKING,
              Category.PROGUARD,
              NO_CODE_SHRINKING,
              /* estimatedBytesSaved= */ null));
    }
    if (!proguardConfig.getObfuscationEnabled()) {
      suggestions.add(
          Suggestion.create(
              IssueType.PROGUARD_NO_OBFUSCATION,
              Category.PROGUARD,
              NO_OBFUSCATION,
              /* estimatedBytesSaved= */ null));
    }
    return suggestions.build();
  }
}
