#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import random
from dataclasses import dataclass
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from typing import Iterable

SEED = 20260322
DEFAULT_TA_COUNT = 72
DEFAULT_RECRUITER_COUNT = 8
DEFAULT_JOB_COUNT = 48
DEFAULT_APPLICATION_COUNT = 960
DEFAULT_CV_COUNT = 18
PASSWORD = "demo123"

FIRST_NAMES = [
    "Amelia", "Benjamin", "Charlotte", "Daniel", "Ethan", "Freya", "Grace", "Henry", "Ivy", "Jack",
    "Katherine", "Leo", "Mia", "Noah", "Olivia", "Patrick", "Queenie", "Ryan", "Sophia", "Thomas",
    "Uma", "Victor", "Willow", "Xavier", "Yasmin", "Zachary", "Aiden", "Bella", "Caleb", "Daisy",
    "Elena", "Felix", "Georgia", "Hugo", "Isla", "Julian", "Layla", "Mason", "Nora", "Oscar",
    "Phoebe", "Ruby", "Sebastian", "Theo", "Violet", "William", "Zoe", "Aaron", "Bianca", "Carter",
    "Delia", "Elliot", "Farah", "Gavin", "Hazel", "Isaac", "Jasmine", "Kai", "Luna", "Mila",
    "Nikhil", "Opal", "Priya", "Quinn", "Riley", "Sienna", "Tristan", "Valerie", "Wesley", "Yvonne"
]

LAST_NAMES = [
    "Chen", "Zhang", "Li", "Wang", "Liu", "Taylor", "Patel", "Evans", "Walker", "Singh",
    "Brown", "Martin", "Clark", "Moore", "Davies", "Hall", "Young", "King", "Allen", "Scott"
]

RECRUITER_NAMES = [
    "Dr Nadia Moore", "Dr Liam Patel", "Dr Hannah Scott", "Dr Oliver Evans",
    "Dr Sophia Walker", "Dr Ethan Brown", "Dr Chloe Martin", "Dr Isaac Hall"
]

PROGRAMMES = [
    "Software Engineering",
    "Computer Science",
    "Artificial Intelligence",
    "Data Science",
    "Cyber Security",
    "Digital Media Technology"
]

AVAILABILITIES = [
    "Mon/Wed afternoon; flexible for lab marking",
    "Tue/Thu morning, Fri afternoon",
    "Weekdays after 14:00; occasional evening support",
    "Mon/Tue evening and weekend morning",
    "Wed/Fri afternoon, flexible during assessment weeks",
    "Tue/Thu afternoon, available for online office hours",
    "Weekday mornings except Wednesday",
    "Flexible on Mon/Thu; weekend revision sessions possible"
]

TA_SKILL_PROFILES = [
    {
        "primary": ["Java", "OOP", "Testing", "Debugging"],
        "secondary": ["Communication", "Lab Support", "Marking"],
        "programme": "Software Engineering",
        "experience": "Peer mentoring in programming labs, formative marking and debugging clinics.",
        "cv": "Built Java coursework projects, supported junior students and documented testing evidence."
    },
    {
        "primary": ["Python", "Machine Learning", "Data Analysis", "Algorithms"],
        "secondary": ["Communication", "Tutoring", "Pandas"],
        "programme": "Artificial Intelligence",
        "experience": "Assisted data labs, explained machine learning concepts and supported notebook-based workshops.",
        "cv": "Completed ML pipelines, exploratory analysis and group tutoring for quantitative modules."
    },
    {
        "primary": ["HTML", "CSS", "JavaScript", "Accessibility"],
        "secondary": ["UI Design", "Prototyping", "Communication"],
        "programme": "Digital Media Technology",
        "experience": "Led web UI coursework reviews, accessibility checks and front-end studio feedback sessions.",
        "cv": "Designed responsive interfaces, user journeys and component libraries for course prototypes."
    },
    {
        "primary": ["SQL", "Database Design", "Data Modelling", "Debugging"],
        "secondary": ["Communication", "Marking", "Data Analysis"],
        "programme": "Data Science",
        "experience": "Supported database labs, schema design exercises and SQL troubleshooting drop-ins.",
        "cv": "Worked on relational modelling, analytics datasets and structured query optimisation tasks."
    },
    {
        "primary": ["Networks", "Linux", "Security", "Scripting"],
        "secondary": ["Risk Analysis", "Troubleshooting", "Communication"],
        "programme": "Cyber Security",
        "experience": "Ran network troubleshooting clinics and security lab walkthroughs for practical sessions.",
        "cv": "Hands-on with Linux tooling, shell scripting and secure systems support for teaching labs."
    },
    {
        "primary": ["Data Structures", "Algorithms", "C", "Linux"],
        "secondary": ["Debugging", "Tutoring", "Concurrency"],
        "programme": "Computer Science",
        "experience": "Tutored algorithm clinics, systems labs and debugging sessions for foundational modules.",
        "cv": "Comfortable with systems programming, concurrency basics and lab-based teaching support."
    }
]

