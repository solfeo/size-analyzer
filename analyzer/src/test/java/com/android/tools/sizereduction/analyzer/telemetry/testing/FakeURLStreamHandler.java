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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A fake implementation of {@link URLStreamHandler} to allow testing of HTTP requests in
 * isolation.
 */
public class FakeURLStreamHandler extends URLStreamHandler  {

  // This uses URI for mappings because the URL.equals() method actually makes a network call
  // to resolve and compare hosts, which is very undesirable
  private final ImmutableMap<URI, FakeURLConnection> urlMappings;

  /**
   * Constructs a FakeUrlStreamHandler whose {@code openConnection} method will return the
   * mocked connection corresponding with the requested URL.
   *
   * <p>Multiple handlers with the same URL (i.e. with different methods) are not supported.
   *
   * @param mockedConnections the mock connection objects to return
   */
  public FakeURLStreamHandler(ImmutableList<FakeURLConnection> mockedConnections) {
    ImmutableMap.Builder<URI, FakeURLConnection> mapBuilder = ImmutableMap.builder();

    try {
      // We can't easily use a stream here because of checked exceptions.
      for (FakeURLConnection conn : mockedConnections) {
        mapBuilder.put(conn.getURL().toURI(), conn);
      }

      urlMappings = mapBuilder.build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    try {
      URI uri = url.toURI();
      assertThat(urlMappings).containsKey(uri);

      return urlMappings.get(uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
