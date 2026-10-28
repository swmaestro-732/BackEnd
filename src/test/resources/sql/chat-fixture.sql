-- 채팅 통합 테스트 픽스처. 채팅방은 V19 시드(시군구마다 1개)를 그대로 쓴다.
-- 사용자: 1 = 테스트 주체(JWT subject), 2 = 다른 사용자.
TRUNCATE TABLE users, chat_members RESTART IDENTITY CASCADE;

INSERT INTO users (nickname) VALUES ('채팅주체'), ('다른사람');