JOB_TEMPLATES = [
    {
        "title": "Java Programming Teaching Assistant",
        "module": "EBU6304",
        "description": "Support weekly Java labs, guide OOP exercises and help with formative code review activities.",
        "required": ["Java", "OOP"],
        "preferred": ["Testing", "Communication"],
        "workload": 6,
        "quota": 3,
        "keywords": ["Java", "OOP", "Testing", "Debugging"]
    },
    {
        "title": "Data Structures Lab TA",
        "module": "EBU6305",
        "description": "Facilitate algorithm clinics, explain complexity trade-offs and support problem-solving workshops.",
        "required": ["Data Structures", "Algorithms"],
        "preferred": ["Tutoring", "Communication"],
        "workload": 5,
        "quota": 3,
        "keywords": ["Data Structures", "Algorithms", "Tutoring", "Debugging"]
    },
    {
        "title": "Web Development Studio TA",
        "module": "EBU6310",
        "description": "Assist front-end studio sessions, review HTML/CSS/JavaScript submissions and advise on accessible UI design.",
        "required": ["HTML", "CSS", "JavaScript"],
        "preferred": ["Accessibility", "UI Design"],
        "workload": 5,
        "quota": 2,
        "keywords": ["HTML", "CSS", "JavaScript", "Accessibility"]
    },
    {
        "title": "Database Systems Support TA",
        "module": "EBU6311",
        "description": "Support SQL labs, data modelling tasks and relational database debugging clinics.",
        "required": ["SQL", "Database Design"],
        "preferred": ["Data Modelling", "Debugging"],
        "workload": 4,
        "quota": 2,
        "keywords": ["SQL", "Database Design", "Data Modelling", "Debugging"]
    },
    {
        "title": "Software Testing Workshop TA",
        "module": "EBU6312",
        "description": "Coach students through unit testing, QA workflows and bug reproduction exercises.",
        "required": ["Testing", "QA"],
        "preferred": ["Automation", "JUnit"],
        "workload": 4,
        "quota": 2,
        "keywords": ["Testing", "QA", "Automation", "JUnit"]
    },
    {
        "title": "Machine Learning Lab TA",
        "module": "EBU6313",
        "description": "Help students run notebook-based ML labs, explain model outputs and support data preprocessing tasks.",
        "required": ["Python", "Machine Learning"],
        "preferred": ["Data Analysis", "Communication"],
        "workload": 6,
        "quota": 3,
        "keywords": ["Python", "Machine Learning", "Data Analysis", "Tutoring"]
    },
    {
        "title": "Networks Practical TA",
        "module": "EBU6314",
        "description": "Assist packet analysis labs, troubleshoot connectivity issues and support networking practical exercises.",
        "required": ["Networks", "Troubleshooting"],
        "preferred": ["Linux", "Communication"],
        "workload": 4,
        "quota": 2,
        "keywords": ["Networks", "Troubleshooting", "Linux", "Communication"]
    },
    {
        "title": "Cyber Security Lab TA",
        "module": "EBU6315",
        "description": "Support secure systems labs, risk review discussions and Linux-based security exercises.",
        "required": ["Security", "Linux"],
        "preferred": ["Risk Analysis", "Scripting"],
        "workload": 5,
        "quota": 2,
        "keywords": ["Security", "Linux", "Risk Analysis", "Scripting"]
    },
    {
        "title": "Human-Computer Interaction Studio TA",
        "module": "EBU6316",
        "description": "Review interaction design artefacts, prototype walkthroughs and accessibility-focused user testing preparation.",
        "required": ["UI Design", "Prototyping"],
        "preferred": ["Accessibility", "Communication"],
        "workload": 4,
        "quota": 2,
        "keywords": ["UI Design", "Prototyping", "Accessibility", "Communication"]
    },
    {
        "title": "Operating Systems Lab TA",
        "module": "EBU6317",
        "description": "Facilitate systems programming labs, explain Linux tooling and support concurrency debugging questions.",
        "required": ["C", "Linux"],
        "preferred": ["Concurrency", "Debugging"],
        "workload": 5,
        "quota": 2,
        "keywords": ["C", "Linux", "Concurrency", "Debugging"]
    },
    {
        "title": "Data Analytics Workshop TA",
        "module": "EBU6318",
        "description": "Support analytics workshops, help students interpret datasets and guide dashboard-based insights.",
        "required": ["Data Analysis", "Python"],
        "preferred": ["Communication", "SQL"],
        "workload": 4,
        "quota": 3,
        "keywords": ["Data Analysis", "Python", "SQL", "Communication"]
    },
    {
        "title": "Agile Project Support TA",
        "module": "EBU6319",
        "description": "Coach team ceremonies, review sprint artefacts and support project communication during development cycles.",
        "required": ["Communication", "Organisation"],
        "preferred": ["Testing", "Mentoring"],
        "workload": 4,
        "quota": 2,
        "keywords": ["Communication", "Organisation", "Testing", "Mentoring"]
    }
]

