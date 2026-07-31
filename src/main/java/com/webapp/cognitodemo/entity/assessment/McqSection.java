package com.webapp.cognitodemo.entity.assessment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mcq_sections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoryId;

    @Column(nullable = false)
    private String title;

    @Column(name = "section_order")
    private int sectionOrder;
}
