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

package com.android.tools.sizereduction.analyzer.telemetry;

import com.android.tools.sizereduction.analyzer.analyzers.Version;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.play.bit.proto.SizeAnalyzerLog.ArtifactType;
import com.google.play.bit.proto.SizeAnalyzerLog.BundleSizeBreakdown;
import com.google.play.bit.proto.SizeAnalyzerLog.ErrorType;
import com.google.play.bit.proto.SizeAnalyzerLog.SizeAnalysisLog;
import com.google.play.bit.proto.SizeAnalyzerLog.SuggestionsByIssueType;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.ClientInfo;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.ClientInfo.ClientType;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.CollectForDebug;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.DesktopClientInfo;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.LogEvent;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.LogRequest;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.LogRequest.LogSource;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLStreamHandler;
import java.time.Clock;
import java.util.Enumeration;
import java.util.Objects;
import java.util.TimeZone;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

/**
 * Sends telemetry logs to Google. This class must only be used with explicit user consent.
 */
public class TelemetryLogger {
  private static final URLStreamHandler STREAM_HANDLER = new sun.net.www.protocol.https.Handler();

  @VisibleForTesting
  static final String ENDPOINT_URL = "https://play.googleapis.com/log";
  @VisibleForTesting
  static final String ZWIEBACK_COOKIE_PREFS_KEY = "zwieback_cookie";
  @VisibleForTesting
  static final String ZWIEBACK_COOKIE_NAME = "NID";

  /*
   * Timeout in milliseconds after which an HTTP request will be abandoned. Used for both HTTP
   * connection establishment and read timeout.
   */
  private static final int HTTP_TIMEOUT_MS = 500;

  /*
   * We truncate bundle sizes in telemetry logs to make it impractical to associate these logs
   * with an identified user if the same artifact is later uploaded to the Play Console.
   */
  private static final int BUNDLE_SIZE_TRUNCATION_BYTES = 1000;

  // These regexes are used to match files in the bundle for size classification.
  private static final Pattern RE_DEX_FILE = Pattern.compile("[^/]+/dex/.*\\.dex");
  private static final Pattern RE_RES_FILE = Pattern.compile("[^/]+/res/.*");
  private static final Pattern RE_RES_PB = Pattern.compile("[^/]+/resources\\.pb");
  private static final Pattern RE_ASSET_FILE = Pattern.compile("[^/]+/assets/.*");
  private static final Pattern RE_LIB_FILE = Pattern.compile("[^/]+/lib/.*");

  private static final String OS_NAME = System.getProperty("os.name");
  private static final String OS_VERSION = System.getProperty("os.version");

  private static final TelemetryLogger INSTANCE =
      new TelemetryLogger(
          Clock.systemUTC(), Preferences.userNodeForPackage(TelemetryLogger.class),
          STREAM_HANDLER);

  private final Clock clock;
  private final Preferences preferences;
  private final URLStreamHandler urlStreamHandler;

  public static TelemetryLogger get() {
    return INSTANCE;
  }

  @VisibleForTesting
  TelemetryLogger(Clock clock, Preferences preferences, URLStreamHandler urlStreamHandler) {
    this.clock = clock;
    this.preferences = preferences;
    this.urlStreamHandler = urlStreamHandler;
  }

  /** Submits telemetry results from a check-project run */
  public void logResultsForProject(ImmutableList<Suggestion> suggestions) {
    uploadLogs(buildResultLog(suggestions, null).setArtifactType(ArtifactType.PROJECT_DIR));
  }

  /** Registers an error from a check-project run */
  public void logErrorForProject(Exception e) {
    uploadLogs(buildResultLog(ImmutableList.of(), e).setArtifactType(ArtifactType.PROJECT_DIR));
  }

  /** Submits telemetry results from a check-bundle run */
  public void logResultsForBundle(
      File bundleFile, ImmutableList<Suggestion> suggestions) {
    uploadLogs(
        buildResultLog(suggestions, null)
            .setArtifactType(ArtifactType.APP_BUNDLE)
            .setBundleSizeBreakdown(computeBundleSizeBreakdown(bundleFile)));
  }

  /** Registers an error from a check-bundle run */
  public void logErrorForBundle(Exception e) {
    uploadLogs(buildResultLog(ImmutableList.of(), e).setArtifactType(ArtifactType.APP_BUNDLE));
  }

