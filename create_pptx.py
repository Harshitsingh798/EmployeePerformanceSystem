from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR

prs = Presentation()
prs.slide_width = Inches(13.33)
prs.slide_height = Inches(7.5)

BG_DARK = RGBColor(0x1B, 0x2A, 0x4A)  # navy
BG_ACCENT = RGBColor(0x29, 0x62, 0xFF)
BG_LIGHT = RGBColor(0xF5, 0xF7, 0xFB)
TEXT_DARK = RGBColor(0x1A, 0x1A, 0x2E)
TEXT_MUTED = RGBColor(0x5A, 0x6C, 0x7D)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)

def set_bg(slide, color):
    fill = slide.background.fill
    fill.solid()
    fill.fore_color.rgb = color

def add_shape(slide, left, top, width, height, fill_color, line_color=None):
    shape = slide.shapes.add_shape(1, left, top, width, height)
    fill = shape.fill
    fill.solid()
    fill.fore_color.rgb = fill_color
    if line_color is None:
        shape.line.fill.background()
    else:
        shape.line.color.rgb = line_color
    return shape

def add_text_box(slide, left, top, width, height, text, font_size=12, bold=False, color=TEXT_DARK, alignment=PP_ALIGN.LEFT, font_name="Calibri"):
    txBox = slide.shapes.add_textbox(left, top, width, height)
    tf = txBox.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = text
    p.font.size = Pt(font_size)
    p.font.bold = bold
    p.font.color.rgb = color
    p.font.name = font_name
    p.alignment = alignment
    return txBox

def add_para(text_frame, text, font_size=11, bold=False, color=TEXT_DARK, alignment=PP_ALIGN.LEFT, space_after=Pt(4), font_name="Calibri", italic=False):
    p = text_frame.add_paragraph()
    p.text = text
    p.font.size = Pt(font_size)
    p.font.bold = bold
    p.font.color.rgb = color
    p.font.name = font_name
    p.font.italic = italic
    p.alignment = alignment
    p.space_after = space_after
    return p

# Helper to create bullet list box
def bullet_box(slide, left, top, width, height, items, font_size=11, color=TEXT_DARK, bullet_color=BG_ACCENT):
    txBox = slide.shapes.add_textbox(left, top, width, height)
    tf = txBox.text_frame
    tf.word_wrap = True
    for i, (title, desc) in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(6)
        p.line_spacing = Pt(16)
        # bullet char
        run = p.add_run()
        run.text = "●  "
        run.font.size = Pt(8)
        run.font.color.rgb = bullet_color
        run.font.name = "Calibri"
        run2 = p.add_run()
        run2.text = title + "  —  " + desc
        run2.font.size = Pt(font_size)
        run2.font.color.rgb = color
        run2.font.name = "Calibri"
        if title and title[0].isupper():
            # make title bold part: we can't bold partial easily, keep plain but items include bold hint
            pass
    return txBox

def code_box(slide, left, top, width, height, lines):
    shape = add_shape(slide, left, top, width, height, RGBColor(0x1E, 0x1E, 0x2E))
    shape.shadow.inherit_shadow = False
    txBox = slide.shapes.add_textbox(left + Inches(0.15), top + Inches(0.12), width - Inches(0.3), height - Inches(0.2))
    tf = txBox.text_frame
    tf.word_wrap = True
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.text = line
        p.font.size = Pt(8)
        p.font.color.rgb = RGBColor(0xCD, 0xD6, 0xF4)
        p.font.name = "Consolas"
        p.space_after = Pt(1)
        p.space_before = Pt(1)
    return shape

# ============ SLIDE 1: Title ============
slide = prs.slides.add_slide(prs.slide_layouts[6])  # blank
set_bg(slide, BG_DARK)
# accent bar
add_shape(slide, Inches(0.6), Inches(0.7), Inches(1.2), Pt(4), BG_ACCENT)
add_text_box(slide, Inches(0.6), Inches(1.0), Inches(7), Inches(0.5), "GUVI HCL Hackathon  •  Problem #18", font_size=11, color=RGBColor(0x8B, 0x9A, 0xCC), font_name="Calibri")
add_text_box(slide, Inches(0.6), Inches(1.55), Inches(8), Inches(1.2), "Performetrics", font_size=42, bold=True, color=WHITE, font_name="Calibri")
add_text_box(slide, Inches(0.6), Inches(2.6), Inches(8), Inches(0.6), "Employee Performance Evaluation System", font_size=18, color=RGBColor(0x8B, 0x9A, 0xCC), font_name="Calibri")
add_text_box(slide, Inches(0.6), Inches(3.3), Inches(8), Inches(0.8), "Evaluate  •  Empower  •  Evolve  —  Core Java  •  Swing  •  JDBC  •  MySQL", font_size=11, color=RGBColor(0xA0, 0xAD, 0xC8), font_name="Calibri")
# info card
card = add_shape(slide, Inches(8.8), Inches(1.2), Inches(3.9), Inches(4.8), WHITE)
card.shadow.inherit_shadow = False
# card content
add_text_box(slide, Inches(9.2), Inches(1.45), Inches(3.2), Inches(0.3), "REVIEW 1  •  OCT 10, 2026", font_size=9, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(9.2), Inches(1.85), Inches(3.2), Inches(0.4), "Team Members", font_size=11, bold=True, color=TEXT_DARK, font_name="Calibri")
tx = slide.shapes.add_textbox(Inches(9.2), Inches(2.25), Inches(3.2), Inches(1.0))
tf = tx.text_frame; tf.word_wrap = True
for name in ["Harshit Singh  —  Team Lead & Developer", "Abhishek  —  Developer"]:
    p = tf.paragraphs[0] if name.startswith("Harshit") else tf.add_paragraph()
    p.text = "•  " + name
    p.font.size = Pt(10); p.font.color.rgb = TEXT_MUTED; p.font.name = "Calibri"; p.space_after = Pt(4)
