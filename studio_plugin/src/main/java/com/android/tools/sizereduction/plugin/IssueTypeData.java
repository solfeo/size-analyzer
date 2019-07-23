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

import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import javax.annotation.Nullable;

/** Wrapper around the Suggestion Category, for displaying in Android Studio. */
final class IssueTypeData {
  private final IssueType issueType;
  private final int totalSuggestions;
  private final long bytesSaved;

  public IssueTypeData(IssueType issueType) {
    this(issueType, 0);
  }

  public IssueTypeData(IssueType issueType, int totalSuggestions) {
    this(issueType, totalSuggestions, 0);
  }

  public IssueTypeData(IssueType issueType, int totalSuggestions, long bytesSaved) {
    this.issueType = issueType;
    this.totalSuggestions = totalSuggestions;
    this.bytesSaved = bytesSaved;
  }

  @Override
  public String toString() {
    return SuggestionDataFactory.issueTypeNodeNames.get(issueType);
  }

  @Nullable
  public String getDesc() {
    return SuggestionDataFactory.issueTypeDescriptions.getOrDefault(issueType, null);
  }

  @Nullable
  public String getAutoFixTitle() {
    return SuggestionDataFactory.issueTypeAutoFixTitles.getOrDefault(issueType, null);
  }

  public int totalSuggestions() {
    return totalSuggestions;
  }

  @Nullable
  public String totalSizeSaved() {
    return DisplayUtil.renderBytesSaved(this.bytesSaved);
  }
}
