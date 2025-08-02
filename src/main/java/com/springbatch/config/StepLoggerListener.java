package com.springbatch.config;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class StepLoggerListener implements StepExecutionListener {
    @Override
    public void beforeStep(StepExecution stepExecution) {
        System.out.println("🔄 Step STARTED: " + stepExecution.getStepName() +
                " at " + stepExecution.getStartTime());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        System.out.println("✅ Step COMPLETED: " + stepExecution.getStepName() +
                " at " + stepExecution.getEndTime() +
                " | Status: " + stepExecution.getExitStatus());
        return stepExecution.getExitStatus();
    }
}
