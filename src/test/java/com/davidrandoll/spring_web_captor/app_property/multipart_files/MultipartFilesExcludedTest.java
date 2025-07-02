package com.davidrandoll.spring_web_captor.app_property.multipart_files;

import com.davidrandoll.spring_web_captor.WebCaptorApplication;
import com.davidrandoll.spring_web_captor.setup.EventCaptureListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = WebCaptorApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "web-captor.event-details.include-multipart-files=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultipartFilesExcludedTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventCaptureListener eventCaptureListener;

    @BeforeEach
    void setup() {
        eventCaptureListener.clearEvents();
    }

    @Test
    void testMultipartFileNotCapturedWhenDisabled() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "skip.txt", "text/plain", "Skip this".getBytes());

        mockMvc.perform(multipart("/test/property/upload").file(file))
                .andExpect(status().isOk());

        var request = eventCaptureListener.getRequestEvents().getFirst();
        MultiValueMap<String, MultipartFile> files = request.getRequestFiles();

        assertFalse(files.containsKey("file"), "Expected multipart file to not be captured when disabled");
    }
}
