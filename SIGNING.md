# Signing the Windows build (SmartScreen / Smart App Control)

Windows 11's **Smart App Control** blocks unsigned applications — there is no
"Run anyway" escape. Plain SmartScreen is friendlier (a "Run anyway" button)
but still scare-screens every download. Both only go away with an
**Authenticode signature**, which cannot be produced from this repository:
a code-signing certificate must come from a CA (or SignPath). This repo is
ready to sign the moment you supply one — nothing else changes.

## Option A (implemented, zero code): own PFX certificate

Buy an OV or EV code-signing certificate (Sectigo/Comodo, DigiCert, SSL.com…)
and export it as a `.pfx` guarded by a password. Then:

```powershell
# base64-encode the .pfx (do this once, on your machine)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\cert\ytdesktop.pfx")) | clip
```

Add two **repository secrets** (Settings → Secrets and variables → Actions):

| Secret                | Value                          |
|-----------------------|--------------------------------|
| `CODESIGN_PFX_B64`    | the base64 string from above    |
| `CODESIGN_PASSWORD`   | the pfx password                |

`.github/workflows/build.yml` already contains a `Sign MSI and EXE` step
(signtool, SHA-256, RFC 3161 timestamp to DigiCert) that activates
automatically when those secrets exist, and a loud `::warning::` when they
don't. Next push produces a **signed** MSI + EXE.

## Option B (recommended for open source, no capital): SignPath

[SignPath](https://signpath.io) offers free signing for approved open-source
projects and never exposes a certificate — it signs your files on its HSM.
Flow: enroll your project, then add two steps to the `windows` job in
`build.yml` (after packaging, before the boot smoke):

```yaml
- uses: signpath/github-action-submit-signing-request@v1
  with:
    api-token: ${{ secrets.SIGNPATH_API_TOKEN }}
    organization-id: ${{ secrets.SIGNPATH_ORGANIZATION_ID }}
    project-slug: yt-desktop
    signing-policy-slug: test-signing
    artifacts-directory: desktopApp/build/compose/binaries/main/
- uses: signpath/github-action-download-signed-artifacts@v1
  with:
    api-token: ${{ secrets.SIGNPATH_API_TOKEN }}
    organization-id: ${{ secrets.SIGNPATH_ORGANIZATION_ID }}
    project-slug: yt-desktop
    signed-artifacts-directory: signed/dist
```

Then upload from `signed/dist` instead of the raw `compose/binaries/main`
paths. (The file names/paths above are illustrative — fill in your real
project slug and policy names.)

## Reality check — reputation

A signature is necessary but not always sufficient. Smart App Control also
scores **publisher reputation**. A brand-new signed app may still warn for a
while; reputation builds as more people install it. Once a signed release
exists, ask a few testers to install so Microsoft's cloud reputation catches
up.

## What the current release looks like

Artifacts are **unsigned** until one of the options above is completed. The
workflow prints this warning on every build that isn't signed.