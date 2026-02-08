package com.jompastech.backend.unit.service;

import com.jompastech.backend.model.dto.AddressRequestDTO;
import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.repository.AddressRepository;
import com.jompastech.backend.service.AddressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AddressService class.
 * This test class verifies all CRUD operations and business methods
 * of the AddressService using Mockito for mocking dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    /**
     * Creates a sample Address object for testing purposes.
     * @return A pre-configured Address instance with test data
     */
    private Address createSampleAddress() {
        Address address = new Address();
        address.setId(1L);
        address.setCep("12345-678");
        address.setNumber("123");
        address.setStreet("Main Street");
        address.setNeighborhood("Downtown");
        address.setCity("São Paulo");
        address.setState("SP");
        return address;
    }

    /**
     * Creates a sample AddressRequestDTO object for testing purposes.
     * @return A pre-configured AddressRequestDTO instance with test data
     */
    private AddressRequestDTO createSampleAddressRequestDTO() {
        AddressRequestDTO dto = new AddressRequestDTO();
        dto.setCep("87654-321");
        dto.setNumber("456");
        dto.setStreet("Updated Street");
        dto.setNeighborhood("Uptown");
        dto.setCity("Rio de Janeiro");
        dto.setState("RJ");
        return dto;
    }

    // =========================================
    // Tests for Save Operation
    // =========================================

    /**
     * Tests that a valid address can be successfully saved.
     * Verifies that the repository's save method is called and returns the expected address.
     */
    @Test
    void save_WithValidAddress_ShouldReturnSavedAddress() {
        // Arrange
        Address address = createSampleAddress();
        when(addressRepository.save(any(Address.class))).thenReturn(address);

        // Act
        Address savedAddress = addressService.save(address);

        // Assert
        assertNotNull(savedAddress);
        assertEquals(1L, savedAddress.getId());
        assertEquals("12345-678", savedAddress.getCep());
        assertEquals("123", savedAddress.getNumber());
        assertEquals("Main Street", savedAddress.getStreet());
        assertEquals("Downtown", savedAddress.getNeighborhood());
        assertEquals("São Paulo", savedAddress.getCity());
        assertEquals("SP", savedAddress.getState());
        verify(addressRepository, times(1)).save(address);
    }

    // =========================================
    // Tests for FindById Operation
    // =========================================

    /**
     * Tests that an existing address can be found by its ID.
     * Verifies the correct address is returned when a valid ID is provided.
     */
    @Test
    void findById_WithExistingId_ShouldReturnAddress() {
        // Arrange
        Address address = createSampleAddress();
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));

        // Act
        Address foundAddress = addressService.findById(1L);

        // Assert
        assertNotNull(foundAddress);
        assertEquals(1L, foundAddress.getId());
        assertEquals("São Paulo", foundAddress.getCity());
        verify(addressRepository, times(1)).findById(1L);
    }

    /**
     * Tests that searching for a non-existing address ID throws a RuntimeException.
     * Verifies the appropriate error message is provided.
     */
    @Test
    void findById_WithNonExistingId_ShouldThrowRuntimeException() {
        // Arrange
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            addressService.findById(999L);
        });

        assertEquals("Address not found!", exception.getMessage());
        verify(addressRepository, times(1)).findById(999L);
    }

    // =========================================
    // Tests for FindAll Operation
    // =========================================

    /**
     * Tests that all addresses are returned when addresses exist in the database.
     * Verifies the repository's findAll method is called and returns the expected list.
     */
    @Test
    void findAll_WhenAddressesExist_ShouldReturnAllAddresses() {
        // Arrange
        List<Address> addresses = Arrays.asList(createSampleAddress(), createSampleAddress());
        when(addressRepository.findAll()).thenReturn(addresses);

        // Act
        List<Address> result = addressService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(addressRepository, times(1)).findAll();
    }

    /**
     * Tests that an empty list is returned when no addresses exist in the database.
     */
    @Test
    void findAll_WhenNoAddresses_ShouldReturnEmptyList() {
        // Arrange
        when(addressRepository.findAll()).thenReturn(List.of());

        // Act
        List<Address> result = addressService.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(addressRepository, times(1)).findAll();
    }

    // =========================================
    // Tests for FindByCity Operation
    // =========================================

    /**
     * Tests that addresses can be found by city name when matching addresses exist.
     * Verifies the repository's findByCity method is called with the correct parameter.
     */
    @Test
    void findByCity_WithExistingCity_ShouldReturnAddresses() {
        // Arrange
        String city = "São Paulo";
        List<Address> addresses = Arrays.asList(createSampleAddress(), createSampleAddress());
        when(addressRepository.findByCity(city)).thenReturn(addresses);

        // Act
        List<Address> result = addressService.findByCity(city);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(addressRepository, times(1)).findByCity(city);
    }

    /**
     * Tests that an empty list is returned when searching for a non-existing city.
     */
    @Test
    void findByCity_WithNonExistingCity_ShouldReturnEmptyList() {
        // Arrange
        String city = "NonExistingCity";
        when(addressRepository.findByCity(city)).thenReturn(List.of());

        // Act
        List<Address> result = addressService.findByCity(city);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(addressRepository, times(1)).findByCity(city);
    }

    /**
     * Tests that providing a null city name throws a NullPointerException.
     */
    @Test
    void findByCity_WithNullCity_ShouldThrowEmptyList() {

        // Arrange
        when(addressRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Address> result = addressService.findAll();

        // Assert
        assertTrue(result.isEmpty());

    // =========================================
    // Tests for FindByState Operation
    // =========================================
        }
    /**
     * Tests that addresses can be found by state when matching addresses exist.
     * Verifies the repository's findByState method is called with the correct parameter.
     */
    @Test
    void findByState_WithExistingState_ShouldReturnAddresses() {
        // Arrange
        String state = "SP";
        List<Address> addresses = Arrays.asList(createSampleAddress(), createSampleAddress());
        when(addressRepository.findByState(state)).thenReturn(addresses);

        // Act
        List<Address> result = addressService.findByState(state);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(addressRepository, times(1)).findByState(state);
    }

    /**
     * Tests that an empty list is returned when searching for a non-existing state.
     */
    @Test
    void findByState_WithNonExistingState_ShouldReturnEmptyList() {
        // Arrange
        String state = "XX";
        when(addressRepository.findByState(state)).thenReturn(List.of());

        // Act
        List<Address> result = addressService.findByState(state);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(addressRepository, times(1)).findByState(state);
    }

    // =========================================
    // Tests for Update Operation with DTO
    // =========================================

    /**
     * Tests that an address can be successfully updated using a DTO.
     * Verifies that all fields are updated correctly and the repository methods are called.
     */
    @Test
    void update_WithValidIdAndDTO_ShouldUpdateAndReturnAddress() {
        // Arrange
        Long id = 1L;
        Address existingAddress = createSampleAddress();
        AddressRequestDTO dto = createSampleAddressRequestDTO();

        when(addressRepository.findById(id)).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenReturn(existingAddress);

        // Act
        Address updatedAddress = addressService.update(id, dto);

        // Assert
        assertNotNull(updatedAddress);
        assertEquals("87654-321", updatedAddress.getCep());
        assertEquals("456", updatedAddress.getNumber());
        assertEquals("Updated Street", updatedAddress.getStreet());
        assertEquals("Uptown", updatedAddress.getNeighborhood());
        assertEquals("Rio de Janeiro", updatedAddress.getCity());
        assertEquals("RJ", updatedAddress.getState());

        verify(addressRepository, times(1)).findById(id);
        verify(addressRepository, times(1)).save(existingAddress);
    }

    /**
     * Tests that updating a non-existing address throws a RuntimeException.
     * Verifies the appropriate error message and that save is never called.
     */
    @Test
    void update_WithNonExistingId_ShouldThrowRuntimeException() {
        // Arrange
        Long id = 999L;
        AddressRequestDTO dto = createSampleAddressRequestDTO();
        when(addressRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            addressService.update(id, dto);
        });

        assertEquals("Address not found!", exception.getMessage());
        verify(addressRepository, times(1)).findById(id);
        verify(addressRepository, never()).save(any(Address.class));
    }

    // =========================================
    // Tests for Update Operation with Entity
    // =========================================

    /**
     * Tests that an address entity can be successfully updated.
     * Verifies the repository's save method is called with the correct entity.
     */
    @Test
    void update_WithValidAddress_ShouldReturnUpdatedAddress() {
        // Arrange
        Address address = createSampleAddress();
        when(addressRepository.save(address)).thenReturn(address);

        // Act
        Address updatedAddress = addressService.update(address);

        // Assert
        assertNotNull(updatedAddress);
        verify(addressRepository, times(1)).save(address);
    }

    // =========================================
    // Tests for Delete Operation
    // =========================================

    /**
     * Tests that an existing address can be successfully deleted by ID.
     * Verifies the repository's existsById and deleteById methods are called.
     */
    @Test
    void deleteById_WithExistingId_ShouldDeleteSuccessfully() {
        // Arrange
        Long id = 1L;
        when(addressRepository.existsById(id)).thenReturn(true);
        doNothing().when(addressRepository).deleteById(id);

        // Act
        addressService.deleteById(id);

        // Assert
        verify(addressRepository, times(1)).existsById(id);
        verify(addressRepository, times(1)).deleteById(id);
    }

    /**
     * Tests that deleting a non-existing address throws a RuntimeException.
     * Verifies the appropriate error message and that deleteById is never called.
     */
    @Test
    void deleteById_WithNonExistingId_ShouldThrowRuntimeException() {
        // Arrange
        Long id = 999L;
        when(addressRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            addressService.deleteById(id);
        });

        assertEquals("Address not found with ID: " + id, exception.getMessage());
        verify(addressRepository, times(1)).existsById(id);
        verify(addressRepository, never()).deleteById(id);
    }

    // =========================================
    // Integration Tests
    // =========================================

    /**
     * Integration test that verifies the save and findById operations work together.
     * Tests that an address saved can be subsequently retrieved by its ID.
     */
    @Test
    void whenSaveAddressThenShouldBeAbleToFindItById() {
        // Arrange
        Address address = createSampleAddress();
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(address));

        // Act
        Address savedAddress = addressService.save(address);
        Address foundAddress = addressService.findById(1L);

        // Assert
        assertNotNull(savedAddress);
        assertNotNull(foundAddress);
        assertEquals(savedAddress.getId(), foundAddress.getId());
        assertEquals(savedAddress.getCep(), foundAddress.getCep());
    }
}