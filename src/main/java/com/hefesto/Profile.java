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
    name = "profile"
)
public class Profile {
  @Column
  private String country;

  @Column
  private String website;

  @Column
  private UUID code;

  @Column
  private String address;

  @Column
  private UUID userId;

  @Column
  private String city;

  @Column
  private String bio;

  @Column
  private String profilePicture;

  @Column
  private String phoneNumber;

  @Column
  @Id
  private UUID id;
}
