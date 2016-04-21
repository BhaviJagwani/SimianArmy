package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;


/**
 * Created by bjagwani on 4/13/16.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args){
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
    }
}
