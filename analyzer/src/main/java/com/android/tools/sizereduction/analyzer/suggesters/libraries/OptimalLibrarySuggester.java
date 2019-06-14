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

import com.android.bundle.AppDependenciesOuterClass.AppDependencies;
import com.android.bundle.AppDependenciesOuterClass.Library;
import com.android.bundle.AppDependenciesOuterClass.MavenLibrary;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.InputStreamSupplier;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

/**
 * Suggester to find whether the App Bundle uses libraries that should be replaced with a different
 * library or a new version.
 */
public final class OptimalLibrarySuggester implements BundleSuggester, ProjectSuggester {

  @VisibleForTesting
  static final ImmutableMap<MavenLibrary, ReplacementLibrary> LIBRARY_REPLACEMENTS =
      ImmutableMap.of(
          MavenLibrary.newBuilder()
              .setGroupId("com.google.android.gms")
              .setArtifactId("play-services-ads")
              .build(),
          ReplacementLibrary.builder()
              .setMavenLibrary(
                  MavenLibrary.newBuilder()
                      .setGroupId("com.google.android.gms")
                      .setArtifactId("play-services-ads-lite")
                      .build())
              .setExtendedSuggestionMessage(
                  " When uploading apps to the Play Store, the ads-lite library can be safely used:"
                      + " https://developers.google.com/admob/android/lite-sdk.")
              .setEstimatedBytesSaved(500L * 1000L)
              .build());

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    Optional<AppDependencies> appDependenciesOptional = readAppDependencies(bundle);
    if (!appDependenciesOptional.isPresent()) {
      return ImmutableList.of();
    }

    AppDependencies appDependencies = appDependenciesOptional.get();
    return checkLibraries(ImmutableList.copyOf(appDependencies.getLibraryList()));
  }

  @Override
  public ImmutableList<Suggestion> processProject(GradleContext context, File projectDir) {
    return checkLibraries(context.getLibraryDependencies());
  }

  private static ImmutableList<Suggestion> checkLibraries(ImmutableCollection<Library> libraries) {
    return libraries.stream()
        .filter(Library::hasMavenLibrary)
        .map(Library::getMavenLibrary)
        .map(OptimalLibrarySuggester::getSuggestion)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toImmutableList());
  }

  private static Optional<Suggestion> getSuggestion(MavenLibrary mavenLibrary) {
    for (Map.Entry<MavenLibrary, ReplacementLibrary> libraryReplacement :
        LIBRARY_REPLACEMENTS.entrySet()) {
      MavenLibrary nonoptimalLibrary = libraryReplacement.getKey();
      if (mavenLibrary.getGroupId().equals(nonoptimalLibrary.getGroupId())
          && mavenLibrary.getArtifactId().equals(nonoptimalLibrary.getArtifactId())) {
        return Optional.of(
            Suggestion.create(
                IssueType.NONOPTIMAL_LIBRARY,
                Category.LIBRARIES,
                Payload.getDefaultInstance(),
                getSuggestionMessage(libraryReplacement.getKey(), libraryReplacement.getValue()),
                libraryReplacement.getValue().getEstimatedBytesSaved(),
                /* autoFix= */ null));
      }
    }
    return Optional.empty();
  }

  @VisibleForTesting
  static String getSuggestionMessage(MavenLibrary library, ReplacementLibrary optimalLibrary) {
    return "Library Dependency: "
        + library.getGroupId()
        + ":"
        + library.getArtifactId()
        + " can be replaced safely with "
        + optimalLibrary.getMavenLibrary().getGroupId()
        + ":"
        + optimalLibrary.getMavenLibrary().getArtifactId()
        + " to reduce the size of your application."
        + optimalLibrary.getExtendedSuggestionMessage();
  }

  private static Optional<AppDependencies> readAppDependencies(AppBundle bundle) {
    Optional<InputStreamSupplier> inputStreamSupplier =
        bundle
            .getBundleMetadata()
            .getFileData("com.android.tools.build.libraries", "dependencies.pb");

    if (!inputStreamSupplier.isPresent()) {
      return Optional.empty();
    }
    try (InputStream entryContent = inputStreamSupplier.get().get()) {
      return Optional.of(AppDependencies.parseDelimitedFrom(entryContent));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The replacement maven library that a developer should use. */
  @AutoValue
  abstract static class ReplacementLibrary {

    /** The maven library for this replacement library. */
    public abstract MavenLibrary getMavenLibrary();

    /** The extended message information to show the developer regarding this library. */
    public abstract String getExtendedSuggestionMessage();

    /** Get estimated size savings for using this library. */
    @Nullable
    public abstract Long getEstimatedBytesSaved();

    public static Builder builder() {
      // by default, all the splits are enabled.
      return new AutoValue_OptimalLibrarySuggester_ReplacementLibrary.Builder();
    }

    /** Builder for the {@link ReplacementLibrary}. */
    @AutoValue.Builder
    abstract static class Builder {

      /** Sets the maven library information. */
      public abstract Builder setMavenLibrary(MavenLibrary mavenLibrary);

      /** Sets the extended message information to show the developer regarding this library. */
      public abstract Builder setExtendedSuggestionMessage(String extendedSuggestionMessage);

      /** Sets estimated size savings for using this library. */
      public abstract Builder setEstimatedBytesSaved(Long estimatedBytesSaved);

      /** Build the ReplacementLibrary object. */
      public abstract ReplacementLibrary build();
    }
  }
}