@dataclass(frozen=True)
class GeneratedUser:
    user_id: str
    username: str
    role: str
    name: str
    student_id: str
    programme: str
    skills: list[str]
    availability: str
    experience: str
    cv_text: str
    email: str
    cv_file_name: str = ""
    cv_uploaded_at: str = ""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate larger RecruitAssist demo seed data.")
    parser.add_argument("--base-dir", default=None, help="Project root. Defaults to the parent of this script.")
    parser.add_argument("--ta-count", type=int, default=DEFAULT_TA_COUNT)
    parser.add_argument("--recruiter-count", type=int, default=DEFAULT_RECRUITER_COUNT)
    parser.add_argument("--job-count", type=int, default=DEFAULT_JOB_COUNT)
    parser.add_argument("--application-count", type=int, default=DEFAULT_APPLICATION_COUNT)
    parser.add_argument("--cv-count", type=int, default=DEFAULT_CV_COUNT)
    return parser.parse_args()


def write_json(path: Path, payload: dict) -> None:
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def ensure_positive(value: int, name: str) -> int:
    if value <= 0:
        raise ValueError(f"{name} must be positive.")
    return value


def build_name(index: int) -> str:
    return f"{FIRST_NAMES[index % len(FIRST_NAMES)]} {LAST_NAMES[(index * 3) % len(LAST_NAMES)]}"


def build_ta_users(count: int, cv_count: int, cv_dir: Path) -> list[GeneratedUser]:
    users: list[GeneratedUser] = []
    now = datetime(2026, 3, 22, 9, 0, tzinfo=timezone.utc)
    for index in range(count):
        profile = TA_SKILL_PROFILES[index % len(TA_SKILL_PROFILES)]
        extra_skill = profile["secondary"][index % len(profile["secondary"])]
        skills = list(dict.fromkeys(profile["primary"] + [extra_skill]))
        user_id = f"U{1101 + index:04d}"
        username = f"ta.{index + 1:03d}"
        name = build_name(index)
        student_id = f"2026{1201 + index:04d}"
        availability = AVAILABILITIES[index % len(AVAILABILITIES)]
        email = username.replace('.', '_') + "@qmul.ac.uk"
        cv_file_name = ""
        cv_uploaded_at = ""
        cv_text = (
            f"{profile['cv']} Strong skills in {', '.join(skills[:3])}, comfortable with student support, marking and technical walkthroughs."
        )

        if index < cv_count:
            cv_file_name = f"{user_id}_cv.txt"
            cv_uploaded_at = (now - timedelta(days=index % 9, hours=index % 5)).isoformat().replace("+00:00", "Z")
            cv_dir.joinpath(cv_file_name).write_text(
                f"{name}\nProgramme: {profile['programme']}\nSkills: {', '.join(skills)}\nExperience: {profile['experience']}\nEvidence: {cv_text}\n",
                encoding="utf-8"
            )

        users.append(GeneratedUser(
            user_id=user_id,
            username=username,
            role="TA",
            name=name,
            student_id=student_id,
            programme=profile["programme"],
            skills=skills,
            availability=availability,
            experience=profile["experience"],
            cv_text=cv_text,
            email=email,
            cv_file_name=cv_file_name,
            cv_uploaded_at=cv_uploaded_at,
        ))
    return users


