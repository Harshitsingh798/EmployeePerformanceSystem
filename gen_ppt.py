#!/usr/bin/env python3
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
import os

DARK = RGBColor(27, 42, 74)
INK = RGBColor(16, 32, 56)
ACCENT = RGBColor(41, 98, 255)
TEAL = RGBColor(0, 105, 92)
LIGHT = RGBColor(246, 248, 252)
WHITE = RGBColor(255, 255, 255)
BLACK = RGBColor(0, 0, 0)
GRAY = RGBColor(105, 115, 130)
CARD = RGBColor(38, 56, 94)
BORDER = RGBColor(226, 232, 240)
GREEN = RGBColor(0, 167, 106)
RED = RGBColor(200, 53, 69)
MUTED = RGBColor(144, 157, 174)
ORANGE = RGBColor(255, 152, 0)

SHOTS = "docs/screenshots"
TEAM = [
    ("Abhishek Kumar", "abhishek.25scse1010473@galgotiasuniversity.ac.in"),
    ("Harshit Singh", "harshit.25scse1010279@galgotiasuniversity.ac.in"),
    ("Amit Yadav", "amit.25scse1010394@galgotiasuniversity.ac.in"),
    ("Aditya Yadav", "aditya.25scse1010373@galgotiasuniversity.ac.in"),
]

prs = Presentation()
prs.slide_width = Inches(13.33)
prs.slide_height = Inches(7.5)

def bg(s, c):
    s.background.fill.solid()
    s.background.fill.fore_color.rgb = c

def txt(s, l, t, w, h, text, size=12, bold=False, color=INK, align=PP_ALIGN.LEFT, italic=False):
    b = s.shapes.add_textbox(l, t, w, h)
    f = b.text_frame
    f.word_wrap = True
    p = f.paragraphs[0]
    p.text = text
    p.font.size = Pt(size)
    p.font.bold = bold
    p.font.color.rgb = color
    p.font.name = "Calibri"
    p.font.italic = italic
    p.alignment = align
    return b

def card(s, l, t, w, h, fill=WHITE, line=BORDER):
    sh = s.shapes.add_shape(1, l, t, w, h)
    try: sh.adjustments[0] = 0.08
    except Exception: pass
    sh.fill.solid(); sh.fill.fore_color.rgb = fill
    sh.line.color.rgb = line; sh.shadow.inherit = False
    return sh

def header(s, kicker, title, accent=ACCENT):
    txt(s, Inches(0.55), Inches(0.26), Inches(12.2), Inches(0.3), kicker, 11, True, accent)
    txt(s, Inches(0.55), Inches(0.6), Inches(12.2), Inches(0.55), title, 26, True, INK)

def footer(s, text="Performetrics  •  GUVI HCL Hackathon  •  Review 2"):
    txt(s, Inches(0.55), Inches(7.06), Inches(12.2), Inches(0.3), text, 8.5, False, MUTED)

def pic(s, name, l, t, width=None, height=None):
    p = os.path.join(SHOTS, name)
    if not os.path.exists(p):
        txt(s, l, t, width or Inches(4), Inches(0.4), "[missing " + name + "]", 9, False, RED)
        return None
    if width is not None and height is not None:
        return s.shapes.add_picture(p, l, t, width, height)
    if width is not None:
        return s.shapes.add_picture(p, l, t, width=width)
    if height is not None:
        return s.shapes.add_picture(p, l, t, height=height)
    return s.shapes.add_picture(p, l, t)

def bullets(s, l, t, w, items, size=10, color=INK, step=Inches(0.4)):
    y = t
    for it in items:
        txt(s, l, y, w, Inches(0.32), "•  " + it, size, False, color)
        y += step
    return y

