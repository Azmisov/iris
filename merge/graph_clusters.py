""" Graphs clusters found in `analyze_patches.py` """
import os, json
from collections import defaultdict
from pathlib import Path
from analyze_patches import Patch, __dir__
from pyvis.network import Network

# patch ids or short filenames to highlight
highlight = set([10055])

# patches/files/short files to exclude from the graph; e.g. those that have been merged already
excluded = set()
if True:
    try:
        with open(os.path.join(__dir__, "complete.json")) as f:
            for key in json.loads(f.read()):
                excluded.add(key)
    except:
        print("Couldn't load complete.json to exclude completed patches")

Patch.parse_hg_log()
clusters = Patch.cluster(None)
# exclude first, which is singletons
clusters.pop(0)

# get nodes/edges
patches = []
files = defaultdict(list) # file -> [patch]
for cluster in clusters:
    for patch in cluster:
        if patch.id in excluded:
            continue
        patches.append(patch)
        for file in patch.lines:
            if file == "/dev/null" or file in excluded:
                continue
            files[file].append(patch)

# build network
net = Network(height="1000", width="100%")
# speeds up first load, but at expense of quality
net.set_options("""
{
  "physics": {
        "stabilization": {
            "enabled": true,
            "fit": true,
            "iterations": 500,
            "onlyDynamicEdges": false,
            "updateInterval": 1
        }
    }
}
""")

# file nodes
# shorten filenames for display; build list of conflicts for trimmed versions of files
files_trimmed = defaultdict(list) # file -> [trimmed]
files_trimmed_conflicts = defaultdict(int) # trimmed -> file count
for file in files:
    parts = Path(file).parts
    for i in range(1, len(parts)+1):
        trimmed = os.path.join(*parts[-i:])
        files_trimmed[file].append(trimmed)
        files_trimmed_conflicts[trimmed] += 1
for file in files:
    # find short name
    label = None
    for trimmed in files_trimmed[file]:
        label = trimmed
        # no conflicts, can use this shortened version
        if files_trimmed_conflicts[trimmed] == 1:
            break
    if label in excluded:
        excluded.add(file)
        continue
    file_patches = files[file]
    links = sorted(map(lambda p: p.id, file_patches))
    links_str = ", ".join(map(lambda p: str(p), links))
    net.add_node(
        file,
        shape=("star" if label in highlight else "dot"),
        label=label,
        title="\n".join([file, "Affected by: "+links_str]),
        group="files",
        level=1
    )

# patch nodes
for patch in patches:
    net.add_node(
        patch.id,
        shape=("star" if patch.id in highlight else "dot"),
        label=str(patch.id),
        title="\n".join([patch.date, patch.user, patch.summary]),
        group="patches",
        level=2
    )
    # patch->file edges, weighted by line count
    for file,lines in patch.lines.items():
        if file == "/dev/null" or file in excluded:
            continue
        net.add_edge(patch.id, file, value=lines)

os.chdir(__dir__)
net.show("./graph.html", notebook=False)
