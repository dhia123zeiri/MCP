# Admin CLI

Standalone CLI for the Doctor Office EAI project. Talks to the REST API
at `$API_BASE` (default `http://localhost:8080`).

## Usage

```bash
# Interactive menu
python admin.py

# Direct commands
python admin.py countries-list
python admin.py country-add TN Tunisia
python admin.py pettypes-list
python admin.py pettype-add Dog Beagle 95.0
python admin.py metrics list
python admin.py metrics revenue-per-item
python admin.py metrics totals
python admin.py metrics top-country-per-item
```

No third-party dependencies — uses only the Python standard library.