# Slide 1 — Title
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, DARK)
txt(s, Inches(0.7), Inches(0.5), Inches(12), Inches(0.35), "GUVI HCL HACKATHON  •  PROBLEM #18  •  REVIEW 2", 11, True, ACCENT)
txt(s, Inches(0.7), Inches(1.15), Inches(12), Inches(1.2), "Performetrics", 60, True, WHITE)
txt(s, Inches(0.7), Inches(2.42), Inches(12), Inches(0.5), "Employee Performance Evaluation System", 24, True, RGBColor(200, 212, 234))
txt(s, Inches(0.7), Inches(3.02), Inches(12), Inches(0.4), "Core Java  •  Swing + FlatLaf  •  JDBC + MySQL  •  JFreeChart  •  JUnit 5", 12, False, RGBColor(150, 166, 194))
y = Inches(4.55)
for i, (name, email) in enumerate(TEAM):
    x = Inches(0.7) + i * Inches(2.95)
    card(s, x, y, Inches(2.75), Inches(1.5), CARD, CARD)
    txt(s, x + Inches(0.18), y + Inches(0.14), Inches(2.4), Inches(0.35), name, 13, True, WHITE)
    txt(s, x + Inches(0.18), y + Inches(0.53), Inches(2.4), Inches(0.32), email, 9, False, RGBColor(170, 185, 210))
txt(s, Inches(0.7), Inches(6.7), Inches(12), Inches(0.3), "github.com/Harshitsingh798/EmployeePerformanceSystem", 9.5, False, RGBColor(144, 157, 174))

# Slide 2 — Problem
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "01  •  THE PROBLEM", "What the System Solves")
card(s, Inches(0.55), Inches(1.35), Inches(6.05), Inches(4.75))
txt(s, Inches(0.85), Inches(1.62), Inches(5.45), Inches(0.3), "THE CHALLENGE", 10, True, RED)
bullets(s, Inches(0.85), Inches(2.05), Inches(5.45), [
    "Managers need a fair, repeatable way to evaluate people",
    "Employees have no visibility of their own progress",
    "Goals live in scattered documents and chat threads",
    "Feedback is unstructured and hard to look back on",
    "Reports are built by hand every single month",
])
card(s, Inches(6.75), Inches(1.35), Inches(6.05), Inches(4.75))
txt(s, Inches(7.05), Inches(1.62), Inches(5.45), Inches(0.3), "THE SOLUTION", 10, True, GREEN)
bullets(s, Inches(7.05), Inches(2.05), Inches(5.45), [
    "One evaluation on four criteria, with the score derived",
    "Everything an employee sees is scoped to their own id",
    "Manager-assigned and personal goals in one place",
    "Feedback with a rating, a comment and a date",
    "Live charts straight from the database, plus CSV export",
])
txt(s, Inches(0.55), Inches(6.35), Inches(12.2), Inches(0.4), "A desktop application running against a real MySQL database — no mock data, no fake screens.", 12, True, ACCENT, PP_ALIGN.CENTER)
footer(s)

# Slide 3 — Screenshots (big, clean)
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "02  •  THE APPLICATION", "Real Screenshots, Real Database")
pic(s, "02-manager-dashboard.png", Inches(0.45), Inches(1.3), width=Inches(6.15))
txt(s, Inches(0.45), Inches(5.05), Inches(6.15), Inches(0.3), "Manager dashboard — performance evaluation", 10, True, INK, PP_ALIGN.CENTER)
pic(s, "04-employee-dashboard.png", Inches(6.75), Inches(1.3), width=Inches(6.15))
txt(s, Inches(6.75), Inches(5.05), Inches(6.15), Inches(0.3), "Employee dashboard — performance tracking", 10, True, INK, PP_ALIGN.CENTER)
card(s, Inches(0.55), Inches(5.45), Inches(12.25), Inches(1.35))
txt(s, Inches(0.85), Inches(5.62), Inches(11.6), Inches(0.3), "BOTH DASHBOARDS LOAD LIVE DATA", 9, True, ACCENT)
txt(s, Inches(0.85), Inches(5.97), Inches(11.6), Inches(0.7), "Every table and chart above was rendered by the running application against a MySQL 8 database. Screenshots captured by src/tools/ScreenshotTool.java.", 10, False, INK)
footer(s)

