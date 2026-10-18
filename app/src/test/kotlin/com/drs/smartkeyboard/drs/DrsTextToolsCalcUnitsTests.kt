/*
 * Copyright (C) 2025-2026 The DRS Smart Keyboard Project
 */

package com.drs.smartkeyboard.drs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

/**
 * the twelfth text-tools round — the compute pair: the smart
 * calculator (CALCULATE, a hand-written recursive-descent parser over
 * the closed operator grammar) and the quantity converter
 * (CONVERT_UNITS, seven closed categories over exact factors with the
 * temperature trio speaking real formulas). Every expectation below
 * was simulated in Python BEFORE the build (the session doctrine) —
 * check_calc_units.py, 71 green checks — and both engines are
 * byte-faithful mirrors of that simulation. The simulation caught
 * real bugs BEFORE the build: the 12-SIGNIFICANT-digit (not
 * decimal-place) formatting, the parens missing from the firing
 * contract, and the canonical Arabic display token of explicit
 * targets.
 */
class DrsTextToolsCalcUnitsTests : FunSpec({

    val en = Locale.US

    // -----------------------------------------------------------
    // CALCULATE — the whole field IS the expression
    // -----------------------------------------------------------

    test("basic arithmetic answers with correct precedence") {
        fun calc(s: String) = DrsTextTools.apply(DrsTextTool.CALCULATE, s, en)
        calc("2+2") shouldBe "4"
        calc("2 + 2") shouldBe "4"
        calc("2+3×4") shouldBe "14"
        calc("(2+3)×4") shouldBe "20"
        calc("(50+25)÷3") shouldBe "25"
        calc("1250×12") shouldBe "15000"
    }

    test("eastern digits and separators fold onto the grammar") {
        fun calc(s: String) = DrsTextTools.apply(DrsTextTool.CALCULATE, s, en)
        calc("٥٠ × ٤") shouldBe "200"
        calc("١٫٥+١٫٥") shouldBe "3"
        calc("1,234+66") shouldBe "1300"
        calc("2\u00A0+\u00A02") shouldBe "4"
    }

    test("powers are right-associative and unary minus works") {
        fun calc(s: String) = DrsTextTools.apply(DrsTextTool.CALCULATE, s, en)
        calc("2^3^2") shouldBe "512"
        calc("2^10") shouldBe "1024"
        calc("2^40") shouldBe "1099511627776"
        calc("2^100") shouldBe "1267650600230000000000000000000"
        calc("-3×5") shouldBe "-15"
        calc("−3×٥") shouldBe "-15"
        calc("((2))") shouldBe "2"
    }

    test("percent binds to its own number — pure math documented") {
        fun calc(s: String) = DrsTextTools.apply(DrsTextTool.CALCULATE, s, en)
        calc("15%") shouldBe "0.15"
        calc("5%") shouldBe "0.05"
        calc("200×15%") shouldBe "30"
        calc("50%+50%") shouldBe "1"
    }

    test("the result never carries a float tail") {
        fun calc(s: String) = DrsTextTools.apply(DrsTextTool.CALCULATE, s, en)
        calc("0.1+0.2") shouldBe "0.3"
        calc("10÷4") shouldBe "2.5"
        calc("1÷3") shouldBe "0.333333333333"
        calc("0-0") shouldBe "0"
        calc("5-8") shouldBe "-3"
    }

    test("the firing contract: no operator, no attempt") {
        fun calc(s: String) = DrsTextTools.apply(DrsTextTool.CALCULATE, s, en)
        calc("1234") shouldBe "1234"
        calc("٤٥٦") shouldBe "٤٥٦"
        calc("hello") shouldBe "hello"
    }

    test("anything the grammar does not name refuses byte-identically") {
        for (hostile in listOf(
            "سعر 2+2 ريال",  // prose
            "(2+3",           // unbalanced paren
            "2+",             // dangling operator
            "()",             // empty parens
            "2+*3",           // double operator
            "5÷0",            // division by zero -> Infinity -> refuse
            "2a+3",           // a foreign letter inside
        )) {
            DrsTextTools.apply(DrsTextTool.CALCULATE, hostile, en) shouldBe hostile
        }
    }

    // -----------------------------------------------------------
    // CONVERT_UNITS — the whole field IS the quantity
    // -----------------------------------------------------------

    test("length speaks its ladder and skips its own line") {
        fun units(s: String) = DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, s, en)
        units("5 كم") shouldBe "5000 م\n500000 سم\n3.10685596119 ميل\n16404.1994751 قدم"
        units("100 cm") shouldBe "1 م\n0.001 كم\n0.000621371192237 ميل\n3.28083989501 قدم"
        units("1 mile") shouldBe "1609.344 م\n1.609344 كم\n160934.4 سم\n5280 قدم"
    }

    test("mass and volume speak their ladders") {
        fun units(s: String) = DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, s, en)
        units("3 kg") shouldBe "3000 جم\n0.003 طن\n6.61386786555 رطل"
        units("1.5 كغ") shouldBe "1500 جم\n0.0015 طن\n3.30693393277 رطل"
        units("2 ل") shouldBe "2000 مل\n0.528344104716 جالون\n8.33333333333 كوب"
        units("500 مل") shouldBe "0.5 ل\n0.132086026179 جالون\n2.08333333333 كوب"
    }

    test("temperature speaks real formulas, not factors") {
        fun units(s: String) = DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, s, en)
        units("38 C") shouldBe "100.4 °ف\n311.15 ك"
        units("38 سلسيوس") shouldBe "100.4 °ف\n311.15 ك"
        units("100 فهرنهايت") shouldBe "37.7777777778 °م\n310.927777778 ك"
        units("300 ك") shouldBe "26.85 °م\n80.33 °ف"
    }

    test("area, speed, data and time speak their ladders") {
        fun units(s: String) = DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, s, en)
        units("2 م²") shouldBe "0.000002 كم²\n0.0002 هكتار\n0.002 دونم"
        units("36 كم/س") shouldBe "10 م/س\n22.3693629205 ميل/س\n19.4384449244 عقدة"
        units("2 gb") shouldBe "2000000 كيلوبايت\n2000 ميجابايت"
        units("90 د") shouldBe "5400 ث\n1.5 ساعة\n0.0625 يوم"
        units("2 ساعة") shouldBe "7200 ث\n120 د\n0.0833333333333 يوم"
    }

    test("an explicit target answers ONE line in its arabic token") {
        fun units(s: String) = DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, s, en)
        units("5 كم إلى م") shouldBe "5000 م"
        units("5كم→م") shouldBe "5000 م"
        units("3 kg to g") shouldBe "3000 جم"
        units("1 mile to km") shouldBe "1.609344 كم"
        units("38 C إلى °ف") shouldBe "100.4 °ف"
        units("38 C to f") shouldBe "100.4 °ف"
        units("100 °ف إلى ك") shouldBe "310.927777778 ك"
    }

    test("signed, decimal and zero quantities all speak") {
        fun units(s: String) = DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, s, en)
        units("-5 كم") shouldBe "-5000 م\n-500000 سم\n-3.10685596119 ميل\n-16404.1994751 قدم"
        units("0 كم") shouldBe "0 م\n0 سم\n0 ميل\n0 قدم"
    }

    test("anything that is not one whole quantity refuses byte-identically") {
        for (hostile in listOf(
            "5 فرسخ",              // unknown unit
            "1234",                 // a bare number is not a quantity
            "كم",                   // a unit without a number
            "عمره 25 سنة",          // prose
            "5 كم إلى كغ",          // cross-category target
            "38 C إلى °م",          // the same temperature scale
            "38 س",                 // bare س is second-adjacent ambiguity — refused
            "100 ف",                // bare ف is not a closed scale spelling
        )) {
            DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, hostile, en) shouldBe hostile
        }
    }

    // -----------------------------------------------------------
    // the compute pair's codes and dispatch
    // -----------------------------------------------------------

    test("codes sit inside the dispatch range and resolve back") {
        DrsTextTool.CALCULATE.code shouldBe -679
        DrsTextTool.CONVERT_UNITS.code shouldBe -680
        DrsTextTool.fromCode(-679) shouldBe DrsTextTool.CALCULATE
        DrsTextTool.fromCode(-680) shouldBe DrsTextTool.CONVERT_UNITS
        (DrsTextTool.CALCULATE.code in DrsTextTool.CODE_RANGE).shouldBe(true)
        (DrsTextTool.CONVERT_UNITS.code in DrsTextTool.CODE_RANGE).shouldBe(true)
    }

    test("the pair is idempotent on its own output") {
        val once = DrsTextTools.apply(DrsTextTool.CALCULATE, "1250×12", en)
        once shouldBe "15000"
        DrsTextTools.apply(DrsTextTool.CALCULATE, once, en) shouldBe once
        val ladder = DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, "5 كم", en)
        // the ladder output is prose-like — the whole-text shape refuses it.
        DrsTextTools.apply(DrsTextTool.CONVERT_UNITS, ladder, en) shouldBe ladder
    }
})
