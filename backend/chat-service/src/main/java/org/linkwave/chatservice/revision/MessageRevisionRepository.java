package org.linkwave.chatservice.revision;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRevisionRepository extends MongoRepository<MessageRevision, String> {
}