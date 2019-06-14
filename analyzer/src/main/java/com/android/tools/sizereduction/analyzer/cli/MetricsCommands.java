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

import com.android.tools.sizereduction.analyzer.cli.TelemetryConsentHelper.ConsentStatus;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** Commands for viewing and changing telemetry consent status. */
@Command(
    name = "metrics",
    mixinStandardHelpOptions = true,
    description = "Displays and changes preferences for anonymous data collection")
public final class MetricsCommands implements Callable<Void> {

  @Override
  public Void call() {
    TelemetryConsentHelper consentHelper = TelemetryConsentHelper.get();

    if (consentHelper.getSavedConsentStatus() == ConsentStatus.OPTED_IN) {
      System.out.println("Currently sending anonymous usage and app size statistics to Google.");
    } else {
      System.out.println("Not sending any statistics to Google.");
    }

    System.out.println(
        "\nYou can update this preference with these commands:\n"
            + "size-analyzer metrics enable\n"
            + "size-analyzer metrics disable");

    return null;
  }

  @Command(
      name = "enable",
      mixinStandardHelpOptions = true,
      description = "Enable sending sending anonymous usage and app size statistics to Google.")
  public Void enable() throws Exception {
    TelemetryConsentHelper.get().updateSavedConsentStatus(ConsentStatus.OPTED_IN);
    System.out.println("Metrics collection has been enabled.");
    return null;
  }

  @Command(
      name = "disable",
      mixinStandardHelpOptions = true,
      description = "Disable sending sending anonymous usage and app size statistics to Google.")
  public Void disable() throws Exception {
    TelemetryConsentHelper.get().updateSavedConsentStatus(ConsentStatus.OPTED_OUT);
    System.out.println("Metrics collection has been disabled.");
    return null;
  }
}