add_shape(slide, Inches(9.2), Inches(3.3), Inches(3.2), Pt(1), RGBColor(0xEA, 0xEE, 0xF3))
add_text_box(slide, Inches(9.2), Inches(3.45), Inches(3.2), Inches(0.3), "Stack", font_size=9, bold=True, color=TEXT_MUTED, font_name="Calibri")
tx2 = slide.shapes.add_textbox(Inches(9.2), Inches(3.75), Inches(3.2), Inches(1.4))
tf2 = tx2.text_frame; tf2.word_wrap = True
stack_items = ["Java 17  •  Swing (FlatLaf)  •  MySQL 8.4  •  JDBC", "JFreeChart  •  SwingWorker  •  File I/O", "GitHub  •  MySQL Workbench"]
for idx, s in enumerate(stack_items):
    p = tf2.paragraphs[0] if idx == 0 else tf2.add_paragraph()
    p.text = s; p.font.size = Pt(9); p.font.color.rgb = TEXT_DARK; p.font.name = "Calibri"; p.space_after = Pt(3)
# bottom bar
add_text_box(slide, Inches(0.6), Inches(6.6), Inches(12), Inches(0.3), "Review focus:  Project Structure  •  Database Design  •  DB Connectivity  •  UI/UX  •  Code Quality", font_size=9, color=RGBColor(0x6C, 0x7E, 0x9E), font_name="Calibri", alignment=PP_ALIGN.LEFT)

# ============ SLIDE 2: Problem Understanding ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "01  —  Problem Understanding & Solution Design", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(12), Inches(0.6), "Why this system matters", font_size=26, bold=True, color=TEXT_DARK, font_name="Calibri")
# left card - problem
card1 = add_shape(slide, Inches(0.5), Inches(1.6), Inches(6.2), Inches(5.2), WHITE)
card1.shadow.inherit_shadow = False
card1.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_shape(slide, Inches(0.5), Inches(1.6), Inches(6.2), Inches(0.45), RGBColor(0xFF, 0xF1, 0xEB))
add_text_box(slide, Inches(0.8), Inches(1.68), Inches(5.5), Inches(0.3), "THE CHALLENGE", font_size=9, bold=True, color=RGBColor(0xD4, 0x63, 0x1A), font_name="Calibri")
tx = slide.shapes.add_textbox(Inches(0.8), Inches(2.15), Inches(5.6), Inches(4.3))
tf = tx.text_frame; tf.word_wrap = True
items = [
    ("Managers struggle", "to evaluate fairly across communication, technical, punctuality & teamwork."),
    ("Employees lack", "visibility into feedback history and progress over time."),
    ("Goals are scattered", "— no single place to set, track & close personal or assigned goals."),
    ("Reports are manual", "— no graphical analytics for team or self-assessment."),
]
for i, (t, d) in enumerate(items):
    p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
    p.space_after = Pt(10)
    r = p.add_run(); r.text = "✗  " + t + " — "; r.font.size = Pt(10); r.font.bold = True; r.font.color.rgb = TEXT_DARK; r.font.name = "Calibri"
    r2 = p.add_run(); r2.text = d; r2.font.size = Pt(10); r2.font.color.rgb = TEXT_MUTED; r2.font.name = "Calibri"
# right card - solution
card2 = add_shape(slide, Inches(6.9), Inches(1.6), Inches(6.0), Inches(5.2), BG_DARK)
tx3 = slide.shapes.add_textbox(Inches(7.2), Inches(1.85), Inches(5.4), Inches(0.4))
tf3 = tx3.text_frame; tf3.word_wrap = True
p = tf3.paragraphs[0]; p.text = "OUR SOLUTION — PERFORMETRICS"; p.font.size = Pt(9); p.font.bold = True; p.font.color.rgb = RGBColor(0x8B, 0x9A, 0xCC); p.font.name = "Calibri"
tx4 = slide.shapes.add_textbox(Inches(7.2), Inches(2.25), Inches(5.4), Inches(4.3))
tf4 = tx4.text_frame; tf4.word_wrap = True
sols = [
    ("Manager evaluates", "with 4-criteria form, auto-weighted score & dated history."),
    ("Employee tracks", "performance via line chart + detailed table (Swing + JFreeChart)."),
    ("Goals unified", "— manager-assigned & personal goals with status & deadline validation."),
    ("Feedback loop", "— rating 1-5 + comments, visible to employee instantly."),
    ("Reports exported", "to CSV via File I/O; team averages computed live."),
]
for i, (t, d) in enumerate(sols):
    p = tf4.paragraphs[0] if i == 0 else tf4.add_paragraph()
    p.space_after = Pt(8)
    r = p.add_run(); r.text = "✓  " + t + " — "; r.font.size = Pt(10); r.font.bold = True; r.font.color.rgb = WHITE; r.font.name = "Calibri"
    r2 = p.add_run(); r2.text = d; r2.font.size = Pt(10); r2.font.color.rgb = RGBColor(0xC2, 0xCC, 0xE8); r2.font.name = "Calibri"

