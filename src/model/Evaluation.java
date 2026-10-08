package model;

import java.time.LocalDate;

/**
 * Evaluation model - Performance Evaluation data
 */
public class Evaluation {
    private int evalId;
    private int employeeId;
    private int managerId;
    private String employeeName; // for display
    private int communication;   // 1-5
    private int technical;       // 1-5
    private int punctuality;     // 1-5
    private int teamwork;        // 1-5
    private double overallScore; // calculated
    private String comments;
    private LocalDate evalDate;

    public Evaluation() {
        this.evalDate = LocalDate.now();
    }

    public Evaluation(int employeeId, int managerId, int communication, int technical, int punctuality, int teamwork, String comments) {
        this.employeeId = employeeId;
        this.managerId = managerId;
        this.communication = communication;
        this.technical = technical;
        this.punctuality = punctuality;
        this.teamwork = teamwork;
        this.comments = comments;
        this.evalDate = LocalDate.now();
        this.overallScore = calculateScore();
    }

    // Polymorphism-ready method
    public double calculateScore() {
        return (communication + technical + punctuality + teamwork) / 4.0;
    }

    // Method overloading
    public double calculateScore(double weightComm, double weightTech, double weightPunct, double weightTeam) {
        return communication * weightComm + technical * weightTech + punctuality * weightPunct + teamwork * weightTeam;
    }

    // Getters & Setters
    public int getEvalId() { return evalId; }
    public void setEvalId(int evalId) { this.evalId = evalId; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public int getCommunication() { return communication; }
    public void setCommunication(int communication) { this.communication = communication; }
    public int getTechnical() { return technical; }
    public void setTechnical(int technical) { this.technical = technical; }
    public int getPunctuality() { return punctuality; }
    public void setPunctuality(int punctuality) { this.punctuality = punctuality; }
    public int getTeamwork() { return teamwork; }
    public void setTeamwork(int teamwork) { this.teamwork = teamwork; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDate getEvalDate() { return evalDate; }
    public void setEvalDate(LocalDate evalDate) { this.evalDate = evalDate; }

    @Override
    public String toString() {
        return "Evaluation{id=" + evalId + ", emp=" + employeeId + ", score=" + overallScore + ", date=" + evalDate + "}";
    }
}
