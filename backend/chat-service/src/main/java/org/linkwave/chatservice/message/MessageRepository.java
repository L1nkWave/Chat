package org.linkwave.chatservice.message;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String>, QuerydslPredicateExecutor<Message> {

    @Query(value = "{ 'chat.$id': { $eq: ObjectId(?0) } }", delete = true)
    void deleteAllChatMessages(String chatId);

}
