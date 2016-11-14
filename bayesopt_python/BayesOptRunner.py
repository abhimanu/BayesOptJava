#!/usr/local/bin/python

import os
import time

from Grid  import *
import ExpectedImprovement as module
from pb2   import *
import branin_objective as job_module


class ExpVar:

	def __init__(self, name, size, type, min, max):
		self.type = type
		self.name = name
		self.size = size
		self.min = min
		self.max = max
	
class Job:

	def __init__(self, job_id, expt_dir, name, language, status, submit_t, param):
		self.id        = job_id
		self.expt_dir  = expt_dir
		self.name      = name
		self.language  = language
		self.status    = 'submitted'
		self.submit_t  = int(time.time())
		self.param	   = param
		self.value	   = None	 
	def setValue(value):
		self.value = value


expt_dir  = os.getcwd()
work_dir  = os.path.realpath('.')
expt_name = os.path.basename(expt_dir)

# Load up the chooser module.
chooser = module.init(expt_dir, "noiseless=1")

max_concurrent = 1
max_finished_jobs = 150

variable = []
variable.append(ExpVar("X", 2, Experiment.ParameterSpec.FLOAT, 0, 1))

grid_size = 200
grid_seed = 1


def run_spearmint_experiment():
	# Loop until we run out of jobs.
	while True:
		runJob()
		# attempt_dispatch(expt_name, expt_dir, work_dir, chooser, options)
		time.sleep(0.1)


def runJob():
	expt_grid = ExperimentGrid(expt_dir,variable,grid_size,grid_seed)
	# Print out the current best function value.
	best_val, best_job = expt_grid.get_best()	
	if best_job >= 0:
		sys.stderr.write("Current best: %f (job %d)\n" % (best_val, best_job))
	else:
		sys.stderr.write("Current best: No results returned yet.\n")

	# Gets you everything - NaN for unknown values & durations.
	grid, values, durations = expt_grid.get_grid()
	
	# Returns lists of indices.
	candidates = expt_grid.get_candidates()
	pending	= expt_grid.get_pending()
	complete   = expt_grid.get_complete()
	sys.stderr.write("%d candidates   %d pending   %d complete\n" % 
					 (candidates.shape[0], pending.shape[0], complete.shape[0]))

	# Verify that pending jobs are actually running.
	for job_id in pending:
		sgeid = expt_grid.get_sgeid(job_id)
		reset_job = False
		
		try:
			# Send an alive signal to proc (note this could kill it in windows)
			os.kill(sgeid, 0)
		except OSError:
			# Job is no longer running but still in the candidate list. Assume it crashed out.
			expt_grid.set_candidate(job_id)

	# Track the time series of optimization.
	print("%d,%f,%d,%d,%d,%d\n"
				   % (time.time(), best_val, best_job,
					  candidates.shape[0], pending.shape[0], complete.shape[0]))

	# Print out the best job results
	print("Best result: %f\nJob-id: %d\nParameters: \n" % 
					  (best_val, best_job))	
	for best_params in expt_grid.get_params(best_job):
		print(str(best_params) + '\n')

	if complete.shape[0] >= max_finished_jobs:
		sys.stderr.write("Maximum number of finished jobs (%d) reached."
						 "Exiting\n" % max_finished_jobs)
		sys.exit(0)

	if candidates.shape[0] == 0:
		sys.stderr.write("There are no candidates left.  Exiting.\n")
		sys.exit(0)

	if pending.shape[0] >= max_concurrent:
		sys.stderr.write("Maximum number of jobs (%d) pending.\n"
						 % (max_concurrent))
		sys.exit(0)
		#return

	# Ask the chooser to actually pick one.
	job_id = chooser.next(grid, values, durations, candidates, pending,
						  complete)

	# If the job_id is a tuple, then the chooser picked a new job.
	# We have to add this to our grid
	if isinstance(job_id, tuple):
		(job_id, candidate) = job_id
		job_id = expt_grid.add_to_grid(candidate)

	sys.stderr.write("Selected job %d from the grid.\n" % (job_id))
	# Convert this back into an interpretable job and add metadata.
	job = Job(job_id, expt_dir, 'branin', 'python', 
			'submitted', int(time.time()), expt_grid.get_params(job_id))

	# Make sure we have a job subdirectory.
	job_subdir = os.path.join(expt_dir, 'jobs')
	if not os.path.exists(job_subdir):
		os.mkdir(job_subdir)

	# Name this job file.
	job_file = os.path.join(job_subdir,
							'%08d.pb' % (job_id))

	# Store the job file.
	#save_job(job_file, job)

	# Make sure there is a directory for output.
	output_subdir = os.path.join(expt_dir, 'output')
	if not os.path.exists(output_subdir):
		os.mkdir(output_subdir)
	output_file_path = os.path.join(output_subdir,
							   '%08d.out' % (job_id))

	#ExperimentGrid.job_running(job.expt_dir, job.id)
	expt_grid.set_running(job.id)
	# Write everything to job_output file
	output_file = open(output_file_path, 'w')
	
	# Update metadata.
	job.start_t = int(time.time())
	job.status  = 'running'

	start_time = time.time()

	# Convert the PB object into useful parameters.
	params = {}
	for param in job.param:
		params[param.name] = np.array(param.dbl_val)
		print "========ABHI: ", param.dbl_val
		# ini above we only assume float params

	result = job_module.main(job.id, params)
	
	output_file.write("Got result %f\n" % (result))
	
	# Change back out.
	os.chdir('..')
	
	# Store the result.
	job.value = result
	#save_job(job_file, job)

	end_time = time.time()
	duration = end_time - start_time
	##########################################################################

	#job = load_job(job_file)
	#output_file.write("Job file reloaded.\n")
	success = True
	#if not job.HasField("value"):
	if job.value is None:
		output_file.write("Could not find value in output file.\n")
		success = False

	if success:
		output_file.write("Completed successfully in %0.2f seconds. [%f]\n" 
						 % (duration, job.value))

		# Update the status for this job.
		#ExperimentGrid.job_complete(job.expt_dir, job.id,
		#							job.value, duration)
		expt_grid.set_complete(job.id, job.value, duration)
	
		# Update metadata.
		job.end_t	= int(time.time())
		job.status   = 'complete'
		job.duration = duration

	else:
		output_file.write("Job failed in %0.2f seconds.\n" % (duration))

		# Update the status for this job.
		#ExperimentGrid.job_broken(job.expt_dir, job.id)
		expt_grid.set_broken(job.id)
	
		# Update metadata.
		job.end_t	= int(time.time())
		job.status   = 'broken'
		job.duration = duration

	#save_job(job_file, job)
	output_file.close()



if __name__=="__main__":
	run_spearmint_experiment()
