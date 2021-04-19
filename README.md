## Run Spring Boot application
```
mvn spring-boot:run
```

## Run following SQL insert statements
```
INSERT INTO `users` (`id`, `email`, `password`, `username`) VALUES (1, 'admin@admin.com', '$2a$10$1mQKGDJOEfCBPvPUCnoiMOQXuhTHTII0keZJbc4N5VgWKe.c1J0Ja', 'admin');//adminadmin 
INSERT INTO `roles` (`id`, `name`) VALUES (1, 'ROLE_ADMIN'); 
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1, 1); 
 
```
