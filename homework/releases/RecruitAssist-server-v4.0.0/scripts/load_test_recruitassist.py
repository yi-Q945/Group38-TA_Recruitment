#!/usr/bin/env python3
"""Small standard-library load tester for RecruitAssist.

The script intentionally defaults to read-only traffic. Mutating scenarios must
be explicitly enabled with --allow-mutations so demo JSON data is not changed by
accident.
"""

from __future__ import annotations

import argparse
import csv
import json
import random
import re
import statistics
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, asdict
from http.cookiejar import CookieJar
from pathlib import Path
from typing import Iterable


JOB_ID_RE = re.compile(r"jobId=([A-Za-z0-9_-]+)")
CSRF_RE = re.compile(r'name="csrfToken" value="([^"]+)"')


@dataclass(frozen=True)
class RequestResult:
    worker: int
    base_url: str
    scenario: str
    method: str
    path: str
    status: int
    ok: bool
    elapsed_ms: float
    bytes_read: int
    error: str = ""


class VirtualUser:
    def __init__(self, worker_id: int, base_url: str, timeout: float) -> None:
        self.worker_id = worker_id
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.cookies = CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.job_ids: list[str] = []
        self.csrf_token = ""

    def request(self, method: str, path: str, data: dict[str, str] | None, scenario: str) -> RequestResult:
        encoded = None
        headers = {
            "User-Agent": "RecruitAssistLoadTest/1.0",
            "Accept": "text/html,application/json;q=0.9,*/*;q=0.8",
        }
        if data is not None:
            encoded = urllib.parse.urlencode(data).encode("utf-8")
            headers["Content-Type"] = "application/x-www-form-urlencoded"

        url = self.base_url + path
        request = urllib.request.Request(url, data=encoded, headers=headers, method=method)
        started = time.perf_counter()
        status = 0
        body = b""
        error = ""
        try:
            with self.opener.open(request, timeout=self.timeout) as response:
                status = response.getcode()
                body = response.read()
        except urllib.error.HTTPError as exc:
            status = exc.code
            body = exc.read()
            error = exc.reason or ""
        except Exception as exc:  # noqa: BLE001 - surfaced in metrics for load experiments.
            error = exc.__class__.__name__ + ": " + str(exc)
        elapsed_ms = (time.perf_counter() - started) * 1000
        ok = 200 <= status < 400 and not error
        if body:
            match = CSRF_RE.search(body.decode("utf-8", errors="ignore"))
            if match:
                self.csrf_token = match.group(1)
        return RequestResult(
            worker=self.worker_id,
            base_url=self.base_url,
            scenario=scenario,
            method=method,
            path=path,
            status=status,
            ok=ok,
            elapsed_ms=elapsed_ms,
            bytes_read=len(body),
            error=error,
        )

    def login(self, username: str, password: str) -> list[RequestResult]:
        warmup = self.request("GET", "/login", None, "login-page")
        login = self.request("POST", "/login", {"csrfToken": self.csrf_token, "username": username, "password": password}, "login")
        return [warmup, login]

    def discover_jobs(self) -> RequestResult:
        result = self.request("GET", "/dashboard", None, "discover-jobs")
        if result.ok:
            # Re-request through opener so we can parse body without complicating RequestResult.
            try:
                with self.opener.open(self.base_url + "/dashboard", timeout=self.timeout) as response:
                    html = response.read().decode("utf-8", errors="ignore")
                self.job_ids = sorted(set(JOB_ID_RE.findall(html)))
            except Exception:
                self.job_ids = []
        return result


def choose_base_url(base_urls: list[str], worker_id: int, strategy: str) -> str:
    if strategy == "random":
        return random.choice(base_urls)
    return base_urls[worker_id % len(base_urls)]


def choose_account(accounts: list[tuple[str, str]], worker_id: int) -> tuple[str, str]:
    return accounts[worker_id % len(accounts)]


def parse_accounts(raw_accounts: Iterable[str], username: str, password: str) -> list[tuple[str, str]]:
    accounts: list[tuple[str, str]] = []
    for raw in raw_accounts:
        if ":" not in raw:
            raise ValueError("--account must use username:password format")
        user, pwd = raw.split(":", 1)
        accounts.append((user.strip(), pwd.strip()))
    if not accounts:
        accounts.append((username, password))
    return accounts


