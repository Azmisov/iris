# Migrate a database through N version changes
# run as postgres user, e.g. `sudo -u postgres -s`
import os, re, subprocess
__dir__ = os.path.dirname(os.path.realpath(__file__))

# To create backup:
# > pg_dump -F c -f db_backup_v47.dump tms
db_backup = "db_backup_v47.dump"
min_version = (4,58) # exclusive
max_version = (5,43) # inclusive

print(f"Import and migrate {db_backup} from {min_version} to {max_version}")
print("WARNING! This will replace the existing tms database")
if input("Continue? (y/n) ").lower() != 'y':
	print("Will not perform migration")
	exit()

# gather list of migration scripts
migration = []
re_fname = re.compile(r"migrate-(\d)\.(\d{1,2})\.sql")
for fname in os.listdir(__dir__):
	match = re_fname.fullmatch(fname)
	if match is not None:
		ver = tuple(map(int, match.groups()))
		if ver <= min_version or ver > max_version:
			continue
		migration.append((ver, fname))
migration.sort()

def cmd(*args):
	print("running command: ", *args)
	res = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
	print(res.stdout.decode())
	if res.returncode:
		print(f"Command exited with error ({res.returncode})")
		exit(res.returncode)

print("\nDropping old tms database")
cmd("dropdb","--if-exists","tms")

# autocreates new db inside maintenance `postgres` db
print("\nImporting tms database backup")
cmd("pg_restore","-C","-d","postgres",db_backup)

print(f"\nRunning {len(migration)} migration scripts")
for ver,fname in migration:
	print(f"Running {ver} migration...")
	cmd("psql","tms","-q","-f",f"{__dir__}/{fname}")

print("Done!")