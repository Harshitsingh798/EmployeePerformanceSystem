#!/usr/bin/env python3
"""
Generates the Review 2 presentation for Performetrics.

    python gen_ppt.py

Produces docs/Review2_Final_Performetrics.pptx with the real application
screenshots from docs/screenshots/. All screenshots were captured from the
running application against a live MySQL database.
"""
import os

from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN

# ----------------------------------------------------------------- palette
DARK = RGBColor(27, 42, 74)      # deep navy — dark backgrounds
INK = RGBColor(16, 32, 56)       # card text
ACCENT = RGBColor(41, 98, 255)   # brand blue
TEAL = RGBColor(0, 105, 92)      # secondary accent
LIGHT = RGBColor(246, 248, 252)  # light slide background
WHITE = RGBColor(255, 255, 255)
BLACK = RGBColor(0, 0, 0)
GRAY = RGBColor(105, 115, 130)
CARD = RGBColor(38, 56, 94)      # dark card inside dark slides
BORDER = RGBColor(226, 232, 240)
GREEN = RGBColor(0, 167, 106)
RED = RGBColor(200, 53, 69)
MUTED = RGBColor(144, 157, 174)

SHOTS = "docs/screenshots"

prs = Presentation()
prs.slide_width = Inches(13.33)
prs.slide_height = Inches(7.5)

# ----------------------------------------------------------------- helpers
def bg(slide, color):
    """Fill the whole slide with one colour."""
    slide.background.fill.solid()
    slide.background.fill.fore_color.rgb = color


def txt(slide, left, top, width, height, text, size=12, bold=False,
        color=INK, align=PP_ALIGN.LEFT, font="Calibri", italic=False):
    """Add a text box with a single run of text."""
    box = slide.shapes.add_textbox(left, top, width, height)
    frame = box.text_frame
    frame.word_wrap = True
    paragraph = frame.paragraphs[0]
    paragraph.text = text
    paragraph.font.size = Pt(size)
    paragraph.font.bold = bold
    paragraph.font.color.rgb = color
    paragraph.font.name = font
    paragraph.font.italic = italic
    paragraph.alignment = align
    return box


def card(slide, left, top, width, height, fill=WHITE, line=BORDER,
         radius=0.08):
    """Add a rounded white card."""
    shape = slide.shapes.add_shape(1, left, top, width, height)
    try:
        shape.adjustments[0] = radius
    except Exception:
        pass
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.color.rgb = line
    shape.shadow.inherit = False
    return shape


def header(slide, kicker, title, accent=ACCENT):
    """Add the standard slide header."""
    txt(slide, Inches(0.55), Inches(0.26), Inches(12.2), Inches(0.3),
        kicker, 11, True, accent)
    txt(slide, Inches(0.55), Inches(0.6), Inches(12.2), Inches(0.55),
        title, 26, True, INK)


def footer(slide, text):
    """Add the standard slide footer."""
    txt(slide, Inches(0.55), Inches(7.06), Inches(12.2), Inches(0.3),
        text, 8.5, False, MUTED)


def picture(slide, name, left, top, width=None, height=None):
    """Add a screenshot, scaled to fit the given box."""
    path = os.path.join(SHOTS, name)
    if not os.path.exists(path):
        txt(slide, left, top, width or Inches(4), Inches(0.4),
            f"[missing {name}]", 9, False, RED)
        return None
    kwargs = {}
    if width is not None:
        kwargs["width"] = width
    if height is not None:
        kwargs["height"] = height
    return slide.shapes.add_picture(path, left, top, **kwargs)


def bullets(slide, left, top, width, items, size=10, color=INK,
            step=Inches(0.4), marker="•"):
    """Write a bulleted list."""
    y = top
    for item in items:
        txt(slide, left, y, width, Inches(0.32), f"{marker} {item}",
            size, False, color)
        y += step
    return y


