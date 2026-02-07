package com.jompastech.backend.Unit.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.jompastech.backend.config.CloudinaryConfig;
import com.jompastech.backend.model.dto.cloudinary.CloudinaryUploadResult;
import com.jompastech.backend.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    private CloudinaryService service;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @BeforeEach
    void setup() throws Exception {
        CloudinaryConfig config = mock(CloudinaryConfig.class);
        when(config.getCloudName()).thenReturn("test");
        when(config.getApiKey()).thenReturn("key");
        when(config.getApiSecret()).thenReturn("secret");
        when(config.isSecure()).thenReturn(true);

        service = new CloudinaryService(config);

        // creates mocks
        Cloudinary cloudinaryMock = mock(Cloudinary.class);
        Uploader uploaderMock = mock(Uploader.class);

        lenient().when(cloudinaryMock.uploader()).thenReturn(uploaderMock);

        // Injects the mocked Cloudinary on service
        Field cloudinaryField =
                CloudinaryService.class.getDeclaredField("cloudinary");
        cloudinaryField.setAccessible(true);
        cloudinaryField.set(service, cloudinaryMock);

        // Keeps to use on tests
        this.cloudinary = cloudinaryMock;
        this.uploader = uploaderMock;
    }


    @Test
    void uploadImage_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "boat.png",
                "image/png",
                "image-content".getBytes()
        );

        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of(
                        "secure_url", "https://cloud/image.png",
                        "public_id", "boat_123"
                ));

        CloudinaryUploadResult result = service.uploadImage(file);

        assertNotNull(result);
        assertEquals("boat_123", result.getPublicId());
        assertEquals("https://cloud/image.png", result.getUrl());
    }

    @Test
    void uploadImage_nullFile() {
        assertThrows(IllegalArgumentException.class,
                () -> service.uploadImage(null));
    }

    @Test
    void uploadImage_emptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.uploadImage(file));
    }

    @Test
    void uploadImage_invalidType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "x".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.uploadImage(file));
    }

    @Test
    void uploadImage_fileTooLarge() {
        byte[] bigFile = new byte[11 * 1024 * 1024];

        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", bigFile);

        assertThrows(IllegalArgumentException.class,
                () -> service.uploadImage(file));
    }

    @Test
    void uploadImage_missingCloudinaryFields() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "boat.png", "image/png", "x".getBytes());

        assertThrows(RuntimeException.class,
                () -> service.uploadImage(file));
    }

    @Test
    void uploadImages_success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "1.png", "image/png", "x".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "2.png", "image/png", "y".getBytes());

        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of(
                        "secure_url", "url",
                        "public_id", "id"
                ));

        List<CloudinaryUploadResult> results =
                service.uploadImages(List.of(file1, file2));

        assertEquals(2, results.size());
    }

    @Test
    void uploadImages_errorOnOneFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "1.png", "image/png", "x".getBytes());

        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new IOException("boom"));

        assertThrows(IOException.class,
                () -> service.uploadImages(List.of(file)));
    }

    @Test
    void deleteImage_success() throws Exception {
        when(uploader.destroy(eq("publicId"), anyMap()))
                .thenReturn(Map.of("result", "ok"));

        service.deleteImage("publicId");

        verify(uploader).destroy(eq("publicId"), anyMap());
    }

    @Test
    void deleteImages_success() throws Exception {
        when(uploader.destroy(anyString(), anyMap()))
                .thenReturn(Map.of("result", "ok"));

        service.deleteImages(List.of("1", "2"));

        verify(uploader, times(2)).destroy(anyString(), anyMap());
    }

    @Test
    void deleteImages_error() throws Exception {
        when(uploader.destroy(eq("1"), anyMap()))
                .thenThrow(new IOException("fail"));

        assertThrows(IOException.class,
                () -> service.deleteImages(List.of("1", "2")));
    }
}
