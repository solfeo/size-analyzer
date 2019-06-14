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

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.version.Version;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.UncompressedNativeLibsUsage;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.UncompressedNativeLibsUsage.Status;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.zip.ZipFile;

/** Analyzes whether the Bundle enables uncompressed native libs */
public final class UncompressedNativeLibsSuggester implements BundleSuggester {

  @VisibleForTesting
  static final String NO_UNCOMPRESSED_NATIVE_LIBS_MESSAGE =
      "Your App Bundle uses native libraries, but is not configured to utilize uncompressed native "
          + "libs. Consider enabling it for maximum app size reduction.";

  @VisibleForTesting
  static final String OLD_BUNDLETOOL_USE_NEW_FOR_UNCOMPRESSED_NATIVE_LIBS_MESSAGE =
      "Your App Bundle was built using an old version of Bundletool. Switch to the newest version "
          + "and enable uncompressed native libraries for maximum app size reduction.";

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    boolean oldBundletool =
        Version.of(bundle.getBundleConfig().getBundletool().getVersion())
            .isOlderThan(Version.of("0.6.0"));

    boolean uncompressedNativeLibsEnabled =
        bundle.getBundleConfig().getOptimizations().getUncompressNativeLibraries().getEnabled();

    boolean hasNativeLibs =
        bundle.getModules().values().stream()
            .map(BundleModule::getNativeConfig)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(
                libs ->
                    libs.getDirectoryList().stream()
                        .anyMatch(directory -> directory.getTargeting().hasAbi()));


    if (!hasNativeLibs) {
      // If there are no native libraries in the bundle it makes no sense to suggest anything.
      return ImmutableList.of();
    }

    if (oldBundletool) {
      return ImmutableList.of(
          uncompressedNativeLibsSuggestion(
              OLD_BUNDLETOOL_USE_NEW_FOR_UNCOMPRESSED_NATIVE_LIBS_MESSAGE, Status.OLD_BUNDLETOOL));
    } else if (!uncompressedNativeLibsEnabled) {
      return ImmutableList.of(
          uncompressedNativeLibsSuggestion(
              NO_UNCOMPRESSED_NATIVE_LIBS_MESSAGE, Status.UNCOMPRESSED_NATIVE_LIBS_NOT_ENABLED));
    }

    return ImmutableList.of();
  }

  private static Suggestion uncompressedNativeLibsSuggestion(String message, Status status) {
    return Suggestion.create(
        IssueType.BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS,
        Category.BUNDLE_CONFIG,
        Payload.newBuilder()
            .setUncompressedNativeLibsUsage(
                UncompressedNativeLibsUsage.newBuilder().setStatus(status))
            .build(),
        message,
        /* estimatedBytesSaved= */ null,
        /* autoFix= */ null);
  }
}
