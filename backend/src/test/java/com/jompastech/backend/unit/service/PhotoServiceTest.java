package com.jompastech.backend.unit.service;

import com.jompastech.backend.model.dto.cloudinary.BoatPhotoResponseDTO;
import com.jompastech.backend.model.dto.cloudinary.CloudinaryUploadResult;
import com.jompastech.backend.model.dto.cloudinary.PhotoOrderUpdateDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatPhoto;
import com.jompastech.backend.repository.BoatPhotoRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.service.CloudinaryService;
import com.jompastech.backend.service.PhotoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    BoatRepository boatRepository;

    @Mock
    BoatPhotoRepository boatPhotoRepository;

    @Mock
    CloudinaryService cloudinaryService;

    @InjectMocks
    PhotoService photoService;

    Boat boat;

    @BeforeEach
    void setup() {
        boat = new Boat();
        boat.setId(1L);
    }

    @Test
    void addPhotosToBoat_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "x".getBytes());

        when(boatRepository.findById(1L))
                .thenReturn(Optional.of(boat));
        when(boatPhotoRepository.countByBoatId(1L))
                .thenReturn(0L);

        CloudinaryUploadResult uploadResult = mock(CloudinaryUploadResult.class);
        when(uploadResult.getUrl()).thenReturn("url");
        when(uploadResult.getPublicId()).thenReturn("pid");
        when(uploadResult.getFileName()).thenReturn("photo.png");

        when(cloudinaryService.uploadImages(any()))
                .thenReturn(List.of(uploadResult));

        List<BoatPhotoResponseDTO> result =
                photoService.addPhotosToBoat(1L, List.of(file));

        assertEquals(1, result.size());
        verify(boatPhotoRepository).saveAll(any());
    }

    @Test
    void addPhotosToBoat_boatNotFound() {
        when(boatRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> photoService.addPhotosToBoat(1L, List.of()));
    }

    @Test
    void addPhotosToBoat_photoLimitExceeded() {
        when(boatRepository.findById(1L))
                .thenReturn(Optional.of(boat));
        when(boatPhotoRepository.countByBoatId(1L))
                .thenReturn(20L);

        assertThrows(IllegalArgumentException.class,
                () -> photoService.addPhotosToBoat(1L, List.of(mock(MockMultipartFile.class))));
    }

    @Test
    void addPhotosToBoat_rollbackOnFailure() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "x".getBytes());

        when(boatRepository.findById(1L))
                .thenReturn(Optional.of(boat));
        when(boatPhotoRepository.countByBoatId(1L))
                .thenReturn(0L);

        CloudinaryUploadResult uploadResult = mock(CloudinaryUploadResult.class);
        when(uploadResult.getPublicId()).thenReturn("pid");

        when(cloudinaryService.uploadImages(any()))
                .thenReturn(List.of(uploadResult));

        doThrow(new RuntimeException("DB fail"))
                .when(boatPhotoRepository).saveAll(any());

        assertThrows(RuntimeException.class,
                () -> photoService.addPhotosToBoat(1L, List.of(file)));

        verify(cloudinaryService).deleteImages(List.of("pid"));
    }

    @Test
    void deletePhoto_success() throws IOException {
        BoatPhoto photo = new BoatPhoto();
        photo.setId(10L);
        photo.setBoat(boat);
        photo.setPublicId("pid");
        photo.setOrdem(0);

        when(boatPhotoRepository.findById(10L))
                .thenReturn(Optional.of(photo));
        when(boatPhotoRepository.findByBoatIdOrderByOrdemAsc(1L))
                .thenReturn(List.of(photo));

        photoService.deletePhoto(1L, 10L);

        verify(cloudinaryService).deleteImage("pid");
        verify(boatPhotoRepository).delete(photo);
    }

    @Test
    void deletePhoto_wrongBoat() {
        Boat otherBoat = new Boat();
        otherBoat.setId(2L);

        BoatPhoto photo = new BoatPhoto();
        photo.setBoat(otherBoat);

        when(boatPhotoRepository.findById(10L))
                .thenReturn(Optional.of(photo));

        assertThrows(IllegalArgumentException.class,
                () -> photoService.deletePhoto(1L, 10L));
    }

    @Test
    void updatePhotoOrder_success() {
        BoatPhoto p1 = new BoatPhoto();
        p1.setId(1L);
        p1.setOrdem(0);

        BoatPhoto p2 = new BoatPhoto();
        p2.setId(2L);
        p2.setOrdem(1);

        when(boatPhotoRepository.findByBoatIdOrderByOrdemAsc(1L))
                .thenReturn(List.of(p1, p2));

        PhotoOrderUpdateDTO dto = new PhotoOrderUpdateDTO(
                List.of(2L, 1L)
        );

        photoService.updatePhotoOrder(1L, dto);

        assertEquals(1, p1.getOrdem());
        assertEquals(0, p2.getOrdem());
        verify(boatPhotoRepository).saveAll(any());
    }

    @Test
    void updatePhotoOrder_invalidPhoto() {
        when(boatPhotoRepository.findByBoatIdOrderByOrdemAsc(1L))
                .thenReturn(List.of());

        PhotoOrderUpdateDTO dto = new PhotoOrderUpdateDTO(List.of(99L));

        assertThrows(IllegalArgumentException.class,
                () -> photoService.updatePhotoOrder(1L, dto));
    }


    @Test
    void getBoatPhotos_success() {
        BoatPhoto photo = new BoatPhoto();
        photo.setId(1L);
        photo.setBoat(boat);

        when(boatPhotoRepository.findByBoatIdOrderByOrdemAsc(1L))
                .thenReturn(List.of(photo));

        List<BoatPhotoResponseDTO> result =
                photoService.getBoatPhotos(1L);

        assertEquals(1, result.size());
    }

    @Test
    void deleteAllBoatPhotos_success() throws IOException {
        BoatPhoto photo = new BoatPhoto();
        photo.setPublicId("pid");

        when(boatPhotoRepository.findByBoatIdOrderByOrdemAsc(1L))
                .thenReturn(List.of(photo));

        photoService.deleteAllBoatPhotos(1L);

        verify(cloudinaryService).deleteImages(List.of("pid"));
        verify(boatPhotoRepository).deleteByBoatId(1L);
    }
}


