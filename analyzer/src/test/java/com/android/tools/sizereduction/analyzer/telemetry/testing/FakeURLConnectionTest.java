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
public class FakeURLConnectionTest {

  private static final byte[] PAYLOAD_BYTES = new byte[] {115, 116, 97, 114, 116};

  @Test
  public void testConnectAndRecordRequest() throws Exception {
    FakeURLConnection conn = new FakeURLConnection(new URL("https://google.coffee"), "POST", 418,
        ImmutableMap.of("Safe", ImmutableList.of("yes")));
    conn.setRequestMethod("POST");
    conn.setRequestProperty("User-Agent", "tdeck");
    conn.getOutputStream().write(PAYLOAD_BYTES);
    conn.connect();

    // These return values would be expected by the application code.
    assertThat(conn.getResponseCode()).isEqualTo(418);
    assertThat(conn.getHeaderFields().get("Safe")).containsExactly("yes");

    // These would be asserted by tests using the fake.
    assertThat(conn.getHeadersSetByClient())
        .containsExactly("User-Agent", ImmutableList.of("tdeck"));
    assertThat(conn.getBytesWrittenByClient()).isEqualTo(PAYLOAD_BYTES);
  }

  @Test
  public void testConnect_wrongMethod() throws Exception {
    FakeURLConnection conn = new FakeURLConnection(new URL("https://google.coffee"), "POST", 200,
        ImmutableMap.of());
    // The default connection method is GET so this should fail.

    assertThrows(AssertionError.class, () -> conn.connect());
  }

}
