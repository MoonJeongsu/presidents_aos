#!/usr/bin/env python3
"""Upload speech .txt files from assets to Firebase Storage."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    import firebase_admin
    from firebase_admin import storage
    from google.cloud import storage as gcs
except ImportError:
    print("Install dependencies: pip install firebase-admin google-cloud-storage")
    sys.exit(1)


def find_bucket_name(project: str, explicit: str | None) -> str:
    if explicit:
        return explicit

    client = gcs.Client(project=project)
    buckets = list(client.list_buckets())
    if not buckets:
        print()
        print("[ERROR] No Storage bucket found.")
        print("Enable Firebase Storage first:")
        print(f"  https://console.firebase.google.com/project/{project}/storage")
        print('Click "Get started" and create the default bucket.')
        sys.exit(1)

    preferred_suffixes = (
        ".firebasestorage.app",
        ".appspot.com",
    )
    for suffix in preferred_suffixes:
        for bucket in buckets:
            if bucket.name == f"{project}{suffix}":
                return bucket.name

    for bucket in buckets:
        if project in bucket.name:
            return bucket.name

    return buckets[0].name


def main() -> int:
    parser = argparse.ArgumentParser(description="Upload speeches to Firebase Storage")
    parser.add_argument(
        "--project",
        default="presidential-speeches-a9f00",
        help="Firebase project ID",
    )
    parser.add_argument(
        "--bucket",
        default="",
        help="Storage bucket name (auto-detected if omitted)",
    )
    parser.add_argument(
        "--source",
        default=str(
            Path(__file__).resolve().parents[1]
            / "app"
            / "src"
            / "main"
            / "assets"
            / "speeches"
        ),
        help="Directory containing speech .txt files",
    )
    args = parser.parse_args()

    source_dir = Path(args.source)
    if not source_dir.is_dir():
        print(f"Source directory not found: {source_dir}")
        return 1

    txt_files = sorted(source_dir.glob("*.txt"))
    if not txt_files:
        print(f"No .txt files found in {source_dir}")
        return 1

    bucket_name = find_bucket_name(args.project, args.bucket.strip() or None)
    print(f"Using bucket: {bucket_name}")

    if not firebase_admin._apps:
        firebase_admin.initialize_app(options={"storageBucket": bucket_name})

    bucket = storage.bucket(bucket_name)
    uploaded = 0

    for file_path in txt_files:
        blob_path = f"speeches/{file_path.name}"
        blob = bucket.blob(blob_path)
        blob.upload_from_filename(str(file_path), content_type="text/plain; charset=utf-8")
        uploaded += 1
        if uploaded % 50 == 0:
            print(f"Uploaded {uploaded}/{len(txt_files)} ...")

    print(f"Done. Uploaded {uploaded} files to gs://{bucket.name}/speeches/")
    print(f"BUCKET_NAME={bucket.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
