package service;

import dao.EvaluationDAO;
import model.Evaluation;
import util.FileLogger;
import util.ReportExporter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds performance reports.
 *
 * <p>Every method here is scoped: an employee report only ever covers that
 * employee, and a manager report only ever covers the evaluations the given
 * manager actually recorded. The service is the layer where the reporting
 * rules live, so the dashboards stay free of formatting code.
 */
public class ReportService {

    /** Data access for the {@code evaluations} table. */
    private final EvaluationDAO evaluationDAO = new EvaluationDAO();

    /**
     * Builds a plain-text report for one employee.
     *
     * @param employeeId the employee to report on
     * @return the report text
     * @throws SQLException if the evaluations cannot be read
     */
    public String generateEmployeeReport(int employeeId) throws SQLException {
        List<Evaluation> evaluations = evaluationDAO.getByEmployeeId(employeeId);
        double average = evaluationDAO.getAverageScore(employeeId);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performance Report for Employee #").append(employeeId).append(" ===\n");
        sb.append("Total Evaluations: ").append(evaluations.size()).append("\n");
        sb.append("Average Score: ")
                .append(String.format(Locale.ROOT, "%.2f", average)).append("/5.0\n\n");
        for (Evaluation evaluation : evaluations) {
            sb.append(String.format(Locale.ROOT,
                    "Date: %s | Score: %.2f | C:%d T:%d P:%d W:%d | %s%n",
                    evaluation.getEvalDate(), evaluation.getOverallScore(),
                    evaluation.getCommunication(), evaluation.getTechnical(),
                    evaluation.getPunctuality(), evaluation.getTeamwork(),
                    evaluation.getComments()));
        }
        FileLogger.logInfo("Report", "Generated report for employee " + employeeId);
        return sb.toString();
    }

    /**
     * Groups every evaluation by the employee it belongs to.
     *
     * @return a map of employee id to that employee's evaluations
     * @throws SQLException if the evaluations cannot be read
     */
    public Map<Integer, List<Evaluation>> getTeamReport() throws SQLException {
        return evaluationDAO.getGroupedByEmployee();
    }

    /**
     * Exports every evaluation to a CSV file.
     *
     * <p>Used by the manager dashboard's "export all" action.
     *
     * @param filePath where to write the CSV
     * @throws SQLException if the evaluations cannot be read
     * @throws IOException  if the file cannot be written
     */
    public void exportTeamReportCSV(String filePath) throws SQLException, IOException {
        List<Evaluation> all = evaluationDAO.getAll();
        ReportExporter.exportEvaluationsToCSV(all, filePath);
        FileLogger.logInfo("Report", "Exported " + all.size() + " evaluations to " + filePath);
    }

    /**
     * Exports the evaluations of one manager's team to a CSV file.
     *
     * <p>Only the evaluations the manager recorded are included, so a manager
     * can never export another manager's team.
     *
     * @param managerId the manager whose evaluations are exported
     * @param filePath  where to write the CSV
     * @return the number of exported rows
     * @throws SQLException if the evaluations cannot be read
     * @throws IOException  if the file cannot be written
     */
    public int exportManagerReportCSV(int managerId, String filePath) throws SQLException, IOException {
        List<Evaluation> evaluations = evaluationDAO.getByManagerId(managerId);
        ReportExporter.exportEvaluationsToCSV(evaluations, filePath);
        FileLogger.logInfo("Report",
                "Exported " + evaluations.size() + " evaluations for manager " + managerId + " to " + filePath);
        return evaluations.size();
    }

    /**
     * Exports one employee's evaluations to a CSV file.
     *
     * @param employeeId the employee to export
     * @param filePath  where to write the CSV
     * @return the number of exported rows
     * @throws SQLException if the evaluations cannot be read
     * @throws IOException  if the file cannot be written
     */
    public int exportEmployeeReportCSV(int employeeId, String filePath) throws SQLException, IOException {
        List<Evaluation> evaluations = evaluationDAO.getByEmployeeId(employeeId);
        ReportExporter.exportEvaluationsToCSV(evaluations, filePath);
        FileLogger.logInfo("Report",
                "Exported " + evaluations.size() + " evaluations for employee " + employeeId + " to " + filePath);
        return evaluations.size();
    }
}
