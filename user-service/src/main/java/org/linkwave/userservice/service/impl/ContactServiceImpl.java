package org.linkwave.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.linkwave.userservice.dto.ContactDto;
import org.linkwave.userservice.dto.UserDto;
import org.linkwave.userservice.entity.ContactEntity;
import org.linkwave.userservice.repository.ContactRepository;
import org.linkwave.userservice.service.ContactService;
import org.modelmapper.ModelMapper;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final ModelMapper modelMapper;

    @Override
    public Pair<Integer, List<ContactDto>> getContactsUsername(Long userId, String username, int offset, int limit) {

        log.debug("-> getContactsUsername(): username = {}, offset = {}, limit = {}", username, offset, limit);

        // convert to SQL pattern
        final var usernamePattern = username + "%";

        // find contacts by pattern
        final List<ContactEntity> contacts = contactRepository.getContactsByUsernameStartsWith(userId, usernamePattern);

        // cut up & map contacts
        final List<ContactDto> mappedContacts = contacts.stream()
                .skip(offset)
                .limit(limit)
                .map(contact -> ContactDto.builder()
                        .user(modelMapper.map(contact.getUser(), UserDto.class))
                        .addedAt(contact.getAddedAt())
                        .alias(contact.getAlias())
                        .build()
                )
                .toList();

        return Pair.of(contacts.size(), mappedContacts);
    }

}