# ============ SLIDE 3: Architecture ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "02  —  System Architecture", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(12), Inches(0.5), "Modular 4-layer design — built for the rubric", font_size=22, bold=True, color=TEXT_DARK, font_name="Calibri")
# layers as horizontal blocks with arrows
layers = [
    ("UI Layer", "Swing • LoginFrame\nManagerDashboard (5 tabs)\nEmployeeDashboard (4 tabs)", BG_DARK, WHITE),
    ("Service", "ReportService\nAuth logic\nValidation", RGBColor(0x29, 0x62, 0xFF), WHITE),
    ("DAO", "DAO<T> Generic\nUser/Eval/Goal/Feedback\nTeamDAO", RGBColor(0x00, 0x96, 0x88), WHITE),
    ("Model + DB", "User→Manager/Employee\nEvaluation, Goal, Feedback\nMySQL 8.4 • JDBC", RGBColor(0xFF, 0x6F, 0x00), WHITE),
]
x = Inches(0.5)
for title, desc, bg, fg in layers:
    card = add_shape(slide, x, Inches(1.7), Inches(2.9), Inches(2.4), bg)
    add_text_box(slide, x + Inches(0.2), Inches(1.85), Inches(2.5), Inches(0.35), title, font_size=11, bold=True, color=fg, font_name="Calibri", alignment=PP_ALIGN.CENTER)
    tx = slide.shapes.add_textbox(x + Inches(0.15), Inches(2.25), Inches(2.6), Inches(1.6))
    tf = tx.text_frame; tf.word_wrap = True
    p = tf.paragraphs[0]; p.text = desc; p.font.size = Pt(9); p.font.color.rgb = fg; p.font.name = "Calibri"; p.alignment = PP_ALIGN.CENTER; p.line_spacing = Pt(13)
    if x < Inches(9.5):
        add_text_box(slide, x + Inches(2.95), Inches(2.75), Inches(0.3), Inches(0.3), "→", font_size=16, color=TEXT_MUTED, alignment=PP_ALIGN.CENTER)
    x += Inches(3.2)
# bottom two columns
cardL = add_shape(slide, Inches(0.5), Inches(4.5), Inches(6.2), Inches(2.4), WHITE)
cardL.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_text_box(slide, Inches(0.8), Inches(4.7), Inches(5.6), Inches(0.3), "User Roles & Flows", font_size=10, bold=True, color=TEXT_DARK, font_name="Calibri")
tx = slide.shapes.add_textbox(Inches(0.8), Inches(5.05), Inches(5.6), Inches(1.7))
tf = tx.text_frame; tf.word_wrap = True
for idx, line in enumerate(["Manager:  Evaluate → Set Goals → Give Feedback → View Reports → Manage Team", "Employee:  Track Progress → View Feedback → Set Personal Goals → Export Reports", "Auth:  Role-based login (UserDAO.authenticate) with demo fallback for offline demo"]):
    p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
    p.text = "•  " + line; p.font.size = Pt(9); p.font.color.rgb = TEXT_MUTED; p.font.name = "Calibri"; p.space_after = Pt(4)
cardR = add_shape(slide, Inches(6.9), Inches(4.5), Inches(6.0), Inches(2.4), WHITE)
cardR.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_text_box(slide, Inches(7.2), Inches(4.7), Inches(5.4), Inches(0.3), "Build & Run", font_size=10, bold=True, color=TEXT_DARK, font_name="Calibri")
code_lines = ["javac -cp \"lib/*\" -d bin -sourcepath src src/App.java", "java  -cp \"bin;lib/*\" App   // Login window opens", "MySQL:  C:\\MySQLData on port 3306 (start-mysql.bat)", "Demo: manager@company.com / manager123  •  priya@company.com / emp123"]
tx = slide.shapes.add_textbox(Inches(7.2), Inches(5.05), Inches(5.4), Inches(1.7))
tf = tx.text_frame; tf.word_wrap = True
for idx, line in enumerate(code_lines):
    p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
    p.text = line; p.font.size = Pt(8); p.font.name = "Consolas"; p.font.color.rgb = TEXT_DARK; p.space_after = Pt(2)
    if idx == 1:
        p.font.color.rgb = TEXT_MUTED

