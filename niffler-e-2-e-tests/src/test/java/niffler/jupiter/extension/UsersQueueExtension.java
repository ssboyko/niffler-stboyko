package niffler.jupiter.extension;

import io.qameta.allure.AllureId;
import niffler.jupiter.annotation.User;
import niffler.jupiter.annotation.User.UserType;
import niffler.model.UserJson;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class UsersQueueExtension implements
        BeforeEachCallback,
        AfterTestExecutionCallback,
        ParameterResolver {

    public static Namespace USER_EXTENSION_NAMESPACE = Namespace.create(UsersQueueExtension.class);

    private static Queue<UserJson> USERS_WITH_FRIENDS_QUEUE = new ConcurrentLinkedQueue<>();
    private static Queue<UserJson> USERS_INVITATION_SENT_QUEUE = new ConcurrentLinkedQueue<>();
    private static Queue<UserJson> USERS_INVITATION_RECEIVED_QUEUE = new ConcurrentLinkedQueue<>();

    static {
        USERS_WITH_FRIENDS_QUEUE.addAll(
                List.of(userJson("dima", "12345"), userJson("barsik", "12345"))
        );
        USERS_INVITATION_SENT_QUEUE.addAll(
                List.of(userJson("emma", "12345"), userJson("emily", "12345"))
        );
        USERS_INVITATION_RECEIVED_QUEUE.addAll(
                List.of(userJson("anna", "12345"), userJson("bill", "12345"))
        );
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final String testId = getTestId(context);
        List<User.UserType> desiredUserTypes = Arrays.stream(context.getRequiredTestMethod().getParameters())
                .filter(p -> p.isAnnotationPresent(User.class))
                .filter(p -> p.getType().isAssignableFrom(UserJson.class))
                .map(p -> p.getAnnotation(User.class).userType()).collect(Collectors.toList());

        Map<UserType, List<UserJson>> mapOfUsers = new HashMap<>();
        desiredUserTypes.forEach(userType -> mapOfUsers.put(userType, new ArrayList<>()));

        for (UserType userType : desiredUserTypes) {
            UserJson user = null;
            while (user == null) {
                switch (userType) {
                    case WITH_FRIENDS -> user = USERS_WITH_FRIENDS_QUEUE.poll();
                    case INVITATION_SENT -> user = USERS_INVITATION_SENT_QUEUE.poll();
                    case INVITATION_RECEIVED -> user = USERS_INVITATION_RECEIVED_QUEUE.poll();
                }
            }
            mapOfUsers.get(userType).add(user);
        }
        context.getStore(USER_EXTENSION_NAMESPACE).put(testId, mapOfUsers);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        final String testId = getTestId(context);
        Map<UserType, List<UserJson>> releaseUsers  = (Map<UserType, List<UserJson>>) context.getStore(USER_EXTENSION_NAMESPACE)
                .get(testId, Map.class);

        for(UserType userType: releaseUsers.keySet()){
            switch (userType) {
                case WITH_FRIENDS -> USERS_WITH_FRIENDS_QUEUE.addAll(releaseUsers.get(userType));
                case INVITATION_SENT -> USERS_INVITATION_SENT_QUEUE.addAll(releaseUsers.get(userType));
                case INVITATION_RECEIVED -> USERS_INVITATION_RECEIVED_QUEUE.addAll(releaseUsers.get(userType));
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().isAnnotationPresent(User.class) &&
                parameterContext.getParameter().getType().isAssignableFrom(UserJson.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public UserJson resolveParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        final String testId = getTestId(extensionContext);
        UserType userType = parameterContext.getParameter().getAnnotation(User.class).userType();

        Map<UserType, List<UserJson>> usersMap = (Map<UserType, List<UserJson>>) extensionContext.getStore(USER_EXTENSION_NAMESPACE)
                .get(testId,Map.class);

        for (UserJson userJson: usersMap.get(userType)){
            return userJson;
        }
        throw new RuntimeException("No user found " + userType);
    }

    private String getTestId(ExtensionContext context) {
        return Objects
                .requireNonNull(context.getRequiredTestMethod().getAnnotation(AllureId.class))
                .value();
    }

    private static UserJson userJson(String userName, String password) {
        UserJson user = new UserJson();
        user.setUsername(userName);
        user.setPassword(password);
        return user;
    }
}
