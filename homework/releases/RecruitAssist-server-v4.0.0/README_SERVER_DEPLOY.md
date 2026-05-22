# RecruitAssist Server Deployment Package

Version: v4.0.0 / Sprint 4  
Package target: `homework/releases/server/RecruitAssist-server-v4.0.0`

## 1. Package Contents

```text
RecruitAssist-server-v4.0.0/
├── app/
│   └── recruitassist-web.war          # Built WAR for servlet container deployment
├── data/                              # Required JSON/TXT data files
│   ├── users/
│   ├── jobs/
│   ├── applications/
│   ├── notifications/
│   ├── cv/
│   └── system/
├── docs/                              # Feature guide and contribution reports
├── framework/recruitassist-web/        # Source module for Maven/Jetty startup
├── logs/
│   ├── access/audit.csv
│   ├── app/
│   └── build/
└── scripts/
    ├── start-maven-jetty.sh            # Recommended quick server startup
    ├── check-health.sh                 # Health check helper
    └── load_test_recruitassist.py      # Optional load test script
```

## 2. Environment Requirements

- OS: Linux/macOS server shell
- Java: JDK 17
- Maven: 3.9+ if using the Maven/Jetty startup script
- Servlet container option: Jetty 12 EE10 or Tomcat 10.1+ if deploying the WAR directly
- Disk write permission for this release directory, because the app stores JSON/CSV/CV files locally
- Network: open the selected HTTP port, default `8080`

Important environment variable:

```bash
export RECRUITASSIST_BASE_DIR=/absolute/path/to/RecruitAssist-server-v4.0.0
```

This must point to the package root containing `data/`, `logs/`, and `framework/`.

## 3. Recommended Startup: Maven + Jetty

From inside this package:

```bash
chmod +x scripts/start-maven-jetty.sh scripts/check-health.sh
./scripts/start-maven-jetty.sh
```

Use another port:

```bash
PORT=8081 ./scripts/start-maven-jetty.sh
```

Then open:

```text
http://SERVER_HOST:8080/home
http://SERVER_HOST:8080/login
http://SERVER_HOST:8080/dashboard
http://SERVER_HOST:8080/health
```

## 4. WAR Deployment Option

If deploying to Tomcat 10.1+ or Jetty 12 EE10:

1. Set `RECRUITASSIST_BASE_DIR` in the service environment to this package root.
2. Copy `app/recruitassist-web.war` into the container webapps directory.
3. Deploy as `ROOT.war` if you want the app at `/`, or keep `recruitassist-web.war` for `/recruitassist-web`.
4. Ensure the server process can write to `data/` and `logs/`.

Example for Tomcat:

```bash
export RECRUITASSIST_BASE_DIR=/opt/RecruitAssist-server-v4.0.0
cp app/recruitassist-web.war "$CATALINA_BASE/webapps/ROOT.war"
```

## 5. Data Notes

- The app is zero-database. All runtime data lives in `data/`.
- Uploaded CV files live in `data/cv/`.
- Permanent protected PDF tokens are stored in each user's JSON profile after generation.
- Audit records live in `logs/access/audit.csv`.
- Do not delete `data/system/id-counters.json`, because it controls generated IDs.

## 6. Verification

After startup:

```bash
./scripts/check-health.sh
```

Expected response contains:

```json
{"status":"UP"}
```

Build verification used for this package:

```bash
mvn -q -DskipTests package
```