# ============ SLIDE 4: Database Design ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "03  —  Database Design", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(7), Inches(0.5), "ER design — 6 tables, real relations", font_size=22, bold=True, color=TEXT_DARK, font_name="Calibri")
# ER diagram simplified as boxes
tables = [
    ("users", "user_id PK\nname, email UNIQUE\npassword, role ENUM\ndepartment, designation\nmanager_id FK → users", Inches(0.5), Inches(1.55)),
    ("evaluations", "eval_id PK\nemployee_id FK\nmanager_id FK\ncommunication..teamwork 1-5\noverall_score\neval_date", Inches(3.4), Inches(1.55)),
    ("goals", "goal_id PK\ntitle, description\nassigned_to/by FK\ntarget_date, status\nis_personal BOOL", Inches(6.3), Inches(1.55)),
    ("feedback", "feedback_id PK\nemployee_id FK\nmanager_id FK\neval_id FK nullable\nrating 1-5\ngiven_on", Inches(9.2), Inches(1.55)),
    ("teams", "team_id PK\nteam_name\nmanager_id FK", Inches(3.4), Inches(4.6)),
    ("team_members", "team_id FK, employee_id FK\nrole\nPK(team_id, employee_id)", Inches(6.3), Inches(4.6)),
]
for name, fields, left, top in tables:
    is_users = name == "users"
    bg = BG_DARK if is_users else WHITE
    fg = WHITE if is_users else TEXT_DARK
    card = add_shape(slide, left, top, Inches(2.7), Inches(1.85), bg)
    if not is_users:
        card.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
    add_text_box(slide, left + Inches(0.15), top + Inches(0.08), Inches(2.4), Inches(0.25), name, font_size=9, bold=True, color=RGBColor(0xFF, 0xAB, 0x40) if is_users else BG_ACCENT, font_name="Consolas", alignment=PP_ALIGN.CENTER)
    tx = slide.shapes.add_textbox(left + Inches(0.15), top + Inches(0.35), Inches(2.4), Inches(1.4))
    tf = tx.text_frame; tf.word_wrap = True
    p = tf.paragraphs[0]; p.text = fields; p.font.size = Pt(7); p.font.name = "Consolas"; p.font.color.rgb = fg if is_users else TEXT_MUTED; p.line_spacing = Pt(9)
# relation lines (simple text)
add_text_box(slide, Inches(0.5), Inches(3.55), Inches(12), Inches(0.25), "users 1 ── N  evaluations    •    users 1 ── N  goals    •    users 1 ── N  feedback    •    teams M ── N  users (via team_members)", font_size=8, color=TEXT_MUTED, font_name="Consolas", alignment=PP_ALIGN.CENTER)
# bottom info
card = add_shape(slide, Inches(0.5), Inches(6.55), Inches(8.2), Inches(0.6), WHITE)
card.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_text_box(slide, Inches(0.7), Inches(6.65), Inches(8.0), Inches(0.4), "Schema: db/schema.sql  •  Seed: db/seed_data.sql  •  4 users (1 manager + 3 employees), 3 evaluations, 3 goals, 2 feedback  •  FOREIGN KEYs + CHECK(1-5) constraints", font_size=8, color=TEXT_MUTED, font_name="Calibri")
add_text_box(slide, Inches(9.0), Inches(6.55), Inches(3.8), Inches(0.6), "MySQL 8.4  •  C:\\MySQLData  •  Port 3306  •  mysql-connector-j 8.0.33", font_size=8, bold=True, color=BG_ACCENT, font_name="Calibri", alignment=PP_ALIGN.RIGHT)

# ============ SLIDE 5: UI/UX ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "04  —  UI/UX Design", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(12), Inches(0.5), "Clean, role-based dashboards", font_size=22, bold=True, color=TEXT_DARK, font_name="Calibri")
# Login mock
card = add_shape(slide, Inches(0.5), Inches(1.5), Inches(3.8), Inches(5.0), WHITE)
card.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_shape(slide, Inches(0.5), Inches(1.5), Inches(3.8), Inches(0.5), BG_DARK)
add_text_box(slide, Inches(0.7), Inches(1.62), Inches(3.4), Inches(0.3), "LOGIN  —  Performetrics", font_size=9, bold=True, color=WHITE, font_name="Calibri", alignment=PP_ALIGN.CENTER)
tx = slide.shapes.add_textbox(Inches(0.8), Inches(2.15), Inches(3.2), Inches(3.8))
tf = tx.text_frame; tf.word_wrap = True
for line in ["Email:    manager@company.com", "Password: •••••••••", "Role:     [ MANAGER  ▾ ]", "", "[      Login      ]", "", "Demo: manager@company.com / manager123", "         priya@company.com / emp123"]:
    p = tf.paragraphs[0] if line.startswith("Email") else tf.add_paragraph()
    p.text = line; p.font.size = Pt(8); p.font.name = "Calibri"; p.font.color.rgb = TEXT_DARK if not line.startswith("Demo") else TEXT_MUTED; p.space_after = Pt(3)
    if line.startswith("[") and "Login" in line:
        p.alignment = PP_ALIGN.CENTER
        p.font.bold = True; p.font.color.rgb = BG_ACCENT
# Manager tabs mock
card2 = add_shape(slide, Inches(4.7), Inches(1.5), Inches(4.0), Inches(5.0), WHITE)
card2.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_shape(slide, Inches(4.7), Inches(1.5), Inches(4.0), Inches(0.45), RGBColor(0x33, 0x33, 0x33))
add_text_box(slide, Inches(4.9), Inches(1.62), Inches(3.6), Inches(0.25), "Manager Dashboard  —  Rajesh Kumar", font_size=8, bold=True, color=WHITE, font_name="Calibri")
# tabs
tabs = ["Evaluation", "Goals", "Feedback", "Reports", "Team"]
x = Inches(4.9)
for idx, t in enumerate(tabs):
    bg = BG_ACCENT if idx == 0 else RGBColor(0xEA, 0xEE, 0xF3)
    fg = WHITE if idx == 0 else TEXT_MUTED
    add_shape(slide, x, Inches(2.05), Inches(0.72), Inches(0.28), bg)
    add_text_box(slide, x, Inches(2.08), Inches(0.72), Inches(0.22), t, font_size=6, bold=True, color=fg, font_name="Calibri", alignment=PP_ALIGN.CENTER)
    x += Inches(0.75)
