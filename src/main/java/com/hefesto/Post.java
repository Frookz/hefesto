package com.hefesto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.lang.String;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "post"
)
public class Post {
  @Column
  private UUID code;

  @ManyToOne
  @JoinColumn(
      name = "user_id",
      referencedColumnName = "id"
  )
  private User user;

  @Column
  @Id
  private UUID id;

  @Column
  private String title;

  @Column
  private String content;
}