def stat(slide, left, top, width, value, label, color=ACCENT):
    """Add a big number with a caption underneath."""
    txt(slide, left, top, width, Inches(0.62), value, 30, True, color,
        PP_ALIGN.CENTER)
    txt(slide, left, top + Inches(0.66), width, Inches(0.3), label, 9, False,
        MUTED, PP_ALIGN.CENTER)


# ------------------------------------------------------------------ slide 1 — title
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, DARK)
txt(s, Inches(0.7), Inches(0.5), Inches(12), Inches(0.35),
    "GUVI HCL HACKATHON  •  PROBLEM #18  •  REVIEW 2", 11, True, ACCENT)
txt(s, Inches(0.7), Inches(1.15), Inches(12), Inches(1.2),
    "Performetrics", 60, True, WHITE)
txt(s, Inches(0.7), Inches(2.42), Inches(12), Inches(0.5),
    "Employee Performance Evaluation System", 24, True,
    RGBColor(200, 212, 234))
txt(s, Inches(0.7), Inches(3.02), Inches(12), Inches(0.4),
    "Core Java  •  Swing + FlatLaf  •  JDBC + MySQL  •  JFreeChart  •  JUnit 5",
    12, False, RGBColor(150, 166, 194))

for i, (value, label) in enumerate([
    ("9", "manager features"),
    ("4", "employee features"),
    ("6", "database tables"),
    ("66", "automated tests"),
]):
    x = Inches(0.7) + i * Inches(2.35)
    card(s, x, Inches(3.9), Inches(2.15), Inches(1.35), CARD, CARD)
    stat(s, x, Inches(4.06), Inches(2.15), value, label, ACCENT)

txt(s, Inches(0.7), Inches(6.7), Inches(12), Inches(0.3),
    "Harshit Singh  •  github.com/Harshitsingh798/EmployeePerformanceSystem",
    9.5, False, RGBColor(144, 157, 174))

# ------------------------------------------------------------------ slide 2 — problem
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "01  •  THE PROBLEM", "What the System Solves")

card(s, Inches(0.55), Inches(1.35), Inches(6.05), Inches(4.75))
txt(s, Inches(0.85), Inches(1.62), Inches(5.45), Inches(0.3),
    "THE CHALLENGE", 10, True, RED)
bullets(s, Inches(0.85), Inches(2.05), Inches(5.45), [
    "Managers need a fair, repeatable way to evaluate people",
    "Employees have no visibility of their own progress",
    "Goals live in scattered documents and chat threads",
    "Feedback is unstructured and hard to look back on",
    "Reports are built by hand every single month",
], 10, INK, Inches(0.5))

card(s, Inches(6.75), Inches(1.35), Inches(6.05), Inches(4.75))
txt(s, Inches(7.05), Inches(1.62), Inches(5.45), Inches(0.3),
    "THE SOLUTION", 10, True, GREEN)
bullets(s, Inches(7.05), Inches(2.05), Inches(5.45), [
    "One evaluation on four criteria, with the score derived",
    "Everything an employee sees is scoped to their own id",
    "Manager-assigned and personal goals in one place",
    "Feedback with a rating, a comment and a date",
    "Live charts straight from the database, plus CSV export",
], 10, INK, Inches(0.5))

txt(s, Inches(0.55), Inches(6.35), Inches(12.2), Inches(0.4),
    "A desktop application running against a real MySQL database — no mock "
    "data, no fake screens.", 12, True, ACCENT, PP_ALIGN.CENTER)
footer(s, "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ------------------------------------------------------------------ slide 3 — application screenshots
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "02  •  THE APPLICATION", "Real Screenshots, Real Database")

picture(s, "02-manager-dashboard.png", Inches(0.45), Inches(1.3),
        width=Inches(6.15))
txt(s, Inches(0.45), Inches(5.05), Inches(6.15), Inches(0.3),
    "Manager dashboard — performance evaluation", 10, True, INK,
    PP_ALIGN.CENTER)

