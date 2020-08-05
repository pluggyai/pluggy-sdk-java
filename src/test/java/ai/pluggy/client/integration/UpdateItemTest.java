package ai.pluggy.client.integration;

import static ai.pluggy.client.integration.helper.ItemHelper.createItem;
import static ai.pluggy.client.integration.helper.ItemHelper.createPluggyBankItem;
import static ai.pluggy.client.integration.helper.ItemHelper.getItemStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.pluggy.client.integration.util.Poller;
import ai.pluggy.client.request.ParametersMap;
import ai.pluggy.client.request.UpdateItemRequest;
import ai.pluggy.client.response.ErrorResponse;
import ai.pluggy.client.response.ItemResponse;
import java.util.Objects;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import retrofit2.Response;

public class UpdateItemTest extends BaseApiIntegrationTest {

  @SneakyThrows
  @Test
  void updateItem_beforeExecutionEnds_errorResponse() {
    // precondition: an item already exists
    Integer connectorId = 1;
    ItemResponse itemResponse = createItem(client, connectorId);

    // build update item request
    ParametersMap newParameters = ParametersMap.map("user", "qwe");
    String newWebhookUrl = "localhost:3000";
    UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
      .webhookUrl(newWebhookUrl)
      .build();

    // run update item request
    Response<ItemResponse> updateItemResponse = client.service()
      .updateItem(itemResponse.getId(), updateItemRequest)
      .execute();

    // expect update item response to be an error
    ErrorResponse errorResponse = client.parseError(updateItemResponse);

    assertFalse(updateItemResponse.isSuccessful());
    assertNotNull(errorResponse);
    assertEquals(errorResponse.getCode(), 400);

    // expect updatedItem response to be null
    ItemResponse updatedItem = updateItemResponse.body();
    assertNull(updatedItem);
  }

  @SneakyThrows
  @Test
  void updateItem_afterWaitingForCreation_ok() {
    // precondition: an item already exists
    ItemResponse createdItemResponse = createPluggyBankItem(client);

    // build update item request
    String newWebhookUrl = "localhost:3000";
    UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
      .webhookUrl(newWebhookUrl)
      .build();

    // wait for creation finish (status: "UPDATED") before updating, to prevent update request error 400.
    Poller.pollRequestUntil(
      () -> getItemStatus(client, createdItemResponse.getId()),
      (ItemResponse itemStatusResponse) -> Objects
        .equals(itemStatusResponse.getStatus(), "UPDATED"),
      500, 45000
    );

    // run update item request
    Response<ItemResponse> updateItemResponse = client.service()
      .updateItem(createdItemResponse.getId(), updateItemRequest)
      .execute();

    ItemResponse updatedItem = updateItemResponse.body();

    // expect response to be successful
    assertTrue(updateItemResponse.isSuccessful());

    // expect item to be updated with the new data
    assertNotNull(updatedItem);
    assertEquals(updatedItem.getWebhookUrl(), newWebhookUrl);
    assertNotEquals(createdItemResponse.getWebhookUrl(), newWebhookUrl);
  }

}
