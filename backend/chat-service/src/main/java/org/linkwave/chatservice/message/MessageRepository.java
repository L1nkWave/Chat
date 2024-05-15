package org.linkwave.chatservice.message;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String>, QuerydslPredicateExecutor<Message> {

    @Query(value = "{ 'chat.$id': { $eq: ObjectId(?0) } }", delete = true)
    void deleteAllChatMessages(String chatId);

    @Aggregation(
            pipeline = {"""      
                    {
                        $match: {
                            'chat.$id': { $eq: ObjectId(?0) }
                        }
                    }
                    """,
                    "{ $sort: { createdAt: -1 } }",
                    "{ $skip : ?1 }",
                    "{ $limit : ?2 }"
            }
    )
    List<Message> getMessages(String chatId, int offset, int limit);

    @Query(value = "{ 'chat.$id': { $eq: ObjectId(?0) } }", count = true)
    long getChatMessagesCount(String chatId);

}
