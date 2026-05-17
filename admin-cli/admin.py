#!/usr/bin/env python3
"""
Doctor Office Admin CLI — Assignment #3.

Talks to the doctor-office REST API to:
  - add and list countries (requirements #1, #2)
  - add and list pet types/items (requirements #3, #4)
  - read all stream-computed metrics (requirements #5-#17)

Usage:
    python admin.py                          # interactive menu
    python admin.py countries-list
    python admin.py country-add PT Portugal
    python admin.py pettypes-list
    python admin.py pettype-add Dog Beagle 95.0
    python admin.py metrics revenue-per-item
    python admin.py metrics totals

Env:
    API_BASE (default: http://localhost:8080)
"""

import os
import sys
import json
import argparse
import urllib.request
import urllib.parse
import urllib.error

API_BASE = os.environ.get("API_BASE", "http://localhost:8080")

# ── HTTP helpers ──────────────────────────────────────────────────────────

def _request(method: str, path: str, params=None, body=None):
    url = f"{API_BASE}{path}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            raw = resp.read().decode() or "{}"
            try:
                return resp.status, json.loads(raw)
            except json.JSONDecodeError:
                return resp.status, raw
    except urllib.error.HTTPError as e:
        body_text = e.read().decode(errors="replace")
        return e.code, body_text


def _print(status, payload):
    print(f"[{status}]")
    if isinstance(payload, (dict, list)):
        print(json.dumps(payload, indent=2, default=str))
    else:
        print(payload)


# ── Commands ──────────────────────────────────────────────────────────────

def cmd_countries_list(_):
    _print(*_request("GET", "/api/refdata/countries"))

def cmd_country_add(args):
    _print(*_request("POST", "/api/refdata/countries",
                     params={"code": args.code, "name": args.name}))

def cmd_pettypes_list(_):
    _print(*_request("GET", "/api/refdata/pettypes"))

def cmd_pettype_add(args):
    _print(*_request("POST", "/api/refdata/pettypes",
                     params={"species": args.species, "breed": args.breed, "price": args.price}))

METRIC_ENDPOINTS = {
    "revenue-per-item":             "/api/metrics/revenue-per-item",
    "expenses-per-item":            "/api/metrics/expenses-per-item",
    "profit-per-item":              "/api/metrics/profit-per-item",
    "totals":                       "/api/metrics/totals",
    "avg-per-appointment-by-item":  "/api/metrics/avg-per-appointment-by-item",
    "avg-per-appointment-all":      "/api/metrics/avg-per-appointment-all",
    "top-profit-item":              "/api/metrics/top-profit-item",
    "windowed-revenue":             "/api/metrics/windowed?metric=revenue",
    "windowed-expenses":            "/api/metrics/windowed?metric=expenses",
    "windowed-profit":              "/api/metrics/windowed?metric=profit",
    "top-country-per-item":         "/api/metrics/top-country-per-item",
}

def cmd_metrics(args):
    key = args.metric
    if key == "list":
        for k in METRIC_ENDPOINTS:
            print(f"  {k}")
        return
    if key not in METRIC_ENDPOINTS:
        print(f"Unknown metric '{key}'. Use 'metrics list' to see options.")
        sys.exit(2)
    _print(*_request("GET", METRIC_ENDPOINTS[key]))


# ── Interactive menu ──────────────────────────────────────────────────────

def interactive():
    menu = """
=============== Doctor Office Admin CLI ===============
  1) List countries           (#2)
  2) Add country              (#1)
  3) List pet types/items     (#4)
  4) Add pet type/item        (#3)
  5) Revenue per item         (#5)
  6) Expenses per item        (#6)
  7) Profit per item          (#7)
  8) Totals (revenue/exp/profit)   (#8 #9 #10)
  9) Avg per appointment by item   (#11)
 10) Avg per appointment overall   (#12)
 11) Item with highest profit      (#13)
 12) Windowed revenue (last hour)  (#14)
 13) Windowed expenses             (#15)
 14) Windowed profit               (#16)
 15) Top country per item          (#17)
  0) Quit
=======================================================
"""
    while True:
        print(menu)
        choice = input("Choice: ").strip()
        if choice == "0":
            return
        elif choice == "1":
            cmd_countries_list(None)
        elif choice == "2":
            code = input("Code (2-3 letters, e.g. PT): ").strip()
            name = input("Name: ").strip()
            cmd_country_add(argparse.Namespace(code=code, name=name))
        elif choice == "3":
            cmd_pettypes_list(None)
        elif choice == "4":
            species = input("Species: ").strip()
            breed = input("Breed: ").strip()
            price = float(input("Price: ").strip())
            cmd_pettype_add(argparse.Namespace(species=species, breed=breed, price=price))
        elif choice in ("5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"):
            key = {
                "5":  "revenue-per-item",
                "6":  "expenses-per-item",
                "7":  "profit-per-item",
                "8":  "totals",
                "9":  "avg-per-appointment-by-item",
                "10": "avg-per-appointment-all",
                "11": "top-profit-item",
                "12": "windowed-revenue",
                "13": "windowed-expenses",
                "14": "windowed-profit",
                "15": "top-country-per-item",
            }[choice]
            cmd_metrics(argparse.Namespace(metric=key))
        else:
            print("Unknown choice")


# ── Entry point ───────────────────────────────────────────────────────────

def main():
    p = argparse.ArgumentParser(description="Doctor Office admin CLI")
    sub = p.add_subparsers(dest="cmd")

    sub.add_parser("countries-list")

    p_country = sub.add_parser("country-add")
    p_country.add_argument("code")
    p_country.add_argument("name")

    sub.add_parser("pettypes-list")

    p_pet = sub.add_parser("pettype-add")
    p_pet.add_argument("species")
    p_pet.add_argument("breed")
    p_pet.add_argument("price", type=float)

    p_met = sub.add_parser("metrics")
    p_met.add_argument("metric", help="Use 'list' to see options.")

    args = p.parse_args()

    if args.cmd is None:
        interactive()
        return

    dispatch = {
        "countries-list": cmd_countries_list,
        "country-add":    cmd_country_add,
        "pettypes-list":  cmd_pettypes_list,
        "pettype-add":    cmd_pettype_add,
        "metrics":        cmd_metrics,
    }
    dispatch[args.cmd](args)


if __name__ == "__main__":
    main()
