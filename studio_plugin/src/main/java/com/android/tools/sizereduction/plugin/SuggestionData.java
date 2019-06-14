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

import com.android.tools.sizereduction.analyzer.suggesters.AutoFix;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import javax.annotation.Nullable;

/** Wrapper around the suggestion field, to be used in a UX Tree.*/
final class SuggestionData {
  private final Suggestion suggestion;
  private final String title;
  private final String description;
  private final String moreInfo;
  private final String autoFixTitle;

  public SuggestionData(
      Suggestion suggestion,
      @Nullable String title,
      @Nullable String description,
      @Nullable String moreInfo,
      @Nullable String autoFixTitle) {
    this.suggestion = suggestion;
    this.title = title;
    this.description = description;
    this.moreInfo = moreInfo;
    this.autoFixTitle = autoFixTitle;
  }

  @Override
  public String toString() {
    return getTitle();
  }

  public String getTitle() {
    return title != null ? title : suggestion.getMessage();
  }

  @Nullable
  public String getDesc() {
    return description;
  }

  @Nullable
  public String getMoreInfo() {
    return moreInfo;
  }

  @Nullable
  public String getBytesSaved() {
    Long bytesSaved = suggestion.getEstimatedBytesSaved();
    return DisplayUtil.renderBytesSaved(bytesSaved);
  }

  @Nullable
  public AutoFix getAutoFix() {
    return suggestion.getAutoFix();
  }

  @Nullable
  public String getAutoFixTitle() {
    return autoFixTitle;
  }
}
