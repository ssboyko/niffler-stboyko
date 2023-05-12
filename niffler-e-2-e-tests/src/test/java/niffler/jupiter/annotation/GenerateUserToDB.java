package niffler.jupiter.annotation;

import niffler.jupiter.extension.GenerateUserDBExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ExtendWith(GenerateUserDBExtension.class)
public @interface GenerateUserToDB {
//    String username();
//    String password();
//    boolean enabled();
//    boolean accountNonExpired();
//    boolean accountNonLocked();
//    boolean credentialsNonExpired();
}
