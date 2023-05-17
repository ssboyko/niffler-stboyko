package niffler.jupiter.extension;

import com.github.javafaker.Faker;
import io.qameta.allure.AllureId;
import niffler.db.dao.NifflerUsersDAO;
import niffler.db.dao.NifflerUsersDAOJdbc;
import niffler.db.entity.Authority;
import niffler.db.entity.AuthorityEntity;
import niffler.db.entity.UserEntity;
import niffler.jupiter.annotation.GenerateUserToDB;
import niffler.model.UserJson;
import org.junit.jupiter.api.extension.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GenerateUserDBExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    public static ExtensionContext.Namespace GENERATE_USER_NAMESPACE = ExtensionContext.Namespace
            .create(GenerateUserDBExtension.class);

    NifflerUsersDAO usersDAO = new NifflerUsersDAOJdbc();
     static Faker faker = new Faker();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final String testId = getTestId(context);

        GenerateUserToDB annotation = context.getRequiredTestMethod()
                .getAnnotation(GenerateUserToDB.class);
        if (annotation != null) {
            UserEntity user = new UserEntity();
            user.setUsername(faker.name().username());
            user.setPassword("12345");
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);
            user.setAuthorities(Arrays.stream(Authority.values()).map(
                    a -> {
                        AuthorityEntity authorityEntity = new AuthorityEntity();
                        authorityEntity.setAuthority(a);
                        authorityEntity.setUser(user);
                        return authorityEntity;
                    }
            ).toList());
            usersDAO.createUser(user);

            context.getStore(GENERATE_USER_NAMESPACE).put(testId, user);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        final String testId = getTestId(context);
        UserEntity user = context.getStore(GENERATE_USER_NAMESPACE).get(testId, UserEntity.class);
        usersDAO.removeUser(user);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(UserEntity.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        final String testId = getTestId(extensionContext);
        return extensionContext.getStore(GENERATE_USER_NAMESPACE).get(testId);
    }

    private String getTestId(ExtensionContext context) {
        return Objects
                .requireNonNull(context.getRequiredTestMethod().getAnnotation(AllureId.class))
                .value();
    }
}
