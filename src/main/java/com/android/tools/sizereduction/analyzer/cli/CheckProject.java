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

import com.android.tools.sizereduction.analyzer.analyzers.ProjectAnalyzer;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.LargeFilesSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.QuestionableFilesSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.WebpSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.proguard.ProguardSuggester;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** This command checks a bundle for any size suggestions. */
@Command(
    name = "check-project",
    mixinStandardHelpOptions = true,
    description = "Checks an Android Studio project directory for size suggestion savings.")
public final class CheckProject implements Callable<Void> {

  @Parameters(description = "Android Studio project directory", arity = "1" /* one parameter */)
  private File directory;

  @Option(
      names = {"-d", "--display-all"},
      description =
          "Displays each individual suggestion within a category."
              + " By default only the category summary is displayed.")
  private boolean displayAll = false;

  @Option(
      names = {"-c", "--category"},
      description =
          "Display only suggestions relating to the provided category."
              + " Valid categories are webp, proguard, and large-files.")
  List<String> categories;

  private static final ProjectAnalyzer PROJECT_ANALYZER =
      new ProjectAnalyzer(
          ImmutableList.of(new ProguardSuggester()),
          ImmutableList.of(
              new WebpSuggester(), new LargeFilesSuggester(), new QuestionableFilesSuggester()));

  @Override
  public Void call() {
    TerminalInterface.create(
            PROJECT_ANALYZER.analyze(directory),
            categories != null ? ImmutableList.copyOf(categories) : ImmutableList.of(),
            displayAll)
        .displaySuggestions();
    return null;
  }
}
