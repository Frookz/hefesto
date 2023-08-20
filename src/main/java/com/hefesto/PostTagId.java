package com.hefesto;

import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class PostTagId implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private UUID postId;

  private UUID tagId;
}