# Slide 4 — Architecture
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "03  •  ARCHITECTURE", "Layered, Modular, Testable")
layers = [
    ("UI LAYER", "Swing + FlatLaf\nLoginFrame · ManagerDashboard\nEmployeeDashboard · ChartPanel", ACCENT),
    ("SERVICE LAYER", "ReportService\nReporting rules live here", TEAL),
    ("DAO LAYER", "DAO<T> interface + DAOs\nDatabaseConnection (singleton)\nConnectionPool", GREEN),
    ("MODEL + MySQL", "User → Manager / Employee\nEvaluation · Goal · Feedback · Team\nMySQL 8 with 6 tables", ORANGE),
]
x = Inches(0.55)
for title, body, color in layers:
    card(s, x, Inches(1.45), Inches(2.85), Inches(2.4))
    band = s.shapes.add_shape(1, x, Inches(1.45), Inches(2.85), Inches(0.45))
    band.fill.solid(); band.fill.fore_color.rgb = color; band.line.color.rgb = color
    txt(s, x + Inches(0.15), Inches(1.53), Inches(2.55), Inches(0.3), title, 10, True, WHITE, PP_ALIGN.CENTER)
    txt(s, x + Inches(0.2), Inches(2.05), Inches(2.45), Inches(1.6), body, 8.5, False, INK, PP_ALIGN.CENTER)
    if x < Inches(9):
        txt(s, x + Inches(2.88), Inches(2.35), Inches(0.3), Inches(0.3), "→", 15, False, MUTED, PP_ALIGN.CENTER)
    x += Inches(3.075)
card(s, Inches(0.55), Inches(4.05), Inches(6.05), Inches(2.6))
txt(s, Inches(0.85), Inches(4.25), Inches(5.45), Inches(0.3), "THE RULES THAT KEEP IT HONEST", 9, True, ACCENT)
bullets(s, Inches(0.85), Inches(4.65), Inches(5.45), [
    "A layer may only call the layer below it",
    "The UI never writes SQL",
    "Every statement is a PreparedStatement",
    "Ownership is a WHERE clause, not a UI check",
], size=9)
card(s, Inches(6.75), Inches(4.05), Inches(6.05), Inches(2.6))
txt(s, Inches(7.05), Inches(4.25), Inches(5.45), Inches(0.3), "ONE INTERFACE, EVERY TABLE", 9, True, TEAL)
txt(s, Inches(7.05), Inches(4.62), Inches(5.45), Inches(0.95), "public interface DAO<T> {\n    boolean add(T entity);\n    T getById(int id);\n    List<T> getAll();\n    boolean update(T entity);\n    boolean delete(int id);\n}", 8, False, INK)
txt(s, Inches(7.05), Inches(5.95), Inches(5.45), Inches(0.55), "EvaluationDAO implements DAO<Evaluation>, GoalDAO implements DAO<Goal> — one contract, type-safe everywhere.", 8.5, False, MUTED)
footer(s)

# Slide 5 — Database design
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "04  •  DATABASE DESIGN", "6 Tables, Real Relationships")
tables = [
    ("users", "PK user_id\nname, email UNIQUE\npassword  (PBKDF2)\nrole ENUM, manager_id FK", Inches(0.55), Inches(1.4)),
    ("evaluations", "PK eval_id\nemployee_id FK, manager_id FK\n4 × rating CHECK 1-5\noverall_score, eval_date", Inches(3.55), Inches(1.4)),
    ("goals", "PK goal_id\nassigned_to FK, assigned_by FK\ntarget_date, status ENUM\nis_personal", Inches(6.55), Inches(1.4)),
    ("feedback", "PK feedback_id\nemployee_id FK, manager_id FK\neval_id FK (optional)\nrating CHECK 1-5", Inches(9.55), Inches(1.4)),
    ("teams", "PK team_id\nteam_name\nmanager_id FK", Inches(2.0), Inches(3.9)),
    ("team_members", "PK (team_id, employee_id)\nrole", Inches(5.0), Inches(3.9)),
]
for name, columns, l, t in tables:
    card(s, l, t, Inches(2.85), Inches(1.95))
    txt(s, l + Inches(0.15), t + Inches(0.12), Inches(2.55), Inches(0.28), name, 10, True, ACCENT, PP_ALIGN.CENTER)
    txt(s, l + Inches(0.15), t + Inches(0.46), Inches(2.55), Inches(1.35), columns, 8, False, INK, PP_ALIGN.CENTER)