def build_recruiters(count: int) -> list[GeneratedUser]:
    users: list[GeneratedUser] = []
    for index in range(count):
        name = RECRUITER_NAMES[index % len(RECRUITER_NAMES)]
        user_id = f"U{9101 + index:04d}"
        username = f"recruiter.{index + 1:02d}"
        skills = ["Curriculum Design", "Assessment", "Module Delivery", "Candidate Review"]
        users.append(GeneratedUser(
            user_id=user_id,
            username=username,
            role="MO",
            name=name,
            student_id="",
            programme="Module Organiser",
            skills=skills,
            availability="Weekdays; live interview and review coverage",
            experience="Coordinates recruitment, ranking, shortlisting and workload-aware staffing across multiple modules.",
            cv_text="Owns module staffing plans, publishes jobs and reviews candidate fit under timeline pressure.",
            email=username.replace('.', '_') + "@qmul.ac.uk"
        ))
    return users


def user_to_payload(user: GeneratedUser) -> dict:
    payload = {
        "userId": user.user_id,
        "username": user.username,
        "password": PASSWORD,
        "role": user.role,
        "name": user.name,
        "studentId": user.student_id,
        "email": user.email,
        "programme": user.programme,
        "skills": user.skills,
        "availability": user.availability,
        "experience": user.experience,
        "cvText": user.cv_text,
    }
    if user.cv_file_name:
        payload["cvFileName"] = user.cv_file_name
    if user.cv_uploaded_at:
        payload["cvUploadedAt"] = user.cv_uploaded_at
    return payload


def build_jobs(count: int, recruiters: list[GeneratedUser]) -> list[dict]:
    jobs: list[dict] = []
    today = date(2026, 3, 22)
    for index in range(count):
        template = JOB_TEMPLATES[index % len(JOB_TEMPLATES)]
        module_suffix = index // len(JOB_TEMPLATES)
        module_code = template["module"] if module_suffix == 0 else f"{template['module']}-{module_suffix + 1}"
        status = "CLOSED" if index % 7 == 0 else "OPEN"
        deadline = today + timedelta(days=6 + (index % 10) * 3)
        jobs.append({
            "jobId": f"J{2101 + index:04d}",
            "ownerId": recruiters[index % len(recruiters)].user_id,
            "title": template["title"],
            "moduleCode": module_code,
            "description": template["description"],
            "requiredSkills": template["required"],
            "preferredSkills": template["preferred"],
            "deadline": deadline.isoformat(),
            "quota": template["quota"] + (1 if index % 5 == 0 else 0),
            "workloadHours": template["workload"],
            "status": status,
            "keywords": template["keywords"],
        })
    return jobs


def score_overlap(user: GeneratedUser, job: dict) -> tuple[float, list[str], list[str]]:
    user_skills = {skill.lower() for skill in user.skills}
    required = job["requiredSkills"]
    preferred = job["preferredSkills"]
    matched_required = [skill for skill in required if skill.lower() in user_skills]
    matched_preferred = [skill for skill in preferred if skill.lower() in user_skills]
    required_ratio = len(matched_required) / max(1, len(required))
    preferred_ratio = len(matched_preferred) / max(1, len(preferred))
    programme_bonus = 0.08 if user.programme.lower().split()[0] in job["title"].lower() or user.programme.lower().split()[0] in job["description"].lower() else 0.0
    score = 0.42 + (required_ratio * 0.34) + (preferred_ratio * 0.12) + programme_bonus
    return min(score, 0.96), matched_required, matched_preferred


