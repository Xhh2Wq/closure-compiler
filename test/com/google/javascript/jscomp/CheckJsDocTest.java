/*
 * Copyright 2015 The Closure Compiler Authors.
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

import static com.google.javascript.jscomp.CheckJSDoc.ANNOTATION_DEPRECATED;
import static com.google.javascript.jscomp.CheckJSDoc.ARROW_FUNCTION_AS_CONSTRUCTOR;
import static com.google.javascript.jscomp.CheckJSDoc.DEFAULT_PARAM_MUST_BE_MARKED_OPTIONAL;
import static com.google.javascript.jscomp.CheckJSDoc.DISALLOWED_MEMBER_JSDOC;
import static com.google.javascript.jscomp.CheckJSDoc.INVALID_MODIFIES_ANNOTATION;
import static com.google.javascript.jscomp.CheckJSDoc.INVALID_NO_SIDE_EFFECT_ANNOTATION;
import static com.google.javascript.jscomp.CheckJSDoc.MISPLACED_ANNOTATION;
import static com.google.javascript.jscomp.CheckJSDoc.MISPLACED_MSG_ANNOTATION;

import com.google.javascript.jscomp.CompilerOptions.LanguageMode;

/**
 * Tests for {@link CheckJSDoc}.
 *
 * @author chadkillingsworth@gmail.com (Chad Killingsworth)
 */

public final class CheckJsDocTest extends CompilerTestCase {

