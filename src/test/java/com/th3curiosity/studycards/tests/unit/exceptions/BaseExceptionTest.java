package com.th3curiosity.studycards.tests.unit.exceptions;

import com.th3curiosity.studycards.StudyCardsApplication;
import com.th3curiosity.studycards.config.TestContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = StudyCardsApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@DisplayName("Базовый класс для обработчиков исключений")
public class BaseExceptionTest {

    @Autowired
    protected MockMvc mockMvc;
}
