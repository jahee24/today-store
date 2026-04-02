-- V4__remove_fk_from_content_images.sql

ALTER TABLE content_images DROP CONSTRAINT IF EXISTS fk_content_images_input_image;
