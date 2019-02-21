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

package com.android.tools.sizereduction.analyzer.cli;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class TerminalInterfaceTest {

  private static final ImmutableList<Suggestion> ALL_SUGGESTIONS =
      ImmutableList.of(
          Suggestion.create(
              Suggestion.IssueType.WEBP,
              Suggestion.Category.WEBP,
              "Suggestion 1",
              /* estimatedBytesSaved= */ 3L),
          Suggestion.create(
              Suggestion.IssueType.PROGUARD_NO_OBFUSCATION,
              Suggestion.Category.PROGUARD,
              "Suggestion 2",
              /* estimatedBytesSaved= */ null),
          Suggestion.create(
              Suggestion.IssueType.LARGE_FILES_DYNAMIC_FEATURE,
              Suggestion.Category.LARGE_FILES,
              "Suggestion 3",
              /* estimatedBytesSaved= */ 200L),
          Suggestion.create(
              Suggestion.IssueType.WEBP,
              Suggestion.Category.WEBP,
              "Suggestion 4",
              /* estimatedBytesSaved= */ 15000L),
          Suggestion.create(
              Suggestion.IssueType.WEBP,
              Suggestion.Category.WEBP,
              "Suggestion 5",
              /* estimatedBytesSaved= */ null),
          Suggestion.create(
              Suggestion.IssueType.QUESTIONABLE_FILE,
              Suggestion.Category.LARGE_FILES,
              "Suggestion 6",
              /* estimatedBytesSaved= */ 3000L),
          Suggestion.create(
              Suggestion.IssueType.WEBP,
              Suggestion.Category.WEBP,
              "Suggestion 7",
              /* estimatedBytesSaved= */ 1L),
          Suggestion.create(
              Suggestion.IssueType.PROGUARD_NO_SHRINKING,
              Suggestion.Category.PROGUARD,
              "Suggestion 8",
              /* estimatedBytesSaved= */ 2L),
          Suggestion.create(
              Suggestion.IssueType.MEDIA_STREAMING,
              Suggestion.Category.LARGE_FILES,
              "Suggestion 9",
              /* estimatedBytesSaved= */ 100L),
          Suggestion.create(
              Suggestion.IssueType.WEBP,
              Suggestion.Category.WEBP,
              "Suggestion 10",
              /* estimatedBytesSaved= */ 4000L));

  @Test
  public void filtersCategories() {
    TerminalInterface terminalInterface =
        TerminalInterface.create(ALL_SUGGESTIONS, ImmutableList.of("webp"), false);
    ImmutableListMultimap<Category, Suggestion> categorizedSuggestions =
        terminalInterface.categorizeSuggestions();

    assertThat(categorizedSuggestions.asMap()).hasSize(1);
    assertThat(categorizedSuggestions)
        .valuesForKey(Suggestion.Category.WEBP)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 4",
                /* estimatedBytesSaved= */ 15000L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 10",
                /* estimatedBytesSaved= */ 4000L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 1",
                /* estimatedBytesSaved= */ 3L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 7",
                /* estimatedBytesSaved= */ 1L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 5",
                /* estimatedBytesSaved= */ null))
        .inOrder();
  }

  @Test
  public void sortsSuggestions() {
    TerminalInterface terminalInterface =
        TerminalInterface.create(ALL_SUGGESTIONS, ImmutableList.of(), false);
    ImmutableListMultimap<Category, Suggestion> categorizedSuggestions =
        terminalInterface.categorizeSuggestions();

    assertThat(categorizedSuggestions.asMap()).hasSize(3);
    assertThat(categorizedSuggestions)
        .valuesForKey(Suggestion.Category.WEBP)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 4",
                /* estimatedBytesSaved= */ 15000L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 10",
                /* estimatedBytesSaved= */ 4000L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 1",
                /* estimatedBytesSaved= */ 3L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 7",
                /* estimatedBytesSaved= */ 1L),
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                "Suggestion 5",
                /* estimatedBytesSaved= */ null))
        .inOrder();
    assertThat(categorizedSuggestions)
        .valuesForKey(Suggestion.Category.LARGE_FILES)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.QUESTIONABLE_FILE,
                Suggestion.Category.LARGE_FILES,
                "Suggestion 6",
                /* estimatedBytesSaved= */ 3000L),
            Suggestion.create(
                Suggestion.IssueType.LARGE_FILES_DYNAMIC_FEATURE,
                Suggestion.Category.LARGE_FILES,
                "Suggestion 3",
                /* estimatedBytesSaved= */ 200L),
            Suggestion.create(
                Suggestion.IssueType.MEDIA_STREAMING,
                Suggestion.Category.LARGE_FILES,
                "Suggestion 9",
                /* estimatedBytesSaved= */ 100L))
        .inOrder();
    assertThat(categorizedSuggestions)
        .valuesForKey(Suggestion.Category.PROGUARD)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.PROGUARD_NO_SHRINKING,
                Suggestion.Category.PROGUARD,
                "Suggestion 8",
                /* estimatedBytesSaved= */ 2L),
            Suggestion.create(
                Suggestion.IssueType.PROGUARD_NO_OBFUSCATION,
                Suggestion.Category.PROGUARD,
                "Suggestion 2",
                /* estimatedBytesSaved= */ null))
        .inOrder();
  }

  @Test
  public void setsProperCategoryOrder() {
    TerminalInterface terminalInterface =
        TerminalInterface.create(ALL_SUGGESTIONS, ImmutableList.of(), false);
    ImmutableList<Category> categoryDisplayOrder =
        terminalInterface.getCategoryDisplayOrder(terminalInterface.categorizeSuggestions());

    assertThat(categoryDisplayOrder)
        .containsExactly(
            Suggestion.Category.WEBP, Suggestion.Category.LARGE_FILES, Suggestion.Category.PROGUARD)
        .inOrder();
  }
}
