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

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.suggesters.AutoFix;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SuggestionDataTest {

  private final SuggestionDataFactory factory = new SuggestionDataFactory();
  private final Suggestion suggestion =
      Suggestion.create(
          Suggestion.IssueType.WEBP,
          Suggestion.Category.WEBP,
          Payload.getDefaultInstance(),
          "Convert drawing.png to webp with lossless encoding",
          1024L,
          new AutoFix() {
            @Override
            public void apply() {

            }
          });
  private final SuggestionData suggestionData = factory.fromSuggestion(suggestion);

  @Test
  public void checkString() throws Exception {
    assertThat(suggestionData.toString()).isEqualTo(suggestion.getMessage());
    assertThat(suggestionData.getTitle()).isEqualTo(suggestion.getMessage());
    assertThat(suggestionData.getDesc())
        .isEqualTo(
            "Converting png images to webp format will reduce the storage that your images take up"
                + " without any reduction in image quality.");
    assertThat(suggestionData.getMoreInfo())
        .isEqualTo("https://developer.android.com/studio/write/convert-webp");
    assertThat(suggestionData.getAutoFixTitle())
        .isEqualTo("Convert image to webp");
  }

  @Test
  public void checkSizeSavedNullWithNoSavedBytes() {
    Suggestion zeroSuggestion =
        Suggestion.create(
            Suggestion.IssueType.WEBP,
            Suggestion.Category.WEBP,
            Payload.getDefaultInstance(),
            "Convert drawing.png to webp with lossless encoding",
            null,
            new AutoFix() {
              @Override
              public void apply() {

              }
            });
    SuggestionData zeroSuggestionData = factory.fromSuggestion(zeroSuggestion);
    assertThat(zeroSuggestionData.getBytesSaved()).isNull();
  }

  @Test
  public void checkSizeSavedWithSavedBytes() {
    assertThat(suggestionData.getBytesSaved()).isEqualTo("1.00 KB");
  }
}
