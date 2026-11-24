package util;

import model.Evaluation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReportExporter}.
 */
@DisplayName("ReportExporter")
class ReportExporterTest {

    /** A temporary directory for the exported files. */
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("a plain value is not quoted")
    void plainValueIsNotQuoted() {
        assertEquals("Solid work", ReportExporter.escape("Solid work"));
    }

    @Test
    @DisplayName("a value containing a comma is quoted")
    void commaIsQuoted() {
        assertEquals("\"Good, very good\"", ReportExporter.escape("Good, very good"));
    }

    @Test
    @DisplayName("an embedded quote is doubled")
    void embeddedQuoteIsDoubled() {
        assertEquals("\"He said \"\"great\"\"\"", ReportExporter.escape("He said \"great\""));
    }

    @Test
    @DisplayName("a line break is collapsed so the row stays on one line")
    void lineBreakIsCollapsed() {
        assertEquals("first second", ReportExporter.escape("first\nsecond"));
        assertEquals("first second", ReportExporter.escape("first\r\nsecond"));
    }

    @Test
    @DisplayName("a formula is neutralised with an apostrophe")
    void formulaIsNeutralised() {
        assertEquals("'=SUM(A1:A9)", ReportExporter.escape("=SUM(A1:A9)"));
        assertEquals("'+1", ReportExporter.escape("+1"));
        assertEquals("'-2", ReportExporter.escape("-2"));
        assertEquals("'@cmd", ReportExporter.escape("@cmd"));
    }

    @Test
    @DisplayName("a null value becomes an empty field")
    void nullValueBecomesEmpty() {
        assertEquals("", ReportExporter.escape(null));
    }

    @Test
    @DisplayName("the exported file has a header, a BOM and one row per evaluation")
    void exportsTheExpectedFile() throws IOException {
        Evaluation evaluation = new Evaluation();
        evaluation.setEvalId(1);
        evaluation.setEmployeeId(2);
        evaluation.setEmployeeName("Priya Sharma");
        evaluation.setCommunication(4);
        evaluation.setTechnical(5);
        evaluation.setPunctuality(4);
        evaluation.setTeamwork(5);
        evaluation.setComments("Excellent, very consistent");

        Path target = tempDir.resolve("report.csv");
        ReportExporter.exportEvaluationsToCSV(List.of(evaluation), target.toString());

        String content = Files.readString(target, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("﻿"), "The file should start with a UTF-8 BOM");
        assertTrue(content.contains("Eval ID,Employee ID"), "The header row should be present");
        assertTrue(content.contains("Priya Sharma"));
        assertTrue(content.contains("\"Excellent, very consistent\""),
                "A comment containing a comma should be quoted");
        assertTrue(content.contains("4.50"), "The overall score should be written with two decimals");
        assertTrue(content.contains("\r\n"), "Rows should be separated by CRLF");
    }

    @Test
    @DisplayName("an empty list still produces a header-only file")
    void exportsHeaderForAnEmptyList() throws IOException {
        Path target = tempDir.resolve("empty.csv");
        ReportExporter.exportEvaluationsToCSV(List.of(), target.toString());

        String content = Files.readString(target, StandardCharsets.UTF_8);
        assertTrue(content.contains("Eval ID"));
        assertFalse(content.contains("Priya"));
    }

    @Test
    @DisplayName("text is written to a file as-is")
    void exportsText() throws IOException {
        Path target = tempDir.resolve("report.txt");
        ReportExporter.exportToText("=== Performance Report ===\n", target.toString());
        assertEquals("=== Performance Report ===\n", Files.readString(target));
    }
}
