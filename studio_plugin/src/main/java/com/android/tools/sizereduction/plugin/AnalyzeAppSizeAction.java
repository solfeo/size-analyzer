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

package com.android.tools.sizereduction.plugin;

import com.android.tools.sizereduction.analyzer.analyzers.ProjectAnalyzer;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.LargeFilesSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.QuestionableFilesSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.WebpSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.bundles.BundleSplitSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.libraries.LibraryEligibleForFeatureSplitSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.libraries.OptimalLibrarySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.proguard.ProguardSuggester;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import java.io.File;
import org.jetbrains.annotations.NotNull;

/** Adds a size analyzer action in the analyze menu of Android Studio. */
public class AnalyzeAppSizeAction extends AnAction {

  private static final ProjectAnalyzer PROJECT_ANALYZER =
      new ProjectAnalyzer(
          ImmutableList.of(
              new ProguardSuggester(),
              new BundleSplitSuggester(),
              new LibraryEligibleForFeatureSplitSuggester(),
              new OptimalLibrarySuggester()),
          ImmutableList.of(
              new LargeFilesSuggester(), new QuestionableFilesSuggester(), new WebpSuggester()));

  private ImmutableListMultimap<Category, Suggestion> categorizedSuggestions;

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = getEventProject(event);
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Analyzing App Size") {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Analyzing app size...");

                categorizedSuggestions =
                    project != null
                        ? gatherAnalysis(project)
                        : ImmutableListMultimap.of();

                ApplicationManager.getApplication()
                    .invokeLater(
                        new Runnable() {
                          @Override
                          public void run() {

                            ImmutableList<Category> categoryDisplayOrder =
                                ImmutableList.of(
                                    Category.WEBP,
                                    Category.LARGE_FILES,
                                    Category.PROGUARD,
                                    Category.BUNDLE_CONFIG,
                                    Category.LIBRARIES);

                            AnalyzeSizeToolWindowFactory factory =
                                new AnalyzeSizeToolWindowFactory(
                                    categorizedSuggestions, categoryDisplayOrder);
                            ToolWindowManager toolWindowManager =
                                ToolWindowManager.getInstance(project);
                            ToolWindow toolWindow =
                                toolWindowManager.getToolWindow(
                                    AnalyzeSizeToolWindowFactory.TOOL_WINDOW_TITLE);
                            if (toolWindow == null) {
                              // NOTE: canWorkInDumbMode must be true or the window will close on
                              // gradle sync.
                              // we may want to invalidate our cache of auto-fixes when there's a
                              // gradle sync
                              // in case they point at out-of-date files).
                              toolWindow =
                                  toolWindowManager.registerToolWindow(
                                      AnalyzeSizeToolWindowFactory.TOOL_WINDOW_TITLE,
                                      /* canCloseContent= */ true,
                                      ToolWindowAnchor.BOTTOM,
                                      project,
                                      /* canWorkinDumbMode= */ true);
                              ContentManager contentManager = toolWindow.getContentManager();
                              contentManager.addContentManagerListener(
                                  new ContentManagerAdapter() {
                                    @Override
                                    public void contentRemoved(ContentManagerEvent event) {
                                      if (contentManager.getContentCount() == 0) {
                                        ToolWindowManager toolWindowManager =
                                            ToolWindowManager.getInstance(project);
                                        toolWindowManager.unregisterToolWindow(
                                            AnalyzeSizeToolWindowFactory.TOOL_WINDOW_TITLE);
                                      }
                                    }
                                  });
                            }
                            factory.createToolWindowContent(project, toolWindow);

                            // Always active the window, in case it was previously minimized.
                            toolWindow.activate(null);
                          }
                        });
              }
            });
  }

  private ImmutableListMultimap<Category, Suggestion> gatherAnalysis(Project project) {
    ImmutableListMultimap.Builder<Category, Suggestion> resultBuilder =
        ImmutableListMultimap.builder();
    if (project.getBasePath() != null) {
      ImmutableList<Suggestion> suggestions =
          PROJECT_ANALYZER.analyze(new File(project.getBasePath()));
      for (Suggestion suggestion : suggestions) {
        resultBuilder.put(suggestion.getCategory(), suggestion);
      }
    }
    return resultBuilder.build();
  }
}
