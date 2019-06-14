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

import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** Wrapper around the Suggestion Category, for displaying in Android Studio. */
final class CategoryData {
  private final Category category;
  private final int totalSuggestions;
  private final long bytesSaved;

  private static final ImmutableMap<Category, String> CATEGORY_TO_STRING =
      ImmutableMap.<Category, String>builder()
          .put(Category.WEBP, "Images")
          .put(Category.LARGE_FILES, "Large Files")
          .put(Category.PROGUARD, "Code Shrinking & Obfuscation")
          .put(Category.BUNDLE_CONFIG, "Android App Bundle")
          .put(Category.BUNDLE_BASE, "App Bundle Base Module")
          .put(Category.LIBRARIES, "App Libraries")
          .build();

  public CategoryData(Category category) {
    this(category, 0);
  }

  public CategoryData(Category category, int totalSuggestions) {
    this(category, totalSuggestions, 0);
  }

  public CategoryData(Category category, int totalSuggestions, long bytesSaved) {
    this.category = category;
    this.totalSuggestions = totalSuggestions;
    this.bytesSaved = bytesSaved;
  }

  @Override
  public String toString() {
    return CATEGORY_TO_STRING.get(category);
  }

  public int totalSuggestions() {
    return totalSuggestions;
  }

  @Nullable
  public String totalSizeSaved() {
    return DisplayUtil.renderBytesSaved(Long.valueOf(bytesSaved));
  }
}