txt(s, Inches(0.55), Inches(3.42), Inches(12.2), Inches(0.3), "users  1:N  evaluations · goals · feedback      users  1:N  users (reports to)      teams  M:N  users", 9, True, GRAY, PP_ALIGN.CENTER)
card(s, Inches(8.1), Inches(3.9), Inches(4.7), Inches(2.1))
txt(s, Inches(8.35), Inches(4.05), Inches(4.2), Inches(0.28), "CONNECTIVITY", 9, True, TEAL)
bullets(s, Inches(8.35), Inches(4.4), Inches(4.2), [
    "DatabaseConnection singleton + ConnectionPool",
    "db.properties on disk; defaults as fallback",
    "Credentials never committed — db.properties.example only",
    "Tested against a live MySQL database",
], size=8)
txt(s, Inches(0.55), Inches(6.15), Inches(12.2), Inches(0.3), "db/schema.sql  •  db/seed_data.sql  •  db/migration_v2.sql  •  docs/ER_DIAGRAM.md", 9, False, MUTED, PP_ALIGN.CENTER)
footer(s)

# Slide 6 — Security
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "05  •  SECURITY", "Passwords and Access Control Done Properly")
card(s, Inches(0.55), Inches(1.4), Inches(6.05), Inches(4.95))
txt(s, Inches(0.85), Inches(1.62), Inches(5.45), Inches(0.3), "PBKDF2-HMAC-SHA256", 11, True, TEAL)
txt(s, Inches(0.85), Inches(2.0), Inches(5.45), Inches(0.5), "pbkdf2$<iterations>$<salt>$<hash>", 11, True, INK)
bullets(s, Inches(0.85), Inches(2.55), Inches(5.45), [
    "210,000 iterations — the OWASP recommendation",
    "A fresh random 16-byte salt per password",
    "Two users with the same password get different hashes",
    "Constant-time comparison, so timing leaks nothing",
    "Legacy plain-text and SHA-256 rows still verify",
], size=9)
txt(s, Inches(0.85), Inches(5.45), Inches(5.45), Inches(0.85), "Before: a bare SHA-256 digest — fast to brute force, no salt.\nNow: stretched, salted and self-describing.", 9, False, MUTED)
card(s, Inches(6.75), Inches(1.4), Inches(6.05), Inches(4.95))
txt(s, Inches(7.05), Inches(1.62), Inches(5.45), Inches(0.3), "ACCESS CONTROL IN THE SQL", 11, True, ACCENT)
txt(s, Inches(7.05), Inches(2.0), Inches(5.45), Inches(0.9), "SELECT * FROM goals\n WHERE goal_id = ?\n   AND assigned_to = ?;", 10, True, INK)
bullets(s, Inches(7.05), Inches(3.05), Inches(5.45), [
    "The employee's id comes from the session — never typed by the user",
    "Dialogs offer a picker filled from the manager's own team",
    "A UI bug cannot leak another user's data",
    "PreparedStatement everywhere",
], size=9)
footer(s)

# Slide 7 — UI/UX
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "06  •  UI / UX", "Clean, Responsive, Role-Based")
card(s, Inches(0.55), Inches(1.35), Inches(6.05), Inches(4.9))
txt(s, Inches(0.85), Inches(1.6), Inches(5.45), Inches(0.3), "MANAGER DASHBOARD", 11, True, ACCENT)
bullets(s, Inches(0.85), Inches(2.05), Inches(5.45), [
    "Evaluate employees — add, edit, delete, search",
    "Goal setting with target dates and status",
    "Feedback with a 1-5 rating and comment",
    "Performance reports — bar & pie charts, CSV export",
    "Team management — add members, assign roles, remove",
], size=9.5)
card(s, Inches(6.85), Inches(1.35), Inches(6.0), Inches(4.9))
txt(s, Inches(7.15), Inches(1.6), Inches(5.4), Inches(0.3), "EMPLOYEE DASHBOARD", 11, True, TEAL)
bullets(s, Inches(7.15), Inches(2.05), Inches(5.4), [
    "Performance tracking — score trend over time",
    "View every feedback comment received",
    "Set personal goals and track to completion",
    "Own criteria averages, CSV export",
    "Everything scoped to the signed-in employee",
], size=9.5)
txt(s, Inches(0.55), Inches(6.45), Inches(12.2), Inches(0.4), "FlatLaf light theme · validation on every form · role-scoped menus · debounced search", 10, True, GRAY, PP_ALIGN.CENTER)
footer(s)

