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

package com.android.tools.sizereduction.analyzer.telemetry.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FakeURLStreamHandlerTest {

  @Test
  public void openConnection_urlFound() throws Exception {
    FakeURLConnection conn =
        new FakeURLConnection(new URL("http://a.com"), "GET", 200, ImmutableMap.of());

    assertThat(
            new FakeURLStreamHandler(ImmutableList.of(conn))
                .openConnection(new URL("http://a.com")))
        .isEqualTo(conn);
  }

  @Test
  public void openConnection_notFound() {
    assertThrows(
        AssertionError.class,
        () -> new FakeURLStreamHandler(ImmutableList.of()).openConnection(new URL("http://a.com")));
  }
}