# table mock
add_shape(slide, Inches(4.9), Inches(2.45), Inches(3.6), Inches(3.2), RGBColor(0xF5, 0xF7, 0xFB))
tx = slide.shapes.add_textbox(Inches(5.0), Inches(2.55), Inches(3.4), Inches(3.0))
tf = tx.text_frame; tf.word_wrap = True
rows = ["Eval | Employee        | Score | Date", "  1    | Priya Sharma  | 4.50  | 2026-10-08", "  2    | Amit Patel     | 4.25  | 2026-10-08", "  3    | Sneha Gupta  | 4.50  | 2026-10-08", "", "[ Add ]  [ Refresh ]  [ Delete ]"]
for idx, r in enumerate(rows):
    p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
    p.text = r; p.font.size = Pt(7); p.font.name = "Consolas"; p.font.color.rgb = TEXT_DARK if idx == 0 else TEXT_MUTED; p.space_after = Pt(2)
    if idx == 0:
        p.font.bold = True; p.font.color.rgb = BG_DARK
# Employee mock
card3 = add_shape(slide, Inches(9.0), Inches(1.5), Inches(3.85), Inches(5.0), WHITE)
card3.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_shape(slide, Inches(9.0), Inches(1.5), Inches(3.85), Inches(0.45), RGBColor(0x00, 0x69, 0x5C))
add_text_box(slide, Inches(9.2), Inches(1.62), Inches(3.45), Inches(0.25), "Employee Dashboard  —  Priya Sharma", font_size=8, bold=True, color=WHITE, font_name="Calibri")
tabs2 = ["Tracking", "Feedback", "Goals", "Reports"]
x = Inches(9.2)
for idx, t in enumerate(tabs2):
    bg = RGBColor(0x00, 0x96, 0x88) if idx == 0 else RGBColor(0xEA, 0xEE, 0xF3)
    fg = WHITE if idx == 0 else TEXT_MUTED
    add_shape(slide, x, Inches(2.05), Inches(0.78), Inches(0.28), bg)
    add_text_box(slide, x, Inches(2.08), Inches(0.78), Inches(0.22), t, font_size=6, bold=True, color=fg, font_name="Calibri", alignment=PP_ALIGN.CENTER)
    x += Inches(0.82)
add_shape(slide, Inches(9.2), Inches(2.45), Inches(3.45), Inches(1.2), RGBColor(0xE8, 0xF5, 0xE9))
tx = slide.shapes.add_textbox(Inches(9.3), Inches(2.6), Inches(3.25), Inches(1.0))
tf = tx.text_frame; tf.word_wrap = True
p = tf.paragraphs[0]; p.text = "Avg: 4.50 / 5.0  •  Your Score Trend (Line Chart)"; p.font.size = Pt(7); p.font.bold = True; p.font.color.rgb = RGBColor(0x2E, 0x7D, 0x32); p.font.name = "Calibri"; p.alignment = PP_ALIGN.CENTER
p2 = tf.add_paragraph(); p2.text = "2026-10-08: 4.5  →  (next eval will extend trend)"; p2.font.size = Pt(7); p2.font.color.rgb = TEXT_MUTED; p2.font.name = "Calibri"; p2.alignment = PP_ALIGN.CENTER
tx = slide.shapes.add_textbox(Inches(9.3), Inches(3.85), Inches(3.25), Inches(1.8))
tf = tx.text_frame; tf.word_wrap = True
rows = ["[ Set Personal Goal ]  [ Mark Complete ]", "", "Goal            | Target     | Status", "Learn Spring ...| 2026-12-01 | Pending", "Complete Alpha | 2026-11-15 | In Progress"]
for idx, r in enumerate(rows):
    p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
    p.text = r; p.font.size = Pt(6); p.font.name = "Consolas"; p.font.color.rgb = TEXT_MUTED; p.space_after = Pt(2)
    if idx == 0:
        p.alignment = PP_ALIGN.CENTER; p.font.color.rgb = BG_ACCENT; p.font.bold = True

