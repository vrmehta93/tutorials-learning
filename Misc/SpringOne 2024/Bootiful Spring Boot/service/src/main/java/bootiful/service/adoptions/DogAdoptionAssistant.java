package bootiful.service.adoptions;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DogAdoptionAssistant {
    @Bean
    ChatClient chatClient(VectorStore vectorStore,
        DogRepository dogRepository, ChatClient.Builder builder){
        dogRepository.findAll().forEach(dog -> {
            var dogument = new Document("id: %s, name: %s, description: %s".formatted(dog.id(), dog.name(), dog.description()));
            vectorStore.add(List.of(dogument));
        });

        return builder
            .defaultAdvisors(new QuestionAnswerAdvisor(
                vectorStore, SearchRequest.defaults()))
            .build();
    }

    @Bean
    ApplicationRunner dogAdoptionAssistantRunner(ChatClient chatClient){
        return args -> {
            var reply = chatClient.prompt()
                    .user("Do you have any neurotic dogs?")
                    .call()
                    .content();
            System.out.println("reply = " + reply);
        };
    }
}
