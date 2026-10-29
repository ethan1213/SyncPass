"""Package only the API source into EC2 user data. No local secrets are included."""
import base64
import io
import pathlib
import tarfile

root = pathlib.Path(__file__).resolve().parents[1]
source = root / "backend"
archive_bytes = io.BytesIO()
with tarfile.open(fileobj=archive_bytes, mode="w:gz") as archive:
    for path in sorted(source.rglob("*")):
        if path.is_file() and "target" not in path.parts and "test" not in path.parts:
            archive.add(path, arcname=path.relative_to(root).as_posix())

payload = base64.b64encode(archive_bytes.getvalue()).decode("ascii")
template = (root / "deploy" / "ec2-user-data.template.sh").read_text(encoding="utf-8")
result = template.replace("__BACKEND_ARCHIVE_BASE64__", payload)
output = root / "deploy" / "ec2-user-data.generated.sh"
output.write_text(result, encoding="utf-8", newline="\n")
if len(result.encode("utf-8")) > 16_384:
    raise SystemExit(f"EC2 user data too large: {len(result.encode('utf-8'))} bytes")
print(f"Wrote {output} ({len(result.encode('utf-8'))} bytes)")