# ============ SLIDE 6: Core Java Concepts ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "05  —  Core Java Concepts Mapping (Rubric)", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(12), Inches(0.5), "Every rubric item is traceable to code", font_size=22, bold=True, color=TEXT_DARK, font_name="Calibri")
concepts = [
    ("OOP — Inheritance", "User (abstract) → Manager, Employee\nEvaluation.calculateScore() polymorphism", "10 marks", "model/User.java, model/Manager.java"),
    ("Interfaces & Abstraction", "DAO<T> generic interface, DAO impls\nPolymorphic auth via UserDAO", "OOP", "dao/DAO.java, dao/UserDAO.java"),
    ("Collections & Generics", "List<Evaluation>, Map<Integer, List>\nArrayList, HashMap, DAO<T>", "6 marks", "dao/EvaluationDAO.getGroupedByEmployee()"),
    ("Exception Handling", "InvalidRatingException (1-5)\nGoalDeadlineException (future date)", "OOP", "exception/, util/Validator.java"),
    ("File I/O", "ReportExporter CSV export\nFileLogger synchronized log", "—", "util/ReportExporter.java, util/FileLogger.java"),
    ("Multithreading", "SwingWorker for DB loads (non-blocking UI)\nsynchronized Singleton + FileLogger", "4 marks", "dao/DatabaseConnection.java, ui/*Dashboard.java"),
    ("JDBC — Classes", "User/Eval/Goal/Feedback/TeamDAO\nPreparedStatement + try-with-resources", "7 marks", "dao/*DAO.java (5 DAOs)"),
    ("DB Connectivity", "DatabaseConnection singleton\nmysql-connector-j 8.0.33 + db.properties", "3 + 3 marks", "dao/DatabaseConnection.java, lib/"),
]
# grid 4x2
for idx, (title, desc, marks, file) in enumerate(concepts):
    col = idx % 4
    row = idx // 4
    left = Inches(0.5) + col * Inches(3.22)
    top = Inches(1.55) + row * Inches(2.75)
    card = add_shape(slide, left, top, Inches(3.05), Inches(2.55), WHITE)
    card.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
    # marks badge
    badge_bg = BG_ACCENT if "marks" in marks else (RGBColor(0x00, 0x96, 0x88) if marks == "OOP" else RGBColor(0xEA, 0xEE, 0xF3))
    badge_fg = WHITE if "marks" in marks or marks == "OOP" else TEXT_MUTED
    add_shape(slide, left + Inches(0.15), top + Inches(0.15), Inches(1.1), Inches(0.22), badge_bg)
    add_text_box(slide, left + Inches(0.15), top + Inches(0.16), Inches(1.1), Inches(0.2), marks, font_size=7, bold=True, color=badge_fg, font_name="Calibri", alignment=PP_ALIGN.CENTER)
    add_text_box(slide, left + Inches(0.15), top + Inches(0.45), Inches(2.75), Inches(0.35), title, font_size=9, bold=True, color=TEXT_DARK, font_name="Calibri")
    tx = slide.shapes.add_textbox(left + Inches(0.15), top + Inches(0.8), Inches(2.75), Inches(1.1))
    tf = tx.text_frame; tf.word_wrap = True
    p = tf.paragraphs[0]; p.text = desc; p.font.size = Pt(8); p.font.color.rgb = TEXT_MUTED; p.font.name = "Calibri"; p.line_spacing = Pt(11)
    add_text_box(slide, left + Inches(0.15), top + Inches(2.0), Inches(2.75), Inches(0.35), file, font_size=7, color=BG_ACCENT, font_name="Consolas")

# ============ SLIDE 7: DB Connectivity Demo ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "06  —  Database Connectivity Demo", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(7), Inches(0.5), "Live MySQL — verified end-to-end", font_size=22, bold=True, color=TEXT_DARK, font_name="Calibri")
# left code
add_text_box(slide, Inches(0.5), Inches(1.45), Inches(6.2), Inches(0.3), "DatabaseConnection.java  —  Synchronized Singleton", font_size=9, bold=True, color=TEXT_DARK, font_name="Calibri")
code_box(slide, Inches(0.5), Inches(1.8), Inches(6.2), Inches(2.6), [
    "public class DatabaseConnection {",
    "  private static DatabaseConnection instance;",
    "  public static synchronized DatabaseConnection getInstance() {",
    "    if (instance == null) instance = new DatabaseConnection();",
    "    return instance;",
    "  }",
    "  public synchronized Connection getConnection() throws SQLException {",
    "    if (connection == null || connection.isClosed())",
    "      connection = DriverManager.getConnection(url, user, password);",
    "    return connection;",
    "  }",
    "}",
])
add_text_box(slide, Inches(0.5), Inches(4.55), Inches(6.2), Inches(0.3), "DAO Example — PreparedStatement + try-with-resources", font_size=9, bold=True, color=TEXT_DARK, font_name="Calibri")
code_box(slide, Inches(0.5), Inches(4.9), Inches(6.2), Inches(1.8), [
    "String sql = \"INSERT INTO evaluations (...) VALUES (?,?,?,?,?,?,?,?)\";",
    "try (Connection c = getConnection();",
    "     PreparedStatement ps = c.prepareStatement(sql)) {",
    "  ps.setInt(1, e.getEmployeeId());",
    "  ps.setDouble(7, e.calculateScore()); // polymorphism",
    "  ps.executeUpdate();",
    "}",
])
# right proof
card = add_shape(slide, Inches(7.0), Inches(1.5), Inches(5.85), Inches(5.2), WHITE)
card.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
add_shape(slide, Inches(7.0), Inches(1.5), Inches(5.85), Inches(0.4), RGBColor(0xE8, 0xF5, 0xE9))
add_text_box(slide, Inches(7.2), Inches(1.58), Inches(5.4), Inches(0.25), "✓  LIVE PROOF  —  MySQL 8.4 on localhost:3306", font_size=9, bold=True, color=RGBColor(0x2E, 0x7D, 0x32), font_name="Calibri")
tx = slide.shapes.add_textbox(Inches(7.2), Inches(2.05), Inches(5.45), Inches(4.5))
tf = tx.text_frame; tf.word_wrap = True
proof_lines = [
    ("JDBC Test Output:", True),
    ("Database connected: jdbc:mysql://localhost:3306/employee_performance_db", False),
    ("Connection successful: true", False),
    ("Manager auth: PASS - Rajesh Kumar", False),
    ("Employee auth: PASS - Priya Sharma", False),
    ("", False),
    ("MySQL Verification (mysql.exe):", True),
    ("users: 4 rows (1 manager + 3 employees)", False),
    ("evaluations: 3 rows  •  goals: 3  •  feedback: 2", False),
    ("SHOW TABLES: users, evaluations, goals, feedback, teams, team_members", False),
    ("", False),
    ("Connection pool: Singleton, synchronized, try-with-resources", False),
    ("Driver: mysql-connector-j-8.0.33.jar in lib/", False),
]
for idx, (line, bold) in enumerate(proof_lines):
    p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
    p.text = line; p.font.size = Pt(8); p.font.name = "Consolas" if not bold else "Calibri"; p.font.bold = bold; p.font.color.rgb = TEXT_DARK if not bold else BG_DARK; p.space_after = Pt(2)
    if line.startswith("Database") or line.startswith("Connection") or line.startswith("Manager") or line.startswith("Employee"):
        p.font.color.rgb = RGBColor(0x2E, 0x7D, 0x32)

