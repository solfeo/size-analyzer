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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static java.util.Comparator.comparingLong;

import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * This class will print out the suggestions in an understandable format in the terminal.
 * Suggestions will be categorized and sorted by size reduction savings estimate.
 */
public final class TerminalInterface {

  private ImmutableList<Suggestion> suggestions;
  private ImmutableList<Suggestion.Category> displayCategories;
  private boolean displayDetails;

  private static final ImmutableMap<Category, String> CATEGORY_TO_STRING =
      ImmutableMap.of(
          Category.WEBP, "Converting images to webp saves up to ",
          Category.LARGE_FILES, "Dynamically delivering large files saves up to ",
          Category.PROGUARD, "Efficiently configuring proguard can save up to ");
  private static final ImmutableMap<String, Category> STRING_TO_CATEGORY =
      ImmutableMap.of(
          "webp",
          Category.WEBP,
          "large-files",
          Category.LARGE_FILES,
          "proguard",
          Category.PROGUARD);

  public static TerminalInterface create(
      ImmutableList<Suggestion> suggestions,
      ImmutableList<String> categories,
      boolean displayDetails) {
    return new TerminalInterface(
        suggestions,
        categories.stream()
            .filter(s -> STRING_TO_CATEGORY.containsKey(s))
            .map(s -> STRING_TO_CATEGORY.get(s))
            .collect(toImmutableList()),
        displayDetails);
  }

  private TerminalInterface(
      ImmutableList<Suggestion> suggestions,
      ImmutableList<Suggestion.Category> displayCategories,
      boolean displayDetails) {
    this.suggestions = suggestions;
    this.displayCategories = displayCategories;
    this.displayDetails = displayDetails;
  }

  public void displaySuggestions() {
    ImmutableListMultimap<Category, Suggestion> categorizedSuggestions = categorizeSuggestions();
    if (categorizedSuggestions.size() == 0) {
      System.out.println("No size saving suggestions found.");
      return;
    }
    ImmutableList<Category> categoryDisplayOrder = getCategoryDisplayOrder(categorizedSuggestions);

    Long runningTotal = 0L;
    for (Category category : categoryDisplayOrder) {
      ImmutableList<Suggestion> suggestions = categorizedSuggestions.get(category);
      Long totalSavings = getBytesSavedForSuggestionList(suggestions);
      System.out.println(
          Ansi.ansi()
              .fg(Color.GREEN)
              .a(CATEGORY_TO_STRING.get(category))
              .fg(Color.RED)
              .a(humanReadableByteCount(totalSavings))
              .reset());
      if (displayDetails) {
        suggestions.forEach(TerminalInterface::prettyPrintSuggestion);
      }
      runningTotal += totalSavings;
    }
    System.out.println(
        Ansi.ansi()
            .fg(Color.GREEN)
            .a("Total size savings of ")
            .fg(Color.RED)
            .a(humanReadableByteCount(runningTotal))
            .reset()
            .a(" found.")
            .reset());
    if (!displayDetails) {
      System.out.println(
          "The -d flag will display a list of individual suggestions for each category.");
    }
  }

  /**
   * Sorts each suggestion into its corresponding category and sorts them by size savings within
   * each category.
   */
  @VisibleForTesting
  ImmutableListMultimap<Category, Suggestion> categorizeSuggestions() {
    return suggestions.stream()
        .filter(
            suggestion ->
                displayCategories.isEmpty() || displayCategories.contains(suggestion.getCategory()))
        .sorted(comparingLong(TerminalInterface::getBytesSavedForSuggestion).reversed())
        .collect(
            toImmutableListMultimap(
                suggestion -> suggestion.getCategory(), suggestion -> suggestion));
  }

  /**
   * Returns the order for displaying each category, sorted by total size savings for each category.
   */
  @VisibleForTesting
  ImmutableList<Category> getCategoryDisplayOrder(
      ImmutableListMultimap<Category, Suggestion> categorizedSuggestions) {
    return categorizedSuggestions.asMap().entrySet().stream()
        .sorted(comparingLong(TerminalInterface::getTotalSizeSavings).reversed())
        .map(entry -> entry.getKey())
        .collect(toImmutableList());
  }

  private static void prettyPrintSuggestion(Suggestion suggestion) {
    String formattedBytesSaved =
        " (saves " + humanReadableByteCount(suggestion.getEstimatedBytesSaved()) + ")";
    System.out.println(
        Ansi.ansi().a(suggestion.getMessage()).fg(Color.RED).a(formattedBytesSaved).reset());
  }

  private static Long getTotalSizeSavings(
      Map.Entry<Category, Collection<Suggestion>> mappedSuggestions) {
    return getBytesSavedForSuggestionList(mappedSuggestions.getValue());
  }

  private static Long getBytesSavedForSuggestion(Suggestion suggestion) {
    return suggestion.getEstimatedBytesSaved() != null ? suggestion.getEstimatedBytesSaved() : 0L;
  }

  private static Long getBytesSavedForSuggestionList(Collection<Suggestion> suggestions) {
    return suggestions.stream()
        .mapToLong(suggestion -> getBytesSavedForSuggestion(suggestion))
        .sum();
  }

  private static String humanReadableByteCount(Long bytes) {
    if (bytes == null || bytes == 0) {
      return "unknown bytes";
    }
    final int unit = 1024;
    if (bytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = "kMGTPE".charAt(exp - 1) + "i";
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
}
