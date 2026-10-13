package ca.bracketwise.domain.allocation;

import ca.bracketwise.domain.Account;
import ca.bracketwise.domain.PersonId;

import java.util.List;

public enum Destination {
    A_TFSA(PersonId.A, Account.TFSA, 0),
    A_RRSP(PersonId.A, Account.RRSP, 1),
    A_FHSA(PersonId.A, Account.FHSA, 2),
    B_TFSA(PersonId.B, Account.TFSA, 3),
    B_RRSP(PersonId.B, Account.RRSP, 4),
    B_FHSA(PersonId.B, Account.FHSA, 5);

    private final PersonId person;
    private final Account account;
    private final int order;

    Destination(PersonId person, Account account, int order) {
        this.person = person;
        this.account = account;
        this.order = order;
    }

    public PersonId person() {
        return person;
    }

    public Account account() {
        return account;
    }

    public int order() {
        return order;
    }

    public String field() {
        return "userAllocation." + person.name() + "." + account.name();
    }

    public static Destination of(PersonId person, Account account) {
        for (Destination d : values()) {
            if (d.person == person && d.account == account) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown destination");
    }

    public static List<Destination> canonical() {
        return List.of(values());
    }
}
