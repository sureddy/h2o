import os, json, unittest, time, shutil, sys
sys.path.extend(['.','..','py'])

import h2o, h2o_cmd, h2o_hosts
import argparse

class Basic(unittest.TestCase):
    def tearDown(self):
        h2o.check_sandbox_for_errors()

    @classmethod
    def setUpClass(cls):
        localhost = h2o.decide_if_localhost()
        if (localhost):
            h2o.build_cloud(2)
        else:
            h2o_hosts.build_cloud_with_hosts()

    @classmethod
    def tearDownClass(cls):
        h2o.tear_down_cloud()

    def test_rf_model_key_unique(self):
        SYNDATASETS_DIR = h2o.make_syn_dir()
        modelKeyDict = {}
        for trial in xrange (1,5,1):
            csvPathname = h2o.find_file('smalldata/iris/iris2.csv')
            # key = h2o.nodes[0].put_file(csvPathname)
            # parseKey = h2o.nodes[0].parse(key, key + "_" + str(trial) + ".hex")

            h2o.verboseprint("Trial", trial)
            start = time.time()
            # rfview=False used to inhibit the rfview completion
            rfResult = h2o_cmd.runRF(trees=6, timeoutSecs=10, rfview=False, csvPathname=csvPathname)
            print "RF #%d" % trial,  "started on ", csvPathname, 'took', time.time() - start, 'seconds'
            model_key = rfResult['model_key']
            print "model_key:", model_key
            if model_key in modelKeyDict:
                raise Exception("same model_key used in RF #%d that matches prior RF #%d" % (trial, modelKeyDict[model_key]))
            modelKeyDict[model_key] = trial

        # just show the jobs still going, if any. maybe none, because short (iris)
        a = h2o.nodes[0].jobs_admin()
        print "jobs_admin():", h2o.dump_json(a)


if __name__ == '__main__':
    h2o.unit_main()
