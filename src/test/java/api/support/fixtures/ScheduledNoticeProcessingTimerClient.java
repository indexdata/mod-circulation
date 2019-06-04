package api.support.fixtures;

import static api.support.RestAssuredClient.post;

import java.net.URL;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import api.support.APITestContext;
import io.vertx.core.json.JsonObject;

public class ScheduledNoticeProcessingTimerClient {

  public void runNoticesProcessing(DateTime mockSystemTime) {
    DateTimeUtils.setCurrentMillisFixed(mockSystemTime.getMillis());
    URL url = APITestContext.circulationModuleUrl("/circulation/scheduled-notices-processing");
    post(new JsonObject(), url, 204, "scheduled-notices-processing-request");
    DateTimeUtils.setCurrentMillisSystem();
  }
}
