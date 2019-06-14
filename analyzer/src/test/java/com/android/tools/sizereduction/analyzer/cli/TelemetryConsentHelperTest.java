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

package com.android.tools.sizereduction.analyzer.cli;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.cli.TelemetryConsentHelper.ConsentStatus;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.prefs.Preferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TelemetryConsentHelperTest {
  private final Preferences preferences;
  private ByteArrayOutputStream stdoutBytes;
  private PrintStream stdout;

  public TelemetryConsentHelperTest() {
    // Use a temporary location that we can clear.
    System.setProperty("java.util.prefs.userRoot", "/tmp");
    preferences = Preferences.userRoot().node("bitsa-tests");
  }

  @Before
  public void setUp() throws Exception {
    preferences.clear();
    stdoutBytes = new ByteArrayOutputStream();
    stdout = new PrintStream(stdoutBytes);
  }

  @Test
  public void updateAndGetSavedConsentStatus() {
    TelemetryConsentHelper helper = new TelemetryConsentHelper(null, stdout, preferences);
    assertThat(helper.getSavedConsentStatus()).isEqualTo(ConsentStatus.UNASKED);
    helper.updateSavedConsentStatus(ConsentStatus.OPTED_OUT);
    assertThat(helper.getSavedConsentStatus()).isEqualTo(ConsentStatus.OPTED_OUT);
  }

  @Test
  public void checkForConsent_unaskedToYes() {
    TelemetryConsentHelper helper =
        new TelemetryConsentHelper(new Scanner("y\n"), stdout, preferences);

    assertThat(helper.checkForConsent()).isTrue();

    assertThat(stdoutBytes.toString()).contains(TelemetryConsentHelper.CONSENT_PROMPT);
    assertThat(helper.getSavedConsentStatus()).isEqualTo(ConsentStatus.OPTED_IN);
  }

  @Test
  public void checkForConsent_unaskedToNo() {
    TelemetryConsentHelper helper =
        new TelemetryConsentHelper(new Scanner("n\n"), stdout, preferences);

   assertThat(helper.checkForConsent()).isFalse();

    assertThat(stdoutBytes.toString()).contains(TelemetryConsentHelper.CONSENT_PROMPT);
    assertThat(helper.getSavedConsentStatus()).isEqualTo(ConsentStatus.OPTED_OUT);
  }

  @Test
  public void checkForConsent_unrecognizedInput() {
    TelemetryConsentHelper helper =
        new TelemetryConsentHelper(new Scanner("indubitably\ny\n"), stdout, preferences);

    assertThat(helper.checkForConsent()).isTrue();

    assertThat(stdoutBytes.toString()).contains(TelemetryConsentHelper.CONSENT_PROMPT);
    assertThat(stdoutBytes.toString()).contains(TelemetryConsentHelper.REPEAT_PROMPT);
    assertThat(helper.getSavedConsentStatus()).isEqualTo(ConsentStatus.OPTED_IN);
  }

  @Test
  public void checkForConsent_noConsole() {
    TelemetryConsentHelper helper =
        new TelemetryConsentHelper(null, stdout, preferences);

    assertThat(helper.checkForConsent()).isFalse();

    assertThat(helper.getSavedConsentStatus()).isEqualTo(ConsentStatus.UNASKED);

    assertThat(stdoutBytes.toString()).doesNotContain(TelemetryConsentHelper.CONSENT_PROMPT);
  }

  @Test
  public void checkForConsent_alreadyReplied() {
    TelemetryConsentHelper helper =
        new TelemetryConsentHelper(new Scanner(""), stdout, preferences);

    helper.updateSavedConsentStatus(ConsentStatus.OPTED_IN);

    assertThat(helper.checkForConsent()).isTrue();

    assertThat(stdoutBytes.toString()).doesNotContain(TelemetryConsentHelper.CONSENT_PROMPT);
  }
}
