package niffler.test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static niffler.jupiter.annotation.User.UserType.INVITATION_SENT;
import static niffler.jupiter.annotation.User.UserType.WITH_FRIENDS;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureId;
import niffler.jupiter.annotation.User;
import niffler.jupiter.extension.UsersQueueExtension;
import niffler.model.UserJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(UsersQueueExtension.class)
public class FriendsWebTest extends BaseWebTest {

  @AllureId("102")
  @Test
  void friendsShouldBeVisible0(@User(userType = WITH_FRIENDS) UserJson user) {
    Allure.step("open page", () -> Selenide.open("http://127.0.0.1:3000/main"));
    $("a[href*='redirect']").click();
    $("input[name='username']").setValue(user.getUsername());
    $("input[name='password']").setValue(user.getPassword());
    $("button[type='submit']").click();

    $("a[href*='friends']").click();
    $$(".table tbody tr").shouldHave(sizeGreaterThan(0));
  }

  @AllureId("103")
  @Test
  void friendsShouldBeVisible1(@User(userType = INVITATION_SENT) UserJson user) {
    Allure.step("open page", () -> Selenide.open("http://127.0.0.1:3000/main"));
    $("a[href*='redirect']").click();
    $("input[name='username']").setValue(user.getUsername());
    $("input[name='password']").setValue(user.getPassword());
    $("button[type='submit']").click();

    $("a[href*='people']").click();
    $$(".table tbody tr").find(Condition.text("Pending invitation"))
        .should(Condition.visible);
  }

  @AllureId("103")
  @Test
  void friendsShouldBeVisible1(@User(userType = User.UserType.WITH_FRIENDS) UserJson userJson,
                               @User(userType = User.UserType.INVITATION_RECEIVED) UserJson userJson2,
                               @User(userType = User.UserType.INVITATION_SENT) UserJson userJson3) {
    Allure.step("open page", () -> Selenide.open("http://127.0.0.1:3000/main"));
    System.out.println("userJson is ->  " + userJson.getUsername());
    System.out.println("userJson2 is ->  " + userJson2.getUsername());
    System.out.println("userJson3 is ->  " + userJson3.getUsername());
    $("a[href*='redirect']").click();
    $("input[name='username']").setValue(userJson.getUsername());
    $("input[name='password']").setValue(userJson.getPassword());
    $("button[type='submit']").click();

    $("a[href*='people']").click();
    $$(".table tbody tr").find(Condition.text("Pending invitation"))
            .should(Condition.visible);
  }

  @AllureId("106")
  @Test
  void manyUsersShouldBeAvaliable(@User(userType = User.UserType.WITH_FRIENDS) UserJson userJson,
                                  @User(userType = User.UserType.WITH_FRIENDS) UserJson userJson1,
                                  @User(userType = User.UserType.INVITATION_RECEIVED) UserJson userJson2,
                                  @User(userType = User.UserType.INVITATION_RECEIVED) UserJson userJson3,
                                  @User(userType = User.UserType.INVITATION_SENT) UserJson userJson4,
                                  @User(userType = User.UserType.INVITATION_SENT) UserJson userJson5) {

    System.out.println("userJson is ->  " + userJson.getUsername());
    System.out.println("userJson is ->  " + userJson1.getUsername());
    System.out.println("userJson2 is ->  " + userJson2.getUsername());
    System.out.println("userJson3 is ->  " + userJson3.getUsername());
    System.out.println("userJson3 is ->  " + userJson4.getUsername());
    System.out.println("userJson3 is ->  " + userJson5.getUsername());
      }
}
