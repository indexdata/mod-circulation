package api;

import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.folio.circulation.support.http.client.Response;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.LoanPolicyBuilder;
import api.support.builders.LostItemFeePolicyBuilder;
import api.support.builders.NoticePolicyBuilder;
import api.support.builders.OverdueFinePolicyBuilder;
import io.vertx.core.json.JsonObject;

public class CirculationRulesAPITests extends APITests {

  private static final String CIRCULATION_RULE_TEMPLATE =
    "priority: t, s, c, b, a, m, g\nfallback-policy: l %s r %s n %s o %s i %s \n";

  @Test
  public void canGet() {
    getRulesText();
  }

  @Test
  public void canPutAndGet() {
    UUID lp1 = UUID.randomUUID();
    UUID lp2 = UUID.randomUUID();
    UUID rp1 = UUID.randomUUID();
    UUID rp2 = UUID.randomUUID();
    UUID np1 = UUID.randomUUID();
    UUID np2 = UUID.randomUUID();
    UUID op1 = UUID.randomUUID();
    UUID op2 = UUID.randomUUID();
    UUID ip1 = UUID.randomUUID();
    UUID ip2 = UUID.randomUUID();

    loanPoliciesFixture.create(new LoanPolicyBuilder()
      .withId(lp1)
      .withName("Example LoanPolicy " + lp1));
    noticePoliciesFixture.create(new NoticePolicyBuilder()
      .withId(np1)
      .withName("Example NoticePolicy " + np1));
    requestPoliciesFixture.allowAllRequestPolicy(rp1);
    overdueFinePoliciesFixture.create(new OverdueFinePolicyBuilder()
      .withId(op1)
      .withName("Example OverdueFinePolicy " + op1));
    lostItemFeePoliciesFixture.create(new LostItemFeePolicyBuilder()
      .withId(ip1)
      .withName("Example lostItemPolicy " + ip1));

    loanPoliciesFixture.create(new LoanPolicyBuilder()
      .withId(lp2)
      .withName("Example LoanPolicy " + lp2));
    noticePoliciesFixture.create(new NoticePolicyBuilder()
      .withId(np2)
      .withName("Example NoticePolicy " + np2));
    requestPoliciesFixture.allowAllRequestPolicy(rp2);
    overdueFinePoliciesFixture.create(new OverdueFinePolicyBuilder()
      .withId(op2)
      .withName("Example OverdueFinePolicy " + op2));
    lostItemFeePoliciesFixture.create(new LostItemFeePolicyBuilder()
      .withId(ip2)
      .withName("Example lostItemPolicy " + ip2));

    String rule = String.format(CIRCULATION_RULE_TEMPLATE, lp1, rp1, np1, op1, ip1);
    setRules(rule);

    assertThat(getRulesText(), is(rule));

    rule = String.format(CIRCULATION_RULE_TEMPLATE, lp2, rp2, np2, op2, ip2);
    setRules(rule);

    assertThat(getRulesText(), is(rule));
  }

  @Test
  public void cannotUpdateCirculationRulesWithInvalidLoanPolicyId() {

    String rule = circulationRulesFixture.soleFallbackPolicyRule(
      UUID.randomUUID().toString(),
      requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
      noticePoliciesFixture.activeNotice().getId().toString(),
      overdueFinePoliciesFixture.facultyStandard().getId().toString(),
      lostItemFeePoliciesFixture.facultyStandard().getId().toString());

    Response response = circulationRulesFixture
      .attemptUpdateCirculationRules(rule);

    assertThat(response.getStatusCode(), is(422));
    assertThat(response.getJson().getString("message"),
      is("The policy l does not exist"));
  }

  @Test
  public void cannotUpdateCirculationRulesWithInvalidNoticePolicyId() {

    String rule = circulationRulesFixture.soleFallbackPolicyRule(
      loanPoliciesFixture.canCirculateFixed().getId().toString(),
      requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
      UUID.randomUUID().toString(),
      overdueFinePoliciesFixture.facultyStandard().getId().toString(),
      lostItemFeePoliciesFixture.facultyStandard().getId().toString());

    Response response = circulationRulesFixture
      .attemptUpdateCirculationRules(rule);

    assertThat(response.getStatusCode(), is(422));
    assertThat(response.getJson().getString("message"),
      is("The policy n does not exist"));
  }

