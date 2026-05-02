package com.circleguard.identity.integration;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import com.circleguard.identity.util.IdentityEncryptionConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(IdentityEncryptionConverter.class)
class IdentityMappingRepositoryIntegrationTest {

    @Autowired
    private IdentityMappingRepository repository;

    @Test
    void shouldPersistAndFindIdentityMappingByHash() {
        String hash = UUID.randomUUID().toString();
        IdentityMapping mapping = IdentityMapping.builder()
                .realIdentity("user@university.edu")
                .identityHash(hash)
                .salt("randomsalt")
                .build();

        repository.save(mapping);

        Optional<IdentityMapping> found = repository.findByIdentityHash(hash);
        assertTrue(found.isPresent());
        assertEquals("user@university.edu", found.get().getRealIdentity());
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownHash() {
        Optional<IdentityMapping> result = repository.findByIdentityHash("nonexistent-hash-xyz");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldEnforceUniqueConstraintOnIdentityHash() {
        String duplicateHash = "same-hash-value-123";
        IdentityMapping first = IdentityMapping.builder()
                .realIdentity("first@university.edu")
                .identityHash(duplicateHash)
                .salt("salt1")
                .build();
        IdentityMapping second = IdentityMapping.builder()
                .realIdentity("second@university.edu")
                .identityHash(duplicateHash)
                .salt("salt2")
                .build();

        repository.save(first);
        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.saveAndFlush(second);
        });
    }

    @Test
    void shouldFindIdentityMappingByGeneratedAnonymousId() {
        IdentityMapping mapping = IdentityMapping.builder()
                .realIdentity("lookup@university.edu")
                .identityHash("unique-hash-lookup")
                .salt("salt")
                .build();

        IdentityMapping saved = repository.save(mapping);
        UUID generatedId = saved.getAnonymousId();
        assertNotNull(generatedId);

        Optional<IdentityMapping> found = repository.findById(generatedId);
        assertTrue(found.isPresent());
        assertEquals("lookup@university.edu", found.get().getRealIdentity());
    }

    @Test
    void shouldDeleteIdentityMappingById() {
        IdentityMapping mapping = IdentityMapping.builder()
                .realIdentity("delete@university.edu")
                .identityHash("hash-to-delete")
                .salt("salt")
                .build();

        IdentityMapping saved = repository.save(mapping);
        UUID id = saved.getAnonymousId();

        repository.deleteById(id);
        assertFalse(repository.findById(id).isPresent());
    }
}