# ============ SLIDE 8: Features Implemented ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "07  —  Features Implemented (Review 1 Scope)", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(12), Inches(0.5), "What works today — wire it, then polish", font_size=22, bold=True, color=TEXT_DARK, font_name="Calibri")
# checklist
features = [
    ("✓  DONE", "Project structure  —  src/model, dao, service, ui, util, exception, db  •  25+ classes, 43 compiled"),
    ("✓  DONE", "Database design  —  schema.sql + seed_data.sql  •  MySQL 8.4 running at C:\\MySQLData"),
    ("✓  DONE", "DB connectivity  —  DatabaseConnection singleton + 5 DAOs  •  JDBC verified (auth PASS)"),
    ("✓  DONE", "UI skeleton  —  LoginFrame + Manager (5 tabs) + Employee (4 tabs)  •  Swing, responsive layout"),
    ("✓  DONE", "Real DB wiring  —  SwingWorker loads + dialogs: Add Evaluation / Goal / Feedback  •  Validation + FileLogger"),
    ("✓  DONE", "File I/O  —  ReportExporter CSV export  •  app.log via synchronized FileWriter"),
    ("◐  NEXT", "Charts polish  —  JFreeChart integration for graphical reports (placeholder ready)"),
    ("◐  NEXT", "Presentation & GitHub  —  This deck + public repo with README before Oct 10 deadline"),
]
y = Inches(1.55)
for badge, desc in features:
    is_done = badge.startswith("✓")
    bg = RGBColor(0xE8, 0xF5, 0xE9) if is_done else RGBColor(0xFF, 0xF3, 0xE0)
    fg = RGBColor(0x2E, 0x7D, 0x32) if is_done else RGBColor(0xE6, 0x51, 0x00)
    add_shape(slide, Inches(0.5), y, Inches(1.0), Inches(0.32), bg)
    add_text_box(slide, Inches(0.5), y + Inches(0.03), Inches(1.0), Inches(0.26), badge, font_size=8, bold=True, color=fg, font_name="Calibri", alignment=PP_ALIGN.CENTER)
    add_text_box(slide, Inches(1.65), y, Inches(11.2), Inches(0.32), desc, font_size=9, color=TEXT_DARK, font_name="Calibri")
    y += Inches(0.48)
# demo flow highlight
card = add_shape(slide, Inches(0.5), Inches(6.05), Inches(12.35), Inches(1.0), BG_DARK)
add_text_box(slide, Inches(0.7), Inches(6.2), Inches(12), Inches(0.3), "END-TO-END DEMO FLOW  (for the review)", font_size=9, bold=True, color=RGBColor(0x8B, 0x9A, 0xCC), font_name="Calibri")
add_text_box(slide, Inches(0.7), Inches(6.5), Inches(12), Inches(0.35), "Run App → Login (manager@company.com) → Manager adds Evaluation for Emp #2 → DB INSERT → Employee logs in (priya@company.com) → Tracking tab shows new score automatically", font_size=9, color=WHITE, font_name="Calibri")

# ============ SLIDE 9: Code Quality ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_LIGHT)
add_text_box(slide, Inches(0.5), Inches(0.3), Inches(12), Inches(0.4), "08  —  Code Quality", font_size=11, bold=True, color=BG_ACCENT, font_name="Calibri")
add_text_box(slide, Inches(0.5), Inches(0.75), Inches(12), Inches(0.5), "Clean, modular, review-ready", font_size=22, bold=True, color=TEXT_DARK, font_name="Calibri")
qualities = [
    ("Modular Packages", "model / dao / service / ui / util / exception\nEach layer has a single responsibility", "✓"),
    ("Naming & Structure", "User → Manager/Employee hierarchy\nDAO<T> generic, descriptive method names", "✓"),
    ("Exception Handling", "Custom exceptions + Validator\ntry-with-resources in all DAOs", "✓"),
    ("Comments & Docs", "Javadoc on key classes\nREADME with setup, run & demo creds", "✓"),
    ("Version Control", "Git repo with .gitignore (bin/, *.log ignored)\nCommit: Review 1 structure + wiring", "✓"),
    ("No Hardcoding", "db.properties for URL/user/pass\nFallback demo auth when DB offline", "✓"),
]
for idx, (title, desc, icon) in enumerate(qualities):
    col = idx % 3
    row = idx // 3
    left = Inches(0.5) + col * Inches(4.3)
    top = Inches(1.6) + row * Inches(2.7)
    card = add_shape(slide, left, top, Inches(4.0), Inches(2.4), WHITE)
    card.line.color.rgb = RGBColor(0xEA, 0xEE, 0xF3)
    add_text_box(slide, left + Inches(0.2), top + Inches(0.15), Inches(0.4), Inches(0.3), icon, font_size=14, color=RGBColor(0x2E, 0x7D, 0x32), alignment=PP_ALIGN.CENTER)
    add_text_box(slide, left + Inches(0.6), top + Inches(0.15), Inches(3.2), Inches(0.3), title, font_size=10, bold=True, color=TEXT_DARK, font_name="Calibri")
    tx = slide.shapes.add_textbox(left + Inches(0.2), top + Inches(0.55), Inches(3.6), Inches(1.6))
    tf = tx.text_frame; tf.word_wrap = True
    p = tf.paragraphs[0]; p.text = desc; p.font.size = Pt(9); p.font.color.rgb = TEXT_MUTED; p.font.name = "Calibri"; p.line_spacing = Pt(13)

