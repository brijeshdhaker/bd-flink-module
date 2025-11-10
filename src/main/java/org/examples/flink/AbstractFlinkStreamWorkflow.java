package org.examples.flink;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobSubmissionResult;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.minicluster.MiniCluster;
import org.apache.flink.runtime.minicluster.MiniClusterJobClient;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.examples.flink.config.WorkflowConfig;
import org.examples.flink.utils.Constants;

public abstract class AbstractFlinkStreamWorkflow extends Workflow {

    public AbstractFlinkStreamWorkflow(WorkflowConfig workflowConfig){
        super(workflowConfig);
    }

    private JobExecutionResult runJobInBackground(StreamExecutionEnvironment env) throws Exception {
        /* 
        AtomicReference<JobExecutionResult> results = null;
        new Thread(() -> {
            try (MiniCluster miniCluster = getMiniCluster()) {
                miniCluster.start();
                // Wait for job completion
                JobSubmissionResult jobSubmissionResult = miniCluster.submitJob(env.getStreamGraph().getJobGraph()).get(); 
                results.set(jobSubmissionResult.getJobExecutionResult());
                //results.set(miniCluster.executeJobBlocking(env.getStreamGraph().getJobGraph()));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).start();
        return results.get();
        */
        try (MiniCluster miniCluster = getMiniCluster()) {
            //
            miniCluster.start();
            
            // 2. Submit a Flink job
            JobGraph jobGraph = env.getStreamGraph().getJobGraph();
            JobExecutionResult jobExecutionResult = miniCluster.executeJobBlocking(jobGraph);
            /* 
            JobSubmissionResult jobSubmissionResult = miniCluster.submitJob(jobGraph).get();
            JobID jobID = jobSubmissionResult.getJobID();
            
            // 3. Create a MiniClusterJobClient for the job
            MiniClusterJobClient jobClient = new MiniClusterJobClient(
                jobID,
                miniCluster,
                Thread.currentThread().getContextClassLoader(),
                MiniClusterJobClient.JobFinalizationBehavior.SHUTDOWN_CLUSTER
            );

            // 4. Interact with the job
            JobExecutionResult jobExecutionResult = jobClient.getJobExecutionResult().get();
            */
            return jobExecutionResult;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }     
    }

    public JobExecutionResult startWorkflow(String workflow_name) throws Exception {

        Map<String,String> workflowConf = workflowConfig.workflowConf();

        StreamExecutionEnvironment env = createPipeline();
        
        String engine_type = workflowConf.get(Constants.ENGINE_TYPE);
        if(engine_type.equalsIgnoreCase(Constants.MINI_CLUSTER)) {
            return runJobInBackground(env);
        }else{
            return env.execute(workflow_name);
        }

    }
    public JobExecutionResult startWorkflow() throws Exception {

        Map<String,String> workflowConf = workflowConfig.workflowConf();
        String workflow_name = workflowConf.get(Constants.WORKFLOW_NAME);

        return startWorkflow(workflow_name);

    }

    public abstract StreamExecutionEnvironment createPipeline() throws Exception ;

}