  @Test
  public void cannotUpdateCirculationRulesWithInvalidRequestPolicyId() {

    String rule = circulationRulesFixture.soleFallbackPolicyRule(
      loanPoliciesFixture.canCirculateFixed().getId().toString(),
      UUID.randomUUID().toString(),
      noticePoliciesFixture.activeNotice().getId().toString(),
      overdueFinePoliciesFixture.facultyStandard().getId().toString(),
      lostItemFeePoliciesFixture.facultyStandard().getId().toString());

    Response response = circulationRulesFixture
      .attemptUpdateCirculationRules(rule);

    assertThat(response.getStatusCode(), is(422));
    assertThat(response.getJson().getString("message"),
      is("The policy r does not exist"));
  }

  @Test
  public void cannotUpdateCirculationRulesWithOverdueFinePolicyId() {

    String rule = circulationRulesFixture.soleFallbackPolicyRule(
      loanPoliciesFixture.canCirculateFixed().getId().toString(),
      requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
      noticePoliciesFixture.activeNotice().getId().toString(),
      UUID.randomUUID().toString(),
      lostItemFeePoliciesFixture.facultyStandard().getId().toString());

    Response response = circulationRulesFixture
      .attemptUpdateCirculationRules(rule);

    assertThat(response.getStatusCode(), is(422));
    assertThat(response.getJson().getString("message"),
      is("The policy o does not exist"));
  }

  @Test
  public void cannotUpdateCirculationRulesWithLostItemPolicyId() {

    String rule = circulationRulesFixture.soleFallbackPolicyRule(
      loanPoliciesFixture.canCirculateFixed().getId().toString(),
      requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
      noticePoliciesFixture.activeNotice().getId().toString(),
      overdueFinePoliciesFixture.facultyStandard().getId().toString(),
      UUID.randomUUID().toString());

    Response response = circulationRulesFixture.attemptUpdateCirculationRules(rule);

    assertThat(response.getStatusCode(), is(422));
    assertThat(response.getJson().getString("message"),
      is("The policy i does not exist"));
  }

  @Test
  public void canUpdateCirculationRulesWithTwentyExistingLoanPolicies() {

    Set<UUID> loanPolicyIds = getSetOfPolicyIds(20);
    addPoliciesToLoanPolicyFixture(loanPolicyIds);

    loanPolicyIds.forEach(loanPolicyId -> {
      String rule = circulationRulesFixture.soleFallbackPolicyRule(
        loanPolicyId.toString(), requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
        noticePoliciesFixture.activeNotice().getId().toString(),
        overdueFinePoliciesFixture.facultyStandard().getId().toString(),
        lostItemFeePoliciesFixture.facultyStandard().getId().toString());
      circulationRulesFixture.updateCirculationRules(rule);
    });
  }

  @Test
  public void canUpdateCirculationRulesWithTwentyExistingNoticePolicies() {

    Set<UUID> noticePolicyIds = getSetOfPolicyIds(20);
    addPoliciesToNoticePolicyFixture(noticePolicyIds);

    noticePolicyIds.forEach(noticePolicyId -> {
      String rule = circulationRulesFixture.soleFallbackPolicyRule(
        loanPoliciesFixture.canCirculateFixed().getId().toString(),
        requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
        noticePolicyId.toString(),
        overdueFinePoliciesFixture.facultyStandard().getId().toString(),
        lostItemFeePoliciesFixture.facultyStandard().getId().toString());
      circulationRulesFixture.updateCirculationRules(rule);
    });
  }

  @Test
  public void canUpdateCirculationRulesWithTwentyExistingRequestPolicies() {

    Set<UUID> requestPolicyIds = getSetOfPolicyIds(20);
    requestPolicyIds.forEach(requestPoliciesFixture::allowAllRequestPolicy);

    requestPolicyIds.forEach(requestPolicyId -> {
      String rule = circulationRulesFixture.soleFallbackPolicyRule(
        loanPoliciesFixture.canCirculateFixed().getId().toString(),
        requestPoliciesFixture.allowAllRequestPolicy(requestPolicyId).getId().toString(),
        noticePoliciesFixture.activeNotice().getId().toString(),
        overdueFinePoliciesFixture.facultyStandard().getId().toString(),
        lostItemFeePoliciesFixture.facultyStandard().getId().toString());
      circulationRulesFixture.updateCirculationRules(rule);
    });
  }