# ============ SLIDE 10: Future Work & Thank You ============
slide = prs.slides.add_slide(prs.slide_layouts[6])
set_bg(slide, BG_DARK)
# future work
add_text_box(slide, Inches(0.6), Inches(0.5), Inches(7), Inches(0.4), "09  —  Future Work (Post Review 1)", font_size=11, bold=True, color=RGBColor(0x8B, 0x9A, 0xCC), font_name="Calibri")
add_text_box(slide, Inches(0.6), Inches(0.9), Inches(7), Inches(0.6), "What's next before Final Review", font_size=26, bold=True, color=WHITE, font_name="Calibri")
items = [
    ("Charts", "JFreeChart bar/pie/line for Performance Reports (both dashboards)"),
    ("Team Management", "Add Member / Assign Role dialogs writing to team_members"),
    ("Search & Filter", "Filter evaluations by date/employee, search goals"),
    ("Security", "Password hashing (BCrypt), session handling, input sanitization"),
    ("Testing", "JUnit for DAOs + Validator, edge cases for ratings & dates"),
    ("Polish", "FlatLaf theme, responsive resizing, keyboard shortcuts"),
]
tx = slide.shapes.add_textbox(Inches(0.6), Inches(1.7), Inches(7), Inches(4.0))
tf = tx.text_frame; tf.word_wrap = True
for i, (t, d) in enumerate(items):
    p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
    p.space_after = Pt(8)
    r = p.add_run(); r.text = "→  " + t + "  —  "; r.font.size = Pt(11); r.font.bold = True; r.font.color.rgb = WHITE; r.font.name = "Calibri"
    r2 = p.add_run(); r2.text = d; r2.font.size = Pt(11); r2.font.color.rgb = RGBColor(0xC2, 0xCC, 0xE8); r2.font.name = "Calibri"
# timeline card
card = add_shape(slide, Inches(8.2), Inches(0.5), Inches(4.5), Inches(4.2), WHITE)
add_text_box(slide, Inches(8.5), Inches(0.7), Inches(3.9), Inches(0.3), "TIMELINE", font_size=9, bold=True, color=BG_ACCENT, font_name="Calibri")
tx = slide.shapes.add_textbox(Inches(8.5), Inches(1.05), Inches(3.9), Inches(3.2))
tf = tx.text_frame; tf.word_wrap = True
timeline = [
    ("Oct 8", "Structure + DB design + JDBC (DONE)", True),
    ("Oct 9", "UI wiring + dialogs + validation", False),
    ("Oct 10", "Review 1: Presentation + GitHub push", False),
    ("Oct 11-15", "Charts + Team Mgmt + Tests", False),
    ("Final", "Polish + Deployment + Demo video", False),
]
for i, (date, desc, done) in enumerate(timeline):
    p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
    p.space_after = Pt(6)
    r = p.add_run(); r.text = ("✓ " if done else "○ ") + date + "  —  "; r.font.size = Pt(9); r.font.bold = True; r.font.color.rgb = RGBColor(0x2E, 0x7D, 0x32) if done else TEXT_DARK; r.font.name = "Calibri"
    r2 = p.add_run(); r2.text = desc; r2.font.size = Pt(9); r2.font.color.rgb = TEXT_MUTED; r2.font.name = "Calibri"
# thank you
add_shape(slide, Inches(8.2), Inches(5.0), Inches(4.5), Inches(1.7), BG_ACCENT)
add_text_box(slide, Inches(8.5), Inches(5.2), Inches(3.9), Inches(0.4), "Thank You!", font_size=20, bold=True, color=WHITE, font_name="Calibri", alignment=PP_ALIGN.CENTER)
add_text_box(slide, Inches(8.5), Inches(5.6), Inches(3.9), Inches(0.5), "Questions & Feedback Welcome\nGitHub: (push to public repo before deadline)", font_size=9, color=WHITE, font_name="Calibri", alignment=PP_ALIGN.CENTER)
add_text_box(slide, Inches(0.6), Inches(6.7), Inches(12), Inches(0.3), "Performetrics  •  GUVI HCL Hackathon  •  Review 1  •  Harshit Singh & Team  •  hackathon.guvi.in", font_size=8, color=RGBColor(0x6C, 0x7E, 0x9E), font_name="Calibri", alignment=PP_ALIGN.CENTER)

out = "docs/Review1_Performetrics_Presentation.pptx"
prs.save(out)
print(f"Saved to {out}")
