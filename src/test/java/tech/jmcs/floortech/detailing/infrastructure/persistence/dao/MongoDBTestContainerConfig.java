package tech.jmcs.floortech.detailing.infrastructure.persistence.dao;//package tech.jmcs.floortech.logservice.infrastructure.persistence.dao;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MongoDBContainer;
//import org.testcontainers.junit.jupiter.Container;
//
////@Configuration
////@EnableReactiveMongoRepositories
//public class MongoDBTestContainerConfig {
//    public static String MONGO_IMAGE = "mongo:latest";
////    @Container
//    public static MongoDBContainer mongoDBContainer = new MongoDBContainer(MONGO_IMAGE)
//            .withExposedPorts(27017);
//
//    static {
//        mongoDBContainer.start();
////        var mappedPort = mongoDBContainer.getMappedPort(27017);
//////        System.setProperty("mongodb.container.port", String.valueOf(mappedPort));
////        System.setProperty("spring.data.mongodb.port", String.valueOf(mappedPort));
////        System.out.println("Connection string: " + mongoDBContainer.getConnectionString());
//    }
//
//    @DynamicPropertySource
//    static void registerProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.mongodb.database", () -> "logs-test");
//        registry.add("spring.data.mongodb.auto-index-creation", () -> "true");
//        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
//        registry.add("spring.data.mongodb.port", () -> mongoDBContainer.getMappedPort(27017));
//    }
//}