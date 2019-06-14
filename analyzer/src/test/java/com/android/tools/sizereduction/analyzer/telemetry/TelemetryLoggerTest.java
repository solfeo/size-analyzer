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

import static com.android.tools.sizereduction.analyzer.telemetry.TelemetryLogger.ENDPOINT_URL;
import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.analyzers.Version;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.IssueType;
import com.android.tools.sizereduction.analyzer.telemetry.testing.FakeURLConnection;
import com.android.tools.sizereduction.analyzer.telemetry.testing.FakeURLStreamHandler;
import com.android.tools.sizereduction.analyzer.utils.TestUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.play.bit.proto.SizeAnalyzerLog.ArtifactType;
import com.google.play.bit.proto.SizeAnalyzerLog.BundleSizeBreakdown;
import com.google.play.bit.proto.SizeAnalyzerLog.ErrorType;
import com.google.play.bit.proto.SizeAnalyzerLog.SizeAnalysisLog;
import com.google.play.bit.proto.SizeAnalyzerLog.SuggestionsByIssueType;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.LogRequest;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics.LogRequest.LogSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TelemetryLoggerTest {

  private static final String SERVER_ZWIEBACK_COOKIE = "NID=abc123";
  private static final long CLOCK_TIME = 1234567890_000L;

  private static final Payload SOME_PAYLOAD = Payload.getDefaultInstance();
  private static final String SOME_MESSAGE = "FOO";

  private static final Clock clock = Clock.fixed(
      Instant.ofEpochMilli(CLOCK_TIME),
      ZoneId.of("America/Aruba"));  // GMT -4, no DST

  private FakeURLConnection clearcutConnection;
  private TelemetryLogger telemetryLogger;
  private final Preferences preferences;

  public TelemetryLoggerTest() {
    // Use a temporary location that we can clear.
    System.setProperty("java.util.prefs.userRoot", "/tmp");
    preferences = Preferences.userRoot().node("bitsa-tests");
  }

  @Before
  public void setUp() throws Exception {
    clearcutConnection = new FakeURLConnection(
        new URL(ENDPOINT_URL),
        "POST",
        200,
        ImmutableMap.of("Set-Cookie", ImmutableList.of(SERVER_ZWIEBACK_COOKIE)));

    preferences.clear();

    telemetryLogger = new TelemetryLogger(clock, preferences, new FakeURLStreamHandler(
        ImmutableList.of(clearcutConnection)));
  }


  @Test
  public void logResultsForProject() throws Exception {
    ImmutableList<Suggestion> suggestions = ImmutableList.of(
        Suggestion.create(
            IssueType.QUESTIONABLE_FILE,
            Category.LARGE_FILES,
            SOME_PAYLOAD,
            SOME_MESSAGE,
            1000L,
            /* autoFix= */ null),
        Suggestion.create(
            IssueType.QUESTIONABLE_FILE,
            Category.LARGE_FILES,
            SOME_PAYLOAD,
            SOME_MESSAGE,
            1000L,
            /* autoFix= */ null),
        Suggestion.create(
            IssueType.LARGE_FILES_DYNAMIC_FEATURE,
            Category.LARGE_FILES,
            SOME_PAYLOAD,
            SOME_MESSAGE,
            5000L,
            /* autoFix= */ null));

    telemetryLogger.logResultsForProject(suggestions);

    assertThat(clearcutConnection.getHeadersSetByClient().get("Cookie")).contains("NID=");

    LogRequest logRequest = inflatePayload(clearcutConnection);

    assertCommonLogFields(logRequest);

    SizeAnalysisLog sizeAnalysisLog = extractSourceExtension(logRequest);

    assertThat(sizeAnalysisLog.getAnalyzerVersion()).isEqualTo(Version.CURRENT_VERSION);
    assertThat(sizeAnalysisLog.getArtifactType()).isEqualTo(ArtifactType.PROJECT_DIR);
    assertThat(sizeAnalysisLog.getErrorType()).isEqualTo(ErrorType.NO_ERROR);

    assertThat(sizeAnalysisLog.getSuggestionsByIssueTypeList())
        .containsExactly(
            SuggestionsByIssueType.newBuilder()
                .setCategory("LARGE_FILES")
                .setIssueType("QUESTIONABLE_FILE")
                .setSuggestionCount(2)
                .setEstimatedSavingsBytes(2000)
                .build(),
            SuggestionsByIssueType.newBuilder()
                .setCategory("LARGE_FILES")
                .setIssueType("LARGE_FILES_DYNAMIC_FEATURE")
                .setSuggestionCount(1)
                .setEstimatedSavingsBytes(5000)
                .build());
    assertThat(sizeAnalysisLog.hasBundleSizeBreakdown()).isFalse();
  }

  @Test
  public void logResultsForBundle() throws Exception {
    File bundle = TestUtils.getTestDataFile("app.aab");

    ImmutableList<Suggestion> suggestions = ImmutableList.of(
        Suggestion.create(
            IssueType.WEBP,
            Category.WEBP,
            SOME_PAYLOAD,
            SOME_MESSAGE,
            1000L,
            /* autoFix= */ null));

    telemetryLogger.logResultsForBundle(bundle, suggestions);

    assertThat(clearcutConnection.getHeadersSetByClient().get("Cookie")).contains("NID=");

    LogRequest logRequest = inflatePayload(clearcutConnection);

    assertCommonLogFields(logRequest);

    SizeAnalysisLog sizeAnalysisLog = extractSourceExtension(logRequest);

    assertThat(sizeAnalysisLog.getAnalyzerVersion()).isEqualTo(Version.CURRENT_VERSION);
    assertThat(sizeAnalysisLog.getArtifactType()).isEqualTo(ArtifactType.APP_BUNDLE);
    assertThat(sizeAnalysisLog.getErrorType()).isEqualTo(ErrorType.NO_ERROR);

    assertThat(sizeAnalysisLog.getSuggestionsByIssueTypeList())
        .containsExactly(
            SuggestionsByIssueType.newBuilder()
                .setCategory("WEBP")
                .setIssueType("WEBP")
                .setSuggestionCount(1)
                .setEstimatedSavingsBytes(1000)
                .build());

    // Note that these sizes are truncated to multiples of 1000 bytes.
    // aab: 1408431
    // res files: 363900
    // dex files: 2211248
    // resources.pb: 462708
    assertThat(sizeAnalysisLog.getBundleSizeBreakdown())
        .isEqualTo(
            BundleSizeBreakdown.newBuilder()
                .setAabFileBytes(1408000)
                .setDexBytes(2211000)
                .setResourceFileBytes(363000)
                .setResourcePbBytes(462000)
                .build());
  }

  @Test
  public void logErrorForProject() throws Exception {
    telemetryLogger.logErrorForProject(new NullPointerException());

    LogRequest logRequest = inflatePayload(clearcutConnection);
    assertCommonLogFields(logRequest);
    SizeAnalysisLog sizeAnalysisLog = extractSourceExtension(logRequest);
    assertThat(sizeAnalysisLog.getErrorType()).isEqualTo(ErrorType.RUNTIME_ERROR);
  }

  @Test
  public void logErrorForBundle() throws Exception {
    telemetryLogger.logErrorForBundle(new NullPointerException());

    LogRequest logRequest = inflatePayload(clearcutConnection);
    assertCommonLogFields(logRequest);
    SizeAnalysisLog sizeAnalysisLog = extractSourceExtension(logRequest);
    assertThat(sizeAnalysisLog.getErrorType()).isEqualTo(ErrorType.RUNTIME_ERROR);
  }

  @Test
  public void rememberZwiebackId() throws Exception {
    telemetryLogger.logResultsForProject(ImmutableList.of());

    FakeURLConnection secondConnection = new FakeURLConnection(
        new URL(ENDPOINT_URL),
        "POST",
        200,
        ImmutableMap.of());

    // Note we re-use the same preferences, where the cookie is stored.
    new TelemetryLogger(clock, preferences, new FakeURLStreamHandler(
        ImmutableList.of(secondConnection))).logResultsForProject(ImmutableList.of());

    assertThat(secondConnection.getHeadersSetByClient().get("Cookie")).contains(
        SERVER_ZWIEBACK_COOKIE);
  }

  private static LogRequest inflatePayload(FakeURLConnection conn) throws IOException {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(conn.getBytesWrittenByClient());
        GZIPInputStream gzis = new GZIPInputStream(bis)) {
      return LogRequest.parseFrom(gzis);
    }
  }

  private static void assertCommonLogFields(LogRequest logRequest) {
    assertThat(logRequest.getLogSource()).isEqualTo(LogSource.BIT_SIZE_ANALYZER);
    assertThat(logRequest.getRequestTimeMs()).isEqualTo(CLOCK_TIME);
    assertThat(logRequest.getLogEventCount()).isEqualTo(1);
    assertThat(logRequest.getLogEvent(0).hasSourceExtension()).isTrue();
  }

  private static SizeAnalysisLog extractSourceExtension(LogRequest logRequest) throws Exception {
    return SizeAnalysisLog.parseFrom(logRequest.getLogEvent(0).getSourceExtension());
  }
}
