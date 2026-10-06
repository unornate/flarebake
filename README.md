# flarebake

A tiny CLI that prepares a [JBake](https://jbake.org) site for deployment to
[Cloudflare Workers](https://developers.cloudflare.com/workers/) using
Cloudflare's Git integration (Workers Builds) — no GitHub Actions required.

`flarebake` adds exactly the files a JBake project needs:

- `wrangler.toml` — an assets-only Worker whose `[build]` command bakes the site.
- `build.sh` — downloads a private Temurin JRE and JBake, then bakes `./output`.
- `.gitignore` — entries for the generated `output/`, `.tools/` and `.wrangler/`.

## Requirements

- Java 27+ (to build; the produced jar runs on Java 27+)

Maven itself is not required: the project ships a Maven Wrapper (`./mvnw` on
Unix, `mvnw.cmd` on Windows).

## Build

```bash
./mvnw clean package
```

On Windows:

```powershell
mvnw.cmd clean package
```

The uber jar is written to `target/flarebake.jar`.

## Usage

```bash
# Scaffold the current directory
java -jar target/flarebake.jar init

# Scaffold another project and set the Worker name explicitly
java -jar target/flarebake.jar init ~/Projects/my-site --name my-site

# Overwrite existing wrangler.toml / build.sh
java -jar target/flarebake.jar init --force
```

Options:

| Option | Description |
| --- | --- |
| `-n`, `--name <name>` | Cloudflare Worker name (default: derived from the directory name). |
| `-f`, `--force` | Overwrite existing `wrangler.toml` and `build.sh`. |
| `--jbake-version <v>` | JBake version used by `build.sh` (default `2.7.0`). |
| `--java-version <v>` | Temurin JRE major version used by `build.sh` (default `21`). |
| `--compatibility-date <d>` | Cloudflare `compatibility_date` (default: today). |

## Cloudflare setup

1. Commit the generated files and push.
2. In the Cloudflare dashboard: **Workers & Pages → Create → Workers → Connect to Git**.
3. Set the **Deploy command** to `npx wrangler deploy` and leave the build command empty
   (Wrangler runs `build.sh` itself via the `[build]` block).
