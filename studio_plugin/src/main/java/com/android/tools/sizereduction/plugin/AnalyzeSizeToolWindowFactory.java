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

import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/**
 * ToolWindowFactory implementation that deals with filling in the suggestions from the app size
 * analysis.
 */
public final class AnalyzeSizeToolWindowFactory implements ToolWindowFactory {
  private final ImmutableListMultimap<Category, Suggestion> categorizedSuggestions;
  private final ImmutableList<Category> categoryDisplayOrder;

  public static final String TOOL_WINDOW_TITLE = "Analyze App Size";
  private static final String CONTENT_TITLE = "Recommendations";

  public AnalyzeSizeToolWindowFactory(
      ImmutableListMultimap<Category, Suggestion> categorizedSuggestions,
      ImmutableList<Category> categoryDisplayOrder) {
    this.categorizedSuggestions = categorizedSuggestions;
    this.categoryDisplayOrder = categoryDisplayOrder;
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    ContentManager contentManager = toolWindow.getContentManager();
    AnalyzeSizeToolWindow analyzeSizeToolWindow =
        new AnalyzeSizeToolWindow(
            toolWindow,
            project,
            categorizedSuggestions,
            categoryDisplayOrder
            );
    Content content =
        ContentFactory.SERVICE
            .getInstance()
            .createContent(analyzeSizeToolWindow.getContent(), CONTENT_TITLE, false);
    // We want only one content tab to exist at a time, but clearing all of them will cause the
    // tool window to disappear, so we add the new tab and remove the others after.
    contentManager.addContent(content);
    contentManager.setSelectedContent(content);
    for (Content otherContent : contentManager.getContents()) {
      if (otherContent != content) {
        contentManager.removeContent(otherContent, true);
      }
    }
    toolWindow.show(null);
  }
}
