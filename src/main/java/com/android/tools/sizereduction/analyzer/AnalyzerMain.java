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

package com.android.tools.sizereduction.analyzer;

import com.android.tools.sizereduction.analyzer.analyzers.BundleAnalyzer;
import com.android.tools.sizereduction.analyzer.analyzers.ProjectAnalyzer;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.LargeFilesSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.QuestionableFilesSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.WebpSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.proguard.ProguardSuggester;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;

/** Main entry point of the size reduction analyzer tool. */
public class AnalyzerMain {
  private static final BundleAnalyzer BUNDLE_ANALYZER =
      new BundleAnalyzer(
          /* bundleSuggesters= */ ImmutableList.of(new ProguardSuggester()),
          /* bundleEntrySuggesters= */ ImmutableList.of(
              new WebpSuggester(), new LargeFilesSuggester(), new QuestionableFilesSuggester()));
  private static final ProjectAnalyzer PROJECT_ANALYZER =
      new ProjectAnalyzer(
          ImmutableList.of(
              new WebpSuggester(), new LargeFilesSuggester(), new QuestionableFilesSuggester()));

  private static final String CHECK_BUNDLE_CMD = "check-bundle";
  private static final String CHECK_PROJECT_CMD = "check-project";
  private static final String VERSION_CMD = "version";

  private static final String CURRENT_VERSION = "0.1.0-alpha1";

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      throw new IllegalStateException(
          String.format(
              "Incorrect number of args. Use analyzer [%s|%s] ...",
              CHECK_BUNDLE_CMD, CHECK_PROJECT_CMD));
    }
    String command = args[0];
    switch (command) {
      case CHECK_BUNDLE_CMD:
        if (args.length < 2) {
          throw new IllegalStateException("Expected a bundle file as argument.");
        }
        File bundleFile = new File(args[1]);
        BUNDLE_ANALYZER.analyze(bundleFile).forEach(System.out::println);
        return;
      case CHECK_PROJECT_CMD:
        if (args.length < 2) {
          throw new IllegalStateException("Expected a project directory as argument.");
        }
        File projectDir = new File(args[1]);
        PROJECT_ANALYZER.analyze(projectDir).forEach(System.out::println);
        return;
      case VERSION_CMD:
        System.out.println("Size Analyzer version " + CURRENT_VERSION);
        return;
      default:
        throw new IllegalStateException("Unrecognized command: " + command);
    }
  }
}