def worker_loop(args: argparse.Namespace, worker_id: int, stop_at: float, results: list[RequestResult], lock: threading.Lock) -> None:
    base_url = choose_base_url(args.base_url, worker_id, args.balance)
    username, password = choose_account(args.accounts, worker_id)
    user = VirtualUser(worker_id, base_url, args.timeout)
    local_results: list[RequestResult] = []

    if args.scenario in {"ta-browse", "ta-apply"}:
        local_results.extend(user.login(username, password))
        local_results.append(user.discover_jobs())

    request_index = 0
    while time.perf_counter() < stop_at:
        if args.scenario == "readonly":
            path = "/health" if request_index % 3 else "/home"
            local_results.append(user.request("GET", path, None, "readonly"))
        elif args.scenario == "ta-browse":
            if user.job_ids and request_index % 2:
                job_id = user.job_ids[request_index % len(user.job_ids)]
                local_results.append(user.request("GET", f"/jobs/detail?jobId={job_id}", None, "job-detail"))
            else:
                local_results.append(user.request("GET", "/dashboard", None, "dashboard"))
        elif args.scenario == "ta-apply":
            job_id = args.apply_job_id or (user.job_ids[0] if user.job_ids else "")
            if not job_id:
                local_results.append(RequestResult(worker_id, base_url, "ta-apply", "POST", "/apply", 0, False, 0, 0, "No job id available"))
                break
            local_results.append(user.request("POST", "/apply", {"csrfToken": user.csrf_token, "jobId": job_id}, "ta-apply"))
            if not args.repeat_mutations:
                break
        request_index += 1
        if args.think_time_ms:
            time.sleep(args.think_time_ms / 1000)

    with lock:
        results.extend(local_results)


def percentile(values: list[float], pct: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = min(len(ordered) - 1, int(round((pct / 100) * (len(ordered) - 1))))
    return ordered[index]


def print_summary(results: list[RequestResult], elapsed: float) -> None:
    latencies = [item.elapsed_ms for item in results if item.elapsed_ms > 0]
    successes = sum(1 for item in results if item.ok)
    failures = len(results) - successes
    summary = {
        "total_requests": len(results),
        "successes": successes,
        "failures": failures,
        "success_rate": round(successes / len(results), 4) if results else 0,
        "requests_per_second": round(len(results) / elapsed, 2) if elapsed else 0,
        "latency_ms": {
            "mean": round(statistics.fmean(latencies), 2) if latencies else 0,
            "p50": round(percentile(latencies, 50), 2),
            "p95": round(percentile(latencies, 95), 2),
            "p99": round(percentile(latencies, 99), 2),
            "max": round(max(latencies), 2) if latencies else 0,
        },
    }
    print(json.dumps(summary, indent=2))

    by_target: dict[str, list[RequestResult]] = {}
    for item in results:
        by_target.setdefault(item.base_url, []).append(item)
    for target, items in sorted(by_target.items()):
        ok = sum(1 for item in items if item.ok)
        print(f"{target}: {ok}/{len(items)} ok")


def write_results(path: Path, results: list[RequestResult]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.suffix.lower() == ".json":
        path.write_text(json.dumps([asdict(item) for item in results], indent=2), encoding="utf-8")
        return

    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(asdict(results[0]).keys()) if results else RequestResult.__dataclass_fields__.keys())
        writer.writeheader()
        for item in results:
            writer.writerow(asdict(item))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="RecruitAssist high-concurrency load experiment runner")
    parser.add_argument("--base-url", action="append", required=True, help="Base URL. Repeat to simulate load balancing across instances.")
    parser.add_argument("--balance", choices=["round-robin", "random"], default="round-robin")
    parser.add_argument("--scenario", choices=["readonly", "ta-browse", "ta-apply"], default="readonly")
    parser.add_argument("--concurrency", type=int, default=20)
    parser.add_argument("--duration", type=float, default=30, help="Test duration in seconds")
    parser.add_argument("--timeout", type=float, default=8)
    parser.add_argument("--think-time-ms", type=int, default=0)
    parser.add_argument("--username", default="alice.ta")
    parser.add_argument("--password", default="demo123")
    parser.add_argument("--account", action="append", default=[], help="Extra account in username:password format")
    parser.add_argument("--apply-job-id", default="", help="Required or auto-discovered for ta-apply")
    parser.add_argument("--allow-mutations", action="store_true", help="Required for ta-apply because it writes application data")
    parser.add_argument("--repeat-mutations", action="store_true", help="Keep posting /apply for the full duration")
    parser.add_argument("--out", type=Path, help="Optional .csv or .json file with per-request results")
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    args.accounts = parse_accounts(args.account, args.username, args.password)

    if args.concurrency < 1:
        parser.error("--concurrency must be >= 1")
    if args.duration <= 0:
        parser.error("--duration must be > 0")
    if args.scenario == "ta-apply" and not args.allow_mutations:
        parser.error("ta-apply mutates JSON data; rerun with --allow-mutations when you intend to test writes")

    started = time.perf_counter()
    stop_at = started + args.duration
    results: list[RequestResult] = []
    lock = threading.Lock()
    threads = [
        threading.Thread(target=worker_loop, args=(args, worker_id, stop_at, results, lock), daemon=True)
        for worker_id in range(args.concurrency)
    ]
    for thread in threads:
        thread.start()
    for thread in threads:
        thread.join()

    elapsed = time.perf_counter() - started
    print_summary(results, elapsed)
    if args.out:
        write_results(args.out, results)
        print(f"wrote {len(results)} rows to {args.out}")
    return 1 if any(not item.ok for item in results) else 0


if __name__ == "__main__":
    sys.exit(main())
