package com.springbatch.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
@Entity
@Table(name = "app_user")
@Data
public class User {
    @Id
    private Long id;
    private String name;
    private String email;
}
