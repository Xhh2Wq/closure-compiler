/*
 * Copyright 2017 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import com.google.javascript.jscomp.CompilerOptions.LanguageMode;

/** Unit tests for {@link Es6CheckModule} */
public final class Es6CheckModuleTest extends CompilerTestCase {

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new Es6CheckModule(compiler);
  }

  /** Specify EcmaScript 2015 (ES6) for tests */
  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    options.setLanguageIn(LanguageMode.ECMASCRIPT_2015);
    options.setLanguageOut(LanguageMode.ECMASCRIPT_2015);
    return options;
  }

  public void testEs6ThisWithExportModule() {
    testError("export {};\nfoo.call(this, 1, 2, 3);", Es6CheckModule.ES6_MODULE_REFERENCES_THIS);
  }

  public void testEs6ThisWithImportModule() {
    testError(
        LINE_JOINER.join("import ln from 'other.x'", "if (x) {", "  alert(this);", "}"),
        Es6CheckModule.ES6_MODULE_REFERENCES_THIS);
  }

  public void testEs6ThisWithConstructor() {
    testSame(
        LINE_JOINER.join(
            "class Foo {",
            "  constructor() {",
            "    this.x = 5;",
            "  }",
            "}",
            "",
            "exports = Foo;"));
  }
}