  @Test
  public void canUpdateCirculationRulesWithTwentyExistingOverdueFinePolicies() {

    Set<UUID> overdueFinePolicyIds = getSetOfPolicyIds(20);
    addPoliciesToOverdueFinePolicyFixture(overdueFinePolicyIds);

    overdueFinePolicyIds.forEach(overdueFinePolicyId -> {
      String rule = circulationRulesFixture.soleFallbackPolicyRule(
        loanPoliciesFixture.canCirculateFixed().getId().toString(),
        requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
        noticePoliciesFixture.activeNotice().getId().toString(),
        overdueFinePolicyId.toString(),
        lostItemFeePoliciesFixture.facultyStandard().getId().toString());
      circulationRulesFixture.updateCirculationRules(rule);
    });
  }

  @Test
  public void canUpdateCirculationRulesWithTwentyExistingLostItemFeePolicies() {

    Set<UUID> lostItemFeePolicyIds = getSetOfPolicyIds(20);
    addPoliciesToLostItemFeePolicyFixture(lostItemFeePolicyIds);

    lostItemFeePolicyIds.forEach(lostItemFeePolicyId -> {
      String rule = circulationRulesFixture.soleFallbackPolicyRule(
        loanPoliciesFixture.canCirculateFixed().getId().toString(),
        requestPoliciesFixture.allowAllRequestPolicy().getId().toString(),
        noticePoliciesFixture.activeNotice().getId().toString(),
        overdueFinePoliciesFixture.facultyStandard().getId().toString(),
        lostItemFeePolicyId.toString());
      circulationRulesFixture.updateCirculationRules(rule);
    });
  }

  @Test
  public void canReportInvalidJson() {
    final Response response = circulationRulesFixture.putRules("foo");

    assertThat(response.getStatusCode(), is(422));
  }

  @Test
  public void canReportValidationError() {
    JsonObject rules = new JsonObject();
    rules.put("rulesAsText", "\t");

    Response response = circulationRulesFixture.putRules(rules.encodePrettily());

    assertThat(response.getStatusCode(), is(422));

    JsonObject json = new JsonObject(response.getBody());

    assertThat(json.getString("message"), containsStringIgnoringCase("tab"));
    assertThat(json.getInteger("line"), is(1));
    assertThat(json.getInteger("column"), is(2));
  }

  /** @return rulesAsText field */
  private String getRulesText() {
    Response response = circulationRulesFixture.getRules();

    assertThat("GET statusCode", response.getStatusCode(), is(200));

    String text = response.getJson().getString("rulesAsText");
    assertThat("rulesAsText field", text, is(notNullValue()));

    return text;
  }

  private void setRules(String rules) {
    circulationRulesFixture.updateCirculationRules(rules);
  }

  private Set<UUID> getSetOfPolicyIds(int numberOfPolicies) {
    Set<UUID> ofpIds = new HashSet<>();
    for (int i = 0; i < numberOfPolicies; i++) {
      ofpIds.add(UUID.randomUUID());
    }
    return ofpIds;
  }

  private void addPoliciesToLoanPolicyFixture(Set<UUID> lpIds) {
    lpIds.forEach(id -> loanPoliciesFixture.create(
      new LoanPolicyBuilder()
        .withId(id)
        .withName("Example LoanPolicy " + id)));
  }

  private void addPoliciesToNoticePolicyFixture(Set<UUID> lpIds) {
    lpIds.forEach(id -> noticePoliciesFixture.create(
      new NoticePolicyBuilder()
        .withId(id)
        .withName("Example NoticePolicy " + id)));
  }

  private void addPoliciesToOverdueFinePolicyFixture(Set<UUID> ofpIds) {
    ofpIds.forEach(id -> overdueFinePoliciesFixture.create(
      new OverdueFinePolicyBuilder()
        .withId(id)
        .withName("Example OverdueFinePolicy " + id)));
  }

  private void addPoliciesToLostItemFeePolicyFixture(Set<UUID> ofpIds) {
    ofpIds.forEach(id -> lostItemFeePoliciesFixture.create(
      new LostItemFeePolicyBuilder()
        .withId(id)
        .withName("Example LostItemFeePolicy " + id)));
  }
}
