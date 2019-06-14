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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.annotation.Nullable;

/** Helper class for collecting and storing telemetry consent. **/
class TelemetryConsentHelper {

  private static final String CONSENT_PREFS_KEY = "telemetry_consent";

  @VisibleForTesting
  static final String CONSENT_PROMPT =
      "This tool can send anonymous usage and app size statistics to Google, allowing\n"
          + "us to build better tools for you in the future. The data will be collected in\n"
          + "accordance with Google's privacy policy at https://policies.google.com/privacy,\n"
          + "and you can change your preferences at any time using the "
          + "`size-analyzer metrics` command.\n"
          + "\n"
          + "Do you consent to share usage metrics with Google (y/n)? ";
  @VisibleForTesting
  static final String REPEAT_PROMPT = "Please enter 'y' or 'n': ";

  @Nullable private final Scanner inputScanner;
  private final PrintStream outStream;
  private final Preferences preferences;

  private static final TelemetryConsentHelper INSTANCE = new TelemetryConsentHelper(
      // System.console() will return null if STDIN or STDOUT is not interactive.
      System.console() == null ? null : new Scanner(System.in, UTF_8.name()),
      System.out,
      Preferences.userNodeForPackage(TelemetryConsentHelper.class));

  static TelemetryConsentHelper get() {
    return INSTANCE;
  }

  /**
   * Constructs a new helper. {@code InputScanner} should be null when {@code System.console()} is
   * null.
   */
  @VisibleForTesting
  TelemetryConsentHelper(
      @Nullable Scanner inputScanner, PrintStream outStream, Preferences preferences) {
    // It would have been preferable to take in a Console object, but unfortunately that class is
    // final so it's impossible to construct a fake.
    this.inputScanner = inputScanner;
    this.outStream = outStream;
    this.preferences = preferences;
  }

  /**
   * Prompts for telemetry opt-in or retrieves a saved opt-in preference as appropriate, and
   * returns true if the user has explicitly opted in to telemetry collection.
   *
   * <p>If the session is connected to a pipe and the user has recorded no preference,
   * this will not display a prompt and will leave the user in the UNASKED (opted-out) state.
   */
  boolean checkForConsent() {
    ConsentStatus consentStatus = getSavedConsentStatus();

    if (consentStatus == ConsentStatus.UNASKED && inputScanner != null) {
      consentStatus =  promptForConsent();
      updateSavedConsentStatus(consentStatus);
    }

    return consentStatus == ConsentStatus.OPTED_IN;
  }

  ConsentStatus getSavedConsentStatus() {
    return ConsentStatus.valueOf(preferences.get(CONSENT_PREFS_KEY, ConsentStatus.UNASKED.name()));
  }

  void updateSavedConsentStatus(ConsentStatus status) {
    preferences.put(CONSENT_PREFS_KEY, status.name());
    try {
      preferences.flush();
    }  catch (BackingStoreException e) {
      if (status == ConsentStatus.OPTED_OUT) {
        // Unfortunately we have to terminate if this unlikely error happens, since the user is
        // be trying to opt out and we can't silently fail in that case.
        throw new RuntimeException(e);
      }
    }
  }

  /** Prompts for consent and returns either {@code ConsentStatus.OPTED_IN} or
   * {@code ConsentStatus.OPTED_OUT}
   */
  private ConsentStatus promptForConsent() {
    outStream.print(CONSENT_PROMPT);
    outStream.flush(); // Prompt doesn't end in a newline and we want it to display.

    while (true) {
      String reply = Ascii.toLowerCase(inputScanner.nextLine().trim());

      if (reply.equals("y")) {
        return ConsentStatus.OPTED_IN;
      }

      if (reply.equals("n")) {
        return ConsentStatus.OPTED_OUT;
      }

      outStream.print(REPEAT_PROMPT);
      outStream.flush();
    }
  }

  enum ConsentStatus {
    // Don't change these names; they correspond with strings stored in Java Preferences.
    UNASKED,
    OPTED_IN,
    OPTED_OUT;
  }
}
