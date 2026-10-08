package model;

import java.time.LocalDateTime;

/**
 * Team model - Manager creates teams
 */
public class Team {
    private int teamId;
    private String teamName;
    private int managerId;
    private LocalDateTime createdAt;

    public Team() {
        this.createdAt = LocalDateTime.now();
    }

    public Team(String teamName, int managerId) {
        this.teamName = teamName;
        this.managerId = managerId;
        this.createdAt = LocalDateTime.now();
    }

    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Team{id=" + teamId + ", name='" + teamName + "'}";
    }
}