# Slide 8 — Screenshots side by side
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "07  •  APPLICATION WALKTHROUGH", "Real App Screens")
pic(s, "01-login.png", Inches(0.6), Inches(1.35), width=Inches(3.9))
pic(s, "03-manager-reports.png", Inches(4.7), Inches(1.35), width=Inches(3.9))
pic(s, "05-employee-reports.png", Inches(8.8), Inches(1.35), width=Inches(3.9))
txt(s, Inches(0.6), Inches(5.45), Inches(12.1), Inches(0.3), "Login  •  Manager performance report  •  Employee performance report", 9, False, GRAY, PP_ALIGN.CENTER)
card(s, Inches(0.55), Inches(5.95), Inches(12.25), Inches(0.9))
txt(s, Inches(0.85), Inches(6.08), Inches(11.6), Inches(0.3), "Screenshots captured automatically by src/tools/ScreenshotTool.java from the running application.", 9.5, False, INK)
footer(s)

# Slide 9 — Team members
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "08  •  TEAM MEMBERS", "Who Built Performetrics")
y = Inches(1.6)
for name, email in TEAM:
    card(s, Inches(0.7), y, Inches(11.9), Inches(0.8), WHITE, BORDER)
    txt(s, Inches(1.0), y + Inches(0.16), Inches(9), Inches(0.35), name, 14, True, INK)
    txt(s, Inches(1.0), y + Inches(0.5), Inches(10), Inches(0.3), email, 10, False, GRAY)
    y += Inches(0.95)
footer(s)

# Slide 10 — Conclusion
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, DARK)
txt(s, Inches(0.7), Inches(0.5), Inches(12), Inches(0.35), "09  •  CONCLUSION", 11, True, ACCENT)
txt(s, Inches(0.7), Inches(0.95), Inches(12), Inches(0.6), "Complete, Verified and Ready to Run", 28, True, WHITE)
card(s, Inches(0.7), Inches(1.85), Inches(6.1), Inches(3.4), CARD, CARD)
txt(s, Inches(1.0), Inches(2.05), Inches(5.5), Inches(0.3), "WHAT IS PROVEN", 10, True, ACCENT)
y = Inches(2.5)
for item in [
    "All 9 manager features and 4 employee features work",
    "Authentication verified against a live MySQL database",
    "Modular layered code with package-by-responsibility",
    "Ownership enforced in SQL, not in the UI",
    "Public GitHub repository with a complete README",
]:
    txt(s, Inches(1.0), y, Inches(5.5), Inches(0.45), "✓  " + item, 10, False, WHITE)
    y += Inches(0.52)
card(s, Inches(7.2), Inches(1.85), Inches(5.45), Inches(3.4), CARD, CARD)
txt(s, Inches(7.5), Inches(2.05), Inches(4.85), Inches(0.3), "MARKING RUBRIC", 10, True, ACCENT)
y = Inches(2.5)
for item, marks in [
    ("Code quality & execution", "5 marks"),
    ("Code quality & testing", "10 marks"),
    ("Innovation & extra effort", "2 marks"),
]:
    txt(s, Inches(7.5), y, Inches(3.7), Inches(0.4), "✓  " + item, 10, False, WHITE)
    txt(s, Inches(11.0), y, Inches(1.45), Inches(0.4), marks, 10, True, ACCENT, PP_ALIGN.RIGHT)
    y += Inches(0.52)
txt(s, Inches(7.5), Inches(4.35), Inches(4.85), Inches(0.6), "PBKDF2 hashing · connection pooling · ownership enforced in SQL · debounced search · CSV export", 9, False, RGBColor(150, 165, 190))
card(s, Inches(0.7), Inches(5.5), Inches(11.95), Inches(1.05), ACCENT, ACCENT)
txt(s, Inches(1.0), Inches(5.7), Inches(11.35), Inches(0.45), "Thank You!", 22, True, WHITE, PP_ALIGN.CENTER)
txt(s, Inches(1.0), Inches(6.15), Inches(11.35), Inches(0.3), "github.com/Harshitsingh798/EmployeePerformanceSystem   •   setup and demo credentials in README.md", 10, False, WHITE, PP_ALIGN.CENTER)

out = "docs/Review2_Final_Performetrics.pptx"
prs.save(out)
print("saved", out, len(prs.slides._sldIdLst), "slides")
