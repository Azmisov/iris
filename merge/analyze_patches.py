""" Takes a list of unified patches, filters them, and analyzes them for conflicts/clusters """
import os, re, pprint
import whatthepatch
__dir__ = os.path.dirname(os.path.realpath(__file__))
pp = pprint.PrettyPrinter(indent=4)

# Directory of *.patch files and hg_log.txt; relative to current file
patch_dir = os.path.join(__dir__, "../patches_v47")
# The changesets you are interested in
changeset_range = [9784,10062]

class Patch:
	patch_index = {}
	""" Mapping from id to Patch; populated by `read_patch` """
	file_index = {}
	""" Mapping from modified file to Patch.id; populuated by `read_patch` """

	@staticmethod
	def get(id: int):
		""" Get Patch by id """
		return Patch.patch_index[id]
	@staticmethod
	def add(patch: "Patch"):
		""" Add the Patch to the index """
		Patch.patch_index[patch.id] = patch
	
	@staticmethod
	def parse_hg_log():
		""" Populates the index of Patch objects from hg_log.txt
			:returns: (list[Patch]) patches that were added
		"""
		print("Parsing hg_log.txt...")
		patches = []
		builder = {}
		def flush():
			nonlocal builder
			p = Patch(builder)
			if not p.should_ignore():
				p.read_patch()
				patches.append(p)
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

		Patch.find_conflicts()
		return patches
	
	def __init__(self, attrs):
		""" Construct a new Patch
			:param attrs (dict): metadata extracted from hg_log.txt
		"""
		self.id = attrs["id"]
		self.hash = attrs["hash"]
		self.date = attrs["date"]
		self.user = attrs["user"]
		self.summary = attrs["summary"]
		self.lines = {}
		""" mapping from filename to count of lines changed; populuated by `read_patch` """
		self.diffs = []
		""" diffs extracted from patch file; populuated by `read_patch` """
		self.conflicts = set()
		""" other changesets that affect same fileset """
	
	def should_ignore(self):
		""" Filter out patches to ignore
			:returns: (bool) true to ignore, otherwise patch will be kept
		"""
		# outside desired range
		if changeset_range[0] <= self.id <= changeset_range[1]:
			# ignore: keyword
			if not re.match("^ignore:", self.summary, re.I):
				return False
			# print("Ignored:", self.summary)
		return True
	
	def read_patch(self):
		""" Read/parse patch file, and add this Patch to the index """
		Patch.add(self)

		with open(os.path.join(patch_dir, f"{self.id}.patch")) as f:
			raw = f.read()
		# which files were modified, and count of lines that chnaged
		diffs = list(whatthepatch.parse_patch(raw))
		# calculate line changes
		for diff in diffs:
			path = diff.header.new_path
			if path.startswith('b/'):
				path = path[2:]
			# this can occur if it is a binary file
			if not diff.changes:
				print(f"Warning, no changes given for patch {self.id}, file {path}")
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
			if path in self.lines:
				self.lines[path] += count
			else:
				self.lines[path] = count
				# track list of files
				if path not in Patch.file_index:
					Patch.file_index[path] = []
				Patch.file_index[path].append(self.id)
			
			self.diffs.append({
				"path": path,
				"diff": diff
			})

	@staticmethod
	def find_conflicts():
		""" Populate Patch.conflicts lists, which are cases where two patches affect the same file """
		# Mark patches that have possible conflicts; e.g. affect same file, so maybe alter the
		# same line number, or are just working on the same module
		print("Building list of conflicts...")
		for ids in Patch.file_index.values():
			# no conflicts for this file
			if len(ids) <= 1:
				continue
			for id in ids:
				c = Patch.get(id).conflicts
				for oid in ids:
					if oid != id:
						c.add(oid)
	
	@staticmethod
	def cluster(ofile="patch_clusters.txt"):
		""" Find clusters of Patches based on possible conflicts
			:param ofile (str): if not None, filepath to write results to; if filepath
				is not absolute or prefixed by ., then it is written to this python files directory
		"""
		# Set of all ids that have already been clustered
		clustered = set()
		# first cluster are patches that have no conflicts
		clusters = [[]]

		for patch in Patch.patch_index.values():
			# already clustered
			if patch.id in clustered:
				continue
			clustered.add(patch.id)
			# singleton
			if not patch.conflicts:
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
					for cid in patch.conflicts:
						if cid not in clustered:
							clustered.add(cid)
							pending.append(Patch.get(cid))
				# next pass
				commit = pending
		
		# sort patches in each cluster by id
		for cluster in clusters:
			cluster.sort(key=lambda p: p.id)

		# sort clusters by their size, else earliest change
		singletons = clusters.pop(0)
		clusters.sort(key=lambda c: (len(c), c[0].id))
		clusters.insert(0, singletons)
		
		if ofile:
			if not (os.path.isabs(ofile) or ofile[0] == '.'):
				ofile = os.path.join(__dir__, ofile)
			print(f"Writing patch clusters to {ofile}")
			with open(ofile,"w") as f:
				for cluster in clusters:
					ids = list(map(lambda v: v.id, cluster))
					f.write(f"Patches: {ids}\n")
					f.write(f"Count: {len(ids)}\n")
					f.write("Summary:\n")
					for patch in cluster:
						f.write(f"\t{patch.summary}\n")

		return clusters

# TODO: check for file existence

if __name__ == "__main__":
	Patch.parse_hg_log()
	Patch.cluster()