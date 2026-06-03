DROP TABLE IF EXISTS cards CASCADE;
DROP TABLE IF EXISTS decks CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                refresh_token VARCHAR(255) NOT NULL,
                                expires_at TIMESTAMP NOT NULL
);

CREATE TABLE decks (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cards (
                       id BIGSERIAL PRIMARY KEY,
                       deck_id BIGINT NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
                       front TEXT NOT NULL,
                       back TEXT NOT NULL
);

INSERT INTO users (username, password) VALUES
                                           ('yuri-plis@example.com', '{bcrypt}$2a$10$VV.PI0mqRGM9ORD8bdtJ4OYuF9EmbzxUkw6Q07MHTRnaDAbvNMTkK'),
                                           ('vik-nik@example.com', '$2a$10$Z.x6XKx8FvZ4qY8LqjQyMO7LqJqLqJqLqJqLqJqLqJqLqJqLqJqLq'),
                                           ('yuri-ktsk@example.com', '$2a$10$Z.x6XKx8FvZ4qY8LqjQyMO7LqJqLqJqLqJqLqJqLqJqLqJqLqJqLq');

INSERT INTO refresh_tokens (user_id, refresh_token, expires_at) VALUES
                                                                    (1, 'test-refresh-token-for-yuri', CURRENT_TIMESTAMP + INTERVAL '7 days'),
                                                                    (2, 'test-refresh-token-for-alice', CURRENT_TIMESTAMP + INTERVAL '7 days');


INSERT INTO decks (user_id, title, description) VALUES
                                                    (1, 'Java Basics', 'Основные вопросы по Java для начинающих'),
                                                    (1, 'Spring Boot', 'Вопросы по Spring Framework и Spring Boot'),
                                                    (2, 'Docker', 'Основные команды и концепции Docker'),
                                                    (3, 'PostgreSQL', 'Вопросы по PostgreSQL и SQL');


INSERT INTO cards (deck_id, front, back) VALUES
                                             (1, 'Что такое JVM?', 'Java Virtual Machine — исполняет байт-код Java'),
                                             (1, 'Что такое JDK?', 'Java Development Kit — набор инструментов для разработки'),
                                             (1, 'Что такое JRE?', 'Java Runtime Environment — среда выполнения Java-программ'),
                                             (1, 'Что такое Garbage Collector?', 'Сборщик мусора — автоматически освобождает память');


INSERT INTO cards (deck_id, front, back) VALUES
                                             (2, 'Что такое @SpringBootApplication?', 'Композитная аннотация: @id117141452 (@Configuration), @EnableAutoConfiguration, @ComponentScan'),
                                             (2, 'Что такое Dependency Injection?', 'Внедрение зависимостей — паттерн, при котором объекты получают свои зависимости извне'),
                                             (2, 'Что такое IoC?', 'Inversion of Control — принцип, при котором контроль над объектами передаётся контейнеру');


INSERT INTO cards (deck_id, front, back) VALUES
                                             (3, 'Что такое Docker?', 'Платформа для контейнеризации приложений'),
                                             (3, 'Что такое контейнер?', 'Изолированная среда для запуска приложения'),
                                             (3, 'Docker vs VM', 'Контейнеры делят ядро ОС, виртуальные машины имеют свою ОС');


INSERT INTO cards (deck_id, front, back) VALUES
                                             (4, 'Что такое индекс в PostgreSQL?', 'Структура данных для ускорения поиска'),
                                             (4, 'Что такое JOIN?', 'Объединение строк из двух или более таблиц по связанному условию');