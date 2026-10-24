-- 알림함 전용 사용자와 코스를 사용해 기존 컨트롤러 픽스처와 격리한다.
DELETE FROM notifications WHERE user_id BETWEEN 570001 AND 570003 OR id BETWEEN 570101 AND 570199;
DELETE FROM notification_settings WHERE user_id BETWEEN 570001 AND 570003;
DELETE FROM course_likes WHERE course_id = 570001;
DELETE FROM courses WHERE id = 570001;
DELETE FROM users WHERE id BETWEEN 570001 AND 570003;

INSERT INTO users (id, nickname, profile_image_url)
VALUES
    (570001, '알림함주체', 'https://example.com/actor.jpg'),
    (570002, '알림함타인', NULL),
    (570003, '알림코스작성자', NULL);

INSERT INTO courses (id, user_id, title, is_published, visibility)
VALUES (570001, 570003, '좋아요 알림 코스', true, 'PUBLIC');

INSERT INTO notifications (id, user_id, type, target_type, target_id, window_start, last_actor_id, actor_count, payload, read_at, created_at, updated_at)
VALUES
    (570101, 570001, 'COURSE_LIKED', 'COURSE', 570001, '2026-10-24T09:00:00Z', 570002, 1,
     '{"actorNickname":"알림함타인","actorProfileImageUrl":null,"courseTitle":"좋아요 알림 코스"}', NULL, '2026-10-24T09:00:01Z', '2026-10-24T09:00:01Z'),
    (570102, 570001, 'COURSE_COMMENTED', 'COURSE', 570001, '2026-10-24T09:10:00Z', 570002, 2,
     '{"actorNickname":"알림함타인","actorProfileImageUrl":null,"courseTitle":"좋아요 알림 코스"}', '2026-10-24T09:12:00Z', '2026-10-24T09:10:01Z', '2026-10-24T09:11:00Z'),
    (570103, 570001, 'USER_FOLLOWED', 'USER', 570001, '2026-10-24T09:20:00Z', 570002, 1,
     '{"actorNickname":"알림함타인","actorProfileImageUrl":null}', NULL, '2026-10-24T09:20:01Z', '2026-10-24T09:20:01Z'),
    (570104, 570002, 'USER_FOLLOWED', 'USER', 570002, '2026-10-24T09:20:00Z', 570001, 1,
     '{"actorNickname":"알림함주체","actorProfileImageUrl":"https://example.com/actor.jpg"}', NULL, '2026-10-24T09:20:01Z', '2026-10-24T09:20:01Z');

INSERT INTO app_version_policies (feature, platform, min_build)
VALUES ('notification', 'android', 5)
ON CONFLICT (feature, platform) DO UPDATE SET min_build = EXCLUDED.min_build;
