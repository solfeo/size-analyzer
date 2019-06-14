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

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LargeFilesInBaseModule;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LargeFilesInBaseModule.FileInfo;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.LargeFilesInBaseModule.FileType;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/** Suggester to find the heaviest files in the base module of the bundle. */
public final class LargeFilesInBaseModuleSuggester implements BundleSuggester {

  @VisibleForTesting
  static final String LARGE_ASSETS_AND_RESOURCES_IN_BASE_MESSAGE =
      "Avoid delivering all of your assets and resources at install time if they aren't needed"
          + " immediately. Reduce your app's install size by placing the largest files in dynamic"
          + " feature module(s) and delivering them conditionally or on demand, or consider"
          + " alternatives such as streaming media files.";

  private final int numberOfLargestFilesInBaseToReport;
  private final long minimumFileInBaseSizeForReporting;

  public LargeFilesInBaseModuleSuggester(
      int numberOfLargestFilesInBaseToReport, long minimumFileInBaseSizeForReporting) {
    this.numberOfLargestFilesInBaseToReport = numberOfLargestFilesInBaseToReport;
    this.minimumFileInBaseSizeForReporting = minimumFileInBaseSizeForReporting;
  }

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    BundleModule base = bundle.getBaseModule();
    return suggestionFromListOfLargeFiles(
        Streams.concat(
                largestModuleEntriesInPath(
                    bundleZip, base, BundleModule.ASSETS_DIRECTORY, FileType.ASSET),
                largestModuleEntriesInPath(
                    bundleZip, base, BundleModule.RESOURCES_DIRECTORY, FileType.RESOURCE),
                largestModuleEntriesInPath(
                    bundleZip, base, BundleModule.ROOT_DIRECTORY, FileType.ROOT_DIRECTORY_FILE))
            .sorted(Comparator.comparingLong(FileInfo::getSize).reversed())
            .limit(numberOfLargestFilesInBaseToReport)
            .collect(toImmutableList()));
  }

  private Stream<FileInfo> largestModuleEntriesInPath(
      ZipFile bundle, BundleModule module, ZipPath directory, FileType type) {
    return module
        .findEntriesUnderPath(directory)
        .map(
            entry ->
                FileInfo.newBuilder()
                    .setType(type)
                    .setSize(moduleEntrySize(bundle, module, entry))
                    .setFinalSplitApkPath(moduleEntryRelativePath(entry, type))
                    .build())
        .filter(file -> Range.atLeast(minimumFileInBaseSizeForReporting).contains(file.getSize()))
        .sorted(Comparator.comparingLong(FileInfo::getSize).reversed())
        .limit(numberOfLargestFilesInBaseToReport);
  }

  private static long moduleEntrySize(ZipFile bundle, BundleModule module, ModuleEntry entry) {
    return bundle.getEntry(moduleEntryFullPath(module, entry)).getCompressedSize();
  }

  private static String moduleEntryFullPath(BundleModule module, ModuleEntry entry) {
    return ZipPath.create(module.getName().toString()).resolve(entry.getPath()).toString();
  }

  private static String moduleEntryRelativePath(ModuleEntry entry, FileType type) {
    // Root files should not have the 'root/' prefix since they will ultimately be in the root of
    // the APK file (hence the name root).
    // Therefore, in order to not confuse the end users, 'root/' is removed from the path.
    if (FileType.ROOT_DIRECTORY_FILE.equals(type)) {
      return entry.getPath().subpath(1, entry.getPath().getNameCount()).toString();
    }

    return entry.getPath().toString();
  }

  private static ImmutableList<Suggestion> suggestionFromListOfLargeFiles(
      ImmutableList<FileInfo> largeFiles) {
    if (largeFiles.isEmpty()) {
      return ImmutableList.of();
    }

    return ImmutableList.of(
        Suggestion.create(
            IssueType.BUNDLE_BASE_LARGE_FILES,
            Category.BUNDLE_BASE,
            Payload.newBuilder()
                .setLargeFilesInBaseModule(
                    LargeFilesInBaseModule.newBuilder().addAllFiles(largeFiles))
                .build(),
            LARGE_ASSETS_AND_RESOURCES_IN_BASE_MESSAGE,
            /* estimatedBytesSaved= */ null,
            /* autoFix= */ null));
  }
}
