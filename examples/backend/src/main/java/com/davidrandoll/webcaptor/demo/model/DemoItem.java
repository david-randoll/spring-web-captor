package com.davidrandoll.webcaptor.demo.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoItem {
    private Long id;
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    private List<String> tags;
    private LocalDateTime createdAt;
}
