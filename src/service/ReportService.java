package service;

import dao.EvaluationDAO;
import model.Evaluation;
import util.FileLogger;
import util.ReportExporter;
import java.util.List;
import java.util.Map;

/**
 * Report Service - generates performance reports (uses Collections & Generics)
 */
public class ReportService {

    private final EvaluationDAO evaluationDAO = new EvaluationDAO();

    public String generateEmployeeReport(int employeeId) throws Exception {
        List<Evaluation> evals = evaluationDAO.getByEmployeeId(employeeId);
        double avg = evaluationDAO.getAverageScore(employeeId);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performance Report for Employee #").append(employeeId).append(" ===\n");
        sb.append("Total Evaluations: ").append(evals.size()).append("\n");
        sb.append("Average Score: ").append(String.format("%.2f", avg)).append("/5.0\n\n");
        for (Evaluation e : evals) {
            sb.append(String.format("Date: %s | Score: %.2f | C:%d T:%d P:%d W:%d | %s%n",
                    e.getEvalDate(), e.getOverallScore(), e.getCommunication(), e.getTechnical(),
                    e.getPunctuality(), e.getTeamwork(), e.getComments()));
        }
        FileLogger.logInfo("Report", "Generated report for employee " + employeeId);
        return sb.toString();
    }

    public Map<Integer, List<Evaluation>> getTeamReport() throws Exception {
        return evaluationDAO.getGroupedByEmployee(); // Collections demo
    }

    public void exportTeamReportCSV(String filePath) throws Exception {
        List<Evaluation> all = evaluationDAO.getAll();
        ReportExporter.exportEvaluationsToCSV(all, filePath);
    }
}
