package bootiful.service.vet;

import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bootiful.service.adoptions.DogAdoptedEvent;

@Service
@Transactional
class Vet {
    // BEFORE
    /* PROBLEM - if there's a 10 sec sleep, the terminal from where I'm running the POST command from, waits for 10 seconds before I can proceed
     * We want the terminal to not wait 10 seconds but we also don't want to lose the message
     * We will use Modulith's ApplicationModuleListener 
     */
    // @EventListener
    
    // AFTER
    // @Async
    // @EventListener
    // @TransactionListener
    @ApplicationModuleListener  // 3 annotations combined
    void on(DogAdoptedEvent dogAdoptedEvent)throws Exception{
        System.out.println("BEFORE Got an event! - " + dogAdoptedEvent);
        Thread.sleep(10_000);
        System.out.println("AFTER Got an event! - " + dogAdoptedEvent);
    }
}
