package com.LinkedKnowledge.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
@Entity
@Table(name="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY )
    private Long id;

    @Column(nullable = false,unique = true,length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true,length =100)
    private String email;

    @Column(name="created_at",updatable = false)
    private LocalDateTime createdAt=LocalDateTime.now();
}

