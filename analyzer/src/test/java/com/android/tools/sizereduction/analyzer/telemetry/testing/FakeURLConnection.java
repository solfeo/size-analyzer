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
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * An limited fake of {@link HttpURLConnection}. On a call to {@code connect()}, instances of this
 * class will assert that they have been provided with the expected request method on connect. The
 * consumer can then set headers and write payload into an internal output stream whose
 * content can be retrieved in a test method.
 *
 * <p>This does not currently support setting a response body simply because we haven't had a need
 * for it yet.
 */
public class FakeURLConnection extends HttpURLConnection {
  private final String expectedMethod;
  private final int responseCodeAfterConnect;
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final ImmutableMap<String, List<String>> responseHeaders;

  /** Constructs a new FakeURLConnection. */
  public FakeURLConnection(URL url, String expectedMethod, int responseCode,
      ImmutableMap<String, List<String>> responseHeaders) {
    super(url);
    this.expectedMethod = expectedMethod;
    this.responseCodeAfterConnect = responseCode;
    this.responseHeaders = responseHeaders;
  }

  /** Returns the map of headers set on the connection by the client. */
  public Map<String, List<String>> getHeadersSetByClient() {
    // Unfortunately the underlying requests object is private, and getRequestProperties
    // throws an exception if the object is already connected. Since we need to allow test callers
    // to inspect these headers, we've got to use this hack.
    boolean conStatus = connected;
    try {
      connected = false;
      return getRequestProperties();
    } finally {
      connected = conStatus;
    }
  }

  /** Returns bytes written to the output stream of the connection. */
  public byte[] getBytesWrittenByClient() {
    return outputStream.toByteArray();
  }

  @Override
  public void connect() {
    assertWithMessage("request method").that(this.method).isEqualTo(this.expectedMethod);

    this.connected = true;
    this.responseCode = this.responseCodeAfterConnect;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public Map<String, List<String>> getHeaderFields() {
    // NOTE: This mock doesn't address the other ways of accessing headers like getHeaderField();
    // we'll need to add overrides for those if we end up using them.
    assertThat(connected).isTrue();
    return responseHeaders;
  }

  @Override
  public void disconnect() {
    // Do nothing
  }

  @Override
  public boolean usingProxy() {
    return false;
  }
}
