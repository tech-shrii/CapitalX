import requests

BASE_URL = "https://stooq.com"
SYMBOL = "gc.f"

def test_html_page():
    """Check the human-readable quote page"""
    url = f"{BASE_URL}/q/?s={SYMBOL}"
    resp = requests.get(url, timeout=10)
    print("==== HTML PAGE ====")
    print("Status:", resp.status_code)
    print(resp.text[:500])  # print first 500 chars


def test_csv_quote():
    """Check CSV quote endpoint"""
    url = f"{BASE_URL}/q/l/"
    params = {
        "s": SYMBOL,
        "f": "sd2t2ohlc",
        "h": "",
        "e": "csv"
    }
    resp = requests.get(url, params=params, timeout=10)

    print("\n==== CSV ENDPOINT ====")
    print("Status:", resp.status_code)
    print("RAW RESPONSE:")
    print(resp.text)

    lines = resp.text.strip().splitlines()

    if not lines:
        print("❌ Empty response")
        return

    # Skip header if present
    if lines[0].lower().startswith("symbol"):
        data_line = lines[1]
    else:
        data_line = lines[0]

    parts = data_line.split(",")

    print("\nParsed Fields:")
    print("Symbol :", parts[0])
    print("Date   :", parts[1])
    print("Time   :", parts[2])
    print("Open   :", parts[3])
    print("High   :", parts[4])
    print("Low    :", parts[5])
    print("Close  :", parts[6])


if __name__ == "__main__":
    test_html_page()
    test_csv_quote()
