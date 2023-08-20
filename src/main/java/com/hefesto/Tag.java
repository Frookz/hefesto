package com.hefesto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.String;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "tag"
)
public class Tag {
  @Column
  private UUID code;

  @Column
  private String name;

  @Column
  private String description;

  @Column
  @Id
  private UUID id;
}