def distribute(total: int, buckets: int) -> list[int]:
    base = total // buckets
    remainder = total % buckets
    return [base + (1 if index < remainder else 0) for index in range(buckets)]


def pick_candidates(job: dict, tas: list[GeneratedUser], application_limit_by_user: dict[str, int], rng: random.Random, count: int) -> list[tuple[GeneratedUser, float, list[str], list[str]]]:
    ranked: list[tuple[GeneratedUser, float, list[str], list[str], float]] = []
    for user in tas:
        score, matched_required, matched_preferred = score_overlap(user, job)
        noise = rng.random() * 0.07
        ranked.append((user, score, matched_required, matched_preferred, noise))
    ranked.sort(key=lambda item: (item[1] + item[4], len(item[2]), len(item[3])), reverse=True)

    selected: list[tuple[GeneratedUser, float, list[str], list[str]]] = []
    for user, score, matched_required, matched_preferred, _ in ranked:
        if application_limit_by_user[user.user_id] <= 0:
            continue
        selected.append((user, score, matched_required, matched_preferred))
        application_limit_by_user[user.user_id] -= 1
        if len(selected) >= count:
            break
    return selected


def explanation_lines(job: dict, matched_required: Iterable[str], matched_preferred: Iterable[str], missing_required: Iterable[str], projected_hours: int) -> list[str]:
    matched_required_list = list(matched_required)
    matched_preferred_list = list(matched_preferred)
    missing_required_list = list(missing_required)
    lines = [
        "Matched required skills: " + (", ".join(matched_required_list) if matched_required_list else "limited direct overlap"),
        "Preferred skill matches: " + (", ".join(matched_preferred_list) if matched_preferred_list else "none identified yet"),
        "Skills to strengthen: " + (", ".join(missing_required_list) if missing_required_list else "no critical gaps detected"),
        f"Projected workload after acceptance: {projected_hours}/12 hours"
    ]
    return lines