  @VisibleForTesting
  static BundleSizeBreakdown computeBundleSizeBreakdown(File bundleFile) {
    int dexBytes = 0;
    int resourceFileBytes = 0;
    int resourcePbBytes = 0;
    int assetBytes = 0;
    int nativeLibBytes = 0;

    try (ZipFile zip = new ZipFile(bundleFile)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement(); 

        if (entry.isDirectory()) {
          continue;
        }

        String name = entry.getName();

        if (RE_DEX_FILE.matcher(name).matches()) {
          dexBytes = (int) (dexBytes + entry.getSize());
        } else if (RE_RES_FILE.matcher(name).matches()) {
          resourceFileBytes = (int) (resourceFileBytes + entry.getSize());
        } else if (RE_RES_PB.matcher(name).matches()) {
          resourcePbBytes = (int) (resourcePbBytes + entry.getSize());
        } else if (RE_ASSET_FILE.matcher(name).matches()) {
          assetBytes = (int) (assetBytes + entry.getSize());
        } else if (RE_LIB_FILE.matcher(name).matches()) {
          nativeLibBytes = (int) (nativeLibBytes + entry.getSize());
        }
      }

      return BundleSizeBreakdown.newBuilder()
          .setAabFileBytes(truncateSize((int) bundleFile.length()))
          .setDexBytes(truncateSize(dexBytes))
          .setResourceFileBytes(truncateSize(resourceFileBytes))
          .setResourcePbBytes(truncateSize(resourcePbBytes))
          .setAssetBytes(truncateSize(assetBytes))
          .setNativeLibBytes(truncateSize(nativeLibBytes))
          .build();
    } catch (IOException e) {

      // Swallow an exception here (seems unlikely since we already opened the file) and just
      // continue on.
      return BundleSizeBreakdown.getDefaultInstance();
    }
  }

  private static SizeAnalysisLog.Builder buildResultLog(
      ImmutableList<Suggestion> suggestions, @Nullable Exception exception) {

    ImmutableListMultimap<IssueType, Suggestion> partitionedSuggestions = Multimaps
        .index(suggestions, Suggestion::getIssueType);

    return SizeAnalysisLog.newBuilder()
        .setAnalyzerVersion(Version.CURRENT_VERSION)
        // Note, we could categorize these exceptions with more specificity in future releases
        .setErrorType(exception == null ? ErrorType.NO_ERROR : ErrorType.RUNTIME_ERROR)
        .addAllSuggestionsByIssueType(
            partitionedSuggestions.keySet().stream()
                .map(
                    issueType -> {
                      ImmutableList<Suggestion> s = partitionedSuggestions.get(issueType);
                      return SuggestionsByIssueType.newBuilder()
                          .setIssueType(issueType.name())
                          // All suggestions with the same issue type should have the same category
                          // as well.
                          .setCategory(s.get(0).getCategory().name())
                          .setSuggestionCount(s.size())
                          .setEstimatedSavingsBytes(
                              s.stream()
                                  .map(Suggestion::getEstimatedBytesSaved)
                                  .filter(Objects::nonNull)
                                  .reduce(0L, Long::sum)
                                  .intValue())
                          .build();
                    })
                .collect(Collectors.toList()));
  }

  private static int truncateSize(int size) {
    return size - (size % BUNDLE_SIZE_TRUNCATION_BYTES);
  }

  private void uploadLogs(SizeAnalysisLog.Builder analysisLogBuilder) {
    try {
      long timestamp = clock.millis();

      LogRequest.Builder requestBuilder =
          LogRequest.newBuilder()
              .setRequestTimeMs(timestamp)
              .setClientInfo(
                  ClientInfo.newBuilder()
                      .setClientType(ClientType.DESKTOP)
                      .setDesktopClientInfo(
                          DesktopClientInfo.newBuilder()
                              .setOs(OS_NAME)
                              .setOsFullVersion(OS_VERSION)
                              .setApplicationBuild(Version.CURRENT_VERSION)
                              .build())
                      .build())
              .setLogSource(LogSource.BIT_SIZE_ANALYZER)
              .addLogEvent(
                  LogEvent.newBuilder()
                      .setEventTimeMs(timestamp)
                      .setTimezoneOffsetSeconds(
                          TimeZone.getTimeZone(clock.getZone()).getOffset(timestamp) / 1000)
                      .setIsUserInitiated(true)
                      .setSourceExtension(analysisLogBuilder.build().toByteString()));


      // We use the complicated URL constructor here so we can mock the underlying implementation of
      // openConnection() in tests.
      HttpURLConnection conn =
          (HttpURLConnection)
              new URL(
                  /* context= */ null,
                  /* spec= */ ENDPOINT_URL,
                  /* handler= */ urlStreamHandler)
                  .openConnection();

      conn.setDoOutput(true);
      conn.setConnectTimeout(HTTP_TIMEOUT_MS);
      conn.setReadTimeout(HTTP_TIMEOUT_MS);
      conn.setInstanceFollowRedirects(false);
      conn.setUseCaches(false);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-gzip");
      conn.setRequestProperty("Content-Encoding", "gzip");

      conn.setRequestProperty("Cookie", getZwiebackCookie());

      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(conn.getOutputStream())) {
        requestBuilder.build().writeTo(gzipOutputStream);
      }

      try {
        conn.connect();

        int statusCode = conn.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
          return;
        }

        // Read and update our stored Zwieback cookie from the response
        // Note: conn.getHeaderField returns only the first matching header, and does case-sensitive
        // name matching (HTTP header names are case-insensitive). So we have to do this instead:
        conn.getHeaderFields()
            .forEach(
                (key, values) -> {
                  if (key != null && Ascii.equalsIgnoreCase(key, "Set-Cookie")) {
                    for (String value : values) {
                      if (value.startsWith(ZWIEBACK_COOKIE_NAME + "=")) {
                        saveZwiebackCookie(value);
                        break; // There should never be two Set-Cookie headers for the same cookie.
                      }
                    }
                  }
                });
      } finally {
        conn.disconnect();
      }
    } catch (Exception e) {
      // Swallow all exceptions here so that issues with telemetry don't block the user from getting
      // their work done.
    }
  }

  private String getZwiebackCookie() {
    // Apparently to get an initial Zwieback ID, you need to send a Zwieback cookie. So
    // we return a dummy one as a default to send to the server on our first request.
    return preferences.get(ZWIEBACK_COOKIE_PREFS_KEY, "NID=");
  }

  private void saveZwiebackCookie(String cookie) {
    preferences.put(ZWIEBACK_COOKIE_PREFS_KEY, cookie);
  }
}
