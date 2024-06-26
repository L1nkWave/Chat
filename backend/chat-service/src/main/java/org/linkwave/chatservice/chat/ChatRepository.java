package org.linkwave.chatservice.chat;

import org.linkwave.chatservice.chat.duo.Chat;
import org.linkwave.chatservice.chat.group.GroupChat;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository<T extends Chat> extends MongoRepository<T, String> {

    @Query(value = """
            {
                '_id': {
                    $eq: ObjectId(?0)
                },
                '_class': {
                    $eq: 'org.linkwave.chatservice.chat.duo.Chat'
                }
            }
            """)
    Optional<Chat> findDuoChat(String id);

    @Query(value = """
            {
                'members._id': {
                    $all: [?0, ?1]
                },
                '_class': {
                    $eq: 'org.linkwave.chatservice.chat.duo.Chat'
                }
            }
            """)
    Optional<Chat> findChatWithPair(Long member1, Long member2);

    @Aggregation(pipeline = {
            """
            {
                $match: {
                    members: {
                        $elemMatch: {
                            _id: {
                                $eq: ?0
                            }
                        }
                    },
                    $or: [
                      {
                        '_class': {
                          $eq: 'org.linkwave.chatservice.chat.group.GroupChat'
                        }
                      },
                      {
                        lastMessage: {
                          $exists: true
                        }
                      }
                    ]
                }
            }
            """,
            "{ $sort: { 'lastMessage.createdAt': -1 } }",
            "{ $skip : ?1 }",
            "{ $limit : ?2 }"
    })
    List<T> getUserChats(Long userId, int offset, int limit);

    @Query(
            value = """
                    {
                      'members': {
                        $elemMatch: {
                          _id: {
                            $eq: ?0
                          }
                        }
                      },
                      $or: [
                          {
                            '_class': {
                              $eq: 'org.linkwave.chatservice.chat.group.GroupChat'
                            }
                          },
                          {
                            lastMessage: {
                              $exists: true
                            }
                          }
                      ]
                    }
                    """,
            count = true)
    long getUserChatsTotalCount(Long userId);

    Optional<GroupChat> findGroupChatById(String id);

    @Query(
            value = """
                    {
                      'members': {
                        $elemMatch: {
                          _id: {
                            $eq: ?0
                          }
                        }
                      }
                    }
                    """,
            fields = "{ '_id': 1 }")
    List<Chat> getUserChatsIds(Long userId);

}
