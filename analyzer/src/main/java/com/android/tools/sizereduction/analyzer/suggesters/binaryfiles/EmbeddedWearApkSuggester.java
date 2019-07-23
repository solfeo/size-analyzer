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

package com.android.tools.sizereduction.analyzer.suggesters.binaryfiles;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.Context;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.zip.ZipFile;

/** Suggests move out embedded wear APK. */
public final class EmbeddedWearApkSuggester implements BundleSuggester, ProjectSuggester {

  @VisibleForTesting
  static final String EMBEDDED_WEAR_APK_SUGGESTION_CONTENT =
      "Wear 1.x APKs no longer need to be embedded into phone/tablet APKs. Please visit"
          + " https://developer.android.com/training/wearables/apps/packaging#migrating"
          + " to learn more.";

  public EmbeddedWearApkSuggester() {}

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    return suggestRemoveEmbeddedWearApk(context);
  }

  @Override
  public ImmutableList<Suggestion> processProject(GradleContext context, File projectDir) {
    return suggestRemoveEmbeddedWearApk(context);
  }

  private static ImmutableList<Suggestion> suggestRemoveEmbeddedWearApk(Context context) {
    // TODO(b/135135523): add size saving for bundle.
    // TODO(b/135446210): add auto-fix.
    if (context.getEmbedsWearApk()) {
      return ImmutableList.of(
          Suggestion.create(
              Suggestion.IssueType.EMBEDDED_WEAR_APK,
              Suggestion.Category.LARGE_FILES,
              Payload.getDefaultInstance(),
              EMBEDDED_WEAR_APK_SUGGESTION_CONTENT,
              /* estimatedBytesSaved= */ null,
              /* autoFix= */ null));
    }
    return ImmutableList.of();
  }
}
