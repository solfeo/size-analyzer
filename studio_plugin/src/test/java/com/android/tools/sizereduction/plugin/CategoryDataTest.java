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

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class CategoryDataTest {

  @Test
  public void checkCategoriesHaveValidStringRepresentations() throws Exception {
    for (Category category : Category.values()) {
      CategoryData categoryData = new CategoryData(category);
      assertThat(categoryData.toString()).isNotEmpty();
    }
  }

  @Test
  public void checkSizeSavedNullWithZeroSavedBytes() {
    CategoryData categoryData = new CategoryData(Category.WEBP);
    assertThat(categoryData.totalSizeSaved()).isNull();
  }

  @Test
  public void checkSizeSavedWithNonZeroSavedBytes() {
    CategoryData categoryData = new CategoryData(Category.WEBP, 1, 1024);
    assertThat(categoryData.totalSizeSaved()).isEqualTo("1.00 KB");
  }
}
