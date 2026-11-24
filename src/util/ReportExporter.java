package util;

import model.Evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Exports performance reports to CSV.
 *
 * <p>The writer follows RFC 4180: fields are quoted whenever they contain a
 * comma, a quote or a line break, embedded quotes are doubled, and rows are
 * separated by CRLF. Values that a spreadsheet would interpret as a formula
 * are prefixed with an apostrophe so that opening the file cannot execute
 * anything.
 */
public final class ReportExporter {

    /** Column header of the exported evaluation report. */
    private static final String HEADER =
            "Eval ID,Employee ID,Employee Name,Communication,Technical,Punctuality,Teamwork,"
                    + "Overall Score,Weighted Score,Comments,Date";

    /** Byte order mark that makes Excel read the file as UTF-8. */
    private static final char BOM = '﻿';

    /** Row separator required by RFC 4180. */
    private static final String LINE_SEPARATOR = "\r\n";

    private ReportExporter() {
        // Utility class - not instantiable.
    }

    /**
     * Writes a list of evaluations to a CSV file.
     *
     * @param list     the evaluations to export
     * @param filePath the destination path
     * @throws IOException when the file cannot be written
     */
    public static void exportEvaluationsToCSV(List<Evaluation> list, String filePath) throws IOException {
        Path target = Path.of(filePath);
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writer.write(HEADER);
            writer.write(LINE_SEPARATOR);
            for (Evaluation evaluation : list) {
                writer.write(toCsvRow(evaluation));
                writer.write(LINE_SEPARATOR);
            }
        }
        FileLogger.logInfo("Export", "Exported " + list.size() + " evaluations to " + filePath);
    }

    /**
     * Writes arbitrary text to a file.
     *
     * @param content  the text to write
     * @param filePath the destination path
     * @throws IOException when the file cannot be written
     */
    public static void exportToText(String content, String filePath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(filePath), StandardCharsets.UTF_8)) {
            writer.write(content == null ? "" : content);
        }
    }

    /**
     * Builds the CSV representation of a single evaluation.
     */
    private static String toCsvRow(Evaluation evaluation) {
        StringBuilder row = new StringBuilder();
        row.append(evaluation.getEvalId()).append(',');
        row.append(evaluation.getEmployeeId()).append(',');
        row.append(escape(evaluation.getEmployeeName())).append(',');
        row.append(evaluation.getCommunication()).append(',');
        row.append(evaluation.getTechnical()).append(',');
        row.append(evaluation.getPunctuality()).append(',');
        row.append(evaluation.getTeamwork()).append(',');
        row.append(String.format(Locale.ROOT, "%.2f", evaluation.calculateScore())).append(',');
        row.append(String.format(Locale.ROOT, "%.2f", weightedScore(evaluation))).append(',');
        row.append(escape(evaluation.getComments())).append(',');
        row.append(evaluation.getEvalDate() == null ? "" : evaluation.getEvalDate().toString());
        return row.toString();
    }

    /**
     * The weighted score used in reports: communication 30%, technical 35%,
     * punctuality 15%, teamwork 20%.
     */
    private static double weightedScore(Evaluation evaluation) {
        return evaluation.calculateScore(0.30, 0.35, 0.15, 0.20);
    }

    /**
     * Escapes a value so that it survives a round trip through a CSV reader.
     *
     * @param value the raw value; may be {@code null}
     * @return the escaped value, without the surrounding quotes
     */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        // Collapse line breaks - a raw newline would split the row in some readers.
        String cleaned = value.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ').trim();
        // Neutralise spreadsheet formula injection.
        if (!cleaned.isEmpty() && "=+-@".indexOf(cleaned.charAt(0)) >= 0) {
            cleaned = "'" + cleaned;
        }
        boolean needsQuotes = cleaned.indexOf(',') >= 0 || cleaned.indexOf('"') >= 0;
        String escaped = cleaned.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
