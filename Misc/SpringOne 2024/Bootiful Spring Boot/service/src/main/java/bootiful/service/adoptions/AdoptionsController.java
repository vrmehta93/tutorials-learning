package bootiful.service.adoptions;

import java.util.Collection;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@Transactional
@Controller
public class AdoptionsController {
    private final DogRepository dogRepository;

    private final ApplicationEventPublisher publisher;

    AdoptionsController(DogRepository dogRepository, ApplicationEventPublisher publisher){
        this.dogRepository = dogRepository;
        this.publisher = publisher;
    }

    @GetMapping("/dogs")
    Collection<Dog> dogs(){
        return this.dogRepository.findAll();
    }

    @PostMapping("/dogs/{dogId}/adoptions")
    void adopt(@PathVariable Integer dogId, @RequestBody Map<String, String> adopter){
        var ownerName = adopter.get("name");
        this.dogRepository.findById(dogId)
            .ifPresent(dog -> {
                var nDog = new Dog(
                    dog.id(),
                    ownerName,
                    dog.name(),
                    dog.description());
                this.dogRepository.save(nDog);
                this.publisher.publishEvent(new DogAdoptedEvent(nDog.id()));
            });
    }
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record Dog(@Id Integer id, String owner, String name, String description) {
}