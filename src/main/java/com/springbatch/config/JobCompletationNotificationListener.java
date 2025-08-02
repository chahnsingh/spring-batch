package com.springbatch.config;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class JobCompletationNotificationListener extends JobExecutionListenerSupport {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.println("!! Job started!  Time to verify the results");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            System.out.println("!! Job Finished! Sending email...");
            // emailService.send("Batch completed successfully");
        } else {
            System.out.println("!! Job failed. Please check logs.");
        }
    }
}
