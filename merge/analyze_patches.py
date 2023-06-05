""" Takes a list of mercurial patches, filters them, and analyzes them for conflicts/clusters
"""
import os, re, pprint
import whatthepatch
__dir__ = os.path.dirname(os.path.realpath(__file__))
pp = pprint.PrettyPrinter(indent=4)

# Directory of *.patch files and hg_log.txt
patch_dir = os.path.join(__dir__, "../patches_v47")
# The changesets you are interested in
changeset_range = [9784,10062]

""" Parsed patch format:
	{
		id: int, the changeset number
		hash: str, hash for changeset
		user: str, who made the change
		date: str, date of change
		summary: str, description of change
		lines: {
			[filename]: int, count of lines changed
		}
		conflicts: set([id]), other changesets that affect same fileset
	}
"""

""" Filter out patches to ignore
	:returns: (bool) true to ignore, otherwise patch will be kept
"""
def ignore_patch(patch: dict):
	# outside desired range
	if patch["id"] < changeset_range[0] or patch["id"] > changeset_range[1]:
		return True
	# ignore: keyword
	if re.match("^ignore:", patch["summary"], re.I):
		# print("Ignored:", patch["summary"])
		return True

def parse_log():
	""" Reads/Parses changesets listed in hg_log.txt and outputs JSON format.
		Filters the patches using function `ignore_patch`
		:returns: (dict)
	"""
	print("Parsing hg_log.txt...")
	patches = []
	builder = {}
	def flush():
		nonlocal builder
		if not ignore_patch(builder):
			patches.append(builder)
		builder = {}
	tokenize = re.compile("(.*?):\s*(.*)")
	with open(os.path.join(patch_dir, "hg_log.txt")) as f:
		lines = f.readlines()
		builder = {}
		for line in lines:
			m = tokenize.match(line)
			if m is None:
				flush()
				continue
			key, val = m.groups()
			if key == "changeset":
				id, hash = val.split(":")
				builder["id"] = int(id)
				builder["hash"] = hash
			else:
				builder[key] = val
	if builder:
		flush()
	return patches

def parse_patches(patches):
	""" Parse actual patch data
		:param (list): output of parse_log, an index of the patches
	"""
	print("Parsing patch data...")
	# Mapping from file->patch to check which patches might conflict
	files = {}
	# Mapping from id->patch
	patches_index = {}

	for patch in patches:
		patches_index[patch["id"]] = patch

		with open(os.path.join(patch_dir, f"{patch['id']}.patch")) as f:
			raw = f.read()
		# possible conflicts with other patches (added later)
		patch["conflicts"] = set()
		# which files were modified, and count of lines that chnaged
		lines = patch["lines"] = {}
		for diff in whatthepatch.parse_patch(raw):
			path = diff.header.new_path
			# this can occur if it is a binary file
			if not diff.changes:
				print(f"Warning, no changes for patch {patch['id']}, file {path}")
				continue
			# line change count
			count = [0,0] # [rmeove,add]
			for change in diff.changes:
				if change.new is None:
					count[0] += 1
				if change.old is None:
					count[1] += 1
			# save file info
			count = max(count)
			if path in lines:
				lines[path] += count
			else:
				lines[path] = count
				# track list of files
				if path not in files:
					files[path] = []
				files[path].append(patch["id"])

	# Mark patches that have possible conflicts; e.g. affect same file, so maybe alter the
	# same line number, or are just working on the same module
	for ids in files.values():
		# no conflicts for this file
		if len(ids) <= 1:
			continue
		for id in ids:
			c = patches_index[id]["conflicts"]
			for oid in ids:
				if oid != id:
					c.add(oid)

	# Find clusters based on possible conflicts;
	# Set of all ids that have already been clustered
	clustered = set()
	# first cluster are patches that have no conflicts
	clusters = [[]]
	for patch in patches:
		id = patch["id"]
		# already clustered
		if id in clustered:
			continue
		clustered.add(id)
		# singleton
		if not patch["conflicts"]:
			clusters[0].append(patch)
			continue
		# build cluster
		cluster = []
		clusters.append(cluster)
		# cluster in two pass manner: add `commit` to cluster, while adding any
		# unclustered children (conflicts) to `pending`; 
		commit = [patch]
		while commit:
			pending = []
			for patch in commit:
				cluster.append(patch)
				# children (conflicts)
				for cid in patch["conflicts"]:
					if cid not in clustered:
						clustered.add(cid)
						pending.append(patches_index[cid])
			# next pass
			commit = pending
	
	# sort clusters by their size, else earliest change
	print("Writing patch clusters to patch_clusters.txt")
	with open("./patch_clusters.txt","w") as f:
		singletons = clusters.pop(0)
		clusters.sort(key=lambda c: (len(c), c[0]["id"]))
		clusters.insert(0, singletons)
		for cluster in clusters:
			ids = list(map(lambda v: v["id"], cluster))
			f.write(f"Patches: {ids}\n")
			f.write(f"Count: {len(ids)}\n")
			f.write("Summary:\n")
			for patch in cluster:
				f.write(f"\t{patch['summary']}\n")

	# TODO: check for file existence
	# TODO: compute updated line numbers if file exists

if __name__ == "__main__":
	patches = parse_log()
	patches.sort(key=lambda v: v["id"])
	print("Patch count:", len(patches))
	parse_patches(patches)