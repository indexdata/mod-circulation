package org.folio.circulation.domain;

import io.vertx.core.json.JsonObject;
import org.folio.circulation.support.*;
import org.folio.circulation.support.http.client.Response;

import java.util.concurrent.CompletableFuture;

import static org.folio.circulation.domain.Request.from;
import static org.folio.circulation.support.HttpResult.failed;
import static org.folio.circulation.support.HttpResult.succeeded;

public class RequestRepository {
  private final CollectionResourceClient requestsStorageClient;
  private final ItemRepository itemRepository;
  private final UserRepository userRepository;

  private RequestRepository(
    CollectionResourceClient requestsStorageClient,
    ItemRepository itemRepository,
    UserRepository userRepository) {

    this.requestsStorageClient = requestsStorageClient;
    this.itemRepository = itemRepository;
    this.userRepository = userRepository;
  }

  public static RequestRepository using(Clients clients) {
    return new RequestRepository(clients.requestsStorage(),
      new ItemRepository(clients, true, false),
      new UserRepository(clients));
  }

  public CompletableFuture<HttpResult<MultipleRecords<Request>>> findBy(String query) {
    return requestsStorageClient.getMany(query)
      .thenApply(this::mapResponseToRequests)
      .thenComposeAsync(requests ->
        itemRepository.fetchItemsFor(requests, Request::withItem));
  }

  //TODO: try to consolidate this further with above
  CompletableFuture<HttpResult<MultipleRecords<Request>>> findBy(
    String query,
    Integer pageLimit) {

    return requestsStorageClient.getMany(query, pageLimit, 0)
      .thenApply(this::mapResponseToRequests)
      .thenComposeAsync(requests ->
        itemRepository.fetchItemsFor(requests, Request::withItem));
  }

  private HttpResult<MultipleRecords<Request>> mapResponseToRequests(Response response) {
    return MultipleRecords.from(response, Request::from, "requests");
  }

  public CompletableFuture<HttpResult<Boolean>> exists(
    RequestAndRelatedRecords requestAndRelatedRecords) {

    return exists(requestAndRelatedRecords.getRequest());
  }

  private CompletableFuture<HttpResult<Boolean>> exists(Request request) {
    return exists(request.getId());
  }

  private CompletableFuture<HttpResult<Boolean>> exists(String id) {
    return new SingleRecordFetcher<>(requestsStorageClient, "request",
      new SingleRecordMapper<>(request -> true, response -> {
        if (response.getStatusCode() == 404) {
          return HttpResult.succeeded(false);
        } else {
          return HttpResult.failed(new ForwardOnFailure(response));
        }
      }))
      .fetch(id);
  }

  public CompletableFuture<HttpResult<Request>> getById(String id) {
    return fetchRequest(id)
      .thenComposeAsync(result -> result.combineAfter(itemRepository::fetchFor,
        Request::withItem))
      .thenComposeAsync(this::fetchRequester)
      .thenComposeAsync(this::fetchProxy);
  }

  private CompletableFuture<HttpResult<Request>> fetchRequest(String id) {
    return new SingleRecordFetcher<>(requestsStorageClient, "request", Request::from)
      .fetch(id);
  }

  //TODO: May need to fetch updated representation of request
  public CompletableFuture<HttpResult<Request>> update(Request request) {
    final JsonObject representation = new RequestRepresentation()
      .storedRequest(request);

    return requestsStorageClient.put(request.getId(), representation)
      .thenApply(response -> {
        if(response.getStatusCode() == 204) {
          return succeeded(request);
        }
        else {
          return failed(new ForwardOnFailure(response));
        }
    });
  }

  public CompletableFuture<HttpResult<RequestAndRelatedRecords>> update(
    RequestAndRelatedRecords requestAndRelatedRecords) {

    return update(requestAndRelatedRecords.getRequest())
      .thenApply(r -> r.map(requestAndRelatedRecords::withRequest));
  }

  public CompletableFuture<HttpResult<RequestAndRelatedRecords>> create(
    RequestAndRelatedRecords requestAndRelatedRecords) {

    final Request request = requestAndRelatedRecords.getRequest();

    JsonObject representation = new RequestRepresentation()
      .storedRequest(request);

    return requestsStorageClient.post(representation)
      .thenApply(response -> {
        if (response.getStatusCode() == 201) {
          return succeeded(requestAndRelatedRecords.withRequest(from(response.getJson())));
        } else {
          return failed(new ForwardOnFailure(response));
        }
    });
  }

  public CompletableFuture<HttpResult<RequestAndRelatedRecords>> delete(
    RequestAndRelatedRecords requestAndRelatedRecords) {

    return delete(requestAndRelatedRecords.getRequest())
      .thenApply(r -> r.map(requestAndRelatedRecords::withRequest));
  }

  public CompletableFuture<HttpResult<Request>> delete(Request request) {
    return requestsStorageClient.delete(request.getId())
      .thenApply(response -> {
        if(response.getStatusCode() == 204) {
          return succeeded(request);
        }
        else {
          return failed(new ForwardOnFailure(response));
        }
    });
  }

  //TODO: Check if need to request requester
  private CompletableFuture<HttpResult<Request>> fetchRequester(HttpResult<Request> result) {
    return result.combineAfter(request ->
      getUser(request.getUserId()), Request::withRequester);
  }

  //TODO: Check if need to request proxy
  private CompletableFuture<HttpResult<Request>> fetchProxy(HttpResult<Request> result) {
    return result.combineAfter(request ->
      getUser(request.getProxyUserId()), Request::withProxy);
  }

  private CompletableFuture<HttpResult<User>> getUser(String proxyUserId) {
    return userRepository.getUser(proxyUserId);
  }
}