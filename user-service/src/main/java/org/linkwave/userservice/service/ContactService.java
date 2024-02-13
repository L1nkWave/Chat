package org.linkwave.userservice.service;

import org.linkwave.userservice.dto.ContactDto;
import org.springframework.data.util.Pair;

import java.util.List;

public interface ContactService {

    /**
     * @param userId   of the user that initiated request
     * @param username match value
     * @param offset   how many records to skip
     * @param limit    how many record to return
     * @return pair the first value of is total count of matched contacts,
     * and the second - selected contacts with offset and limit params
     */
    Pair<Integer, List<ContactDto>> getContactsByUsername(Long userId, String username, int offset, int limit);

}