  @Override
  protected CompilerPass getProcessor(final Compiler compiler) {
    return new CheckJSDoc(compiler);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setAcceptedLanguage(LanguageMode.ECMASCRIPT_2017);
  }

  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    options.setWarningLevel(
        DiagnosticGroups.MISPLACED_TYPE_ANNOTATION, CheckLevel.WARNING);
    return options;
  }

  public void testInlineJsDoc_ES6() {
    testSame("function f(/** string */ x) {}");
    testSame("function f(/** number= */ x=3) {}");
    testSame("function f(/** !Object */ {x}) {}");
    testSame("function f(/** !Array */ [x]) {}");

    testWarning("function f([/** number */ x]) {}", MISPLACED_ANNOTATION);
  }

  // TODO(tbreisacher): These should be a MISPLACED_ANNOTATION warning instead of silently failing.
  public void testInlineJsDocInsideObjectParams() {
    testSame("function f({ prop: {/** string */ x} }) {}");
    testSame("function f({ prop: {x: /** string */ y} }) {}");
    testSame("function f({ /** number */ x }) {}");
    testSame("function f({ prop: /** number */ x }) {}");
  }

  public void testInvalidClassJsdoc() {
    testSame("class Foo { /** @param {number} x */ constructor(x) {}}");

    testWarning(
        "class Foo { /** @constructor */ constructor() {}}",
        DISALLOWED_MEMBER_JSDOC);

    testWarning(
        "class Foo { /** @interface */ constructor() {}}",
        DISALLOWED_MEMBER_JSDOC);

    testWarning(
        "class Foo { /** @extends {Foo} */ constructor() {}}",
        DISALLOWED_MEMBER_JSDOC);

    testWarning(
        "class Foo { /** @implements {Foo} */ constructor() {}}",
        DISALLOWED_MEMBER_JSDOC);
  }

  public void testMisplacedParamAnnotation() {
    testWarning(LINE_JOINER.join(
        "/** @param {string} x */ var Foo = goog.defineClass(null, {",
        "  constructor(x) {}",
        "});"), MISPLACED_ANNOTATION);

    testWarning(LINE_JOINER.join(
        "/** @param {string} x */ const Foo = class {",
        "  constructor(x) {}",
        "};"), MISPLACED_ANNOTATION);
  }

  public void testAbstract_method() {
    testSame("class Foo { /** @abstract */ doSomething() {}}");
    testSame(LINE_JOINER.join(
        "/** @constructor */",
        "var Foo = function() {};",
        "/** @abstract */",
        "Foo.prototype.something = function() {}"));
    testSame(LINE_JOINER.join(
        "/** @constructor */",
        "let Foo = function() {};",
        "/** @abstract */",
        "Foo.prototype.something = function() {}"));
    testSame(LINE_JOINER.join(
        "/** @constructor */",
        "const Foo = function() {};",
        "/** @abstract */",
        "Foo.prototype.something = function() {}"));
  }

  public void testAbstract_getter_setter() {
    testSame("class Foo { /** @abstract */ get foo() {}}");
    testSame("class Foo { /** @abstract */ set foo(val) {}}");
    testWarning("class Foo { /** @abstract */ static get foo() {}}", MISPLACED_ANNOTATION);
    testWarning("class Foo { /** @abstract */ static set foo(val) {}}", MISPLACED_ANNOTATION);
  }

  public void testAbstract_nonEmptyMethod() {
    testWarning(
        "class Foo { /** @abstract */ doSomething() { return 0; }}",
        MISPLACED_ANNOTATION);
    testWarning(
        LINE_JOINER.join(
            "/** @constructor */",
            "var Foo = function() {};",
            "/** @abstract */",
            "Foo.prototype.something = function() { return 0; }"),
        MISPLACED_ANNOTATION);
  }

  public void testAbstract_staticMethod() {
    testWarning(
        "class Foo { /** @abstract */ static doSomething() {}}",
        MISPLACED_ANNOTATION);
    testWarning(
        LINE_JOINER.join(
            "/** @constructor */",
            "var Foo = function() {};",
            "/** @abstract */",
            "Foo.something = function() {}"),
        MISPLACED_ANNOTATION);
  }

  public void testAbstract_class() {
    testSame("/** @abstract */ class Foo { constructor() {}}");
    testSame("/** @abstract */ exports.Foo = class {}");
    testSame("/** @abstract */ const Foo = class {}");
    testSame("/** @abstract @constructor */ exports.Foo = function() {}");
    testSame("/** @abstract @constructor */ var Foo = function() {};");
    testSame("/** @abstract @constructor */ var Foo = function() { var x = 1; };");
  }

  public void testAbstract_defineClass() {
    testSame("/** @abstract */ goog.defineClass(null, { constructor: function() {} });");
    testSame("/** @abstract */ var Foo = goog.defineClass(null, { constructor: function() {} });");
    testSame("/** @abstract */ ns.Foo = goog.defineClass(null, { constructor: function() {} });");
    testSame(LINE_JOINER.join(
        "/** @abstract */ ns.Foo = goog.defineClass(null, {",
        "  /** @abstract */ foo: function() {}",
        "});"));
    testSame(LINE_JOINER.join(
        "/** @abstract */ ns.Foo = goog.defineClass(null, {",
        "  /** @abstract */ foo() {}",
        "});"));
    testWarning("/** @abstract */ var Foo;", MISPLACED_ANNOTATION);
    testWarning(LINE_JOINER.join(
        "/** @abstract */ goog.defineClass(null, {",
        "  /** @abstract */ constructor: function() {}",
        "});"), MISPLACED_ANNOTATION);
    testWarning(LINE_JOINER.join(
        "/** @abstract */ goog.defineClass(null, {",
        "  /** @abstract */ constructor() {}",
        "});"), MISPLACED_ANNOTATION);
  }

  public void testAbstract_constructor() {
    testWarning(
        "class Foo { /** @abstract */ constructor() {}}",
        MISPLACED_ANNOTATION);
    // ES5 constructors are treated as class definitions and tested above.

    // This is valid if foo() returns an abstract class constructor
    testSame(
        "/** @constructor */ var C = foo(); /** @abstract */ C.prototype.method = function() {};");
  }

  public void testAbstract_field() {
    testWarning(
        "class Foo { constructor() { /** @abstract */ this.x = 1;}}",
        MISPLACED_ANNOTATION);
    testWarning(
        LINE_JOINER.join(
            "/** @constructor */",
            "var Foo = function() {",
            "  /** @abstract */",
            "  this.x = 1;",
            "};"),
        MISPLACED_ANNOTATION);
  }

  public void testAbstract_var() {
    testWarning(
        "class Foo { constructor() {/** @abstract */ var x = 1;}}",
        MISPLACED_ANNOTATION);
    testWarning(
        LINE_JOINER.join(
            "/** @constructor */",
            "var Foo = function() {",
            "  /** @abstract */",
            "  var x = 1;",
            "};"),
        MISPLACED_ANNOTATION);
  }

  public void testAbstract_function() {
    testWarning(
        "class Foo { constructor() {/** @abstract */ var x = function() {};}}",
        MISPLACED_ANNOTATION);
    testWarning(
        LINE_JOINER.join(
            "/** @constructor */",
            "var Foo = function() {",
            "  /** @abstract */",
            "  var x = function() {};",
            "};"),
        MISPLACED_ANNOTATION);
  }

  public void testInlineJSDoc() {
    testSame("function f(/** string */ x) {}");
    testSame("function f(/** @type {string} */ x) {}");

    testSame("var /** string */ x = 'x';");
    testSame("var /** @type {string} */ x = 'x';");
    testSame("var /** string */ x, /** number */ y;");
    testSame("var /** @type {string} */ x, /** @type {number} */ y;");
  }

  public void testFunctionJSDocOnMethods() {
    testSame("class Foo { /** @return {?} */ bar() {} }");
    testSame("class Foo { /** @return {?} */ static bar() {} }");
    testSame("class Foo { /** @return {?} */ get bar() {} }");
    testSame("class Foo { /** @param {?} x */ set bar(x) {} }");

    testSame("class Foo { /** @return {?} */ [bar]() {} }");
    testSame("class Foo { /** @return {?} */ static [bar]() {} }");
    testSame("class Foo { /** @return {?} */ get [bar]() {} }");
    testSame("class Foo { /** @return {?} x */ set [bar](x) {} }");
  }

  public void testObjectLiterals() {
    testSame("var o = { /** @type {?} */ x: y };");
    testWarning("var o = { x: /** @type {?} */ y };", MISPLACED_ANNOTATION);
  }

  public void testMethodsOnObjectLiterals() {
    testSame("var x = { /** @return {?} */ foo() {} };");
    testSame("var x = { /** @return {?} */ [foo]() {} };");
    testSame("var x = { /** @return {?} */ foo: someFn };");
    testSame("var x = { /** @return {?} */ [foo]: someFn };");
  }

  public void testExposeDeprecated() {
    testWarning("/** @expose */ var x = 0;", ANNOTATION_DEPRECATED);
  }

  public void testJSDocFunctionNodeAttachment() {
    testWarning("var a = /** @param {number} index */5;"
        + "/** @return boolean */function f(index){}", MISPLACED_ANNOTATION);
  }

  public void testJSDocDescAttachment() {
    testWarning(
        "function f() { return /** @type {string} */ (g(1 /** @desc x */)); };",
        MISPLACED_MSG_ANNOTATION);

    testWarning("/** @desc Foo. */ var bar = goog.getMsg('hello');",
        MISPLACED_MSG_ANNOTATION);
    testWarning("/** @desc Foo. */ x.y.z.bar = goog.getMsg('hello');",
        MISPLACED_MSG_ANNOTATION);
    testWarning("var msgs = {/** @desc x */ x: goog.getMsg('x')}",
        MISPLACED_MSG_ANNOTATION);
    testWarning("/** @desc Foo. */ bar = goog.getMsg('x');",
        MISPLACED_MSG_ANNOTATION);
  }

  public void testJSDocDescInExterns() {
    testWarning("/** @desc Foo. */ x.y.z.MSG_bar;", MISPLACED_MSG_ANNOTATION);
    testSame("/** @desc Foo. */ x.y.z.MSG_bar;", "");
  }

  public void testJSDocTypeAttachment() {
    testWarning(
        "function f() {  /** @type {string} */ if (true) return; };", MISPLACED_ANNOTATION);

    testWarning(
        "function f() {  /** @type {string} */  return; };", MISPLACED_ANNOTATION);
  }

  public void testJSDocOnExports() {
    testSame(LINE_JOINER.join(
        "goog.module('foo');",
        "/** @const {!Array<number>} */",
        "exports = [];"));
  }

  public void testMisplacedTypeAnnotation1() {
    // misuse with COMMA
    testWarning(
        "var o = {}; /** @type {string} */ o.prop1 = 1, o.prop2 = 2;", MISPLACED_ANNOTATION);
  }

  public void testMisplacedTypeAnnotation2() {
    // missing parentheses for the cast.
    testWarning(
        "var o = /** @type {string} */ getValue();",
        MISPLACED_ANNOTATION);
  }

  public void testMisplacedTypeAnnotation3() {
    // missing parentheses for the cast.
    testWarning(
        "var o = 1 + /** @type {string} */ value;",
        MISPLACED_ANNOTATION);
  }

  public void testMisplacedTypeAnnotation4() {
    // missing parentheses for the cast.
    testWarning(
        "var o = /** @type {!Array.<string>} */ ['hello', 'you'];",
        MISPLACED_ANNOTATION);
  }

  public void testMisplacedTypeAnnotation5() {
    // missing parentheses for the cast.
    testWarning(
        "var o = (/** @type {!Foo} */ {});",
        MISPLACED_ANNOTATION);
  }

  public void testMisplacedTypeAnnotation6() {
    testWarning(
        "var o = /** @type {function():string} */ function() {return 'str';}",
        MISPLACED_ANNOTATION);
  }

  public void testMisplacedTypeAnnotation7() {
    testWarning(
        "var x = /** @type {string} */ y;",
        MISPLACED_ANNOTATION);
  }

  public void testAllowedNocollapseAnnotation1() {
    testSame("var foo = {}; /** @nocollapse */ foo.bar = true;");
  }

  public void testAllowedNocollapseAnnotation2() {
    testSame(
        "/** @constructor */ function Foo() {};\n"
        + "var ns = {};\n"
        + "/** @nocollapse */ ns.bar = Foo.prototype.blah;");
  }

  public void testMisplacedNocollapseAnnotation1() {
    testWarning(
        "/** @constructor */ function foo() {};"
            + "/** @nocollapse */ foo.prototype.bar = function() {};",
        MISPLACED_ANNOTATION);
  }

  public void testNocollapseInExterns() {
    testSame("var foo = {}; /** @nocollapse */ foo.bar = true;",
        "foo.bar;", MISPLACED_ANNOTATION);
  }

  public void testArrowFuncAsConstructor() {
    testWarning("/** @constructor */ var a = ()=>{}; var b = a();",
        ARROW_FUNCTION_AS_CONSTRUCTOR);
    testWarning("var a = /** @constructor */ ()=>{}; var b = a();",
        ARROW_FUNCTION_AS_CONSTRUCTOR);
    testWarning("/** @constructor */ let a = ()=>{}; var b = a();",
        ARROW_FUNCTION_AS_CONSTRUCTOR);
    testWarning("/** @constructor */ const a = ()=>{}; var b = a();",
        ARROW_FUNCTION_AS_CONSTRUCTOR);
    testWarning("var a; /** @constructor */ a = ()=>{}; var b = a();",
        ARROW_FUNCTION_AS_CONSTRUCTOR);
  }

  public void testDefaultParam() {
    testWarning("function f(/** number */ x=0) {}", DEFAULT_PARAM_MUST_BE_MARKED_OPTIONAL);
    testSame("function f(/** number= */ x=0) {}");
  }

  private void testBadTemplate(String code) {
    testWarning(code, MISPLACED_ANNOTATION);
  }

  public void testGoodTemplate1() {
    testSame("/** @template T */ class C {}");
    testSame("class C { /** @template T \n @param {T} a\n @param {T} b \n */ "
        + "constructor(a,b){} }");
    testSame("class C {/** @template T \n @param {T} a\n @param {T} b \n */ method(a,b){} }");
    testSame("/** @template T \n @param {T} a\n @param {T} b\n */ var x = function(a, b){};");
    testSame("/** @constructor @template T */ var x = function(){};");
    testSame("/** @interface @template T */ var x = function(){};");
  }

  public void testGoodTemplate2() {
    testSame("/** @template T */ x.y.z = goog.defineClass(null, {constructor: function() {}});");
  }

  public void testGoodTemplate3() {
    testSame("var /** @template T */ x = goog.defineClass(null, {constructor: function() {}});");
  }

  public void testGoodTemplate4() {
    testSame("x.y.z = goog.defineClass(null, {/** @return T @template T */ m: function() {}});");
  }

  public void testBadTemplate1() {
    testBadTemplate("/** @template T */ foo();");
  }

  public void testBadTemplate2() {
    testBadTemplate(LINE_JOINER.join(
        "x.y.z = goog.defineClass(null, {",
        "  /** @template T */ constructor: function() {}",
        "});"));
  }

  public void testBadTemplate3() {
    testBadTemplate("/** @template T */ function f() {}");
    testBadTemplate("/** @template T */ var f = function() {};");
    testBadTemplate("/** @template T */ Foo.prototype.f = function() {};");
  }

  public void testBadTypedef() {
    testWarning(
        "/** @typedef {{foo: string}} */ class C { constructor() { this.foo = ''; }}",
        MISPLACED_ANNOTATION);

    testWarning(
        LINE_JOINER.join(
            "/** @typedef {{foo: string}} */",
            "var C = goog.defineClass(null, {",
            "  constructor: function() { this.foo = ''; }",
            "});"),
        MISPLACED_ANNOTATION);
  }

  public void testInvalidAnnotation1() throws Exception {
    testWarning("/** @nosideeffects */ function foo() {}", INVALID_NO_SIDE_EFFECT_ANNOTATION);
  }

  public void testInvalidAnnotation2() throws Exception {
    testWarning("var f = /** @nosideeffects */ function() {}", INVALID_NO_SIDE_EFFECT_ANNOTATION);
  }

  public void testInvalidAnnotation3() throws Exception {
    testWarning("/** @nosideeffects */ var f = function() {}", INVALID_NO_SIDE_EFFECT_ANNOTATION);
  }

  public void testInvalidAnnotation4() throws Exception {
    testWarning(
        "var f = function() {};" + "/** @nosideeffects */ f.x = function() {}",
        INVALID_NO_SIDE_EFFECT_ANNOTATION);
  }

  public void testInvalidAnnotation5() throws Exception {
    testWarning(
        "var f = function() {};" + "f.x = /** @nosideeffects */ function() {}",
        INVALID_NO_SIDE_EFFECT_ANNOTATION);
  }

  public void testInvalidModifiesAnnotation() throws Exception {
    testWarning("/** @modifies {this} */ var f = function() {};", INVALID_MODIFIES_ANNOTATION);
  }
}
