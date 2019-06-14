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

package com.android.tools.sizereduction.analyzer.model;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** Information regarding line numbers of BundleConfig fields inside the build.gradle file */
@AutoValue
public abstract class BundleConfigLocation {
  /** Line number of language splits. */
  @Nullable
  public abstract Integer getLanguageSplitLineNumber();

  /** Line number of density splits in */
  @Nullable
  public abstract Integer getDensitySplitLineNumber();

  /** Whether this abi splits are enabled. */
  @Nullable
  public abstract Integer getAbiSplitLineNumber();

  public static Builder builder() {
    return new AutoValue_BundleConfigLocation.Builder();
  }

  /** Builder for the {@link BundleConfigLocation}. */
  @AutoValue.Builder
  public abstract static class Builder {
    /** Sets line number of language splits field. */
    public abstract Builder setLanguageSplitLineNumber(Integer languageSplitLineNumber);

    /** Set line number of thee density splits field. */
    public abstract Builder setDensitySplitLineNumber(Integer densitySplitLineNumber);

    /** Set line number of abi splits field. */
    public abstract Builder setAbiSplitLineNumber(Integer abiSplitLineNumber);

    /** Build the bundle config object. */
    public abstract BundleConfigLocation build();
  }
}
