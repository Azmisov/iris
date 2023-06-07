""" With this file, you can take a list of Patch's, as parsed from analyze_patches.py, and test them
    out on the source. A directory structure is created:
        + diff
        |   + base
        |   + patched
        |   prefix

    Where base/patched directories contain the original and patched files, allowing you to do a
    directory diff with an external program like meld. The patched file structure is mirrored, but
    with common path prefix stripped and logged inside `prefix` file. Patching is performed using
    external `patch` program, which can be customized in `apply` method.

    Use like so:
        > python apply_patch.py 9123 1003
    Which builds staging for those two patches

    After verifying or manually modifying files from `patched`, you can commit them to the main
    source code using the following:
        > python apply_patch.py commit
    Append --dryrun flag to do a dryrun without copying
"""
import sys, os, shutil, subprocess
from analyze_patches import Patch, __dir__

def apply(ipath, opath, diffs, fuzz:int=2):
    """ Apply one diff from a patch file 
        :param ipath (str): path to input to be patched
        :param opath (str): path to patched output file; rejects are the same with .rej extension appended
        :param diff (list): list of the whatthepatch diff objects to be applied
        :param fuzz (int): fuzzy matching for line locations
        :returns: (bool) True if patching succeeded
    """
    patch = "\n".join(map(lambda d: d.text, diffs))
    ret = subprocess.run([
        "patch",
        "-F", str(fuzz),
        "-ul", # universal format, ignore whitespace
        "--follow-symlinks",
        "--reject-format=unified",
        "-o", opath, # successful patch
        "-r", opath+".rej", # failed patch
        ipath,
    ], input=patch, text=True)
    return not ret.returncode

if len(sys.argv) <= 1:
    raise RuntimeError("Pass list of changeset ID's as arguments, or single `commit` command")

# Commit changes
if sys.argv[1] == 'commit':
    dryrun = len(sys.argv) > 2 and sys.argv[2] == "--dryrun"
    if dryrun:
        print("Doing a dry run, will not copy files")
    with open(os.path.join(__dir__, "diff", "prefix")) as f:
        prefix = f.read()
    root = os.path.join(__dir__, "diff", "patched")
    for (dir, dnames, fnames) in os.walk(root):
        for fname in fnames:
            if fname.endswith(".rej"):
                print("Ignoring reject file:", fname)
                continue
            ifile = os.path.join(dir, fname)
            ofile = os.path.join(prefix, os.path.relpath(ifile, root))
            print("Copying")
            print("   src:", ifile)
            print("   dst:", ofile)
            if not dryrun:
                shutil.copy(ifile, ofile)
    sys.exit()

# Get patches
Patch.parse_hg_log()
print("\nPreparing diff")
def get_patch(id):
    p = None
    try:
        p = Patch.get(int(id))
    except: pass
    if not p:
        raise ValueError("Unknown changeset ID: "+str(id))
    # print date info, which is useful for git blame; can see if upstream branched has changed since then
    print(f"Patch {p.id}:")
    print(f"\t{p.summary}")
    print(f"\t{p.date}")
    return p
patches = list(map(get_patch, sys.argv[1:]))
print()

# cleanup previous
shutil.rmtree(os.path.join(__dir__, "diff"), ignore_errors=True)
os.makedirs(os.path.join(__dir__, "diff", "patches"))

# get list of files that will be affected
# {[filename]: [diff_objects]}
files = {}
for patch in patches:
    # link patch file
    os.symlink(patch.path, os.path.join(__dir__, "diff", "patches", os.path.basename(patch.path)))
    for diff in patch.diffs:
        file = diff["path"]
        diff = diff["diff"]
        if file == "/dev/null":
            print(f"Skipping file deletion in patch {patch.id}")
            continue
        if not os.path.isfile(file):
            print("Can't find base file:", file)
        else:
            if file in files:
                files[file].append(diff)
            else:
                files[file] = [diff]

# strip prefix for our staging directory
file_list = list(files.keys())
if len(file_list) == 1:
    single = file_list[0]
    fname = os.path.basename(single)
    prefix = single[:-len(fname)]
else:
    prefix = os.path.commonprefix(file_list)
if prefix:
    print("Using common prefix:", prefix)
with open(os.path.join(__dir__, "diff", "prefix"), "w") as f:
    f.write(prefix)

# create diff directory, which holds base/staging copies; run patch
fails = 0
for file, diffs in files.items():
    stripped = file.removeprefix(prefix)
    base = os.path.join(__dir__, "diff", "base", stripped)
    patched = os.path.join(__dir__, "diff", "patched", stripped)
    # create folder structure
    os.makedirs(os.path.dirname(base), exist_ok=True)
    os.makedirs(os.path.dirname(patched), exist_ok=True)
    # copy source file to base
    os.symlink(os.path.abspath(file), base)
    # run patch
    if not apply(base, patched, diffs):
        fails += 1
print("Failures:", fails)
