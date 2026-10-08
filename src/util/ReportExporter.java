package util;

import model.Evaluation;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * File I/O - Export reports to CSV (demonstrates File I/O for rubric)
 */
public class ReportExporter {

    public static void exportEvaluationsToCSV(List<Evaluation> list, String filePath) throws IOException {
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write("Eval ID,Employee ID,Employee Name,Communication,Technical,Punctuality,Teamwork,Overall Score,Comments,Date\n");
            for (Evaluation e : list) {
                fw.write(String.format("%d,%d,%s,%d,%d,%d,%d,%.2f,\"%s\",%s%n",
                        e.getEvalId(), e.getEmployeeId(),
                        e.getEmployeeName() != null ? e.getEmployeeName() : "",
                        e.getCommunication(), e.getTechnical(), e.getPunctuality(), e.getTeamwork(),
                        e.getOverallScore(),
                        e.getComments() != null ? e.getComments().replace("\"", "'") : "",
                        e.getEvalDate()));
            }
        }
        FileLogger.logInfo("Export", "Exported " + list.size() + " evaluations to " + filePath);
    }

    public static void exportToText(String content, String filePath) throws IOException {
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(content);
        }
    }
}
