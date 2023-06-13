# Merge workflow

## Setup

- Install `requirements.txt` in python environment.

- Set the `patch_dir` inside `analyze_patches.py`, as well as the `changeset_range` you are
  interested in. The directory should hold a flat list of patches in the form `####.patch` and an
  `hg_log.txt` holding a summary of the patches.

- If desired, modify the `Patch.should_ignore` method in `analyze_patches.py`. By default, it
  ignores patches outside `changeset_range` and patches whose summary begins with the string
  "ignore".

## Cluster analysis

Running `python merge/analyze_patches.py` will generate a file `patch_clusters.txt`. It holds
clusters of changesets, based on the graph of affected files. The first cluster contains patches
that were independent of any other. The second and greater all have possible file conflicts. The
first and smaller clusters should theoretically be easier to merge first, since they're more
isolated/independent.

Run `python merge/graph_clusters.py` to visualize the clusters in your web browser. Use this to do a
deeper analysis and figure out which changesets will be easiest to tackle. The web view doesn't let
you search for files/patches, but you can edit the `highlight` variable in `graph_clusters.py` with
ones you wish to identify. It will display highlighted nodes as stars instead of circles.

File deletions are ommitted from the cluster analysis. Additionally, `graph_clusters.py` will ignore:
- patch ids, full filenames, or shortened filenames that are listed in `complete.json`
- the same as the previous, but listed in the `excluded` variable inside `graph_clusters.py`
- patches that have no conflicts with other patches (modify `graph_clusters.py` to disable this)

You can use this to simply ignore certain files/patches in the graph, even if they aren't necessarily
complete. E.g. if there is a metadata file that gets modified by many patches, it can be helpful to
exclude it to cleanup the graph visualization.

## Applying patches

Run `python merge/apply_patch.py PATCH_ID...` to attempt to apply 1+ patches to the source. It
generates a directory structure:
```
+ diff
|   prefix
|   + base
|   + patched
|   + patches
```
Inside `base`/`patched` are a mirrored directory tree for the patched files.
- `prefix`: path prefix that was stripped from the modified files when creating `base`/`patched` directories
- `base`: linked files with the source code we're applying patches to
- `patched`: patched files; additionally reject files (`.rej` extension) with patches that failed
- `patches`: linked `###.patch` files, for easy access

This can be used to *stage* the merge. You can run `merge/visual.sh` to launch meld on the staging
`base`/`patched` to easily verify the merge was correct.

Once you've verified, or manually fixed the files in `patched`, you can call `python
merge/apply_patch.py commit` to overwrite the source. Use `--dryrun` option to preview the copy. Any
leftover `.rej` files are ignored. Generally, I will do a 1st pass using the staged files, then
commit and do a second pass on the actual source if there are any Java errors detected by
intellisense.

Once merge is complete, commit modified files with the patch number in the commit message.
Committing the changes immediately allows you to easily revert when working on the next patch.
Optionally, add the patch numbers to `complete.json`, to ignore them in subsequent cluster analysis
runs (`graph_clusters.py`).

### Patching issues
Some common merge issues and my strategy for fixing them:
- Base file wasn't found, e.g. the file was renamed or deleted. First, check if the file was newly
  created. Go to the `###.patch` file and see if the base was `/dev/null`. Otherwise, run the
  following:
  ```
  git log --all --full-history -- "**/FILENAME_HERE.*"
  ```
  ... to find possible commits where the file was renamed/deleted. Check the commit history, and if
  renamed, modify the `###.patch` file with the new file location.
- Patch had no changes. This indicates a binary file was replaced. Go find the binary file and copy
  it over.
- A patch's hunk couldn't be matched in the source file. You'll need to go in and manually apply the
  change. You can look through the git timeline to see when the source code did match, then step
  through the commits to find where they diverged. Or you can use git blame if the line was not
  deleted, simply modified.

## TODO
- script that automatically runs apply_patches.py on every patch sequentially to see which ones
  can be merged easily without conflicts; maybe set fuzz factor to zero; possibly run without
  cleaning up directory, to get cases where file is modified multiple times by different patches