def build_applications(total_applications: int, tas: list[GeneratedUser], jobs: list[dict]) -> list[dict]:
    rng = random.Random(SEED)
    per_job_counts = distribute(total_applications, len(jobs))
    application_limit_by_user = {user.user_id: max(10, (total_applications // max(1, len(tas))) + 5) for user in tas}
    accepted_hours_by_user = {user.user_id: 0 for user in tas}
    applications: list[dict] = []
    base_time = datetime(2026, 3, 3, 9, 0, tzinfo=timezone.utc)

    app_index = 0
    for job_index, job in enumerate(jobs):
        selected = pick_candidates(job, tas, application_limit_by_user, rng, per_job_counts[job_index])
        if not selected:
            continue

        accepted_slots = min(job["quota"], 1 + (job_index % 2))
        accepted_ids: list[str] = []
        for user, _, _, _ in selected:
            if len(accepted_ids) >= accepted_slots:
                break
            projected = accepted_hours_by_user[user.user_id] + job["workloadHours"]
            if projected <= 12 or len(accepted_ids) == 0:
                accepted_ids.append(user.user_id)
                accepted_hours_by_user[user.user_id] = projected

        shortlisted_slots = min(3, max(0, len(selected) - len(accepted_ids) - 3))
        withdrawn_slots = 1 if len(selected) >= 12 and job_index % 3 == 0 else 0
        rejected_slots = min(2, max(0, len(selected) - len(accepted_ids) - shortlisted_slots - withdrawn_slots - 1))

        shortlisted_ids = [user.user_id for user, _, _, _ in selected if user.user_id not in accepted_ids][:shortlisted_slots]
        withdrawn_ids = [
            user.user_id for user, _, _, _ in reversed(selected)
            if user.user_id not in accepted_ids and user.user_id not in shortlisted_ids
        ][:withdrawn_slots]
        rejected_ids = [
            user.user_id for user, _, _, _ in reversed(selected)
            if user.user_id not in accepted_ids and user.user_id not in shortlisted_ids and user.user_id not in withdrawn_ids
        ][:rejected_slots]

        for position, (user, score, matched_required, matched_preferred) in enumerate(selected):
            if user.user_id in accepted_ids:
                status = "ACCEPTED"
            elif user.user_id in shortlisted_ids:
                status = "SHORTLISTED"
            elif user.user_id in withdrawn_ids:
                status = "WITHDRAWN"
            elif user.user_id in rejected_ids:
                status = "REJECTED"
            else:
                status = "SUBMITTED"

            projected_hours = accepted_hours_by_user[user.user_id] if status == "ACCEPTED" else min(12, accepted_hours_by_user[user.user_id] + job["workloadHours"])
            missing_required = [skill for skill in job["requiredSkills"] if skill not in matched_required]
            score_adjusted = min(0.97, score + (0.04 if status in {"ACCEPTED", "SHORTLISTED"} else 0.0) - (0.06 if status == "REJECTED" else 0.0))
            apply_time = base_time + timedelta(hours=(job_index * 9) + position * 3)
            applications.append({
                "applicationId": f"A{4001 + app_index:04d}",
                "jobId": job["jobId"],
                "applicantId": user.user_id,
                "applyTime": apply_time.isoformat().replace("+00:00", "Z"),
                "status": status,
                "recommendationScore": round(score_adjusted, 2),
                "explanation": explanation_lines(job, matched_required, matched_preferred, missing_required, projected_hours)
            })
            app_index += 1
    return applications


def remove_previous_generated(directory: Path, prefix: str, start: int) -> None:
    for file in directory.glob(f"{prefix}*.json"):
        try:
            numeric = int(file.stem[1:])
        except ValueError:
            continue
        if numeric >= start:
            file.unlink()


def main() -> None:
    args = parse_args()
    base_dir = Path(args.base_dir).resolve() if args.base_dir else Path(__file__).resolve().parent.parent

    ta_count = ensure_positive(args.ta_count, "ta_count")
    recruiter_count = ensure_positive(args.recruiter_count, "recruiter_count")
    job_count = ensure_positive(args.job_count, "job_count")
    application_count = ensure_positive(args.application_count, "application_count")
    cv_count = max(0, min(args.cv_count, ta_count))

    users_dir = base_dir / "data" / "users"
    jobs_dir = base_dir / "data" / "jobs"
    applications_dir = base_dir / "data" / "applications"
    cv_dir = base_dir / "data" / "cv"
    system_dir = base_dir / "data" / "system"

    for path in [users_dir, jobs_dir, applications_dir, cv_dir, system_dir]:
        path.mkdir(parents=True, exist_ok=True)

    remove_previous_generated(users_dir, "U", 1101)
    remove_previous_generated(jobs_dir, "J", 2101)
    remove_previous_generated(applications_dir, "A", 4001)
    for file in cv_dir.glob("U11*_cv.txt"):
        file.unlink()

    ta_users = build_ta_users(ta_count, cv_count, cv_dir)
    recruiters = build_recruiters(recruiter_count)
    generated_users = ta_users + recruiters
    jobs = build_jobs(job_count, recruiters)
    applications = build_applications(application_count, ta_users, jobs)

    for user in generated_users:
        write_json(users_dir / f"{user.user_id}.json", user_to_payload(user))

    for job in jobs:
        payload = {key: value for key, value in job.items() if key != "keywords"}
        write_json(jobs_dir / f"{job['jobId']}.json", payload)

    for application in applications:
        write_json(applications_dir / f"{application['applicationId']}.json", application)

    counters_path = system_dir / "id-counters.json"
    counters = {"user": 9999, "job": 2003, "application": 3003}
    if counters_path.exists():
        counters.update(json.loads(counters_path.read_text(encoding="utf-8")))
    counters["user"] = max(int(counters.get("user", 0)), 9999)
    counters["job"] = max(int(counters.get("job", 0)), 2100 + len(jobs))
    counters["application"] = max(int(counters.get("application", 0)), 4000 + len(applications))
    write_json(counters_path, counters)

    print(json.dumps({
        "baseDir": str(base_dir),
        "generated": {
            "taUsers": len(ta_users),
            "recruiters": len(recruiters),
            "jobs": len(jobs),
            "applications": len(applications),
            "cvFiles": cv_count
        },
        "totalsAfterGeneration": {
            "users": len(list(users_dir.glob('*.json'))),
            "jobs": len(list(jobs_dir.glob('*.json'))),
            "applications": len(list(applications_dir.glob('*.json')))
        }
    }, indent=2))


if __name__ == "__main__":
    main()
