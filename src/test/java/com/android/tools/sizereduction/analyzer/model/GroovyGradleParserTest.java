/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.android.tools.sizereduction.analyzer.utils.TestUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class GroovyGradleParserTest {

  private static final String PROJECT_BUILD_FILE = "projects/simple_bundle_app/build.gradle";
  private static final String APP_BUILD_FILE = "projects/simple_bundle_app/app/build.gradle";
  private static final String DYNAMIC_FEATURE_BUILD_FILE =
      "projects/simple_bundle_app/dynamic_feature/build.gradle";
  private static final String PROGUARD_BUILD_FILE = "proguard_configs.build.gradle";
  private static final String VARIABLE_MINSDK_BUILD_FILE = "variable_minSdkVersion.build.gradle";

  @Test
  public void parsesPluginType_Application() throws Exception {
    File buildFile = TestUtils.getTestDataFile(APP_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1).build();
    assertThat(context.getPluginType()).isEqualTo(GradleContext.PluginType.APPLICATION);
  }

  @Test
  public void parsesPluginType_DynamicFeature() throws Exception {
    File buildFile = TestUtils.getTestDataFile(DYNAMIC_FEATURE_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1).build();
    assertThat(context.getPluginType()).isEqualTo(GradleContext.PluginType.DYNAMIC_FEATURE);
  }

  @Test
  public void parsesPluginType_Unknown() throws Exception {
    File buildFile = TestUtils.getTestDataFile(PROJECT_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1).build();
    assertThat(context.getPluginType()).isEqualTo(GradleContext.PluginType.UNKNOWN);
  }

  @Test
  public void parsesMinSdkVersion() throws Exception {
    File buildFile = TestUtils.getTestDataFile(APP_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1).build();
    assertThat(context.getMinSdkVersion()).isEqualTo(15);
  }

  @Test
  public void parsesMinSdkVersionInTupleExpression() throws Exception {
    File buildFile = TestUtils.getTestDataFile(DYNAMIC_FEATURE_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1).build();
    assertThat(context.getMinSdkVersion()).isEqualTo(14);
  }

  @Test
  public void parsesProguardConfigs() throws Exception {
    File buildFile = TestUtils.getTestDataFile(PROGUARD_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1).build();
    ImmutableMap<String, ProguardConfig> expectedMap =
        ImmutableMap.of(
            "release",
            ProguardConfig.builder()
                .setMinifyEnabled(true)
                .setHasProguardRules(true)
                .setObfuscationEnabled(true)
                .build(),
            "shrinkOnly",
            ProguardConfig.builder()
                .setMinifyEnabled(true)
                .setHasProguardRules(true)
                .setObfuscationEnabled(false)
                .build(),
            "debug",
            ProguardConfig.builder()
                .setMinifyEnabled(false)
                .setHasProguardRules(true)
                .setObfuscationEnabled(true)
                .build());

    assertThat(context.getProguardConfigs()).isEqualTo(expectedMap);
  }

  @Test
  public void variableMinSdkVersionDefaultsToDefaultvalue() throws Exception {
    File buildFile = TestUtils.getTestDataFile(VARIABLE_MINSDK_BUILD_FILE);
    int defaultMinSdkVersion = 345;
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context =
        GroovyGradleParser.parseGradleBuildFile(content, defaultMinSdkVersion).build();
    assertThat(context.getMinSdkVersion()).isEqualTo(defaultMinSdkVersion);
  }
}
