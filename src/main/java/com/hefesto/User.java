package com.hefesto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.String;
import java.sql.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "user"
)
public class User {
  @Column
  private String password;

  @Column
  private UUID code;

  @Column
  private Date dateOfBirth;

  @Column
  private String lastName;

  @Column
  @Id
  private UUID id;

  @Column
  private String firstName;

  @Column
  private String email;

  @Column
  private String username;

  @OneToMany(
      mappedBy = "user"
  )
  private List<Post> posts;
}
