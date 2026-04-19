"""
site_base — resolve the public GitHub Pages URL of a repo.

This fork (teslasolar) builds a SCADA view that watches TWO hosts:

  mine()    — wherever THIS workflow is running. Derived from
              GH_TAG_REPO ("owner/repo" env) or the SITE_BASE override.
              Falls back to https://teslasolar.github.io/aicraftspeopleguild.github.io
              so local dev works without env vars.

  origin()  — the canonical guild site, always
              https://aicraftspeopleguild.github.io. Overridable with
              ORIGIN_BASE env for forks of the fork.

Pages URLs follow two shapes:
  owner.github.io/repo-name     when repo != <owner>.github.io   (project page)
  owner.github.io               when repo == <owner>.github.io   (user/org page)
"""
import os


ORIGIN_DEFAULT = "https://aicraftspeopleguild.github.io"
MINE_FALLBACK  = "https://teslasolar.github.io/aicraftspeopleguild.github.io"


def _from_repo_slug(slug: str) -> str:
    owner, _, name = slug.partition("/")
    if not name:
        return MINE_FALLBACK
    if name == f"{owner}.github.io":
        return f"https://{owner}.github.io"
    return f"https://{owner}.github.io/{name}"


def mine() -> str:
    override = os.environ.get("SITE_BASE", "").rstrip("/")
    if override:
        return override
    repo = os.environ.get("GH_TAG_REPO", "").strip()
    if repo:
        return _from_repo_slug(repo)
    return MINE_FALLBACK


def origin() -> str:
    return os.environ.get("ORIGIN_BASE", ORIGIN_DEFAULT).rstrip("/")


def site_base() -> str:
    """Legacy single-value accessor — returns mine(). New code should
    explicitly call mine() or origin() for clarity."""
    return mine()


if __name__ == "__main__":
    print(f"mine()   = {mine()}")
    print(f"origin() = {origin()}")
