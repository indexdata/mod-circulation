package org.folio.circulation.domain.policy;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.support.HttpResult;
import org.joda.time.DateTime;

class UnknownDueDateStrategy extends DueDateStrategy {
  private static final String CHECK_OUT_UNRECOGNISED_PROFILE_MESSAGE =
    "Item can't be checked out as profile \"%s\" in the loan policy is not recognised.";

  private static final String RENEWAL_UNRECOGNISED_PROFILE_MESSAGE =
    "Item can't be renewed as profile \"%s\" in the loan policy is not recognised.";

  private final String profileId;
  private final boolean isRenewal;

  UnknownDueDateStrategy(
    String loanPolicyId,
    String loanPolicyName,
    String profileId,
    boolean isRenewal) {

    super(loanPolicyId, loanPolicyName);
    this.profileId = profileId;
    this.isRenewal = isRenewal;
  }

  @Override
  HttpResult<DateTime> calculateDueDate(Loan loan, DateTime systemDate) {
    if(isRenewal) {
      return fail(String.format(RENEWAL_UNRECOGNISED_PROFILE_MESSAGE, profileId));
    }
    else {
      return fail(String.format(CHECK_OUT_UNRECOGNISED_PROFILE_MESSAGE, profileId));
    }
  }
}
