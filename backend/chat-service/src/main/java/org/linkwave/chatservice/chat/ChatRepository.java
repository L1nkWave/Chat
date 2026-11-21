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
                    }
                }
            }
            """,
            // đoạn chat đã ghim của người này lên đầu (ghim gần nhất đứng trước), sau đó theo tin nhắn mới nhất
            """
            {
                $addFields: {
                    pinOrder: {
                        $max: {
                            $map: {
                                input: {
                                    $filter: {
                                        input: '$members',
                                        as: 'm',
                                        cond: {
                                            $and: [
                                                { $eq: ['$$m._id', ?0] },
                                                { $eq: ['$$m.pinned', true] }
                                            ]
                                        }
                                    }
                                },
                                as: 'p',
                                in: '$$p.pinnedAt'
                            }
                        }
                    }
                }
            }
            """,
            "{ $sort: { pinOrder: -1, 'lastMessage.createdAt': -1 } }",
            "{ $project: { pinOrder: 0 } }",
            "{ $skip : ?1 }",
            "{ $limit : ?2 }"
    })
    List<T> getUserChats(Long userId, int offset, int limit);

    /** Số đoạn chat mà người này đã ghim, đếm trên mọi đoạn chat người đó là thành viên. */
    @Query(
            value = """
                    {
                      'members': {
                        $elemMatch: {
                          _id: { $eq: ?0 },
                          pinned: { $eq: true }
                        }
                      }
                    }
                    """,
            count = true)
    long countPinnedChats(Long userId);

    /** Nhóm học vụ theo khóa đồng bộ. Dùng @Query vì khóa chỉ có ở GroupChat, không có ở Chat. */
    @Query("{ 'academicKey': { $eq: ?0 } }")
    Optional<GroupChat> findGroupChatByAcademicKey(String academicKey);

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