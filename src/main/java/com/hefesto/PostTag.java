package com.hefesto;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "post_tag"
)
public class PostTag {
  @EmbeddedId
  private PostTagId id;

  @ManyToOne
  @JoinColumn(
      name = "post_id"
  )
  private Post post;

  @ManyToOne
  @JoinColumn(
      name = "tag_id"
  )
  private Tag tag;
}