picture(s, "04-employee-dashboard.png", Inches(6.75), Inches(1.3),
        width=Inches(6.15))
txt(s, Inches(6.75), Inches(5.05), Inches(6.15), Inches(0.3),
    "Employee dashboard — performance tracking", 10, True, INK,
    PP_ALIGN.CENTER)

card(s, Inches(0.55), Inches(5.45), Inches(12.25), Inches(1.35))
txt(s, Inches(0.85), Inches(5.62), Inches(11.6), Inches(0.3),
    "BOTH DASHBOARDS LOAD LIVE DATA", 9, True, ACCENT)
txt(s, Inches(0.85), Inches(5.97), Inches(11.6), Inches(0.7),
    "Every table and chart above was rendered by the running application "
    "against a MySQL 8 database. Screenshots are captured from the live app "
    "by src/tools/ScreenshotTool.java.", 10, False, INK)
footer(s, "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ------------------------------------------------------------------ slide 4 — architecture
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "03  •  ARCHITECTURE", "Layered, Modular, Testable")

layers = [
    ("UI LAYER", "Swing + FlatLaf\nLoginFrame · ManagerDashboard\nEmployeeDashboard · ChartPanel", ACCENT),
    ("SERVICE LAYER", "ReportService\nReporting rules live here", TEAL),
    ("DAO LAYER", "DAO<T> interface + 5 DAOs\nDatabaseConnection (Singleton)\nConnectionPool", GREEN),
    ("MODEL + MySQL", "User → Manager / Employee\nEvaluation · Goal · Feedback · Team\nMySQL 8 with 6 tables", RGBColor(255, 152, 0)),
]
x = Inches(0.55)
for title, body, color in layers:
    card(s, x, Inches(1.45), Inches(2.85), Inches(2.4))
    band = s.shapes.add_shape(1, x, Inches(1.45), Inches(2.85), Inches(0.45))
    band.fill.solid()
    band.fill.fore_color.rgb = color
    band.line.color.rgb = color
    txt(s, x + Inches(0.15), Inches(1.53), Inches(2.55), Inches(0.3),
        title, 10, True, WHITE, PP_ALIGN.CENTER)
    txt(s, x + Inches(0.2), Inches(2.05), Inches(2.45), Inches(1.6),
        body, 8.5, False, INK, PP_ALIGN.CENTER)
    if x < Inches(9):
        txt(s, x + Inches(2.88), Inches(2.35), Inches(0.3), Inches(0.3),
            "→", 15, False, MUTED, PP_ALIGN.CENTER)
    x += Inches(3.075)

card(s, Inches(0.55), Inches(4.05), Inches(6.05), Inches(2.6))
txt(s, Inches(0.85), Inches(4.25), Inches(5.45), Inches(0.3),
    "THE RULES THAT KEEP IT HONEST", 9, True, ACCENT)
bullets(s, Inches(0.85), Inches(4.65), Inches(5.45), [
    "A layer may only call the layer below it",
    "The UI never writes SQL",
    "Every statement is a PreparedStatement",
    "Ownership is a WHERE clause, not a UI check",
], 9, INK, Inches(0.45))

card(s, Inches(6.75), Inches(4.05), Inches(6.05), Inches(2.6))
txt(s, Inches(7.05), Inches(4.25), Inches(5.45), Inches(0.3),
    "ONE INTERFACE, EVERY TABLE", 9, True, TEAL)
txt(s, Inches(7.05), Inches(4.62), Inches(5.45), Inches(0.95),
    "public interface DAO<T> {\n"
    "    boolean add(T entity) throws SQLException;\n"
    "    T getById(int id) throws SQLException;\n"
    "    List<T> getAll() throws SQLException;\n"
    "    boolean update(T entity) throws SQLException;\n"
    "    boolean delete(int id) throws SQLException;\n"
    "}", 8, False, INK)
