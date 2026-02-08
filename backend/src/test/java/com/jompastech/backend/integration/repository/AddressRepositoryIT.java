package com.jompastech.backend.integration.repository;

import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class AddressRepositoryIT {

    @Autowired
    AddressRepository repository;

    @Test
    void shouldFindByCity() {
        Address a = new Address();
        a.setCity("Curitiba");
        a.setState("PR");

        repository.save(a);

        List<Address> result = repository.findByCity("Curitiba");

        assertEquals(1, result.size());
    }
}
