package com.springbatch.config;


import com.springbatch.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private StepLoggerListener stepLoggerListener;  // Listener injection
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private DataSource dataSource;
    // 1. READ DATA FROM DATABASE
    @Bean
    public JdbcCursorItemReader<User> reader() {
        return new JdbcCursorItemReaderBuilder<User>()
                .dataSource(dataSource)
                .name("userReader")
                .sql("SELECT id, name, email FROM app_user")
                .rowMapper(new BeanPropertyRowMapper<>(User.class))
                .build();
    }

  /*  // 2. WRITE DATA TO EXCLEL SHEET
    @Bean
    public ItemWriter<User> writer() {
        return users -> {
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("Users");
                Row hdr = sheet.createRow(0);
                hdr.createCell(0).setCellValue("ID");
                hdr.createCell(1).setCellValue("Name");
                hdr.createCell(2).setCellValue("Email");

                int rowNum = 1;
                for (User u : users) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(u.getId());
                    row.createCell(1).setCellValue(u.getName());
                    row.createCell(2).setCellValue(u.getEmail());
                }

                try (FileOutputStream fos = new FileOutputStream("C:\\git\\users.xlsx")) {
                    wb.write(fos);
                }
            }
        };
    }*/
    //2. WRITE DATA TO CSV
    @Bean
    public FlatFileItemWriter<User> writer() {
        FlatFileItemWriter<User> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("C:\\git\\users.txt"));
        writer.setEncoding("UTF-8");
        writer.setAppendAllowed(Boolean.FALSE);
        writer.setLineAggregator(new DelimitedLineAggregator<User>() {{
            setDelimiter(",");
            setFieldExtractor(new BeanWrapperFieldExtractor<User>() {{
                setNames(new String[] { "id", "name", "email" });
            }});
        }});

        return writer;
    }

    // 3. READ DATA AND WRITE DATA TO SHEET
    @Bean
    public Step exportUsersToExcelStep() {
        return new StepBuilder("exportUsersToExcelStep", jobRepository)
                .<User, User>chunk(5, transactionManager)
                .reader(reader())   // Read from database
                .writer(writer())   // Write to Excel file
                .listener(stepLoggerListener)
                .build();
    }

    @Bean
    public Job exportUsersToExcelJob() {
        return new JobBuilder("exportUsersToExcelJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(exportUsersToExcelStep())
                .build();
    }

    // 1. CSV Reader
    @Bean
    public FlatFileItemReader<User> csvReader() {
        return new FlatFileItemReaderBuilder<User>()
                .name("userCsvReader")
                .resource(new ClassPathResource("users.csv"))
                .delimited()
                .names("id", "name", "email")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(User.class);
                }})
                .linesToSkip(1) // Skip header
                .build();

    }
    // 2. JDBC Writer
    @Bean
    public JdbcBatchItemWriter<User> jdbcWriter() {
        return new JdbcBatchItemWriterBuilder<User>()
                .dataSource(dataSource)
                .sql("INSERT INTO app_user (id, name, email) VALUES (:id, :name, :email)")
                .beanMapped()
                .build();
    }
    // 3. Step
    @Bean
    public Step importsUsersCSVToDatabase() {
        return new StepBuilder("importsUsersCSVToDatabase", jobRepository)
                .<User, User>chunk(5, transactionManager)
                .reader(csvReader())
                .writer(jdbcWriter())
                .listener(stepLoggerListener)
                .build();
    }

    // 4. Job
    @Bean
    public Job importUserJob() {
        return new JobBuilder("importUserJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importsUsersCSVToDatabase())
                .build();
    }

    @Bean
    public Job fullJob(JobCompletationNotificationListener listener) {
        return new JobBuilder("fullJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener) // ✅ Listener added
                .start(importsUsersCSVToDatabase())        // Step 1: CSV to DB
                .next(exportUsersToExcelStep())

                // Step 2: DB to Excel
                .build();
    }
}