txt(s, Inches(7.05), Inches(5.95), Inches(5.45), Inches(0.55),
    "EvaluationDAO implements DAO<Evaluation>, GoalDAO implements "
    "DAO<Goal> — one contract, type-safe everywhere.", 8.5, False, MUTED)
footer(s, "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ------------------------------------------------------------------ slide 5 — database
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
for name, columns, left, top in tables:
    card(s, left, top, Inches(2.85), Inches(1.95))
    txt(s, left + Inches(0.15), top + Inches(0.12), Inches(2.55), Inches(0.28),
        name, 10, True, ACCENT, PP_ALIGN.CENTER)
    txt(s, left + Inches(0.15), top + Inches(0.46), Inches(2.55), Inches(1.35),
        columns, 8, False, INK, PP_ALIGN.CENTER)

txt(s, Inches(0.55), Inches(3.42), Inches(12.2), Inches(0.3),
    "users  1:N  evaluations · goals · feedback      "
    "users  1:N  users (reports to)      teams  M:N  users",
    9, True, GRAY, PP_ALIGN.CENTER)

card(s, Inches(8.1), Inches(3.9), Inches(4.7), Inches(2.1))
txt(s, Inches(8.35), Inches(4.05), Inches(4.2), Inches(0.28),
    "DESIGN DECISIONS", 9, True, TEAL)
bullets(s, Inches(8.35), Inches(4.4), Inches(4.2), [
    "One goals table — personal and assigned differ by a flag",
    "Ratings are NOT NULL as well as CHECK-constrained",
    "overall_score is always derived from the four criteria",
    "ON DELETE CASCADE keeps the data consistent",
], 8, INK, Inches(0.36))

txt(s, Inches(0.55), Inches(6.15), Inches(12.2), Inches(0.3),
    "db/schema.sql  •  db/seed_data.sql  •  db/migration_v2.sql  •  "
    "docs/ER_DIAGRAM.md", 9, False, MUTED, PP_ALIGN.CENTER)
footer(s, "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ------------------------------------------------------------------ slide 6 — security
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "05  •  SECURITY", "Passwords and Access Control Done Properly")

card(s, Inches(0.55), Inches(1.4), Inches(6.05), Inches(4.95))
txt(s, Inches(0.85), Inches(1.62), Inches(5.45), Inches(0.3),
    "PBKDF2-HMAC-SHA256", 11, True, TEAL)
txt(s, Inches(0.85), Inches(2.0), Inches(5.45), Inches(0.5),
    "pbkdf2$<iterations>$<salt>$<hash>", 11, True, INK)
bullets(s, Inches(0.85), Inches(2.55), Inches(5.45), [
    "210,000 iterations — the OWASP recommendation",
    "A fresh random 16-byte salt per password",
    "Two users with the same password get different hashes",
    "Constant-time comparison, so timing leaks nothing",
    "Self-describing format: the iteration count can be raised later "
    "with no schema change",
    "Legacy plain-text and SHA-256 rows still verify and are upgraded "
    "silently on the next sign-in",
], 9, INK, Inches(0.42))
txt(s, Inches(0.85), Inches(5.45), Inches(5.45), Inches(0.85),
    "Before: a bare SHA-256 digest — fast to brute force, and no salt at "
    "all.\nNow: stretched, salted and self-describing.", 9, False, MUTED)

card(s, Inches(6.75), Inches(1.4), Inches(6.05), Inches(4.95))
txt(s, Inches(7.05), Inches(1.62), Inches(5.45), Inches(0.3),
    "ACCESS CONTROL IN THE SQL", 11, True, ACCENT)
txt(s, Inches(7.05), Inches(2.0), Inches(5.45), Inches(0.9),
    "SELECT * FROM goals\n"
    " WHERE goal_id = ?\n"
    "   AND assigned_to = ?;", 10, True, INK)
bullets(s, Inches(7.05), Inches(3.05), Inches(5.45), [
    "The employee's id comes from the session, never from a text field "
    "the user can type into",
    "Dialogs offer a JComboBox of the manager's own team, so only valid "
    "ids are on offer",
    "A bug in the interface cannot leak another user's data",
    "Verified by integration tests against a live database",
], 9, INK, Inches(0.42))
txt(s, Inches(7.05), Inches(5.45), Inches(5.45), Inches(0.85),
    "Also: PreparedStatement everywhere, CSV formula-injection defence, "
    "and no swallowed exceptions.", 9, False, MUTED)
footer(s, "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ------------------------------------------------------------------ slide 7 — testing
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "06  •  TESTING & QUALITY", "66 Automated Tests, Zero Warnings")

for i, (value, label) in enumerate([
    ("66", "tests, all passing"),
    ("0", "compiler warnings"),
    ("-Xlint:all", "every warning enabled"),
    ("20", "live database checks"),
]):
    x = Inches(0.55) + i * Inches(3.1)
    card(s, x, Inches(1.35), Inches(2.9), Inches(1.2))
    stat(s, x, Inches(1.45), Inches(2.9), value, label, ACCENT)

coverage = [
    ("EvaluationTest", "9 tests", "Score calculation, weighted score, rating bounds, equality"),
    ("GoalTest", "7 tests", "Deadline validation, status vocabulary"),
    ("FeedbackTest", "5 tests", "Rating validation, optional evaluation link"),
    ("PasswordUtilTest", "12 tests", "Hashing, salting, legacy formats, malformed input"),
    ("ValidatorTest", "10 tests", "Ratings, deadlines, e-mail, blank text"),
    ("ReportExporterTest", "8 tests", "RFC 4180 quoting, formula-injection defence"),
    ("DaoIntegrationTest", "15 tests", "Authentication, CRUD, ownership scoping, the pool"),
]
y = Inches(2.85)
for name, count, covers in coverage:
    card(s, Inches(0.55), y, Inches(7.65), Inches(0.52))
    txt(s, Inches(0.75), y + Inches(0.11), Inches(2.4), Inches(0.28),
        name, 9, True, INK)
    txt(s, Inches(3.2), y + Inches(0.11), Inches(0.9), Inches(0.28),
        count, 9, True, ACCENT)
    txt(s, Inches(4.15), y + Inches(0.13), Inches(3.9), Inches(0.28),
        covers, 8, False, MUTED)
    y += Inches(0.57)

card(s, Inches(8.4), Inches(2.85), Inches(4.4), Inches(4.0))
txt(s, Inches(8.65), Inches(3.05), Inches(3.9), Inches(0.3),
    "HOW TO RUN THEM", 9, True, TEAL)
txt(s, Inches(8.65), Inches(3.45), Inches(3.9), Inches(0.5),
    "run-tests.bat", 13, True, INK)
txt(s, Inches(8.65), Inches(3.95), Inches(3.9), Inches(0.6),
    "No Maven needed — just a JDK and the jars in lib/. "
    "With Maven, mvn test runs the same suite.", 9, False, MUTED)
txt(s, Inches(8.65), Inches(4.7), Inches(3.9), Inches(0.3),
    "THE DAO TESTS", 9, True, TEAL)
bullets(s, Inches(8.65), Inches(5.05), Inches(3.9), [
    "Run against a real MySQL, not a mock — that is the only way to "
    "prove the WHERE clauses exist",
    "Skip cleanly when no database is reachable",
    "Create and clean up their own rows",
], 8, INK, Inches(0.4))
footer(s, "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ------------------------------------------------------------------ slide 8 — audit fixes
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "07  •  WHAT WAS FIXED", "A Full Audit Before the Final Review")

card(s, Inches(0.55), Inches(1.35), Inches(12.25), Inches(0.55), DARK, DARK)
txt(s, Inches(0.8), Inches(1.47), Inches(11.7), Inches(0.3),
    "25 ISSUES FOUND  •  25 FIXED  •  0 OUTSTANDING", 12, True, WHITE,
    PP_ALIGN.CENTER)

fixes = [
    ("Demo passwords were dead", "The migration hashed \"password\" into every "
     "account. Now real PBKDF2 hashes, generated by a tool.", RED),
    ("One shared Connection", "The first DAO to finish closed it for everyone "
     "else. Now a pooled, thread-safe connection set.", RED),
    ("Employees could read any goal", "Goals were fetched by id alone. Now every "
     "read and write is scoped in SQL.", RED),
    ("The tracking chart was blank", "Two panels shared one field, so the chart "
     "was drawn on the wrong tab.", RGBColor(255, 152, 0)),
    ("Plain SHA-256 passwords", "Replaced with PBKDF2, 210,000 iterations, "
     "random salt, constant-time compare.", RED),
    ("Errors were swallowed", "Unreachable catch blocks hid the real message. "
     "Now unwrapped and logged.", RGBColor(255, 152, 0)),
    ("Raw ids typed into dialogs", "Replaced with pickers filled from the "
     "manager's own team.", RGBColor(255, 152, 0)),
    ("No tests, no build system", "66 JUnit tests, a pom.xml, and scripts that "
     "need no Maven.", GREEN),
    ("Credentials were tracked in git", "db.properties was committed despite "
     "being ignored. Now untracked for real.", RED),
]
for index, (title, detail, color) in enumerate(fixes):
    column = index % 3
    row = index // 3
    left = Inches(0.55) + column * Inches(4.2)
    top = Inches(2.05) + row * Inches(1.45)
    card(s, left, top, Inches(4.0), Inches(1.3))
    bar = s.shapes.add_shape(1, left, top, Inches(0.09), Inches(1.3))
    bar.fill.solid()
    bar.fill.fore_color.rgb = color
    bar.line.color.rgb = color
    txt(s, left + Inches(0.22), top + Inches(0.12), Inches(3.6), Inches(0.28),
        title, 9, True, INK)
    txt(s, left + Inches(0.22), top + Inches(0.44), Inches(3.6), Inches(0.75),
        detail, 8, False, MUTED)

txt(s, Inches(0.55), Inches(6.45), Inches(12.25), Inches(0.3),
    "…and 16 more — every one of them listed, with the code that changed, in "
    "docs/AUDIT_REPORT.md", 10, True, ACCENT, PP_ALIGN.CENTER)
footer(s, "Full write-up in docs/AUDIT_REPORT.md  •  "
          "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ------------------------------------------------------------------ slide 9 — features + screenshots
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, LIGHT)
header(s, "08  •  FEATURES DELIVERED", "Every Feature in the Problem Statement")

features = [
    ("MANAGER", ACCENT, [
        "Performance evaluation — add, edit, delete, search",
        "Goal setting with target dates and status",
        "Feedback provision with a 1-5 rating",
        "Performance reports — bar and pie charts, CSV export",
        "Team management — add members, assign roles, remove",
    ]),
    ("EMPLOYEE", TEAL, [
        "Performance tracking — score trend over time",
        "View feedback — every comment received",
        "Set personal goals — and track them to completion",
        "Performance reports — own criteria averages, CSV export",
        "Everything scoped to the signed-in employee",
    ]),
]
x = Inches(0.55)
for title, color, items in features:
    card(s, x, Inches(1.35), Inches(6.05), Inches(2.95))
    band = s.shapes.add_shape(1, x, Inches(1.35), Inches(6.05), Inches(0.55))
    band.fill.solid()
    band.fill.fore_color.rgb = color
    band.line.color.rgb = color
    txt(s, x + Inches(0.2), Inches(1.46), Inches(5.65), Inches(0.3),
        f"{title} DASHBOARD", 11, True, WHITE, PP_ALIGN.CENTER)
    bullets(s, x + Inches(0.3), Inches(2.05), Inches(5.45), items, 10, INK,
            Inches(0.45))
    x += Inches(6.45)

picture(s, "01-login.png", Inches(0.7), Inches(4.4), width=Inches(3.85))
picture(s, "03-manager-reports.png", Inches(4.75), Inches(4.4),
        width=Inches(3.85))
picture(s, "05-employee-reports.png", Inches(8.8), Inches(4.4),
        width=Inches(3.85))
txt(s, Inches(0.7), Inches(6.65), Inches(11.9), Inches(0.3),
    "Login  •  Manager performance report  •  Employee performance report",
    8.5, False, MUTED, PP_ALIGN.CENTER)
footer(s, "Performetrics  •  GUVI HCL Hackathon  •  Review 2")

# ----------------------------------------------------------------- slide 10 — conclusion
s = prs.slides.add_slide(prs.slide_layouts[6])
bg(s, DARK)
txt(s, Inches(0.7), Inches(0.5), Inches(12), Inches(0.35),
    "09  •  CONCLUSION", 11, True, ACCENT)
txt(s, Inches(0.7), Inches(0.95), Inches(12), Inches(0.6),
    "Complete, Verified and Ready to Run", 28, True, WHITE)

card(s, Inches(0.7), Inches(1.85), Inches(6.1), Inches(3.4), CARD, CARD)
txt(s, Inches(1.0), Inches(2.05), Inches(5.5), Inches(0.3),
    "WHAT IS PROVEN", 10, True, ACCENT)
y = Inches(2.5)
for item in [
    "All 9 manager features and 4 employee features work",
    "Authentication verified against a live MySQL database",
    "66 automated tests pass, 0 compiler warnings",
    "25 audit issues found and fixed, none outstanding",
    "Public GitHub repository with a complete README",
]:
    txt(s, Inches(1.0), y, Inches(5.5), Inches(0.45), f"✓  {item}",
        10, False, WHITE)
    y += Inches(0.52)

card(s, Inches(7.2), Inches(1.85), Inches(5.45), Inches(3.4), CARD, CARD)
txt(s, Inches(7.5), Inches(2.05), Inches(4.85), Inches(0.3),
    "MARKING RUBRIC", 10, True, ACCENT)
y = Inches(2.5)
for item, marks in [
    ("Code quality & execution", "5 marks"),
    ("Code quality & testing", "10 marks"),
    ("Innovation & extra effort", "2 marks"),
]:
    txt(s, Inches(7.5), y, Inches(3.7), Inches(0.4), f"✓  {item}", 10, False,
        WHITE)
    txt(s, Inches(11.0), y, Inches(1.45), Inches(0.4), marks, 10, True, ACCENT,
        PP_ALIGN.RIGHT)
    y += Inches(0.52)

txt(s, Inches(7.5), Inches(4.35), Inches(4.85), Inches(0.6),
    "PBKDF2 hashing · connection pooling · ownership enforced in SQL · "
    "debounced search · CSV export · 66 tests",
    9, False, RGBColor(150, 165, 190))

card(s, Inches(0.7), Inches(5.5), Inches(11.95), Inches(1.05), ACCENT, ACCENT)
txt(s, Inches(1.0), Inches(5.7), Inches(11.35), Inches(0.45),
    "Thank You!", 22, True, WHITE, PP_ALIGN.CENTER)
txt(s, Inches(1.0), Inches(6.15), Inches(11.35), Inches(0.3),
    "github.com/Harshitsingh798/EmployeePerformanceSystem   •   "
    "setup and demo credentials in README.md", 10, False, WHITE,
    PP_ALIGN.CENTER)

out = "docs/Review2_Final_Performetrics.pptx"
prs.save(out)
print(f"PPT saved to {out}  ({len(prs.slides._sldIdLst)} slides)")
